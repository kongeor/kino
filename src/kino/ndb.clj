(ns kino.ndb
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [ragtime.jdbc :as ragtime]
            [ragtime.repl :as rrepl]
            [system.repl :refer [system]]))

(def q {:select [:event_offset]
        :from [:tx_events]
        :where [:< :event_offset 5]})

(comment
  (-> system :ndb)
  )

(comment
  (jdbc/execute!
    (-> system :ndb :datasource)
    (sql/format q)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-artists [artists]
  (jdbc/execute!
    (-> system :ndb :datasource)
    (->
      (insert-into :artists)
      (columns :name :external_id)
      (values artists)
      sql/format)))

(comment
  (insert-artists [["foo" "bar"]]))

;; migrations

(comment
  (rrepl/migrate {:datastore  (ragtime/sql-database (-> system :ndb :db-spec))
                  :migrations (ragtime/load-resources "migrations")}))
