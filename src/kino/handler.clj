(ns kino.handler
  (:require
    [compojure.route :as route]
    [compojure.core :refer [defroutes GET POST ANY]]
    [ring.util.response :refer [response content-type charset]]
    [ring.middleware.format :refer [wrap-restful-format]]
    [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
    [kino.db :as db]
    [kino.spot :as spot]
    [kino.html :as html]
    [kino.oauth :as oauth]
    [clj-spotify.core :as spotify]
    [system.repl :refer [system]]
    [ring.util.response :as response]))


(defn- handle-oauth-callback [params]
  (let [keys (oauth/get-authentication-response "foo" params)]
    (if keys
      (let [{access_token :access_token refresh_token :refresh_token} keys
            user (spotify/get-current-users-profile {} access_token)]
        (if user
          (let [u (db/upsert-user user refresh_token)]
            (spot/fetch-and-persist u)
            u))))))

(defn- fmt-latest-tracks [uid]
  (if uid
    (->> (db/get-play-data uid)
         (map #(-> % :track :name))
         (clojure.string/join "<br>"))))

(defn manifest-map
  "Returns the mainAttributes of the manifest of the passed in class as a map."
  [clazz]
  (->> (str "jar:"
            (-> clazz
              .getProtectionDomain
              .getCodeSource
              .getLocation)
            "!/META-INF/MANIFEST.MF")
       clojure.java.io/input-stream
       java.util.jar.Manifest.
       .getMainAttributes
       (map (fn [[k v]] [(str k) v]))
       (into {})))

(defroutes routes
  (GET "/" []
       (fn [{session :session}]
         (let [uid (:spot.user/id session)]
           (html/index uid))))
  (GET "/yo" []
       (fn [{session :session}]
         (taoensso.timbre/info "getting yo")
         (->
           (response/response (db/get-artists))
           (assoc :session (assoc session :foo :bar))))
       #_(->  (get-artists)
           response
           (content-type "application/json")
           (charset "UTF-8")))
  (GET "/count" []
       (fn [{session :session}]
         (let [count (:count session 0)
               session (assoc session :count (inc count))]
           (-> (response (str "You accessed this page " count " times."))
             (assoc :session session)))))
  (GET "/foo" []
       (html/index "foo"))
  (GET "/boom" []
       (ex-info "boom!" {}))
  (GET "/version" []
       (response/response
         (manifest-map (Class/forName "ify.core"))))
  (GET "/tracks" []
       (db/get-tracks))
  (GET "/entity/:id" []
       (fn [{params :params}]
         (let [e (db/get-entity (-> params :id keyword))]
           (-> e
             response
             (content-type "application/json")
             (charset "UTF-8")))))
  (GET "/login" []
       (response/redirect (oauth/authorize-uri "foo")))
  (GET "/oauth/callback" []
       (fn [{params :params session :session}]
         (let [user (handle-oauth-callback params)
               session (assoc session :spot.user/id (:crux.db/id user))]
           (->
             (response/redirect "/")
             (assoc :session session)))))
  (route/not-found "404"))

(defn wrap-exception [handler]
  (fn [request]
    (try (handler request)
         (catch Exception e
           (taoensso.timbre/fatal e)
           {:status 500
            :body "Oh no! :'("}))))

(def app
  (-> routes
    (wrap-restful-format :formats [:json])
    (wrap-defaults (-> site-defaults
                     #_(assoc-in [:session :cookie-attrs :max-age] 3600)
                     (assoc-in [:session :cookie-attrs :same-site] :lax))) ;; TODO check
    wrap-exception))
