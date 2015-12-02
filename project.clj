(defproject proteus "0.1.5"
  :description "local. mutable. variables."
  :license {:name "MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[riddley "0.1.11"]]
  :profiles {:dev {:dependencies [[org.clojure/clojure "1.8.0-RC2"]
                                  [criterium "0.4.1"]]}}
  :java-source-paths ["src"]
  :javac-options ["-target" "1.5" "-source" "1.5"]
  :test-selectors {:default (complement :benchmark)
                   :benchmark :benchmark}
  :jvm-opts ^:replace ["-server"])
