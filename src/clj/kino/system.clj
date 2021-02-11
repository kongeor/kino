(ns kino.system
  (:require [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            [kino.handler :as handler]
            [kino.util :as u]
            [kino.ndb :as db]
            [kino.spot :as spot]
            [taoensso.timbre :as timbre]
            [clojurewerkz.quartzite.scheduler :as qs]
            [clojurewerkz.quartzite.conversion :as qc]
            [clojurewerkz.quartzite.triggers :as t]
            [clojurewerkz.quartzite.schedule.simple :refer [schedule repeat-forever with-interval-in-minutes]]
            [clojurewerkz.quartzite.jobs :refer [defjob] :as j]))

;; scheduling

(defjob HistoryWatcher [ctx]
  (let [m (qc/from-job-data ctx)
        db-conn (get m "db")]
    (doseq [u (db/get-users db-conn)]
      (timbre/info "fetching recent plays for user" (:external_id u))
      (spot/fetch-and-persist db-conn u)
      (timbre/info "processed recent plays for user" (:external_id u)))))

(defn schedule-history-watcher [scheduler db]
  (let [job (j/build
              (j/of-type HistoryWatcher)
              (j/using-job-data {"db" db})
              (j/with-identity (j/key "jobs.history.1")))
        trigger (t/build
                  (t/with-identity (t/key "triggers.1"))
                  (t/start-now)
                  (t/with-schedule (schedule
                                     (repeat-forever)
                                     (with-interval-in-minutes (or (-> env :history-watcher-interval u/str->int) 30)))))]
    (qs/schedule scheduler job trigger)))

(def db-spec {:dbtype   "postgresql"
               :dbname   (env :pg-db)
               :user     (env :pg-user)
               :password (env :pg-pass)})

(def config
  {:adapter/jetty {:handler (ig/ref :handler/run-app) :port (Integer. (or (env :http-port) 3000))}
   :handler/run-app {:db (ig/ref :database.sql/connection)}
   :database.sql/connection db-spec
   :scheduler/play-fetcher {:db (ig/ref :database.sql/connection)}})

(defmethod ig/init-key :scheduler/play-fetcher [_ {:keys [db] :as opts}]
  (let [s (-> (qs/initialize) qs/start)]
    (schedule-history-watcher s db)
    {:play-fetcher s}))

(defmethod ig/halt-key! :scheduler/play-fetcher [_ {:keys [play-fetcher]}]
  (timbre/info "Shutting down recent plays fetcher")
  (qs/shutdown play-fetcher))

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler] :as opts}]
  (run-server handler (-> opts (dissoc handler) (assoc :join? false))))

(defmethod ig/init-key :handler/run-app [_ {:keys [db]}]
  (handler/app db))

(defmethod ig/init-key :database.sql/connection [_ db-spec]
  (jdbc/get-datasource db-spec)
  #_(let [conn ]
    (populate conn (:dbtype db-spec))
    conn))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (server))

(defn -main []
  (ig/init config))