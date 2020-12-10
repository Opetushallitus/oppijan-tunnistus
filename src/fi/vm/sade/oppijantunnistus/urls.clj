(ns fi.vm.sade.oppijantunnistus.urls
  (:require [fi.vm.sade.oppijantunnistus.config :refer [cfg]]))

(defn kayttooikeus-service-kayttooikeus-kayttaja-url [username]
  (str (-> cfg :host-virkailija) "/kayttooikeus-service/kayttooikeus/kayttaja?username=" username))

(defn redirect-to-login-failed-page-url []
  (str (-> cfg :host-virkailija) "/oppijan-tunnistus/virhe"))

(defn oppijantunnistus-login-url []
  (str (-> cfg :host-virkailija) "/oppijan-tunnistus/auth/cas"))

(defn cas-login-url []
  (let [host (-> cfg :host-virkailija)]
    (str host "/cas/login?service=" host "/oppijan-tunnistus/auth/cas")))
(defn cas-logout-url []
  (let [host (-> cfg :host-virkailija)]
    (str host "/cas/logout?service=" host "/oppijan-tunnistus/auth/cas")))

(defn cas-redirect-url []
  (str (-> cfg :host-virkailija) "/oppijan-tunnistus/auth/checkpermission"))
