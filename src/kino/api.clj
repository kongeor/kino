(ns kino.api
  (:require [cheshire.core :as json]
            [clojure.string :as str]
            [kino.ndb :as ndb]
            [kino.util :as util]
            [org.httpkit.client :as http]
            [taoensso.timbre :as log])
  (:import (java.time Instant)))

(defn request [opts]
  (let [{:keys [status body]} @(http/request opts)]
    (if (= status 200)
      (json/parse-string body true)
      (throw (ex-info "request failed" {:opts opts
                                        :status status
                                        :body body})))))

(defn find-album [album-name artist]
  (let [album (->
                (request {:url
                          "https://musicbrainz.org/ws/2/release"
                          :query-params {:query (str "recording:'" album-name "' AND artist:'" artist "'")
                                         :fmt "json"}})
                :releases
                first
                )]
    (if (nil? album)
      (throw (ex-info "album not found" {:album album-name
                                         :artist artist}))
      (select-keys album [:id :title :track-count :artist-credit]))))

(comment
  (find-album "For Django" "Joe Pass"))

(comment
  (find-album "When The Wind Forgets Your Name" "Built To Spill"))

(defn find-track [release-id track-name]
  ;; remove not needed inc
  (let [tracks (-> (request {:url (str "https://musicbrainz.org/ws/2/release/" release-id "?fmt=json&inc=artist-credits+labels+discids+recordings")})
                   :media
                   first
                   :tracks)]
    (prn (map #(dissoc % :artist-credit) tracks))
    (let [;track (first (filter #(= (:title %) track-name) tracks))
          ;; let's be brave for now
          track (first (sort-by #(util/levenshtein (:title %) track-name) tracks))]
      (if (nil? track)
        (throw (ex-info "track not found" {:release-id release-id
                                           :track-name track-name}))
        track))))

(comment
  (find-track "eead80ca-d51d-4b2c-ba60-cba4443b3ccd" "For Django"))

(comment
  (find-track "da4d0df8-b8c7-40b7-a5af-4cf530b84fef" "Fool's Gold"))

(comment
  (str/replace "Fool's" "'" "’"))

(comment
  (def tracks [{:length 466000, :artist-credit [{:joinphrase "", :artist {:type "Person", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :sort-name "Tame Impala"}, :name "Tame Impala"}], :title "Let It Happen", :position 1, :id "53cf81bc-0508-4775-bf34-3385d7ea5a1e", :number "1", :recording {:id "b85f7f6d-813a-4ebc-8a1d-5998ab02be9a", :first-release-date "2015-03-11", :video false, :disambiguation "", :title "Let It Happen", :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}}], :length 466893}} {:number "2", :recording {:disambiguation "", :video false, :id "c9f0e0e0-3789-4a78-a91e-03bc0c3ea998", :first-release-date "2015-07-15", :artist-credit [{:joinphrase "", :artist {:id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala", :type "Person", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df"}, :name "Tame Impala"}], :title "Nangs", :length 107000}, :id "54c28aa8-d8ce-4f7d-ae86-b6fdd02dcdec", :position 2, :length 107000, :title "Nangs", :artist-credit [{:artist {:name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :type "Person", :sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation ""}, :joinphrase "", :name "Tame Impala"}]} {:artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala"}}], :title "The Moment", :length 255000, :position 3, :id "b8d150d1-7419-41ad-b0d7-3cf738c506c7", :recording {:title "The Moment", :artist-credit [{:joinphrase "", :artist {:disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :sort-name "Tame Impala", :type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala"}, :name "Tame Impala"}], :length 255000, :id "700b5bca-f8a0-4a3b-b260-1751016dc30b", :video false, :first-release-date "2015-07-15", :disambiguation ""}, :number "3"} {:recording {:title "Yes I’m Changing", :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}}], :length 270000, :first-release-date "2015-07-15", :id "173324ec-808e-467a-bab8-0fa3a78bf90d", :video false, :disambiguation ""}, :number "4", :id "ab2e573b-cb34-4aa0-94e5-f01fc0dad4a4", :position 4, :title "Yes I’m Changing", :artist-credit [{:joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :type "Person"}, :name "Tame Impala"}], :length 270000} {:length 319000, :artist-credit [{:name "Tame Impala", :artist {:sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :type "Person"}, :joinphrase ""}], :title "Eventually", :number "5", :recording {:length 319000, :artist-credit [{:artist {:id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala", :type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala"}, :joinphrase "", :name "Tame Impala"}], :title "Eventually", :disambiguation "", :id "9db8c054-4379-4f1a-8b5e-5d6b1c2b0f3a", :video false, :first-release-date "2015-05-08"}, :position 5, :id "d1b70927-a897-4cd1-af23-cfa8035bac0e"} {:id "0489b8ba-5745-4fc6-92cf-734529d95cb1", :position 6, :number "6", :recording {:disambiguation "", :id "e030d5d2-f8d2-4495-bfcc-c3af7eef6286", :first-release-date "2015-07-15", :video false, :length 55000, :artist-credit [{:joinphrase "", :artist {:id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala", :type "Person", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df"}, :name "Tame Impala"}], :title "Gossip"}, :length 55000, :title "Gossip", :artist-credit [{:artist {:type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :sort-name "Tame Impala"}, :joinphrase "", :name "Tame Impala"}]} {:position 7, :id "0d444855-db97-4f9e-b956-89ab055cc021", :recording {:length 217000, :title "The Less I Know the Better", :artist-credit [{:artist {:id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala", :type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala"}, :joinphrase "", :name "Tame Impala"}], :video false, :id "78be434c-0908-458f-9693-f16cdebb68ef", :first-release-date "2015-07-15", :disambiguation ""}, :number "7", :artist-credit [{:joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}, :name "Tame Impala"}], :title "The Less I Know the Better", :length 218000} {:title "Past Life", :artist-credit [{:name "Tame Impala", :artist {:name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :type "Person", :sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d"}, :joinphrase ""}], :length 227000, :id "aad970f8-251b-417a-b300-9c0512591d40", :position 8, :recording {:disambiguation "", :id "5eb905ff-22a9-42ee-8985-699f8c1e18b3", :first-release-date "2015-07-15", :video false, :length 227000, :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:type "Person", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :sort-name "Tame Impala"}}], :title "Past Life"}, :number "8"} {:id "165625c2-7c76-4ef5-859e-65092d1916c5", :position 9, :recording {:length 108000, :title "Disciples", :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person", :sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d"}}], :id "9cbbdb07-8942-48c9-b80e-568149744453", :first-release-date "2015-05-01", :video false, :disambiguation ""}, :number "9", :title "Disciples", :artist-credit [{:joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}, :name "Tame Impala"}], :length 108000} {:length 241000, :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:type "Person", :name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :sort-name "Tame Impala"}}], :title "’Cause I’m a Man", :position 10, :id "4c0bb546-063c-4941-a09e-bdc5536a983a", :number "10", :recording {:disambiguation "", :first-release-date "2015-04-07", :id "aee5f1d1-5c05-4d5b-a7a8-79ee4bc2c506", :video false, :length 241000, :artist-credit [{:joinphrase "", :artist {:sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation "", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}, :name "Tame Impala"}], :title "Cause I’m a Man"}} {:position 11, :id "25f7d697-4f86-44ec-8326-a68a4dfe73ce", :recording {:id "9f3e2a21-0897-4a56-ba03-3c36628337ef", :video false, :first-release-date "2015-07-15", :disambiguation "", :length 252000, :title "Reality in Motion", :artist-credit [{:joinphrase "", :artist {:type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person", :sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d"}, :name "Tame Impala"}]}, :number "11", :artist-credit [{:name "Tame Impala", :joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}}], :title "Reality in Motion", :length 252000} {:artist-credit [{:name "Tame Impala", :artist {:name "Tame Impala", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :type "Person", :sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation ""}, :joinphrase ""}], :title "Love/Paranoia", :length 186000, :recording {:title "Love/Paranoia", :artist-credit [{:artist {:type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person", :sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation ""}, :joinphrase "", :name "Tame Impala"}], :length 186000, :id "88d35fac-b78b-41b5-a4b1-4e75ac0f30e6", :first-release-date "2015-07-15", :video false, :disambiguation ""}, :number "12", :position 12, :id "cec17f48-1a0a-478b-86ee-a2e8fe53bcba"} {:number "13", :recording {:disambiguation "", :first-release-date "2015-07-15", :id "f078699c-9b68-4040-9ba4-b1e4f7d86598", :video false, :length 362000, :artist-credit [{:artist {:type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person", :sort-name "Tame Impala", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :disambiguation ""}, :joinphrase "", :name "Tame Impala"}], :title "New Person, Same Old Mistakes"}, :id "2e4c3aa5-7002-43a0-a62a-c294d5d7d002", :position 13, :length 362000, :title "New Person, Same Old Mistakes", :artist-credit [{:joinphrase "", :artist {:sort-name "Tame Impala", :disambiguation "", :id "63aa26c3-d59b-4da4-84ac-716b54f1ef4d", :type-id "b6e035f4-3ce9-331c-97df-83397230b0df", :name "Tame Impala", :type "Person"}, :name "Tame Impala"}]}]))

(comment
  (map #(dissoc % :artist-credit) tracks))

(comment
  (levenshtein "The Less I Know The Better" "The Less I Know the Better"))

(comment
  (map #(levenshtein (:title %) "The Less I Know The Better") (map #(dissoc % :artist-credit) tracks)))

(comment
  (sort-by #(util/levenshtein (:title %) "The Less I Know The Better") (map #(dissoc % :artist-credit) tracks)))

(defn track-data->spot-fmt [track-data]
  (let [{:keys [id title position] :as _track} track-data]
    {:id id
     :name title
     :explicit false                                        ;; don't have this
     :track_number position}))

(comment
  (track-data->spot-fmt (find-track "eead80ca-d51d-4b2c-ba60-cba4443b3ccd" "For Django 1")))

(defn find-album-cover [release-id]
  (try
    (let [response (request {:url (str "https://coverartarchive.org/release/" release-id)})]
      (:large (:thumbnails (first (filter :front (:images response))))))
    (catch Exception e
      (log/warn e (str "Finding album cover failed for " release-id " Exception data" (ex-data e))))))

(comment
  (find-album-cover "eead80ca-d51d-4b2c-ba60-cba4443b3ccd"))

(defn get-data-for-listen
  "Spot compatible format"
  [artist-name track-name release-name]
  (let [{:keys [id title track-count artist-credit] :as _album} (find-album release-name artist-name)]
    {:track (track-data->spot-fmt (find-track id track-name))
     :album {:id id
             :name title
             :total_tracks track-count
             :img_url (find-album-cover id)}
     :artists (map #(select-keys (:artist %) [:name :id]) artist-credit)}))

;; maybe fuzzy search?
(defn replace-chars [s]
  s
  #_(str/replace s "'" "’"))

(defn submit-listens [db api-token body]
  #_(def body body)
  (if-let [user (ndb/get-user-by-api-key db api-token)]
    (try
      (let [type (:listen_type body)
            _ (prn "type" type body)
            ;; TODO handle many
            {:keys [artist_name
                    track_name
                    release_name]} (-> body :payload first :track_metadata)
            _ (println "searching " release_name artist_name)
            data (get-data-for-listen (replace-chars artist_name)
                                      (replace-chars track_name)
                                      (replace-chars release_name))
            ]

        (ndb/insert-all-track-data db data (Instant/now) (:external_id user))
        )
      (catch Exception e
        (log/error e)))
    {:status 401
     :body {:message "Invalid api-key"}})
  )
