(ns kino.systems
  (:require [system.core :refer [defsystem]]
            (system.components
              [http-kit :refer [new-web-server]])
            [kino.handler :refer [app]]
            [kino.db :as db]
            [kino.spot :as spot]
            [kino.loggly :as loggly]
            [environ.core :refer [env]]
            [com.stuartsierra.component :as component]
            [taoensso.timbre :as timbre]
            [system.repl :refer [system]]
            [crux.api :as crux]
            [crux.jdbc]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.simple :refer [schedule repeat-forever with-interval-in-minutes]]
            [clojurewerkz.quartzite.jobs :refer [defjob] :as j])
  )

(if-let [token (env :loggly-api-token)]
  (if-not (clojure.string/blank? token)
    (timbre/merge-config!
      {:appenders
       {:loggly (loggly/loggly-appender
                  {:tags [:kino]
                   :token token})}})))

;; crux

(defrecord CruxDb [db]
  component/Lifecycle
  (start [component]
    (let [db
          #_(crux/start-node {:crux.node/topology '[crux.standalone/topology
                                                  crux.kv.rocksdb/kv-store]
                            :crux.kv/db-dir "data"})
          (crux/start-node {:crux.node/topology '[crux.jdbc/topology
                                                  crux.kv.rocksdb/kv-store]
                            :crux.kv/db-dir "data"
                               :crux.jdbc/dbtype "postgresql"
                               :crux.jdbc/dbname (env :pg-db)
                               :crux.jdbc/host (env :pg-host)
                               :crux.jdbc/user (env :pg-user)
                               :crux.jdbc/password (env :pg-pass)})]
      (timbre/info "starting crux")
      (assoc component :db db)))
  (stop [component]
    (when db
      (timbre/info "stopping crux")
      (.close db)
      component)))

(defn- new-db []
  (map->CruxDb nil))

;; quartzite

(defjob HistoryWatcher
        [ctx]
        (doseq [u (db/get-users)]
          (timbre/info "fetching history for user" (:crux.db/id u))
          (spot/fetch-and-persist u)
          (timbre/info "processed history for user" (:crux.db/id u))))

(defn str->int [s]
  (Integer/parseInt s))

(defn schedule-history-watcher [scheduler]
  (let [job (j/build
              (j/of-type HistoryWatcher)
              (j/with-identity (j/key "jobs.history.1")))
        trigger (t/build
                  (t/with-identity (t/key "triggers.1"))
                  (t/start-now)
                  (t/with-schedule (schedule
                                     (repeat-forever)
                                     (with-interval-in-minutes (or (-> env :history-watcher-interval str->int) 30)))))]
    (qs/schedule scheduler job trigger)))

(defrecord Scheduler [scheduler]
  component/Lifecycle
  (start [component]
    (let [s (-> (qs/initialize) qs/start)]
      (schedule-history-watcher s)                          ;; TODO ask
      (assoc component :scheduler s)))
  (stop [component]
    (qs/shutdown scheduler)
    component))

(defn new-scheduler
  []
  (map->Scheduler {}))

(defsystem base-system
           [:db (new-db)
            :web (new-web-server (Integer. (env :http-port)) app)
            :scheduler (new-scheduler)])