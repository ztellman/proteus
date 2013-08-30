(ns proteus
  (:import
    [clojure.lang
     Compiler$LocalBinding]))

;;; stolen from potemkin, which in turn adapted clojure.walk

(defn- walk
  "Like `clojure.walk/walk`, but preserves metadata."
  [inner outer form]
  (let [x (cond
            (list? form) (outer (apply list (map inner form)))
            (instance? clojure.lang.IMapEntry form) (outer (vec (map inner form)))
            (seq? form) (outer (doall (map inner form)))
            (coll? form) (outer (into (empty form) (map inner form)))
            :else (outer form))]
    (if (instance? clojure.lang.IObj x)
      (with-meta x (meta form))
      x)))

(defn postwalk
  "Like `clojure.walk/postwalk`, but preserves metadata."
  [f form]
  (walk (partial postwalk f) f form))

(defn- prewalk
  "Like `clojure.walk/prewalk`, but preserves metadata."
  [f form]
  (walk (partial prewalk f) identity (f form)))

(defn- macroexpand+
  "Expands both macros and inline functions."
  [x]
  (let [x* (macroexpand x)]
    (if-let [inline-fn (and (seq? x*)
                         (symbol? (first x*))
                         (not (-> x* meta ::transformed))
                         (-> x first resolve meta :inline))]
      (let [x** (apply inline-fn (rest x*))]
        (recur
          ;; unfortunately, static function calls can look a lot like what we just
          ;; expanded, so prevent infinite expansion
          (if (= '. (first x**))
            (concat (butlast x**) [(with-meta (last x**) {::transformed true})])
            x**)))
      x*)))

(defn- macroexpand-all
  "Fully macroexpands all forms."
  [x]
  (prewalk macroexpand+ x))

;;;

(defmulti ^:private walk-binding-forms
  (fn [f x _]
    (if (seq? x)
      (first x)
      ::default))
  :default ::default)

(defmethod walk-binding-forms ::default [f x vs]
  (f x vs))

(defn- transform-binding-form [f [x bindings & rest] vs]
  (let [ks (->> bindings (partition 2) (map first))
        bindings (interleave
                   ks
                   (->> bindings
                     (partition 2)
                     (map second)
                     (map #(walk-binding-forms f % vs))))
        vs' (apply dissoc vs ks)]
    `(~x ~(vec bindings) ~@(map #(walk-binding-forms f % vs') rest))))

(defmethod walk-binding-forms 'case* [f x vs]
  (let [prefix (butlast (take-while (complement map?) x))
        default (last (take-while (complement map?) x))
        body (first (drop-while (complement map?) x))
        suffix (rest (drop-while (complement map?) x))]
    (concat
      prefix
      [(walk-binding-forms f default vs)]
      [(->> body
         (map
           (fn [[k [idx form]]]
             [k [idx (walk-binding-forms f form vs)]]))
         (into {}))]
      suffix)))

(defmethod walk-binding-forms 'let* [f x vs]
  (transform-binding-form f x vs))

(defmethod walk-binding-forms 'loop* [f x vs]
  (transform-binding-form f x vs))

(defmethod walk-binding-forms 'catch [f [_ type v & body] vs]
  (let [vs' (dissoc vs v)]
    `(catch ~type ~v
       ~@(map #(walk-binding-forms f % vs') body))))

(defmethod walk-binding-forms 'fn* [f x vs]
  (if (-> x meta :local)

    ;; give them just enough rope to hang themselves
    `(fn* ~@(map #(walk-binding-forms f % vs) (rest x)))

    `(let [~@(interleave
               (map #(with-meta % nil) (keys vs))
               (map :read-form (vals vs)))]
       (fn* ~@(map #(walk-binding-forms f % #{}) (rest x))))))

;;;

(defn- transform-let-mutable-form [x vs]
  (let [x (macroexpand-all x)]
    (walk-binding-forms
      (fn this [x vs]
        (cond

          (and (symbol? x) (contains? vs x))
          (->> x (get vs) :read-form)

          (and (seq? x) (= 'set! (first x)))
          (do
            (assert (= 3 (count x)))
            (if-let [v (get vs (second x))]
              ((:write-form v) (walk-binding-forms this (nth x 2) vs))
              (map #(walk-binding-forms this % vs) x)))

          (seq? x)
          (map #(walk-binding-forms this % vs) x)

          (vector? x)
          (vec (map #(walk-binding-forms this % vs) x))

          (set? x)
          (set (map #(walk-binding-forms this % vs) x))

          (map? x)
          (into {} (for [[k v] x] [(walk-binding-forms this k vs) (walk-binding-forms this v vs)]))

          :else
          x))
      x
      vs)))

(defn- typeof [x env]
  (if-let [^Compiler$LocalBinding binding (get env x)]
    (when (.hasJavaClass binding)
      (.getJavaClass binding))
    (cond
      (instance? Boolean x) Boolean/TYPE
      (instance? Long x) Long/TYPE
      (instance? Double x) Double/TYPE)))

(defmacro let-mutable
  "Acts as a let-binding for variables that can be modified with `set!`.

   (let-mutable [x 0]
     (dotimes [_ 100]
       (set! x (inc x)))
     x)

   The mutable variable cannot escape the scope in which it's defined; if the variable is
   closed over, the current value will be captured.  Wherever possible, unboxed numbers are
   used, giving significantly better performance than `clojure.core/with-local-vars`."
  [bindings & body]
  (let [ks (->> bindings
             (partition 2)
             (map first))
        vs (->> bindings
             (partition 2)
             (map second))
        types (map
                #(condp = (typeof % &env)
                   Boolean/TYPE "proteus.Containers$B"
                   Long/TYPE "proteus.Containers$L"
                   Double/TYPE "proteus.Containers$D"
                   "proteus.Containers$O")
                vs)
        ks (map
             (fn [k type]
               (with-meta k {:tag type}))
             ks
             types)]
    `(let [~@(interleave
               ks
               (map (fn [v type] `(new ~(symbol type) ~v)) vs types))]
       ~(transform-let-mutable-form
          `(do ~@body)
          (zipmap
            ks
            (map
              (fn [k]
                 {:read-form `(.x ~k)
                  :write-form (fn [x] `(do (.set ~k ~x) nil))})
              ks))))))
