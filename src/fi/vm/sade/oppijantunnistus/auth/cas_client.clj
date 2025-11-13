(ns fi.vm.sade.oppijantunnistus.auth.cas-client
  (:require [fi.vm.sade.oppijantunnistus.config :refer [cfg]])
  (:import [fi.vm.sade.javautils.nio.cas CasClientBuilder CasConfig$CasConfigBuilder]))

(def csrf-value "oppijantunnistus")
(def caller-id "1.2.246.562.10.00000000001.oppijantunnistus.backend")

(defrecord CasClientState [client session-cookie-name session-id])

(defn new-cas-client []
  (let [{username :username
         password :password
         cas-url :url} (-> cfg :cas)
        cas-config (-> (new CasConfig$CasConfigBuilder username password cas-url "" csrf-value caller-id "")
                       (.setJsessionName "JSESSIONID")
                       (.build))
        cas-client (CasClientBuilder/build cas-config)]
    cas-client))
