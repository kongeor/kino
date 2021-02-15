(ns kino.router
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :refer [dispatch]]))

(def routes
  ["/" {""         :home
        "playlists" :playlists

        "login"    :login
        "logout"   :logout
        "stats"    :stats
        "register" :register
        "settings" :settings
        "editor/"  {[:slug]    :editor}
        "article/" {[:slug]    :article}
        "profile/" {[:user-id] {""           :profile
                                "/favorites" :favorited}}}])

(defn- parse-url
  [url]
  (bidi/match-route routes url))

(defn- dispatch-route
  [matched-route]
  (dispatch [:kino.events/set-active-page {:page      (:handler matched-route)
                                           :slug      (get-in matched-route [:route-params :slug])
                                           :profile   (get-in matched-route [:route-params :user-id])
                                           :favorited (get-in matched-route [:route-params :user-id])}]))

(defn start!
  []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))

(def history (pushy/pushy dispatch-route (partial bidi/match-route routes)))

(defn set-token!
  [token]
  (pushy/set-token! history token))