(ns kino.events
  (:require
    [kino.db :refer [default-db]]
    [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]
    [kino.router :as router]
    [day8.re-frame.http-fx] ;; even if we don't use this require its existence will cause the :http-xhrio effect handler to self-register with re-frame
    [day8.re-frame.tracing :refer-macros [fn-traced]]
    [ajax.core :refer [json-request-format json-response-format]]
    [clojure.string :as str]))


(reg-event-db
  ::initialize-db
  (fn-traced [db _]
    (assoc db :initialized? true)))

(defn calc-fetching-event [active-page]
  (let [page (:page active-page)
        playlist-id (-> active-page :route-params :id)]
    (cond
      (and (= :playlists page) (not (nil? playlist-id))) [::fetch-playlist-tracks playlist-id]
      (= :playlists page) [::fetch-user-playlists])))

(reg-event-fx
  ::set-active-page
  (fn-traced [{:keys [db]} [_ active-panel]]
    (let [evt (calc-fetching-event active-panel)
          data {:db (assoc db :active-page active-panel)}]
      (if evt
        (assoc data :dispatch evt)
        data))))

(reg-event-fx
  ::fetch-current-user
  (fn-traced [{:keys [db]} [_ _]]
    {:db   (assoc db :fetching-current-user true)
     :http-xhrio {:method          :get
                  :uri             (str "/api/me")
                  :timeout         8000                                           ;; optional see API docs
                  :response-format (json-response-format {:keywords? true})  ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-current-user-result]
                  :on-failure      [::failed-current-user-result]}}))

(reg-event-fx
  ::success-current-user-result
  (fn-traced [{:keys [db]} [_ result]]
    {:db (assoc db :user result :fetching-current-user false)
     :dispatch [::fetch-user-plays]}))

(reg-event-db
  ::failed-current-user-result
  (fn-traced [db [_ _]]
    (assoc db :user nil :fetching-current-user false)))

(reg-event-fx
  ::fetch-user-plays
  (fn-traced [{:keys [db]} [_ _]]
    (let [last-played-at (-> db :plays last :played_at)]
      {:db         (assoc db :fetching-user-plays true)
       :http-xhrio {:method          :get
                    :uri             (str "/api/plays?before=" (or last-played-at ""))
                    :timeout         8000                      ;; optional see API docs
                    :response-format (json-response-format {:keywords? true}) ;; IMPORTANT!: You must provide this.
                    :on-success      [::success-user-plays-result]
                    :on-failure      [::failed-user-plays-result]}})))

(reg-event-db
  ::success-user-plays-result
  (fn-traced [db [_ result]]
    (assoc db :plays (concat (:plays db) result) :fetching-user-plays false)))

(reg-event-db
  ::failed-user-plays-result
  (fn-traced [db [_ _]]
    (assoc db :fetching-user-plays false)))

; playlists

(reg-event-fx
  ::fetch-user-playlists
  (fn-traced [{:keys [db]} [_ _]]
    {:db         (assoc db :fetching-user-playlists true)
     :http-xhrio {:method          :get
                  :uri             "/api/playlists"
                  :timeout         8000                       ;; optional see API docs
                  :response-format (json-response-format {:keywords? true}) ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-user-playlists-result]
                  :on-failure      [::failed-user-playlists-result]}}))

(reg-event-db
  ::success-user-playlists-result
  (fn-traced [db [_ result]]
    (assoc db :playlists result :fetching-user-playlists false)))

(reg-event-db
  ::failed-user-playlists-result
  (fn-traced [db [_ _]]
    (assoc db :fetching-user-playlists false)))

(reg-event-fx
  ::fetch-playlist-tracks
  (fn-traced [{:keys [db]} [_ playlist-id]]
    {:db         (assoc db :fetching-playlist-tracks true)
     :http-xhrio {:method          :get
                  :uri             (str "/api/playlists/" playlist-id "/tracks")
                  :timeout         8000                       ;; optional see API docs
                  :response-format (json-response-format {:keywords? true}) ;; IMPORTANT!: You must provide this.
                  :on-success      [::success-playlist-tracks-result playlist-id]
                  :on-failure      [::failed-playlist-trakcs-result]}}))

(reg-event-db
  ::success-playlist-tracks-result
  (fn-traced [db [_ playlist-id result] ]
    (assoc-in (assoc db :fetching-playlist-tracks false)
      [:playlist playlist-id :tracks] result)))

(reg-event-db
  ::failed-playlist-trakcs-result
  (fn-traced [db [_ _]]
    (assoc db :fetching-playlist-tracks false)))
