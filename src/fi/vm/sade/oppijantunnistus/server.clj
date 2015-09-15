(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.config :refer [config]])
  (:require [ring.middleware.logger :as logger]
            [ring.middleware.reload :as reload]
            [clj-util.cas :as cas]
            [clojure.data.json :as json]
            [ring.middleware.conditional :refer [if-url-doesnt-match]]
            [compojure.handler :refer [site]]
            [clojure.tools.logging :as log]))



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
