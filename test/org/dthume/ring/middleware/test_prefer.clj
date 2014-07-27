(ns org.dthume.ring.middleware.test-prefer
  (:require [midje.sweet :refer :all]
            [org.dthume.ring.middleware.prefer
             :refer [parse-prefer-header preference wrap-prefer]]
            [ring.mock.request :refer [header request]]))

(defn basic-handler [req] req)

(def handler (-> basic-handler
                 wrap-prefer))

(def pref preference)

(tabular
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 (fact "parse-prefer-header works on strings"
   (parse-prefer-header ?input) => ?expected)
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 ?input                                 ?expected
 ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
 
 "respond-async; me=you; them=us"       (pref "respond-async"
                                              {"me" "you"
                                               "them" "us"})
 "another-v=\"foo\""                    (pref "another-v" "foo" {})
 "p=v"                                  (pref "p" "v" {}))

(fact "wrap-prefer adds `:prefer` key to request based on `Prefer` header"
  (-> (request :get "/foo")
      (header "Prefer" "return=minimal; foo=\"some parameter\"")
      handler)
  => (contains {:prefer [(pref "return" "minimal"
                               {"foo" "some parameter"})]}))
