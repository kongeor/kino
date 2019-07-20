(ns kino.html
  (:require
    (hiccup [page :refer [html5 include-js include-css]])
    [kino.db :as db]))


(defn base [content]
  (html5
    [:head
     [:meta {:charset "utf-8"}]
     [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
     [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
     [:meta {:name "description" :content "Kino"}]
     [:meta {:name "author" :content "Kostas Georgiadis"}]
     [:title "Kino"]

     [:link {:href "//fonts.googleapis.com/css?family=Raleway:400,300,600"
             :rel "stylesheet"
             :type "text/css"}]

     (include-css
       "//cdnjs.cloudflare.com/ajax/libs/normalize/8.0.1/normalize.min.css"
       "//cdnjs.cloudflare.com/ajax/libs/skeleton/2.0.4/skeleton.min.css")

     "<!--[if lt IE 9]>"
     [:script {:src "https://oss.maxcdn.com/libs/html5shiv/3.7.0/html5shiv.js"}]
     [:script {:src "https://oss.maxcdn.com/libs/respond.js/1.4.2/respond.min.js"}]
     "<![endif]-->"]
    [:body

     [:div#main-area.container
      [:h1 "Kino"]
      content]]))

(defn index [uid]
  (let [user (and uid (db/get-entity uid))]
    (if user
      (let [play-data (db/get-play-data uid)]
        #_(clojure.pprint/pprint play-data)
        (base
          [:div [:p (str "Welcome " (:display_name user))]
           (for [p play-data]
             [:div.row
              [:div.two.columns
               [:img.u-max-full-width {:src (get-in p [:kino.play/track :kino.track/album :kino.album/images 1 :url])}]]
              [:div.ten.columns
               [:h5 (-> p :kino.play/track :kino.track/name)]
               [:p (str "by " (->> (-> p :kino.play/track :kino.track/artists)
                                   (map :kino.artist/name)
                                   (clojure.string/join ", ")))]
               [:p (-> p :kino.play/played-at)]]])]))
      (base [:a.button.button-primary {:href "/login"} "Login"]))))