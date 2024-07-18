(ns kino.system
  (:require [cprop.core :as cp]
            [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [kino.handler :as handler]
            [kino.ndb :as db]
            [kino.spot :as spot]
            [next.jdbc.connection :as connection]
            [next.jdbc.result-set :as rs]
            [ragtime.jdbc :as rt-jdbc]
            [ragtime.repl :as rt-repl]
            [taoensso.timbre :as log]
            [chime.core :as chime]

            [ring.adapter.jetty :refer [run-jetty]]
            )
  (:import (com.mchange.v2.c3p0 ComboPooledDataSource PooledDataSource)
           (java.time Duration Instant)
           (next.jdbc.default_options DefaultOptions)
           (org.eclipse.jetty.server Server))
  (:gen-class))

;; scheduling

(defn start-play-fetcher [db settings]
  (let [now (Instant/now)
        fetch-every-mins (or (-> settings :history-watcher-interval) 30)]
    (chime/chime-at
      (chime/periodic-seq (Instant/now) (Duration/ofMinutes fetch-every-mins))

      (fn [time]
        (doseq [u (db/get-users db)]
          (log/info "fetching recent plays for user" (:external_id u))
          (spot/fetch-and-persist settings db u)
          (log/info "processed recent plays for user" (:external_id u))))

      {:on-finished (fn []
                      (log/info "Play fetcher finished"))
       :error-handler (fn [e]
                        (log/error e "Oops")
                        true)})))

(def config
  {:adapter/jetty           {:handler (ig/ref :handler/run-app)
                             :settings (ig/ref :config/settings)}
   :handler/run-app         {:db (ig/ref :database.sql/connection)
                             :settings (ig/ref :config/settings)}
   :database.sql/connection {:settings (ig/ref :config/settings)}
   :database.sql/migrations {:settings (ig/ref :config/settings)}
   :config/settings         {}
   :scheduler/play-fetcher {:db       (ig/ref :database.sql/connection)
                            :settings (ig/ref :config/settings)}})

(defmethod ig/init-key :adapter/jetty [_ {:keys [handler settings] :as opts}]
  (run-jetty handler (-> {:port (:http-port settings)
                          :host (:host settings)} (assoc :join? false))))

(defmethod ig/init-key :handler/run-app [_ {:keys [db settings]}]
  (handler/app db settings))

(defmethod ig/halt-key! :adapter/jetty [_ ^Server server]
  (.stop server))

(defmethod ig/init-key :config/settings [_ _]
  (cp/load-config))

(defmethod ig/init-key :scheduler/play-fetcher [_ {:keys [db settings]}]
  (start-play-fetcher db settings))

(defmethod ig/halt-key! :scheduler/play-fetcher [_ timer]
  (log/info "Shutting down recent plays fetcher")
  (.close timer))

(defmethod ig/init-key :database.sql/connection [_ {:keys [settings]}]
  (let [^PooledDataSource ds (connection/->pool ComboPooledDataSource (:db settings))
        ds-opts              (jdbc/with-options ds {:builder-fn rs/as-unqualified-lower-maps})]
    ;; this code initializes the pool and performs a validation check:
    (.close (jdbc/get-connection ds-opts))
    ;; otherwise that validation check is deferred until the first connection
    ds-opts))

(defmethod ig/halt-key! :database.sql/connection [_ ^DefaultOptions ds]
  (.close (:connectable ds)))

(defmethod ig/init-key :database.sql/migrations [_ {:keys [settings]}]
  (let [db-spec   (:db settings)
        rt-config {:datastore  (rt-jdbc/sql-database db-spec)
                   :migrations (rt-jdbc/load-resources "migrations")}]
    (rt-repl/migrate rt-config)))


(defn -main [& args]
  (ig/init config))