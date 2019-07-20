(ns user
  (:require
    [system.repl :refer [system set-init! start stop reset]]
    [kino.systems :refer [base-system]]))

(set-init! #'base-system)
; type (start) in the repl to start your development-time system.