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