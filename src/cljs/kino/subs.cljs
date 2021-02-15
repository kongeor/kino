(ns kino.subs
  (:require [re-frame.core :refer [reg-sub subscribe]]))

(reg-sub
  :initialized?
  (fn [db _]
    (:initialized? db)))

(reg-sub
 :active-page
 (fn [db _]
   (:active-page db)))

(reg-sub
  ::user
  (fn [db _]
    (:user db)))

(reg-sub
  ::plays
  (fn [db _]
    (:plays db)))

(reg-sub
  ::playlists
  (fn [db _]
    (:playlists db)))
