(ns kino.html
  (:require
    (hiccup [page :refer [html5 include-js include-css]])
    [kino.util :as util]
    [kino.db :as db]
    [kino.stats :as stats]
    [clojure.contrib.humanize :as hmn])
  (:import (java.util Date)))


(defn base [content]
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
       "//cdn.jsdelivr.net/npm/bulma@0.8.2/css/bulma.min.css")]
    [:body
     [:section.section
      [:div.container
       [:h1.title "Kino"]
       content
       [:footer.footer
        [:div.content.has-text-centered
         [:p (str "version " (util/project-version))]
         [:p (str "total users: " (count (db/get-users)))]]]]]]))

(defn index [uid]
  (let [user (and uid (db/get-entity uid))]
    (if user
      (let [play-data (db/get-play-data uid 20)
            now (inst-ms (Date.))]
        #_(clojure.pprint/pprint play-data)
        (base
          [:div
           [:div
            [:p (str "Welcome " (:display_name user))]      ;; TODO style and factor out
            [:p [:a {:href "/stats"} "stats"]]]
           [:div
            (for [part-data (partition-all 6 play-data)]
              [:div.columns
               (for [p part-data]
                 [:div.column
                  [:div.card
                   [:div.card-image
                    [:figure.image
                     [:img {:src (get-in p [:kino.play/track :kino.track/album :kino.album/images 1 :url])}]]]
                   [:div.card-content
                    [:p.title.is-4 (-> p :kino.play/track :kino.track/name)]
                    [:p.subtitle.is-6 (->> (-> p :kino.play/track :kino.track/artists)
                                        (map :kino.artist/name))]
                    ;[:p.subtitle.is-6 (->> (-> p :kino.play/track :kino.track/album)
                    ;                    (map :kino.album/name))] ;; TODO album
                    [:p (-> p :kino.play/played-at inst-ms hmn/datetime)]]]])])]]))
      (base [:a.button.is-primary {:href "/login"} "Login"])))) ;; TODO factor out

(defn stats [uid]
  (let [user (and uid (db/get-entity uid))]
    (if user
      (let [album-data (stats/album-plays uid)]
        #_(clojure.pprint/pprint play-data)
        (base
          [:div
           [:div
            [:p (str "Welcome " (:display_name user))]
            [:p [:a {:href "/"} "home"]]]
           [:div
            (for [part-data (partition-all 6 album-data)]
              [:div.columns
               (for [a part-data]
                 [:div.column
                  [:div.card
                   [:div.card-image
                    [:figure.image
                     [:img {:src (get-in a [:album :kino.album/images 1 :url])}]]]
                   [:div.card-content
                    [:p.title.is-4 (-> a :album :kino.album/name)]
                    [:p (str "tracks " (->> (-> a :tracks)
                                         (clojure.string/join ", ")))] ;; TODO what to do with
                    [:p (-> a :played-at inst-ms hmn/datetime)]]]])])]]))
      (base [:a.button.is-primary {:href "/login"} "Login"]))))
