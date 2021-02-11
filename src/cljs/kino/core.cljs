(ns kino.core
  (:require [re-frame.core :refer [dispatch dispatch-sync clear-subscription-cache!]]
            [reagent.core :as reagent]
            [reagent.dom :as dom]
            [kino.router :as router]
            [kino.events]
            [kino.subs]
            [kino.views]))

(defn ^:export main []
  (dispatch-sync [:kino.events/initialize-db])
  (router/start!)
  (dom/render [kino.views/kino-app]
    (.getElementById js/document "app")))