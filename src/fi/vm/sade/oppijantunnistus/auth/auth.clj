(ns fi.vm.sade.oppijantunnistus.auth.auth
  (:require [clj-ring-db-session.authentication.login :as crdsa-login]
            [clj-ring-db-session.session.session-store :as crdsa-session-store]
            [ring.util.http-response :refer [ok]]
            [ring.util.response :as resp]
            [fi.vm.sade.oppijantunnistus.urls :as urls]
            [cheshire.core :as json]
            [fi.vm.sade.oppijantunnistus.auth.cas-client :as cas]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log])
  (:import (fi.vm.sade.utils.cas CasLogout)))

(defn cas-login [cas-client ticket request]
  (fn []
    (if-not ticket
      (throw (ex-info (str "No service ticket found in request " request)
                      {:request request}))
      [(.run (.validateServiceTicketWithVirkailijaUsername
               cas-client (urls/oppijantunnistus-login-url) ticket))
       ticket])))

(defn- login-failed
  ([e]
   (log/error e "Error in login ticket handling")
   (resp/redirect (urls/redirect-to-login-failed-page-url))))

(def oph-organization "1.2.246.562.10.00000000001")

(def ^:private
  oikeus-to-right
  {{:palvelu "OPPIJANTUNNISTUS" :oikeus "CRUD"}         :oppijantunnistus-crud})

(defn virkailija->right-organization-oids
  [virkailija]
  (select-keys (->> (:organisaatiot virkailija)
                    (mapcat (fn [{:keys [organisaatioOid kayttooikeudet]}]
                              (map (fn [right] {right [organisaatioOid]})
                                   (keep oikeus-to-right kayttooikeudet))))
                    (reduce (partial merge-with concat) {}))
               (vals oikeus-to-right)))

(defn- login-succeeded [response virkailija]
  (let [organization-oids (set (map (fn [{:keys [organisaatioOid]}]
                                      organisaatioOid) (:organisaatiot virkailija)))
        rights (set (map first (virkailija->right-organization-oids virkailija)))]
    (update-in
      response
      [:session :identity]
      assoc
      :oid (:oidHenkilo virkailija)
      :rights rights
      :superuser (contains? organization-oids oph-organization))))

(defn fetch-kayttaja-from-kayttoikeus-service [kayttooikeus-cas-client username]
  (let [url (urls/kayttooikeus-service-kayttooikeus-kayttaja-url username)
        {:keys [status body]} (cas/cas-authenticated-get kayttooikeus-cas-client url)]
    (if (= 200 status)
      (if-let [virkailija (first (json/parse-string body true))]
        virkailija
        (throw (new RuntimeException
                    (str "No virkailija found by username " username))))
      (throw (new RuntimeException
                  (str "Could not get virkailija by username " username
                       ", status: " status
                       ", body: " body))))))

(defn login [login-provider
             redirect-url
             kayttooikeus-cas-client]
  (try
    (if-let [[username ticket] (login-provider)]
      (let [virkailija (fetch-kayttaja-from-kayttoikeus-service kayttooikeus-cas-client username)
            response (crdsa-login/login
                       {:username             username
                        :ticket               ticket
                        :success-redirect-url redirect-url})]
        (login-succeeded response virkailija))
      (login-failed (ex-info "login-provider did not provide credentials" {})))
    (catch Throwable e
      (login-failed e))))

(defn cas-initiated-logout [logout-request session-store]
  (log/info "cas-initiated logout")
  (let [ticket (CasLogout/parseTicketFromLogoutRequest logout-request)]
    (log/info "logging out ticket" ticket)
    (if (.isEmpty ticket)
      (log/error "Could not parse ticket from CAS request" logout-request)
      (crdsa-session-store/logout-by-ticket! session-store (.get ticket)))
    (ok)))
