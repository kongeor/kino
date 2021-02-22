(ns kino.views
  (:require [reagent.core  :as reagent]
            [kino.router :refer [url-for set-token!]]
            [re-frame.core :refer [subscribe dispatch] :as rf]
            [clojure.string :as str :refer [trim split]]))

;; main wrapper

(defn main-wrapper [content]
  (let [user @(subscribe [:kino.subs/user])
        uid (:id user)]
    [:div.container
     [:nav.navbar.mb-4 {:role "navigation" :aria-label "main navigation"}
      [:div.navbar-brand
       [:a.navbar-item {:href "http://localhost:3000"}
        [:h1.title "Kino"]]]
      [:div.navbar-menu
       [:div.navbar-start
        (when uid
          [:div.navbar-item
           [:a.button.is-light {:href "/playlists"} "Playlists"]])]]
      [:div.navbar-end
       [:div.navbar-item
        [:div.buttons
         (if-not uid
           [:a.button.is-primary {:href "/api/login"}
            [:strong "Login"]]
           [:a.button.is-light {:href "/api/logout"} "Logout"])]]]]
     [:div.mb-4
      content]
     [:footer.footer
      [:div.content.has-text-centered
       [:p (str "version cljs")]
       #_[:p (str "total users: " (count (db/get-users)))]]]]))

;; user plays

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
  (let [user @(subscribe [:kino.subs/user])
        uid (:id user)]
    (when uid
      [:div
       (if-let [play-data @(subscribe [:kino.subs/plays])]
         (map-indexed
           (fn [idx item]
             ^{:key idx} [:div.columns
                          (for [p item]
                            ^{:key (:played_at p)} [:div.column [play-card p]])])
           (partition-all 6 play-data)))
       [:div.columns.is-centered
        [:button.button.is-primary {:on-click #(rf/dispatch [:kino.events/fetch-user-plays])} "Load moar"]]])))

;; user playlists

(defn playlist-card [p]
  (when (:name p)
    [:a {:href (str "/playlists/" (:id p)) :on-click #(rf/dispatch [:kino.events/set-active-page {:page :playlists :route-params {:id (:id p)}}])}
     [:div.card
      [:div.card-image
       [:figure.image
        [:img {:src (:img_url p)}]]]
      [:div.card-content
       [:p.title.is-4 (:name p)]
       [:p.subtitle.is-6 (str "Total tracks: ") (:total_tracks p)]
       [:p.subtitle.is-6 (:description p)]]]]))

(defn playlist-tracks [playlist-id]
  (let [playlist-tracks @(subscribe [:kino.subs/playlist-tracks playlist-id])]
    [:table.table
     [:thead
      [:tr
       [:th "Name"]
       [:th "Album"]]]
     [:tbody
      (for [t playlist-tracks]
        ^{:key (:track_id t)} [:tr
                               [:td (:track_name t)]
                               [:td (:album_name t)]])]]
    ))

(defn user-playlists-view []
  (let [user @(subscribe [:kino.subs/user])
        uid (:id user)]
    (when uid
      (let [route-params (:route-params @(subscribe [:kino.subs/active-page]))]
        (if-let [playlist-id (:id route-params)]
          [playlist-tracks playlist-id]
          [:div
           (if-let [playlist-data @(subscribe [:kino.subs/playlists])]
             (let [n-to-fill (- 6 (mod (count playlist-data) 6))
                   all-data (concat playlist-data (mapv (fn [i] {:id (- i)}) (range n-to-fill)))
                   part-data (partition-all 6 all-data)]
               (map-indexed
                 (fn [idx item]
                   ^{:key idx} [:div.columns
                                (for [p item]
                                  ^{:key (:id p)} [:div.column [playlist-card p]])])
                 part-data)))])))))

(defn home []
  [user-plays-view])

(defn login []
  [:h2 "login"])

(defn profile []
  [:h2 "profile"])

(defn stats []
  [:h2 "stats"])

(defn playlists []
  (rf/dispatch [:kino.events/fetch-user-playlists])
  [user-playlists-view])

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
    :playlists [playlists]
    :login    [login]
    :profile  [profile]
    [home]))

(defn kino-app []
  (let [ready? (subscribe [:kino.subs/initialized?])]
    (if-not @ready?
      [:div "initializing ..."]
      (let [active-page @(subscribe [:kino.subs/active-page])]
        (main-wrapper
          [pages active-page])))))
