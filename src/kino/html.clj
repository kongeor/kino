(ns kino.html
  (:require
    [hiccup [page :refer [html5 include-js include-css]]]
    [environ.core :refer [env]]
    [kino.util :as util]
    [kino.ndb :as ndb]
    [kino.stats :as stats]
    [clojure.contrib.humanize :as hmn])
  (:import (java.util Date)))


(defn base [uid content]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:meta {:name "description" :content "Kino"}]
     [:meta {:name "author" :content "Kostas Georgiadis"}]
     [:title "Kino"]

     [:script {:src "//use.fontawesome.com/releases/v5.3.1/js/all.js"
               :defer true
               }]

     (include-css
       "//cdn.jsdelivr.net/npm/bulma@0.9.1/css/bulma.min.css")]
    [:body
     [:div.container
      [:nav.navbar.mb-4 {:role "navigation" :aria-label "main navigation"}
       [:div.navbar-brand
        [:a.navbar-item {:href (:app-host env)}
         [:h1.title "Kino"]]]
       [:div.navbar-menu
        [:div.navbar-start
         [:div.navbar-item
          [:a.button.is-light {:href "/stats"} "Stats"]]]]
       [:div.navbar-end
        [:div.navbar-item
         [:div.buttons
          (if-not uid
            [:a.button.is-primary {:href "/login"}
             [:strong "Login"]]
            [:a.button.is-light {:href "/logout"} "Logout"])]]]]

      [:div.mb-4
       content]
      [:footer.footer
       [:div.content.has-text-centered
        [:p (str "version " (util/project-version))]
        #_[:p (str "total users: " (count (db/get-users)))]]]]]))

(defn index [db uid]
  (let [play-data (ndb/get-recent-user-plays db uid)
        now (inst-ms (Date.))]
    (base
      uid
      [:div
       (for [part-data (partition-all 6 play-data)]
         [:div.columns
          (for [p part-data]
            [:div.column
             [:div.card
              [:div.card-image
               [:figure.image
                [:img {:src (:img_url p)}]]]
              [:div.card-content
               [:p.title.is-4 (:track_name p)]
               #_[:p.subtitle.is-6 (->> (-> p :kino.play/track :kino.track/artists)
                                     (map :kino.artist/name))]
               [:p.subtitle.is-6 (:album_name p)]
               (if-let [played-at (p :played_at)]
                 [:p (-> played-at inst-ms hmn/datetime)])]]])])])))

(defn stats [db uid]
  (let [album-data (stats/album-plays db uid)]
    (base
      uid
      [:div
       (for [part-data (partition-all 6 album-data)]
         [:div.columns
          (let [c (count part-data)
                pd (concat part-data (repeat (- 6 c) nil))]
            (for [a pd]
              [:div.column
               (when a
                 [:div.card
                  [:div.card-image
                   [:figure.image
                    [:img {:src (:img_url a)}]]]
                  [:div.card-content
                   [:p.title.is-4 (-> a :album_name)]
                   [:p (str "tracks " (->> (-> a :tracks)
                                        (clojure.string/join ", ")))] ;; TODO what to do with
                   [:p (-> a :played-at inst-ms hmn/datetime)]]])]))])])))
