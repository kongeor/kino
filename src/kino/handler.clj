(ns kino.handler
  (:require
    [compojure.route :as route]
    [compojure.core :refer [defroutes GET POST ANY]]
    [ring.util.response :refer [response content-type charset]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [kino.ndb :as ndb]
    [kino.spot :as spot]
    [kino.html :as html]
    [kino.oauth :as oauth]
    [clj-spotify.core :as spotify]
    [system.repl :refer [system]]
    [ring.util.response :as response]))


(defn- handle-oauth-callback [db params]
  (let [keys (oauth/get-authentication-response "foo" params)]
    (if keys
      (let [{access_token :access_token refresh_token :refresh_token} keys
            user (spotify/get-current-users-profile {} access_token)]
        (if user
          (let [u (ndb/upsert-user db user refresh_token)]
            (spot/fetch-and-persist db u)
            u))))))

(defroutes routes
  (GET "/" []
       (fn [{session :session db :db :as req}]
         (let [uid (:spot.user/id session)]
           (html/index db uid))))
  (GET "/stats" []
       (fn [{session :session db :db}]
         (let [uid (:spot.user/id session)]
           (html/stats db uid))))
  (GET "/count" []
       (fn [{session :session db :db}]
         (let [count (:count session 0)
               session (assoc session :count (inc count))]
           (-> (response (str "You accessed this page " count " times."))
             (assoc :session session)))))
  (GET "/login" []
       (fn [{session :session}]
         (response/redirect (oauth/authorize-uri "foo"))))
  (GET "/logout" []
    (fn [req]
      (->
        (response/redirect "/")
        (assoc :session nil))))
  (GET "/oauth/callback" []
       (fn [{params :params session :session db :db}]
         (let [user (handle-oauth-callback db params)
               session (assoc session :spot.user/id (:id user))]
           (->
             (response/redirect "/")
             (assoc :session session)))))
  (route/not-found "404"))

(defn wrap-db [handler db]
  (fn [req]
    (handler (assoc req :db db))))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (taoensso.timbre/fatal e)
           {:status 500
            :body "Oh no! :'("}))))

(defn app [db]
  (-> routes
    (wrap-db db)
    (wrap-restful-format :formats [:json])
    (wrap-defaults (-> site-defaults
                     #_(assoc-in [:session :cookie-attrs :max-age] 3600)
                     (assoc-in [:session :cookie-attrs :same-site] :lax))) ;; TODO check
    wrap-exception))
