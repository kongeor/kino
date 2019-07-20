(ns kino.loggly
  (:require [clojure.string :as string]
            [taoensso.encore :as enc]
            [cheshire.core :as json]
            [org.httpkit.client :as http]
            [taoensso.timbre :as timbre]))

(defn data->json [data]
  (json/encode
    (merge (:context data)
           {:level (:level data)
            :namespace (:?ns-str data)
            :file (:?file data)
            :line (:?line data)
            :stacktrace (some-> (:?err data) (timbre/stacktrace {:stacktrace-fonts {}}))
            :hostname (force (:hostname_ data))
            :message (force (:msg_ data))
            :timestamp (force (:timestamp_ data))})))

(defn loggly-appender [& [opts]]
  (let [{:keys [token tags]} opts]
    {:enabled? true
     :async? true
     :rate-limit [[1 (enc/ms :secs 1)]]
     :fn (fn [data]
           (let [{:keys [output_]} data
                 tag (string/join "," (map name (if (nil? tags) '[:timbre] tags)))
                 url (str "http://logs-01.loggly.com/inputs/" token "/tag/" tag "/")
                 {:keys [status error body] :as res} @(http/request
                                                        {:url url
                                                         :body (data->json data) #_(force output_)
                                                         :method :post
                                                         :headers {"Content-Type" "application/json"}})]
             #_(println "loggly res: " status error body)))}))
