(ns kino.oauth
  (:require [cheshire.core :as json]
            [taoensso.timbre :as timbre]
            [org.httpkit.client :as http])
  (:import (java.net URLEncoder)))

(defn url-encode [url]
  (URLEncoder/encode url "UTF-8"))

;; thank you
;; http://leonid.shevtsov.me/post/oauth2-is-easy/

(defn ->oauth2-params [settings]
  {:client-id (:spotify-client-id settings)
   :client-secret (:spotify-client-secret settings)
   :authorize-uri  "https://accounts.spotify.com/authorize"
   :redirect-uri (str (:app-host settings) "/oauth/callback")
   :access-token-uri "https://accounts.spotify.com/api/token"
   :scope "user-read-recently-played"})

(defn authorize-uri
  [settings csrf-token]
   (let [client-params (->oauth2-params settings)]
     (str
       (:authorize-uri client-params)
       "?response_type=code"
       "&client_id="
       (url-encode (:client-id client-params))
       "&redirect_uri="
       (url-encode (:redirect-uri client-params))
       "&scope="
       (url-encode (:scope client-params))
       "&state="
       (url-encode csrf-token))))

(defn get-authentication-response [settings csrf-token response-params]
  (let [oauth2-params (->oauth2-params settings)]
    (if (= csrf-token (:state response-params))
      (let [options {:url        (:access-token-uri oauth2-params)
                     :method     :post
                     :form-params
                     {:code         (:code response-params)
                      :grant_type   "authorization_code"
                      :redirect_uri (:redirect-uri oauth2-params)}
                     :basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]
                     }
            {:keys [status error body] :as res} @(http/request options)]
        (if (= status 200)
          (json/parse-string body true)
          (timbre/error "Couldn't get auth response" status error body)))
      nil)))

(defn get-access-token [settings refresh-token]
  (let [oauth2-params (->oauth2-params settings)
        options {:url (:access-token-uri oauth2-params)
                 :method :post
                 :form-params
                 {:grant_type "refresh_token"
                  :refresh_token refresh-token}
                 :basic-auth [(:client-id oauth2-params) (:client-secret oauth2-params)]}
            {:keys [status error body] :as res} @(http/request options)]
        (if (= status 200)
          (:access_token (json/parse-string body true))
          (timbre/error "Couldn't get access token" status error body))))

