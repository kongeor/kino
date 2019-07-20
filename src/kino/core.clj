(ns kino.core
  (:require [system.repl :refer [set-init! go]]
            [kino.systems :refer [base-system]])
  (:gen-class))

(defn -main
  "Start a production system, unless a system is passed as argument (as in the dev-run task)."
  [& args]
  (let [system (or (first args) #'base-system)]
    (set-init! system)
    (go)))

