![](https://dl.dropboxusercontent.com/u/174179/proteus/the-blob.jpg)

Proteus gives you back the local mutable variables you've so dearly missed.

## usage

Add this to your `project.clj`:

```clj
[proteus "0.1.0"]
```

Proteus exposes a single macro, `let-mutable`:

```clj
(let-mutable [x 0]
  (dotimes [_ 100]
    (set! x (inc x)))
  x)
```

`let-mutable` gives you variables that can be set using `set!` within the scope.  These variables cannot escape the local scope; if passed into a function or closed over, the current value of the variable will be captured.  Since unboxed numbers are supported, this can be significantly faster than `clojure.core/with-local-vars`.

That's it.  That's the end of the library.

## license

Copyright © 2013 Zachary Tellman

Distributed under the MIT License.
