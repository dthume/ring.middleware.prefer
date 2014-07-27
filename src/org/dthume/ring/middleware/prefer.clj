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
prefer    = <WS?> token value? (<WS?> <';'> <WS?> parameter)*
parameter = token value?
<value>   = <WS?> <'='> <WS?> word
<word>    = <'\"'> #'[^\\\"]+' <'\"'> | token
<token>   = #'[a-zA-Z!#\\$%&\\'\\*\\+\\-\\.\\^_`\\|~]+'
<WS>      = #'\\s+'"))

(defn parse-prefer-header
  [^String v]
  (let [[_ n & r] (instaparse-prefer-header v)
        [v p]     (if (string? (first r))
                    (tuple (first r) (rest r))
                    (tuple nil r))
        ps        (->> p
                       (r/map (fn [x] (tuple (nth x 1) (nth x 2))))
                       (into {}))]
    (->Preference n v ps)))

(defn wrap-prefer-header
  [req v]
  (update-in req [:prefer] (fnil conj [])
             (parse-prefer-header v)))

(defn wrap-prefer-headers
  [req vs]
  (if (string? vs)
    (wrap-prefer-header req vs)
    (reduce wrap-prefer-header vs)))

(defn wrap-prefer
  [f]
  (fn [req]
    (let [h (get-in req [:headers "prefer"])]
      (cond-> req
        h     (wrap-prefer-headers h)
        true  f))))
