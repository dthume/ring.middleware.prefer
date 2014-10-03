(ns ^{:author "David Thomas Hume"
      :doc "Ring middleware providing
[RFC7240 (Prefer Header for HTTP)](http://tools.ietf.org/html/rfc7240) support.

Most users will only need to add [[wrap-prefer]] to their middleware stack,
although [[prefer-request]] and [[prefer-response]] may be useful for
[Pedestal](https://github.com/pedestal/pedestal) interceptors."}
  org.dthume.ring.middleware.prefer
  (:require [clojure.core.reducers :as r]
            [instaparse.core :as insta]))

(defrecord Preference [name value params])

(alter-meta! #'->Preference assoc :no-doc true)
(alter-meta! #'map->Preference assoc :no-doc true)

(defn preference
  "Construct a `Preference`, with mandatory `name` and an optional `value`
and / or `paramMap`. `Preference` records have three keys:

`name`
: The name of the preference.

`value`
: The primary value of the preference, or `nil` if there is none.

`params`
: A map of any secondary parameters specified by the preference."
  ([name]
     (preference name nil {}))
  ([name stringValueOrParamMap]
     (if (string? stringValueOrParamMap)
       (preference name stringValueOrParamMap {})
       (preference name nil stringValueOrParamMap)))
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
                    [(first r) (rest r)]
                    [nil r])
        ps        (->> p
                       (r/map (fn [x] [(nth x 1) (nth x 2)]))
                       (r/reduce keep-first-param {}))]
    (->Preference n v ps)))

(defn- keep-first-prefer-header
  [m {:keys [name] :as h}]
  (cond-> m
    (not (contains? m name)) (assoc name h)))

(defn parse-prefer-header
  "Parse one or more `Prefer` headers into a map of preference names to
`Preference` instances."
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
    (update-in req [:prefer] #(if %1 (into %2 %1) %2) pv)
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
               (preference? prefs) [prefs]
               (map? prefs)        (vals prefs)
               :else               prefs)]
    (->> prefs
         (r/map as-preference-applied)
         (update-in resp [:headers "Preference-Applied"]
                    (fnil into [])))))

(defn prefer-request
  "Apply RFC 7240 handling to `request` map.

`Prefer` request headers will be parsed into `Preference` record instances and
added to the request map under the key `:prefer` as a map of preference
name -> `Preference`."
  [request]
  (if-let [h (get-in request [:headers "prefer"])]
    (extract-prefer-headers request h)
    request))

(defn prefer-response
  "Apply RFC 7240 handling to `response` map.

The response map may contain a `:prefer` key, whose value may be a single
`Preference` instance, a map with `Preference` values, or collection of
`Preference` instances. These preferences will be used to add
`Preference-Applied` headers to the response map."
  [response]
  (if-let [prefs (get response :prefer)]
    (add-preference-applied-headers response prefs)
    response))

(defn wrap-prefer
  "Wrap `handler` with middleware for dealing with RFC 7240 preference headers
in both request and response maps. See [[prefer-request]] and
[[prefer-response]] for more details on request / response handling."
  [handler]
  (comp prefer-response handler prefer-request))
