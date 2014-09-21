# ring.middleware.prefer

Ring middleware providing
[RFC7240 (Prefer Header for HTTP)](http://tools.ietf.org/html/rfc7240) support

[![Build Status](https://travis-ci.org/dthume/ring.middleware.prefer.svg?branch=master)](https://travis-ci.org/dthume/ring.middleware.prefer)

## Leiningen

[![Clojars Project](http://clojars.org/org.dthume/ring.middleware.prefer/latest-version.svg)](http://clojars.org/org.dthume/ring.middleware.prefer)

## Usage

The `org.dthume.ring.middleware.prefer/wrap-prefer` function can be used to
wrap a handler with middleware which will check the incoming response for
`Prefer` headers, parse them according to RFC7240, and add them to the request
under the key `:prefer`, whose value will be a map of preference names to
preference instances.

Each preference is an instance of the
`org.dthume.ring.middleware.prefer/Preference` record type, which defines three
primary keys:

`name`
: The name of the preference. Required.

`value`
: The primary value of the preference. Optional.

`params`
: A map of any secondary parameters specified by the preference. Optional

Note that all values (preference name, value, param names and param values) are
strings.

## TODO

- Add `Preference-Applied` support for responses

## License

Copyright (C) 2014 David Thomas Hume.
Distributed under the Eclipse Public License, the same as Clojure.