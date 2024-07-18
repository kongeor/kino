(ns kino.util
  (:require [clojure.string :as string]))

(defn iso-date-str->instant [s]
  (java.time.Instant/parse s))