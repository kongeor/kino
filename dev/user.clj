(ns user
  (:require [integrant.repl :as ig-repl]
            [clojure.tools.namespace.repl :refer [set-refresh-dirs]]
            [kino.system :as system]))

#_(set-init! #'base-system)
; type (start) in the repl to start your development-time system.

(set-refresh-dirs "src")

(ig-repl/set-prep! (fn [] system/config))

(def go ig-repl/go)
(def halt ig-repl/halt)
(def reset ig-repl/reset)
(def reset-all ig-repl/reset-all)

(comment
  (go)
  (halt)
  (reset)
  (reset-all))