(ns proteus-test
  (:require
    [clojure.test :refer :all]
    [proteus :refer :all]
    [criterium.core :as c]))

(set! *unchecked-math* true)

(defrecord Test [x])

(deftest test-let-mutable
  (is (= 1
        (let-mutable [x 0]
          (set! x (inc x))
          x)))

  (is (= false
        (let-mutable [x true]
          (set! x (not x))
          x)))

  (is (= 100
        (let-mutable [x 0]
          (dotimes [_ 100]
            (set! x (inc x)))
          x)))

  (is (= 0 (let-mutable [x 0
                         y 10]
             (set! x y)
             (let [y 0]
               (set! x y)
               x))))

  (is (= 42
        ((let-mutable [x 0]
           (set! x 42)
           (let [f (fn [] x)]
             (set! x -1)
             f)))))

  (is (= 0
        ((let-mutable [x 0]
           (set! x 42)
           (let [f ^:local (fn []
                             (set! x (inc x))
                             x)]
             (set! x -1)
             f)))))

  (is (= 42
        (let-mutable [x 1]
          (letfn [(f [y] (+ x y))]
            (set! x 40)
            (+ x (f 1))))))

  (is (thrown? Exception
        (let-mutable [x 0]
          (try
            (set! x (/ 1 0))
            (catch Exception x
              (throw x))))))

  (is (= 3 (let-mutable [x 3]
             (case x
               3 3))))

  (is (= [0 :x]
        (let-mutable [x 0] [x :x])))

  (is (= #{0 :x}
        (let-mutable [x 0] #{x :x})))

  (is (= {0 1 :x :y}
        (let-mutable [x 0 y 1] {x y :x :y})))

  (is (= (Test. nil)
        (let-mutable [x 0] #proteus_test.Test{:x nil})))
  
  (is (= 2 (let-mutable [a 1 b (let-mutable [c 2] c)] b)))

  (is (= 8 (let-mutable [a 1 
                         b (let-mutable [c 2] 
                             (set! a 3)
                             (set! c 5)
                             (+ a c))]
             b)))
  
  (is (= 88 (let-mutable [a 1 
                          b (let-mutable [c 2
                                          d (let-mutable [e 80] 
                                              e)]
                              (set! a 3)
                              (set! c 5)
                              (+ a c d))]
              b))))

(deftest ^:benchmark benchmark-sum
  (c/quick-bench
    (let-mutable [x 0]
      (dotimes [_ 10000]
        (set! x (+ x 1)))
      x)))

(deftest ^:benchmark benchmark-volatile-sum
  (c/quick-bench
    (let [x (volatile! 0)]
      (dotimes [_ 10000]
        (vswap! x inc))
      x)))
