(ns kino.spot
  (:require [clj-spotify.core :as spotify]
            [system.repl :refer [system]]
            [kino.ndb :as ndb]
            [kino.oauth :as oauth]
            [kino.util :as util]
            [taoensso.timbre :as timbre]
            [kino.util :as util])
  (:import (java.security MessageDigest)))


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


(defn persist-all-data-sql [db data ext-user-id]
  (let [items (:items data)]
    (doall
      (for [item items]
        (let [track-data (get-track-data (:track item))
              played-at (-> item :played_at util/iso-date-str->instant)]
          #_(timbre/info "persisting track" track-data played-at ext-user-id)
          (ndb/insert-all-track-data db track-data played-at ext-user-id))))))

#_(db/get-entity :36053687af37294a87a6121267aa6e17)

(defn fetch-and-persist [db {id :id ext-id :external_id refresh-token :refresh_token}]
  (let [access_token (oauth/get-access-token refresh-token)
        last-played-at (-> (ndb/get-last-played-track db id) :played_at)
        opts {:limit 50}
        opts (if last-played-at (assoc opts :after (inst-ms last-played-at)) opts)
        _ (timbre/info "fetching tracks for user" id "with opts" opts)
        data (spotify/get-current-users-recently-played-tracks opts access_token)]
    (timbre/info "persisting" (-> data :items count) "tracks for user" id)
    #_(persist-all-data id data)
    (persist-all-data-sql db data ext-id)))

(defn format-playlist-for-db [sp-pl]
  {:name (:name sp-pl)
   :external_id (:id sp-pl)
   :external_owner_id (-> sp-pl :owner :id)
   :snapshot_id (:snapshot_id sp-pl)
   :public (:public sp-pl)
   :total_tracks (-> sp-pl :tracks :total)
   :type (:type sp-pl)
   :collaborative (:collaborative sp-pl)
   :description (:description sp-pl)
   :img_url (-> sp-pl :images first :url)})

(defn persist-playlist [db spotify-playlist]
  (let [playlist (format-playlist-for-db spotify-playlist)
        user (ndb/get-user-by-ext-id db (:external_owner_id playlist))
        playlist' (assoc playlist :owner_id (:id user))]
    (ndb/insert-playlist db playlist')))

(defn fetch-and-persist-playlists [db {id :id ext-id :external_id refresh-token :refresh_token}]
  (let [access_token (oauth/get-access-token refresh-token)
        opts {:user_id ext-id :limit 50}                    ;; TODO fetch moar
        _ (timbre/info "fetching playlists for user" id "with opts" opts)
        data (spotify/get-a-list-of-a-users-playlists opts access_token)]
    (timbre/info "persisting" (-> data :items count) "playlists for user" id)
    #_(persist-all-data id data)
    (doall
      (for [playlist (:items data)]
        (persist-playlist db playlist)))))

(comment
  (let [{:keys [external_id refresh_token]} (->
             (ndb/get-users (:database.sql/connection integrant.repl.state/system))
             first)]
    (def access-token (oauth/get-access-token refresh_token))))

(comment
  (let [db (ndb/get-users (:database.sql/connection integrant.repl.state/system))
        {:keys [external_id]} (-> db first)
        playlists (spotify/get-a-list-of-a-users-playlists {:user_id external_id} access-token)]
    playlists))

(comment
  (let [db (:database.sql/connection integrant.repl.state/system)
        user (-> (ndb/get-users db) first)]
    (fetch-and-persist-playlists db user)))

(comment
  (ndb/get-user-by-ext-id
    "08uc4dh5sl6f8888eydkq2sbz"))