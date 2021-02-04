(ns kino.views
  (:require [reagent.core  :as reagent]
            [kino.router :refer [url-for set-token!]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str :refer [trim split]]))

(defn main-wrapper [content]
  [:div.container
   [:nav.navbar.mb-4 {:role "navigation" :aria-label "main navigation"}
    [:div.navbar-brand
     [:a.navbar-item {:href "http://localhost:3000"}
      [:h1.title "Kino"]]]
    [:div.navbar-menu
     [:div.navbar-start
      [:div.navbar-item
       [:a.button.is-light {:href "/stats"} "Stats"]]]]
    [:div.navbar-end
     [:div.navbar-item
      [:div.buttons
       [:a.button.is-primary {:href "/login"}
        [:strong "Login"]]
       [:a.button.is-light {:href "/logout"} "Logout"]]]]]
   [:div.mb-4
    content]
   [:footer.footer
    [:div.content.has-text-centered
     [:p (str "version cljs")]
     #_[:p (str "total users: " (count (db/get-users)))]]]])

(defn home []
  [:h2 "home"])

(defn login []
  [:h2 "login"])

(defn profile []
  [:h2 "profile"])

(defn stats []
  [:h2 "stats"])

(defn nav []
  [:ul
   [:li
    [:a {:href (url-for :home)} "Home"]]
   [:li
    [:a {:href (url-for :login)} "Login"]]])


(defn pages [page-name]
  (case (:page page-name)
    :home     [home]
    :stats    [stats]
    :login    [login]
    :profile  [profile]
    [home]))

(defn kino-app []
  (let [active-page @(subscribe [:active-page])]
    (main-wrapper
      [pages active-page])))
