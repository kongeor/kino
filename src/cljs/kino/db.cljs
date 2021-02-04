(ns kino.db
  (:require [cljs.reader]
            [re-frame.core :refer [reg-cofx]]))

(def default-db {:active-page {:page :home}})

