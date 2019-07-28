(ns kino.stats
  (:require [kino.db :as db]))

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
                        (let [v (-> p :kino.play/track :kino.track/album-id)
                              t (-> p :kino.play/track :kino.track/number)
                              v1 (-> c :kino.play/track :kino.track/album-id)
                              t1 (-> c :kino.play/track :kino.track/number)]
                          (println "-> " v t v1 t1)
                          (and (= v v1)
                               (= t (inc t1))
                               #_(or (= t (inc t1))
                                   #_(= t t1)))))))
    data))

(comment
  (->>
    (db/get-play-data :08uc4dh5sl6f8888eydkq2sbz)
    (map :kino.play/track)
    (map #(select-keys % [:kino.track/album-id :kino.track/number]))
    split-by-conseq-plays
    ))

(defn album-plays [uid]
  (let [data (db/get-play-data uid)]
    (->>
      (split-by-conseq-plays data)
      #_(filter #(> (count %) 1))
      #_(map #(-> % first :kino.play/track :kino.track/album))
      (map (fn [a]
             (let [tracks (map #(-> % :kino.play/track :kino.track/number) a)
                   album (-> a first :kino.play/track :kino.track/album)]
               {:album album
                :tracks tracks
                :play-ratio (/ (count tracks) (:kino.album/total-tracks album))
                :played-at (first (map :kino.play/played-at a))})))
      (filter #(> (-> % :play-ratio) 0.5)))))



#_(album-plays :08uc4dh5sl6f8888eydkq2sbz)

