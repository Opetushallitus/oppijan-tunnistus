(ns fi.vm.sade.oppijantunnistus.urls
  (:require [fi.vm.sade.oppijantunnistus.config :refer [cfg]]))

(defn kayttooikeus-service-kayttooikeus-kayttaja-url [username]
  (str (-> cfg :virkailija-host) "/kayttooikeus-service/kayttooikeus/kayttaja?username=" username))

(defn redirect-to-login-failed-page-url []
  (str (-> cfg :virkailija-host) "/oppijantunnistus/virhe"))

(defn oppijantunnistus-login-url []
  (str (-> cfg :virkailija-host) "/oppijantunnistus/auth/cas"))

(defn cas-login-url []
  (let [host (-> cfg :virkailija-host)]
    (str host "/cas/login?service=" host "/oppijantunnistus/auth/cas")))
(defn cas-logout-url []
  (let [host (-> cfg :virkailija-host)]
    (str host "/cas/logout?service=" host "/oppijantunnistus/auth/cas")))

(defn cas-redirect-url []
  (str (-> cfg :virkailija-host) "/oppijantunnistus/auth/checkpermission"))
