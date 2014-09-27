(ns ^{:author "David Thomas Hume"}
  org.dthume.ring.middleware.prefer
  (:require [clojure.core.reducers :as r]
            [clj-tuple :refer [tuple]]
            [instaparse.core :as insta]))

(defrecord Preference [name value params])

(defn preference
  "Construct a preference, with an optional value and / or map of parameters"
  ([name]
     (preference name nil {}))
  ([name valueOrParams]
     (if (string? valueOrParams)
       (preference name valueOrParams {})
       (preference name nil valueOrParams)))
  ([name value params]
     (->Preference name value params)))

(defn preference?
  "Return `true` iff `p` is a `Preference` record instance."
  [p]
  (instance? Preference p))

(def ^:private instaparse-prefer-header
  (insta/parser "
S         = prefer (<WS?> <','> prefer)*
prefer    = <WS?> token value? (<WS?> <';'> <WS?> parameter)* <WS?>
parameter = token value?
<value>   = <WS?> <'='> <WS?> word
<word>    = <'\"'> #'[^\\\"]*' <'\"'> | token
<token>   = #'[a-zA-Z0-9!#\\$%&\\'\\*\\+\\-\\.\\^_`\\|~]+'
<WS>      = #'\\s+'"))

(defn- keep-first-param
  [m [k v]]
  (if (contains? m k) m (assoc m k v)))

(defn- as-preference
  [pref]
  (let [[_ n & r] pref
        [v p]     (if (string? (first r))
                    (tuple (first r) (rest r))
                    (tuple nil r))
        ps        (->> p
                       (r/map (fn [x] (tuple (nth x 1) (nth x 2))))
                       (r/reduce keep-first-param {}))]
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

(defn- extract-prefer-header
  [req v]
  (if-let [pv (parse-prefer-header v)]
    (update-in req (tuple :prefer) #(if %1 (into %2 %1) %2) pv)
    req))

(defn- extract-prefer-headers
  [req vs]
  (if (string? vs)
    (extract-prefer-header req vs)
    (r/reduce extract-prefer-header req vs)))

(defn- as-preference-applied
  [{:keys [name value] :as pref}]
  (if (nil? value)
    name
    (str name "=" value)))

(defn- add-preference-applied-headers
  [resp prefs]
  (let [prefs (cond
               (preference? prefs) (tuple prefs)
               (map? prefs)        (vals prefs)
               :else               prefs)]
    (->> prefs
         (r/map as-preference-applied)
         (update-in resp (tuple :headers "Preference-Applied")
                    (fnil into [])))))

(defn prefer-request
  [req]
  (if-let [h (get-in req (tuple :headers "prefer"))]
    (extract-prefer-headers req h)
    req))

(defn prefer-response
  [resp]
  (if-let [prefs (get resp :prefer)]
    (add-preference-applied-headers resp prefs)
    resp))

(defn wrap-prefer
  "Wrap `handler` with middleware for dealing with RFC 7240 preference headers
in both request and response maps.

`Prefer` request headers will be parsed into `Preference` record instances and
added to the request map under the key `:prefer` as a map of preference
name -> `Preference`.

The response map may contain a `:prefer` key, whose value may be a single
`Preference` instance, a map with `Preference` values, or collection of
`Preference` instances. These preferences will be used to add
`Preference-Applied` headers to the response map."
  [handler]
  (comp prefer-response handler prefer-request))
