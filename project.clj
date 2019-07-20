(defproject kino "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "The MIT License"
            :url "http://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.danielsz/system "0.4.3"]
                 [compojure "1.6.1"]
                 [environ"1.1.0"]
                 [ring/ring-defaults "0.3.2"]
                 [ring-middleware-format "0.7.4"]
                 [http-kit "2.4.0-alpha3"]
                 [juxt/crux "19.07-1.1.1-alpha"]
                 [org.rocksdb/rocksdbjni "6.0.1"]
                 [clj-spotify "0.1.9"]
                 [hiccup "1.0.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [clojurewerkz/quartzite "2.1.0"]
                 [javax.xml.bind/jaxb-api "2.4.0-b180830.0359"]]
  :plugins [[lein-environ "1.0.0"]]
  :profiles {:dev {:source-paths ["dev"]
                   :env {:http-port 3000}}
             :prod {:env {:http-port 8000
                          :repl-port 8001}
                    :dependencies [[org.clojure/tools.nrepl "0.2.13"]]}
             :uberjar {:aot :all}}
  :main ^:skip-aot kino.core
  :repl-options {:init-ns user})
