(ns kino.system
  (:require [integrant.core :as ig]
            [next.jdbc :as jdbc]
            [environ.core :refer [env]]
            [org.httpkit.server :refer [run-server]]
            #_[ring.adapter.jetty :refer [run-jetty]]
            [kino.handler :as handler]
            #_[usermanager.handler :as handler]
            #_[usermanager.model.user-manager :refer [populate]]))

(def db-spec {:dbtype   "postgresql"
               :dbname   (env :pg-db)
               :user     (env :pg-user)
               :password (env :pg-pass)})

(def config
  {:adapter/jetty {:handler (ig/ref :handler/run-app) :port (Integer. (or (env :http-port) 3000))}
   :handler/run-app {:db (ig/ref :database.sql/connection)}
   :database.sql/connection db-spec})

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