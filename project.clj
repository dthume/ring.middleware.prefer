(defproject org.dthume/ring.middleware.prefer "0.1.0-SNAPSHOT"
  :description "Ring middleware providing RFC7240 (Prefer Header for HTTP) support."
  :url "http://github.com/dthume/ring.middleware.prefer"

  :license {:name "Eclipse Public License 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :scm {:name "git"
        :url "github.com/dthume/ring.middleware.prefer"}

  :plugins [[codox "0.8.10"]
            [lein-marginalia "0.7.1"]
            [lein-midje "3.0.0"]]

  :codox {:defaults {:doc/format :markdown}
          :output-dir "doc/codox"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [clj-tuple "0.1.6"]
                 [instaparse "1.3.4"]]

  :javac-options ["-target" "1.6" "-source" "1.6"]

  :profiles
  {:dev
   {:dependencies [[midje "1.6.3"]
                   [ring-mock "0.1.5"]]}

   :site {}}

  :aliases
  {"ci-build"
   ^{:doc "Perform CI build"}
   ["do" ["clean"] ["check"] ["midje"]]
   
   "dev-repl"
   ^{:doc "Start a clean development NREPL session"}
   ["do" ["clean"] ["repl"]]

   "dev-test"
   ^{:doc "Run development unit tests"}
   ["do" ["clean"] ["midje"]]

   "all-doc"
   ^{:doc "Generate project documentation"}
   ["with-profile" "site"
    ["do"
     ["clean"]
     ["doc"]]]})
