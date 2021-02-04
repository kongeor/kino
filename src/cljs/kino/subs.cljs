(ns kino.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))
