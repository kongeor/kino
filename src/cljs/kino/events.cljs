(ns kino.events
  (:require
   [kino.db :refer [default-db]]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx trim-v after path debug]]
   [kino.router :as router]
   [day8.re-frame.http-fx] ;; even if we don't use this require its existence will cause the :http-xhrio effect handler to self-register with re-frame
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   [ajax.core :refer [json-request-format json-response-format]]
   [clojure.string :as str]))


(reg-event-fx
 ::initialize-db
 (fn-traced [_ _]
  {:db default-db
   :dispatch-n [[::fetch-current-user]]}))

(reg-event-db
 ::set-active-page
 (fn-traced [db [_ active-panel]]
  (assoc db :active-page active-panel)))

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
 (fn-traced [db [_ result]]
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
