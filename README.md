![](docs/the-blob.jpg)

Proteus gives you back the local mutable variables you've so dearly missed.

## usage

Add this to your `project.clj`:

```clj
[proteus "0.1.6"]
```

Proteus exposes a single macro, `proteus/let-mutable`:

```clj
(use 'proteus)

(let-mutable [x 0]
  (dotimes [_ 100]
    (set! x (inc x)))
  x)
```

`let-mutable` gives you variables that can be set using `set!` within the scope.  Since it's unsynchronized and doesn't box numbers, it's faster (often significantly) than Clojure's volatiles, atoms, or refs.  However, these variables cannot escape the local scope; if passed into a function or closed over, the current value of the variable will be captured.  This means that even though this is unsynchronized mutable state, there's no potential for race conditions.

Unless, of course, you want there to be.  It can be sometimes useful to close over the variable rather than the value, for instance when trying to communicate more than the new value from within a `swap!` call.

```clj
(let [a (atom 0)]
  (let-mutable [x :foo]

    (swap! a
      ^:local
      (fn [a]
        (set! x :bar)
        (inc a)))

    x))
```

Here we've hinted the closure as `:local`, meaning that it's only called within the local scope.  Since this is true for `swap!`, we can safely use the mutable variable as a side-channel for communication.  However, using `:local` on a non-local closure may have strange, reality-defying effects.  Use at your own risk.

That's it.  That's the end of the library.

## license

Copyright © 2013 Zachary Tellman

Distributed under the MIT License.
