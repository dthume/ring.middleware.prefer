(defproject org.dthume/ring.middleware.prefer "0.1.0-SNAPSHOT"
  :description "Ring middleware providing RFC7240 (Prefer Header for HTTP) support."

  :plugins [[codox "0.8.9"]
            [lein-marginalia "0.7.1"]
            [lein-midje "3.0.0"]
            [perforate "0.3.3"]]

  :codox {:defaults {:doc/format :markdown}
          :output-dir "doc/codox"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-tuple "0.1.6"]
                 [instaparse "1.3.3"]]

  :javac-options ["-target" "1.6" "-source" "1.6"]

  :perforate
  {:benchmark-paths ["src/benchmark/clj"]}

  :profiles
  {:dev
   {:source-paths ["src/dev/clj"]
    :dependencies [[midje "1.6.3"]
                   [ring-mock "0.1.5"]]}

   :site {}}

  :aliases
  {"ci-build"
   ^{:doc "Perform CI build"}
   ["do" ["clean"] ["check"]]
   
   "dev-repl"
   ^{:doc "Start a clean development NREPL session"}
   ["do" ["clean"] ["repl"]]

   "dev-test"
   ^{:doc "Run development unit tests"}
   ["do" ["clean"] ["midje"]]})
