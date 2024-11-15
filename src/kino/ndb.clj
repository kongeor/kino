(ns kino.ndb
  (:require [honey.sql :as sql]
            [honey.sql.helpers :refer :all :as helpers]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as rs]
            [next.jdbc.date-time]
            [taoensso.timbre :as timbre]
            [ragtime.jdbc :as ragtime]
            [ragtime.repl :as rrepl]
            [kino.util :as u])
  (:refer-clojure :exclude [update]))

(def q {:select [:event_offset]
        :from [:tx_events]
        :where [:< :event_offset 5]})



(defn insert-artists [db artists]
  (jdbc/execute!
    db
    (->
      (insert-into :artists)
      (columns :name :external_id)
      (values artists)
      sql/format)))

(comment
  (insert-artists [["foo" "bar"]]))

;; queries


;; artists

(defn get-artist-by-ext-id [db ext-id]
  (jdbc/execute-one!
    db
    (sql/format {:select :* :from :artists :where [:= :external_id ext-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-artist-by-ext-id "06nsZ3qSOYZ2hPVIMcr1IN"))

(comment
  (get-artist-by-ext-id "foo"))

(defn insert-artist
  "Expects the following format {:id 5, :name \"J.J. Cale\", :external_id \"06nsZ3qSOYZ2hPVIMcr1IN\"}"
  [db artist]
  (jdbc/execute-one!
    db
    (->
      (insert-into :artists)
      (columns :name :external_id)
      (values [(mapv artist [:name :id])])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-artist {:name "yo" :id "lo"}))

(defn insert-or-get-artist [db artist]
  (if-let [a (get-artist-by-ext-id db (:id artist))]
    a
    (insert-artist db artist)))

(comment
  (insert-or-get-artist {:id "austin" :name "powers"}))

;; albums

(defn get-album-by-ext-id [db ext-id]
  (jdbc/execute-one!
    db
    (sql/format {:select :* :from :albums :where [:= :external_id ext-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-album-by-ext-id "3NoP1ifIejWkGSDsO9T2xH"))

(defn insert-album [db album]
  (jdbc/execute-one!
    db
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

(defn insert-or-get-album [db album]
  (if-let [a (get-album-by-ext-id db (:id album))]
    a
    (insert-album db album)))

(comment
  (insert-or-get-album {:name "The Nat King Cole Story",
                        :id "3NoP1ifIejWkGSDsO9T2xH",
                        :total_tracks 36,
                        :img_url "https://i.scdn.co/image/ab67616d0000b273deac5adf07affb5fec422701"}))

;; tracks

(defn get-track-by-ext-id [db ext-id]
  (jdbc/execute-one!
    db
    (sql/format {:select :* :from :tracks :where [:= :external_id ext-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-track-by-ext-id "3NoP1ifIejWkGSDsO9T2xH"))

(defn insert-track [db track album-id]
  (jdbc/execute-one!
    db
    (->
      (insert-into :tracks)
      (columns :name :external_id :explicit :track_number :album_id)
      (values [(conj (mapv track [:name :id :explicit :track_number]) album-id)])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-track {:name "If I May", :explicit false, :id "0CHnZHSNhsNLkLEwSpCQma"} 1))

(defn insert-or-get-track [db track album-id]
  (if-let [t (get-track-by-ext-id db (:id track))]
    t
    (insert-track db track album-id)))

(comment
  (insert-or-get-track {:name "If I May", :explicit false, :id "0CHnZHSNhsNLkLEwSpCQma"} 2))

(defn get-last-played-track [db user-id]
  (jdbc/execute-one!
    db
    (->
      (select :*)
      (from :user_plays)
      (where [:= :user_id user-id])
      (order-by [:played_at :desc])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-last-played-track 1))

;; track artists

(defn get-track-artist [db track-id artist-id]
  (jdbc/execute-one!
    db
    (->
      (select :*)
      (from :track_artists)
      (where [:= :track_id track-id] [:= :artist_id artist-id]) sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-track-artists [db track-id artist-ids]
  (let [vs (map vector (repeat track-id) artist-ids)
        vs' (filter (fn [[tid aid]] (nil? (get-track-artist db tid aid))) vs)]
    (when (seq vs')
      (jdbc/execute!
        db
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

(defn get-user-play [db user-id track-id played-at]
  (jdbc/execute-one!
    db
    (->
      (select :*)
      (from :user_plays)
      (where [:= :user_id user-id] [:= :track_id track-id] [:= :played_at played-at])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(defn insert-user-play [db user-id track-id played-at]
  (when-not (get-user-play db user-id track-id played-at)
    (jdbc/execute!
      db
      (->
        (insert-into :user_plays)
        (columns :user_id :track_id :played_at)
        (values [[user-id track-id played-at]])
        sql/format)
      {:builder-fn rs/as-unqualified-lower-maps :return-keys true})))

;; users

(defn get-users [db]
  (jdbc/execute!
    db
    (->
      (select :*)
      (from :users)
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(defn insert-user [db user refresh-token]
  (jdbc/execute-one!
    db
    (->
      (insert-into :users)
      (columns :external_id :refresh_token)
      (values [(conj (mapv user [:id]) refresh-token)])
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps :return-keys true}))

(comment
  (insert-track {:name "if i may", :explicit false, :id "0chnzhsnhsnlklewspcqma"} 1))

(defn get-user-by-ext-id [db ext-id]
  (jdbc/execute-one!
    db
    (sql/format {:select :* :from :users :where [:= :external_id ext-id]})
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-track-by-ext-id "3nop1ifiejwkgsdso9t2xh"))


;; todo upsert
(defn upsert-user [db user refresh-token]
  (if-let [u (get-user-by-ext-id db (:id user))]
    u
    (insert-user db user refresh-token)))


(defn insert-all-track-data [db data played-at user-ext-id]
  (let [user (get-user-by-ext-id db user-ext-id)
        artists (mapv (partial insert-or-get-artist db) (:artists data))
        album (insert-or-get-album db (:album data))
        track (insert-or-get-track db (:track data) (:id album))
        _ (insert-track-artists db (:id track) (map :id artists))]
    (insert-user-play db (:id user) (:id track) played-at)))

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

(defn get-recent-user-plays [db user-id & {:keys [cnt before] :or {cnt 36 before (java.time.Instant/now)}}]
  (jdbc/execute!
    db
    (->
      (select [:t.name :track_name] [:up.played_at :played_at] [:a.name :album_name] [:a.img_url :img_url] [:a.id :album_id] :up.track_id :t.track_number :a.total_tracks)
      (from [:user_plays :up])
      (join [:tracks :t] [:= :t.id :up.track_id])
      (left-join [:albums :a] [:= :a.id :t.album_id])
      (where [:= :up.user_id user-id] (when before [:< :up.played_at before]))
      (order-by [:up.played_at :desc])
      (limit cnt)
      sql/format)
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-recent-user-plays 1))

;; migrations

(defn migrate [db-spec]
  (timbre/info "migrating ...")
  (rrepl/migrate {:datastore  (ragtime/sql-database db-spec)
                  :migrations (ragtime/load-resources "migrations")}))

(comment
  (migrate kino.system/db-spec))

(comment
  (get-recent-user-plays (:database.sql/connection integrant.repl.state/system)
    2 :cnt 2 :before (u/iso-date-str->instant "2021-02-06T14:27:14Z")))

(comment
  (get-recent-user-plays (:database.sql/connection integrant.repl.state/system)
    2 :cnt 2 :before nil))


#_(comment
  (rrepl/rollback {:datastore  (ragtime/sql-database (-> system :ndb :db-spec))
                  :migrations (ragtime/load-resources "migrations")}))

;; user stats

(defn get-user-most-played-artists [db user-id]
  (jdbc/execute!
    db
    (sql/format {:select   [[:a.id]
                            [:a.name]
                            [[:count :*] :plays]]
                 :from     [[:user-plays :up]]
                 :join     [[:tracks :t] [:= :t.id :up.track-id]
                            [:track-artists :ta] [:= :ta.track-id :t.id]
                            [:artists :a] [:= :ta.artist-id :a.id]]
                 :where    [[:= :up.user-id user-id]]
                 :group-by [:a.id]
                 :order-by [[:plays :desc]]
                 :limit    20})
    {:builder-fn rs/as-unqualified-lower-maps}))

(comment
  (get-user-most-played-artists
    (:database.sql/connection integrant.repl.state/system)
    1)

  )