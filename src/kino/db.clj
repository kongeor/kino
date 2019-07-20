(ns kino.db
  (:require [crux.api :as crux]
            [system.repl :refer [system]]))

(defn get-entity [id]
  (let [sys (-> system :db :db)]
    (crux/entity (crux/db sys) id)))

(defn get-entities [ids]
  (->>
    (crux/q (crux/db (-> system :db :db))
            {:find '[?e]
             :where '[[?e :crux.db/id ?]
                      [(get ?ids ?e)]]
             :args [{'?ids ids}]})
    (map first)
    (into #{})))

#_(get-entities #{:5vxBOzakDbJleNA1rbA7FQ})

(defn- entity-data
  ([entities]
   (entity-data (-> system :db :db) entities))
  ([system entities]
    (let [sys (partial (crux/db system))]
      (map #(crux/entity sys (first %)) entities))))

(defn get-users []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :type "user"]]})))

#_(get-users)

(defn get-artists []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :kino.artist/name ?]]})))

#_(get-artists)

(defn get-albums []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :kino.album/name ?]]})))

#_(get-albums)

(defn get-tracks []
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       '{:find [e]
                         :where [[e :kino.track/name ?]]})))

(comment
  (->>
    (entity-data
      (-> system :db :db)
      (crux/q (crux/db (-> system :db :db))
              '{:find [e]
                :where [[e :kino.play/track-id ?t]
                        [e1 :kino.play/track-id ?t]
                        [e :kino.play/user-id u1]
                        [e1 :kino.play/user-id u2]
                        [(not= e e1)]
                        [(not= u1 u2)]]}))
    (map (fn [e] [(:kino.play/track-id e)]))
    distinct
    (entity-data (-> system :db :db))))

#_ (get-tracks)

; (def data (get-tracks))

#_(->> (group-by :kino.track/album-id data)
     (map (fn [[k v]] [k (sort (map :kino.track/number v))])))

#_(partition-by (fn [v] (or (> 3 v) (> v 6))) [1 2 3 4 5 6 7 8 ])


(defn get-plays [user-id & {:keys [offset limit] :or {offset 0 limit 50}}]
  (entity-data (-> system :db :db)
               (crux/q (crux/db (-> system :db :db))
                       {:find '[?e ?played-at]                 ;; TODO ask about this
                        :where [['?e :kino.play/user-id user-id]
                                ['?e :kino.play/played-at '?played-at]]
                        :order-by [['?played-at :desc]]
                        :limit limit
                        :offset offset
                        })))

#_(get-plays :08uc4dh5sl6f8888eydkq2sbz :limit 2 :offset 10)

(comment
  (defn naughty []
    (let [all
          (->>
            (get-plays :08uc4dh5sl6f8888eydkq2sbz :limit 200 :offset 0)
            (map #(-> % :kino.play/track-id))
            (map get-entity))
          cnt (count all)
          expl (count (filter :kino.track/explicit all))]
      (/ expl cnt))))

(defn get-last-play [user-id]
  (get-plays user-id :limit 1))

#_(get-last-play :08uc4dh5sl6f8888eydkq2sbz)

(defn get-play-data [user-id]
  (->> (get-plays user-id)
       (map #(assoc % :kino.play/track (get-entity (:kino.play/track-id %))))
       (map #(assoc-in % [:kino.play/track :kino.track/album] (get-entity (-> % :kino.play/track :kino.track/album-id))))
       (map (fn [t] (assoc-in t [:kino.play/track :kino.track/artists] (mapv get-entity (-> t :kino.play/track :kino.track/artist-ids)))))))

(comment
  (def data (get-play-data :08uc4dh5sl6f8888eydkq2sbz))

  (map #(-> % :kino.play/track :kino.track/album :kino.album/name) data))

(comment

  (defn ->part-split [pred]
    (let [a (atom nil)]
      (fn [e]
        (let [res (pred @a e)]
          (reset! a e)
          res))))

  (->>
    (partition-by
      (->part-split (fn [p c]
                      (or (nil? p)
                          (let [v (-> p :kino.play/track :kino.track/album-id)
                                v1 (-> c :kino.play/track :kino.track/album-id)]
                               (= v v1)))))
      data)
    (filter #(> (count %) 1))
    #_(map #(-> % first :kino.play/track :kino.track/album))
    (map (fn [a]
           [(-> a first :kino.play/track :kino.track/album)
            (map #(-> % :kino.play/track :kino.track/number) a)]))))

#_(-> system :db :db)

(def user-keys [:display_name :type])

(defn upsert-user [user refresh_token]
  (let [user-data (select-keys user user-keys)
        id (-> user :id keyword)
        user-data (assoc user-data :crux.db/id id :kino.user/refresh-token refresh_token)
        existing (get-entity id)]
    (if existing
      existing
      (do
        (crux/submit-tx
          (-> system :db :db)
          [[:crux.tx/put
            user-data]])
        user-data))))

