(ns kino.handler
  (:require
    [compojure.route :as route]
    [compojure.core :refer [defroutes GET POST ANY]]
    [ring.util.response :refer [response content-type charset]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
    [kino.ndb :as ndb]
    [kino.spot :as spot]
    [kino.html :as html]
    [kino.oauth :as oauth]
    [clj-spotify.core :as spotify]
    [taoensso.timbre :as log]
    [ring.util.response :as response]))

;; utils

(defn- handle-oauth-callback [settings db params]
  (let [keys (oauth/get-authentication-response settings "foo" params)]
    (if keys
      (let [{access_token :access_token refresh_token :refresh_token} keys
            user (spotify/get-current-users-profile {} access_token)]
        (if user
          (let [u (ndb/upsert-user db user refresh_token)]
            (spot/fetch-and-persist settings db u)
            u))))))

(defroutes routes
  (GET "/" []
    (fn [{session :session db :db settings :settings :as req}]
      (let [uid (:spot.user/id session)]
        (html/index settings db uid))))
  (GET "/stats" []
    (fn [{session :session db :db settings :settings}]
      (let [uid (:spot.user/id session)]
        (html/stats settings db uid))))
  (GET "/most-played" []
    (fn [{session :session db :db settings :settings}]
      (let [uid (:spot.user/id session)]
        (html/user-most-played-artists settings db uid))))
  (GET "/count" []
    (fn [{session :session db :db}]
      (let [count (:count session 0)
            session (assoc session :count (inc count))]
        (-> (response (str "You accessed this page " count " times."))
            (assoc :session session)))))
  (GET "/login" []
    (fn [{session :session settings :settings}]
      ;; TODO csrf
      (response/redirect (oauth/authorize-uri settings "foo"))))
  (GET "/logout" []
    (fn [req]
      (->
        (response/redirect "/")
        (assoc :session nil))))
  (GET "/oauth/callback" []
    (fn [{params :params session :session db :db settings :settings}]
      (let [user (handle-oauth-callback settings db params)
            session (assoc session :spot.user/id (:id user))]
        (->
          (response/redirect "/")
          (assoc :session session)))))
  (route/not-found "404"))

(defn wrap-db [handler db]
  (fn [req]
    (handler (assoc req :db db))))

(defn wrap-settings [handler settings]
  (fn [req]
    (handler (assoc req :settings settings))))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (log/fatal e)
           {:status 500
            :body "Oh no! :'("}))))

(defn app [db settings]
  (-> routes
    (wrap-db db)
    (wrap-settings settings)
    (wrap-defaults (-> site-defaults
                     #_(assoc-in [:session :cookie-attrs :max-age] 3600)
                     (assoc-in [:session :cookie-attrs :same-site] :lax)
                     (assoc-in [:security :anti-forgery] false))) ;; TODO check
    #_wrap-dir-index
    wrap-exception))
