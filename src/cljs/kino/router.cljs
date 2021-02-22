(ns kino.router
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]
            [re-frame.core :refer [dispatch]]))

(def routes
  ["/" {""         :home
        "playlists" :playlists
        "playlists/" {[:id] :playlists}

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
                                           :route-params (get-in matched-route [:route-params])}]))

(defn start!
  []
  (pushy/start! (pushy/pushy dispatch-route parse-url)))

(def url-for (partial bidi/path-for routes))

(def history (pushy/pushy dispatch-route (partial bidi/match-route routes)))

(defn set-token!
  [token]
  (pushy/set-token! history token))