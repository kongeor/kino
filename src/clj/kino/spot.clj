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


(defn persist-all-track-data [db data ext-user-id]
  (let [items (:items data)]
    (doall
      (for [item items]
        (let [track-data (get-track-data (:track item))
              played-at (-> item :played_at util/iso-date-str->instant)]
          #_(timbre/info "persisting track" track-data played-at ext-user-id)
          (ndb/insert-all-track-data db track-data played-at ext-user-id))))))

(defn persist-all-playlist-data [db items playlist-id]
  (doseq [item items]
    (let [track-data (get-track-data (:track item))]
      (timbre/info "persisting playlist" playlist-id "track")
      (ndb/insert-all-playlist-track-data db track-data playlist-id))))

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
    (persist-all-track-data db data ext-id)))

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
    (def fetched-playlists data)
    (timbre/info "persisting" (-> data :items count) "playlists for user" id)
    #_(persist-all-data id data)
    (doall
      (for [playlist (:items data)]
        (do
          (println "!!!" playlist)
          (let [playlist (persist-playlist db playlist)
                playlist-ext-id (:external_id playlist)
                playlist-id (:id playlist)
                playlist-tracks (util/multi-page-fetch spotify/get-a-playlists-tracks {:user_id ext-id :playlist_id playlist-ext-id} access_token)] ;; TODO fetch moar
            (println "playlist tracks " playlist-tracks)
            (persist-all-playlist-data db playlist-tracks playlist-id))))
      #_(for [playlist (:items data)]
        (let [
              playlist (persist-playlist db playlist)
              playlist-id (:id playlist)
              playlist-tracks (spotify/get-a-playlists-tracks {:user_id ext-id :playlist_id playlist-id} access_token)]
          (persist-all-playlist-data db playlist-tracks playlist-id))))))

(comment
  (let [db (:database.sql/connection integrant.repl.state/system)
        user (first (ndb/get-users db))]
    (fetch-and-persist-playlists db user)
    #_(persist-all-playlist-data db playlist-tracks 28)))

(comment
  #_(:items playlist-tracks)
  #_playlist-tracks
  fetched-playlists
  )


(comment
  (let [{:keys [external_id refresh_token]} (->
             (ndb/get-users (:database.sql/connection integrant.repl.state/system))
             first)]
    (def access-token (oauth/get-access-token refresh_token))))

(comment
  (let [playlist-id "66HzIUP99db7gkdECJJiBC"]
    (util/multi-page-fetch spotify/get-a-playlists-tracks {:user_id ext-id :playlist_id playlist-ext-id} access_token)))

(comment
  (let [playlist-id "66HzIUP99db7gkdECJJiBC"
        db (ndb/get-users (:database.sql/connection integrant.repl.state/system))
        {:keys [external_id] :as user} (-> db first)
        _ (println user)
        access-token (oauth/get-access-token (:refresh_token user))
        ; playlists (spotify/get-a-list-of-a-users-playlists {:user_id external_id} access-token)
        ]
    #_(spotify/get-a-playlists-tracks {:user_id external_id :playlist_id playlist-id} access-token)
    #_(util/multi-page-fetch spotify/get-a-playlists-tracks {:user_id external_id :playlist_id playlist-id} access-token)))

(comment
  (let [db (:database.sql/connection integrant.repl.state/system)
        user (-> (ndb/get-users db) second)]
    (fetch-and-persist-playlists db user)))

(comment
  (let [playlist-id "2L9nuwnZNJQl628262x524"
        db (:database.sql/connection integrant.repl.state/system)
        user (first (ndb/get-users db))
        external-id (:external_id user)
        access_token (oauth/get-access-token (:refresh_token user))
        playlist-tracks' (spotify/get-a-playlists-tracks {:user_id external-id :playlist_id playlist-id} access_token)]
    (def playlist-tracks playlist-tracks')
    #_(persist-all-track-data db playlist-tracks external-id)))

(comment
  (-> playlist-tracks :items first :track get-track-data))

(comment
  (let [db (:database.sql/connection integrant.repl.state/system)
        user (-> (ndb/get-users db) first)
        api-playlists (:items fetched-playlists)
        api-playlists-ids (into #{} (map #(:id %) api-playlists))
        local-playlists (ndb/get-user-playlists db (:id user))
        local-playlists-ids (into #{} (map #(:external_id %) local-playlists))
        ]

    ;; playlists that were removed on spotify
    #_(filter (fn [p] (not (contains? api-playlists-ids (:external_id p)))) local-playlists)

    ;; playlists that do not exist locally
    #_(filter (fn [p] (not (contains? local-playlists-ids (:id p)))) api-playlists)

    ;; playlists that exist locally but have changed
    #_(filter (fn [p]
              (if-let [existing (first (filter #(= (:id p) (:external_id %)) local-playlists))]
                (not= (:snapshot_id p) (:snapshot_id existing)))) api-playlists)

    #_(filter #(= "zep" (:name %)) api-playlists)

    ;;
    #_(filter #(contains? local-playlists-ids %) (map (fn [p] [(:id p) (:snapshot_id p)]) api-playlists))
    ; api-playlists-ids
    ; local-playlists
    ; local-playlists-ids
    ))

(comment
  (contains? #{[1 1] [1 2]} [1 2]))

(comment
  (ndb/get-user-by-ext-id
    "08uc4dh5sl6f8888eydkq2sbz"))