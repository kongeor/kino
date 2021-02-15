(ns kino.handler
  (:require
    [compojure.route :as route]
    [compojure.core :refer [defroutes GET POST ANY]]
    [ring.util.response :refer [response content-type charset]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [muuntaja.core :as m]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults api-defaults]]
    [kino.ndb :as ndb]
    [kino.spot :as spot]
    [kino.html :as html]
    [kino.oauth :as oauth]
    [kino.util :as u]
    [clj-spotify.core :as spotify]
    [system.repl :refer [system]]
    [taoensso.timbre :as timbre]
    [ring.util.response :as response]))

;; utils

(defn json [content]
  (response/content-type {:status 200
                          :body (m/encode "application/json" content)} "application/json"))

(defn- handle-oauth-callback [db params]
  (let [keys (oauth/get-authentication-response "foo" params)]
    (if keys
      (let [{access_token :access_token refresh_token :refresh_token} keys
            user (spotify/get-current-users-profile {} access_token)]
        (if user
          (let [u (ndb/upsert-user db user refresh_token)]
            (spot/fetch-and-persist db u)
            u))))))

(defn me-handler [{session :session db :db}]
  (let [uid (:spot.user/id session)]
    (json {:id uid})))

(defn plays-handler [{session :session db :db params :query-params}]
  (let [uid (:spot.user/id session)
        before-str (u/trim-to-nil (get params "before"))
        before (when before-str (u/iso-date-str->instant before-str))]
    (timbre/info "fetching plays for" uid "before" before)
    (json (ndb/get-recent-user-plays db uid :before before))))

(defn playlists-handler [{session :session db :db}]
  (let [uid (:spot.user/id session)]
    (timbre/info "fetching playlists for" uid)
    (json (ndb/get-user-playlists db uid))))

(defroutes routes
  (GET "/" []
    (fn []
      (response/resource-response "public/index.html")
      #_(response/resource-response "index.html" {:root "public"}))
    #_(fn [{session :session db :db :as req}]
         (let [uid (:spot.user/id session)]
           (html/index db uid))))
  (GET "/api/me" [] me-handler)
  (GET "/api/plays" [] plays-handler)
  (GET "/api/playlists" [] playlists-handler)
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
  (GET "/api/login" []
       (fn [{session :session}]
         (response/redirect (oauth/authorize-uri "foo"))))
  (GET "/api/logout" []
    (fn [req]
      (->
        (response/redirect "/")
        (assoc :session nil))))
  (GET "/api/oauth/callback" []
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

(defn wrap-dir-index [handler]
  (fn [req]
    (handler
      (update
        req
        :uri
        #(if (= "/" %) "/index.html" %)))))

(defn app [db]
  (-> routes
    (wrap-db db)
    (wrap-defaults (-> site-defaults
                     #_(assoc-in [:session :cookie-attrs :max-age] 3600)
                     (assoc-in [:session :cookie-attrs :same-site] :lax)
                     (assoc-in [:security :anti-forgery] false))) ;; TODO check
    wrap-dir-index
    wrap-exception))
