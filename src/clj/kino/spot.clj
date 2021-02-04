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
    (spit "sample.edn" (with-out-str (pr data)))
    (timbre/info "persisting" (-> data :items count) "tracks for user" id)
    #_(persist-all-data id data)
    (persist-all-data-sql db data ext-id)))

