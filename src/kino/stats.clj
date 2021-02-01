(ns kino.stats
  (:require [kino.ndb :as ndb]))

(defn ->part-split [pred]
  (let [a (atom nil)
        idx (atom 1)]
    (fn [e]
      (let [res (pred @a e)]
        (reset! a e)
        (if res
          @idx
          (swap! idx inc))))))

(defn split-by-conseq-plays [data]
  (partition-by
    (->part-split (fn [p c]
                    (or (nil? p)
                        (let [v (-> p :album_id)
                              t (-> p :track_number)
                              v1 (-> c :album_id)
                              t1 (-> c :track_number)]
                          (and (= v v1)
                               (= t (inc t1))
                               #_(or (= t (inc t1))
                                   #_(= t t1)))))))
    data))

(defn album-plays [db uid]
  (let [data (ndb/get-recent-user-plays db uid :cnt 2000)]
    (->>
      (split-by-conseq-plays data)
      #_(filter #(> (count %) 1))
      #_(map #(-> % first :kino.play/track :kino.track/album))
      (map (fn [a]
             (let [tracks (map #(-> % :track_number) a)]
               {:album_name (-> a first :album_name)
                :img_url (-> a first :img_url)
                :tracks tracks
                :play-ratio (/ (count tracks) (-> a first :total_tracks))
                :played-at (first (map :played_at a))})))
      (filter #(> (-> % :play-ratio) 0.4)))))



#_(album-plays :08uc4dh5sl6f8888eydkq2sbz)

