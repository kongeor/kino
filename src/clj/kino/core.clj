(ns kino.core
  (:require [integrant.repl :as ig-repl]
            [kino.system :as sys]
            [kino.ndb :as ndb]
            [environ.core :refer [env]]
            [clojure.tools.nrepl.server :as serv])
  (:gen-class))

(defn -main
  "Start a production system, unless a system is passed as argument (as in the dev-run task)."
  [& args]
  (ig-repl/set-prep! (fn [] sys/config))
  (ig-repl/go)
  ;; TODO check
  (ndb/migrate sys/db-spec))

