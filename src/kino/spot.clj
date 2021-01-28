(ns kino.spot
  (:require [clj-spotify.core :as spotify]
            [system.repl :refer [system]]
            [kino.db :as db]
            [kino.ndb :as ndb]
            [kino.oauth :as oauth]
            [kino.util :as util]
            [crux.api :as crux]
            [taoensso.timbre :as timbre]
            [kino.util :as util])
  (:import (java.security MessageDigest)))

(def track-keys [:explicit :name :track_number :type])

#_(def track (-> data :items first :track))

(defn track-artist-ids [track]
  (into #{} (map #(-> % :id keyword) (:artists track))))

(defn track-album-id [track]
  (-> track :album :id keyword))

#_(track-album-id track)

#_(track-artist-ids track)

(defn get-tracks [data]
  (mapv #(let [track (:track %)
               t (select-keys track track-keys)
               t (assoc t :crux.db/id (-> track :id keyword))
               t (assoc t :album_id (track-album-id track))
               t (assoc t :artist_ids (track-artist-ids track))]
           (assoc {}
             :crux.db/id (-> track :id keyword)
             :kino.track/album-id (track-album-id track)
             :kino.track/artist-ids (track-artist-ids track)
             :kino.track/explicit (:explicit t)
             :kino.track/number (:track_number t)
             :kino.track/name (:name t))) (-> data :items)))

;; 1
#_(get-tracks data)

(defn md5 [^String s]
  (let [algorithm (MessageDigest/getInstance "MD5")
        raw (.digest algorithm (.getBytes s))]
    (format "%032x" (BigInteger. 1 raw))))

(defn get-user-plays [uid data]
  (let [d (:items data)]
    (map #(let [played_at (-> % :played_at clojure.instant/read-instant-date)
                track_id (-> % :track :id keyword)]
            {:crux.db/id (keyword (md5 (str (name uid) (name track_id) (inst-ms played_at))))
             :kino.play/user-id uid
             :kino.play/track-id track_id
             :kino.play/played-at played_at
             ;:type "play"
             }) d)))

(def user-keys [:display_name :type])

(defn get-user-data [user-data]
  (let [u (select-keys user-data user-keys)]
    (assoc u :crux.db/id (-> user-data :id keyword))))

#_(get-user-data user-data)

;; 2
#_(get-user-plays :asdf data)

(def artist-keys [:name :type])

(defn get-artists [data]
  (apply concat
         (map (fn [items]
                (let [artists (-> items :track :album :artists)]
                  (mapv (fn [artist]
                          (let [a (select-keys artist artist-keys)]
                            {:crux.db/id (-> artist :id keyword)
                             :kino.artist/name (:name a)})) artists)))
              (:items data))))

;; TODO move
(defn distinct-by [f coll]
  (let [groups (group-by f coll)]
    (map #(first (groups %)) (distinct (map f coll)))))

(comment
  (distinct-by :foo [{:foo 1} {:foo 2} {:foo 1}]))

(defn get-artist-data [data]
  (mapv #(map % [:name :id])
    (distinct-by :id
      (apply concat
        (mapv (fn [items]
                (-> items :track :album :artists))
          (:items data))))))

(defn get-track-data [track]
  (let [track' (select-keys track [:name :explicit :track_number :id])
        album (merge
                 (select-keys (:album track) [:release_data :name :id :total_tracks])
                 {:img_url (-> track :album :images first :url)})
        artists (map #(select-keys % [:name :id]) (:artists track))]
    {:track track'
     :album album
     :artists artists}))

(comment
  (-> (read-string (slurp "sample.edn"))
    :items
    first
    :track
    get-track-data))

(comment
  (get-artist-data (read-string (slurp "sample.edn"))))

#_(:items (read-string (slurp "sample.edn")))


#_(-> data :items first :track :album :artists)

;; 3
#_(get-artists data)

(def album-keys [:name :release_date :type :images :total_tracks])

(defn get-albums [data]
  (map (fn [items]
         (let [album (-> items :track :album)]
           (let [a (select-keys album album-keys)]
             {:crux.db/id (-> album :id keyword)
              :kino.album/name (:name a)
              :kino.album/release-date (:release_date a)
              :kino.album/images (:images a)
              :kino.album/total-tracks (:total_tracks a)
              :kino.album/artist-ids (track-artist-ids album)})))
       (:items data)))

;; 4
#_(get-albums data)

(defn get-all-the-things [uid data]
  (let [tracks (get-tracks data)
        plays (get-user-plays uid data)
        artists (get-artists data)
        albums (get-albums data)]
    (concat tracks plays artists albums)))


(defn prepare-for-tx [data]
  (mapv (fn [d]
         [:crux.tx/put
          d]) data))

#_(get-all-the-things :asdf #_(-> user-data get-user-data :crux.db/id) data)

#_(prepare-for-tx
  (get-all-the-things (-> user-data get-user-data :crux.db/id) data))

#_(-> system :db :db)

(comment
  (let [token (-> (db/get-users) first :kino.user/refresh-token)
        access-token (oauth/get-access-token token)]
    (def data (spotify/get-current-users-recently-played-tracks {:limit 2} access-token))))

(comment
  (let [uid (-> (db/get-users) first :crux.db/id)]
    (let [data' (get-all-the-things uid data)
          data-ids (->> (map :crux.db/id data') (into #{}))
          existing-ids (db/get-entities data-ids)
          data'' (remove #(existing-ids (:crux.db/id %)) data')]
      data'')))

(defn persist-all-data [uid data]
  (let [data' (get-all-the-things uid data)
        data-ids (->> (map :crux.db/id data') (into #{}))
        existing-ids (db/get-entities data-ids)
        data'' (remove #(existing-ids (:crux.db/id %)) data')]
    (timbre/info "filtered" (count data'') "from a total of" (count data-ids) "for" uid)
    (crux/submit-tx
      (-> system :db :db)
      (prepare-for-tx data''))))


(defn persist-all-data-sql [data ext-user-id]
  (let [items (:items data)]
    (doall
      (for [item items]
        (let [track-data (get-track-data (:track item))
              played-at (-> item :played_at util/iso-date-str->instant)]
          #_(timbre/info "persisting track" track-data played-at ext-user-id)
          (ndb/insert-all-track-data track-data played-at ext-user-id))))))

#_(db/get-entity :36053687af37294a87a6121267aa6e17)

(defn fetch-and-persist [{id :id ext-id :external_id refresh-token :refresh_token}]
  (let [access_token (oauth/get-access-token refresh-token)
        last-played-at (-> (ndb/get-last-played-track id) :played_at)
        opts {:limit 50}
        opts (if last-played-at (assoc opts :after (inst-ms last-played-at)) opts)
        _ (timbre/info "fetching tracks for user" id "with opts" opts)
        data (spotify/get-current-users-recently-played-tracks opts access_token)]
    (spit "sample.edn" (with-out-str (pr data)))
    (timbre/info "persisting" (-> data :items count) "tracks for user" id)
    #_(persist-all-data id data)
    (persist-all-data-sql data ext-id)))

