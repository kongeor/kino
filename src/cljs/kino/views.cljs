(ns kino.views
  (:require [reagent.core  :as reagent]
            [kino.router :refer [url-for set-token!]]
            [re-frame.core :refer [subscribe dispatch]]
            [clojure.string :as str :refer [trim split]]))

;; main wrapper

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

;; home

(defn play-card [p]
  [:div.card
   [:div.card-image
    [:figure.image
     [:img {:src (:img_url p)}]]]
   [:div.card-content
    [:p.title.is-4 (:track_name p)]
    #_[:p.subtitle.is-6 (->> (-> p :kino.play/track :kino.track/artists)
                          (map :kino.artist/name))]
    [:p.subtitle.is-6 (:album_name p)]
    #_(if-let [played-at (p :played_at)]
      [:p (-> played-at inst-ms hmn/datetime)])]])

(defn user-plays-view []
  [:div
   (if-let [play-data @(subscribe [:kino.subs/plays])]
     (map-indexed
       (fn [idx item]
         ^{:key idx} [:div.columns
          (for [p item]
            ^{:key (:played_at p)} [:div.column [play-card p]])])
       (partition-all 6 play-data)))])

(defn home []
  [user-plays-view])

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
