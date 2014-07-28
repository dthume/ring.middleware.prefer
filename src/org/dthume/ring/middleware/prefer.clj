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
prefer    = <WS?> token value? (<WS?> <';'> <WS?> parameter)* <WS?>
parameter = token value?
<value>   = <WS?> <'='> <WS?> word
<word>    = <'\"'> #'[^\\\"]+' <'\"'> | token
<token>   = #'[a-zA-Z0-9!#\\$%&\\'\\*\\+\\-\\.\\^_`\\|~]+'
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

(defn- keep-first-prefer-header
  [m {:keys [name] :as h}]
  (cond-> m
    (not (contains? m name)) (assoc name h)))

(defn parse-prefer-header
  [^String v]
  (let [p (instaparse-prefer-header v)]
    (if (insta/failure? p)
      nil
      (->> p
           rest
           (r/map as-preference)
           (r/reduce keep-first-prefer-header {})))))

(defn wrap-prefer-header
  [req v]
  (if-let [pv (parse-prefer-header v)]
    (update-in req [:prefer] #(if %1 (into %2 %1) %2)
               pv)
    req))

(defn wrap-prefer-headers
  [req vs]
  (if (string? vs)
    (wrap-prefer-header req vs)
    (r/reduce wrap-prefer-header req vs)))

(defn wrap-prefer
  [f]
  (fn [req]
    (let [h (get-in req [:headers "prefer"])]
      (f (cond-> req
           h     (wrap-prefer-headers h))))))
