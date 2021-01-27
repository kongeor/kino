(ns kino.ndb
  (:require [honeysql.core :as sql]
            [honeysql.helpers :refer :all :as helpers]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.date-time]
            [ragtime.jdbc :as ragtime]
            [ragtime.repl :as rrepl]
            [system.repl :refer [system]])
  (:refer-clojure :exclude [update]))

(def q {:select [:event_offset]
        :from [:tx_events]
        :where [:< :event_offset 5]})

(comment
  (-> system :ndb)
  )

(comment
  (jdbc/execute!
    (-> system :ndb :datasource)
    (sql/format q)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-artists [artists]
  (jdbc/execute!
    (-> system :ndb :datasource)
    (->
      (insert-into :artists)
      (columns :name :external_id)
      (values artists)
      sql/format)))

(comment
  (insert-artists [["foo" "bar"]]))

;; queries


;; artists

(defn get-artist-by-ext-id [ext-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (sql/format (sql/build :select :* :from :artists :where [:= :external_id ext-id]))
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-artist-by-ext-id "06nsZ3qSOYZ2hPVIMcr1IN"))

(comment
  (get-artist-by-ext-id "foo"))

(defn insert-artist
  "Expects the following format {:id 5, :name \"J.J. Cale\", :external_id \"06nsZ3qSOYZ2hPVIMcr1IN\"}"
  [artist]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (->
      (insert-into :artists)
      (columns :name :external_id)
      (values [(mapv artist [:name :id])])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-artist {:name "yo" :id "lo"}))

(defn insert-or-get-artist [artist]
  (if-let [a (get-artist-by-ext-id (:id artist))]
    a
    (insert-artist artist)))

(comment
  (insert-or-get-artist {:id "austin" :name "powers"}))

;; albums

(defn get-album-by-ext-id [ext-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (sql/format (sql/build :select :* :from :albums :where [:= :external_id ext-id]))
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-album-by-ext-id "3NoP1ifIejWkGSDsO9T2xH"))

(defn insert-album [album]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (->
      (insert-into :albums)
      (columns :name :external_id :img_url :total_tracks)
      (values [(mapv album [:name :id :img_url :total_tracks])])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-album {:name "The Nat King Cole Story",
                 :id "3NoP1ifIejWkGSDsO9T2xH",
                 :total_tracks 36,
                 :img_url "https://i.scdn.co/image/ab67616d0000b273deac5adf07affb5fec422701"}))

(defn insert-or-get-album [album]
  (if-let [a (get-album-by-ext-id (:id album))]
    a
    (insert-album album)))

(comment
  (insert-or-get-album {:name "The Nat King Cole Story",
                        :id "3NoP1ifIejWkGSDsO9T2xH",
                        :total_tracks 36,
                        :img_url "https://i.scdn.co/image/ab67616d0000b273deac5adf07affb5fec422701"}))

;; tracks

(defn get-track-by-ext-id [ext-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (sql/format (sql/build :select :* :from :tracks :where [:= :external_id ext-id]))
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-track-by-ext-id "3NoP1ifIejWkGSDsO9T2xH"))

(defn insert-track [track album-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (->
      (insert-into :tracks)
      (columns :name :external_id :explicit :album_id)
      (values [(conj (mapv track [:name :id :explicit]) album-id)])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-track {:name "If I May", :explicit false, :id "0CHnZHSNhsNLkLEwSpCQma"} 1))

(defn insert-or-get-track [track album-id]
  (if-let [t (get-track-by-ext-id (:id track))]
    t
    (insert-track track album-id)))

(comment
  (insert-or-get-track {:name "If I May", :explicit false, :id "0CHnZHSNhsNLkLEwSpCQma"} 2))

;; track artists

(defn get-track-artist [track-id artist-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (-> (select :*) (from :track_artists) (where [:= :track_id track-id] [:= :artist_id artist-id]) sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-track-artists [track-id artist-ids]
  (let [vs (map vector (repeat track-id) artist-ids)
        vs' (filter (fn [[tid aid]] (nil? (get-track-artist tid aid))) vs)]
    (when (seq vs')
      (jdbc/execute!
        (-> system :ndb :datasource)
        (->
          (insert-into :track_artists)
          (columns :track_id :artist_id)
          (values vs')
          sql/format)
        {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))))

(comment
  (let [x 1
        ys [2 3 4]]
    (map vector (repeat x) ys)))

(comment
  (insert-track-artists 1 [1 2 3]))

;; user plays

(defn get-user-play [user-id track-id played-at]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (-> (select :*) (from :user_plays) (where [:= :user_id user-id] [:= :track_id track-id] [:= :played_at played-at]) sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-user-play [user-id track-id played-at]
  (when-not (get-user-play user-id track-id played-at)
    (jdbc/execute!
      (-> system :ndb :datasource)
      (->
        (insert-into :user_plays)
        (columns :user_id :track_id :played_at)
        (values [[user-id track-id played-at]])
        sql/format)
      {:builder-fn rs/as-unqualified-lower-maps :return-keys true})))

;; users

(defn insert-user [user refresh-token]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (->
      (insert-into :users)
      (columns :external_id :refresh_token)
      (values [(conj (mapv user [:id]) refresh-token)])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-track {:name "if i may", :explicit false, :id "0chnzhsnhsnlklewspcqma"} 1))

(defn get-user-by-ext-id [ext-id]
  (jdbc/execute-one!
    (-> system :ndb :datasource)
    (sql/format (sql/build :select :* :from :users :where [:= :external_id ext-id]))
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-track-by-ext-id "3nop1ifiejwkgsdso9t2xh"))


;; todo upsert
(defn upsert-user [user refresh-token]
  (println "***" user)
  (if-let [u (get-user-by-ext-id (:id user))]
    u
    (insert-user user refresh-token)))


(defn insert-all-track-data [data played-at user-ext-id]
  (let [user (get-user-by-ext-id user-ext-id)
        artists (mapv insert-or-get-artist (:artists data))
        album (insert-or-get-album (:album data))
        track (insert-or-get-track (:track data) (:id album))
        _ (insert-track-artists (:id track) (map :id artists))]
    (insert-user-play (:id user) (:id track) played-at)))

(comment
  (let [data
        {:track   {:name "if i may", :explicit false, :id "0chnzhsnhsnlklewspcqma"},
         :album   {:name         "the nat king cole story",
                   :id           "3nop1ifiejwkgsdso9t2xh",
                   :total_tracks 36,
                   :img_url      "https://i.scdn.co/image/ab67616d0000b273deac5adf07affb5fec422701"},
         :artists '({:name "nat king cole", :id "7v4ims0mosygdxylgvtiv7"})}]
    (insert-all-track-data data nil nil)))

;;

(defn get-recent-user-plays [user-id]
  (jdbc/execute!
    (-> system :ndb :datasource)
    (->
      (select [:t.name :track_name] [:up.played_at :played_at] [:a.name :artist_name] [:a.img_url :img_url])
      (from [:user_plays :up])
      (join [:tracks :t] [:= :t.id :up.track_id])
      (left-join [:albums :a] [:= :a.id :t.album_id])
      (where [:= :up.user_id user-id])
      (order-by [:up.played_at :desc])
      (limit 36)
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-recent-user-plays 1))

;; migrations

(comment
  (rrepl/migrate {:datastore  (ragtime/sql-database (-> system :ndb :db-spec))
                  :migrations (ragtime/load-resources "migrations")}))

(comment
  (rrepl/rollback {:datastore  (ragtime/sql-database (-> system :ndb :db-spec))
                  :migrations (ragtime/load-resources "migrations")}))
