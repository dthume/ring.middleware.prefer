(ns org.dthume.ring.middleware.prefer
  (:require [clojure.core.reducers :as r]
            [clj-tuple :refer [tuple]]
            [instaparse.core :as insta]))

(defrecord Preference [name value params])

(defn preference
  ([name]
     (preference name nil {}))
  ([name valueOrParams]
     (if (string? valueOrParams)
       (preference name valueOrParams {})
       (preference name nil valueOrParams)))
  ([name value params]
     (->Preference name value params)))

(def ^:private instaparse-prefer-header
  (insta/parser "
S         = prefer (<WS?> <','> prefer)*
prefer    = <WS?> token value? (<WS?> <';'> <WS?> parameter)*
parameter = token value?
<value>   = <WS?> <'='> <WS?> word
<word>    = <'\"'> #'[^\\\"]+' <'\"'> | token
<token>   = #'[a-zA-Z!#\\$%&\\'\\*\\+\\-\\.\\^_`\\|~]+'
<WS>      = #'\\s+'"))

(defn- as-preference
  [pref]
  (let [[_ n & r] pref
        [v p]     (if (string? (first r))
                    (tuple (first r) (rest r))
                    (tuple nil r))
        ps        (->> p
                       (r/map (fn [x] (tuple (nth x 1) (nth x 2))))
                       (into {}))]
    (->Preference n v ps)))

(defn parse-prefer-header
  [^String v]
  (->> (instaparse-prefer-header v)
       rest
       (r/map as-preference)
       (into [])))

(defn wrap-prefer-header
  [req v]
  (update-in req [:prefer] (fnil into [])
             (parse-prefer-header v)))

(defn wrap-prefer-headers
  [req vs]
  (if (string? vs)
    (wrap-prefer-header req vs)
    (r/reduce wrap-prefer-header req vs)))

(defn wrap-prefer
  [f]
  (fn [req]
    (let [h (get-in req [:headers "prefer"])]
      (cond-> req
        h     (wrap-prefer-headers h)
        true  f))))
