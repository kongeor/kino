(ns kino.db
  (:require [cljs.reader]
            [re-frame.core :refer [reg-cofx]]))

(def default-db {:plays []
                 :active-page {:page :home}})

