(ns fi.vm.sade.oppijantunnistus.server
  (:use   [clojure.edn]
          [environ.core :refer [env]])
  (:require [ring.middleware.logger :as logger]
            [ring.middleware.reload :as reload]
            [clj-util.cas :as cas]
            [clojure.data.json :as json]
            [ring.middleware.conditional :refer [if-url-doesnt-match]]
            [compojure.handler :refer [site]]
            [clojure.tools.logging :as log]))

(defonce config (-> (or (env :config) "config/reference.edn")
                    (slurp)
                    (clojure.edn/read-string)))

(cas/set-cas (-> config :cas :url))

(defn handler [request]
  (do
    (println (-> config :email))
    @(cas/post
       (apply cas/cas-params (-> config :ryhmasahkoposti :params))
       (-> config :ryhmasahkoposti :url)
       (json/write-str (-> config :message))
       :content-type ["application" "json"])
    {:status 200
     :headers {"Content-Type" "text/html"}
     :body "Hello World"}))
