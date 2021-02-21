(ns kino.util
  (:require [clojure.string :as string]))

(defn str->int [s]
  (Integer/parseInt s))

(defn trim-to-nil [s]
  (when s
    (let [s' (string/trim s)]
      (if-not (empty? s')
        s'))))

(defn manifest-map
  "Returns the mainAttributes of the manifest of the passed in class as a map."
  [clazz]
  (->> (str "jar:"
            (-> clazz
              .getProtectionDomain
              .getCodeSource
              .getLocation)
            "!/META-INF/MANIFEST.MF")
       clojure.java.io/input-stream
       java.util.jar.Manifest.
       .getMainAttributes
       (map (fn [[k v]] [(str k) v]))
       (into {})))

(defn project-version []
  (if-let [version (System/getProperty "kino.version")]
    version
    (get (manifest-map (Class/forName "kino.core")) "Leiningen-Project-Version")))

(defn iso-date-str->instant [s]
  (java.time.Instant/parse s))


(defn multi-page-fetch [spotify-func params access_token & {:keys [limit] :or {limit 50}}]
  (let [params (assoc params :limit 50)
        data (spotify-func params access_token)
        total (:total data)]
    (loop [offset 0
           params params
           items (:items data)]
      (println ">>>>> " data)
      (if (>= (+ limit offset) total)
        items
        (let [offset (+ offset limit)
              params (assoc params :offset offset)
              data (spotify-func params access_token)]
          (println "fetching " spotify-func "with params" params)
          (recur offset params (concat items (:items data))))))))

(comment
  (loop [i 0
         x []]
    (if (> i 5)
      x
      (recur (inc i) (concat x [i])))))