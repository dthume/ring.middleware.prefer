(ns org.dthume.ring.middleware.test-prefer
  (:require [midje.sweet :refer :all]
            [org.dthume.ring.middleware.prefer
             :refer [parse-prefer-header preference wrap-prefer]]
            [ring.mock.request :refer [header request]]))

(defn basic-handler [req] req)

(def handler (-> basic-handler
                 wrap-prefer))

(def p preference)

(tabular
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 (fact "parse-prefer-header works on strings"
   (parse-prefer-header ?input) => ?expected)
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ?input                                ?expected
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 "respond-async, wait=100"             {"respond-async" (p "respond-async" {})
                                        "wait"          (p "wait" "100")}

 "handling=lenient"                    {"handling" (p "handling" "lenient" {})}

 "p=v"                                 {"p" (p "p" "v" {})}

 "return=minimal; foo=\"some param\""  {"return" (p "return" "minimal"
                                                    {"foo" "some param"})})

(fact "wrap-prefer adds `:prefer` key to request based on `Prefer` header"
  (-> (request :get "/foo")
      (header "Prefer" "return=minimal; foo=\"some parameter\"")
      handler)
  => (contains {:prefer {"return" (p "return" "minimal"
                                     {"foo" "some parameter"})}}))
