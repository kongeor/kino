(ns kino.core
  (:require [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [kino.router :as router]
            [kino.events]
            [kino.subs]
            [kino.views]))

(defn ^:export main []
  (dispatch [:kino.events/initialize-db])
  (dispatch [:kino.events/fetch-current-user])
  (router/start!)
  (dom/render [kino.views/kino-app]
    (.getElementById js/document "app")))