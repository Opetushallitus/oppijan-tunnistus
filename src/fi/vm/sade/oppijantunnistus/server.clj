(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.oppijan-tunnistus])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response]]
            [ring.util.http-response :refer [ok unauthorized!]]
            [cheshire.core :refer [parse-string]]
            [fi.vm.sade.oppijantunnistus.urls :as urls]
            [environ.core :refer [env]]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db]
            [compojure.api.sweet :refer [undocumented middleware api defroutes GET POST context]]
            [compojure.api.swagger :refer [swagger-docs swagger-ui]]
            [fi.vm.sade.oppijantunnistus.auth.auth :as auth]
            [fi.vm.sade.oppijantunnistus.auth.cas-client :as cas]
            [clj-ring-db-session.session.session-store :refer [create-session-store]]
            [clj-ring-db-session.authentication.login :as crdsa-login]
            [fi.vm.sade.oppijantunnistus.auth.session-timeout :as session-timeout]
            [clj-ring-db-session.session.session-client :as session-client]
            [clj-ring-db-session.authentication.auth-middleware :as crdsa-auth-middleware]
            [compojure.route :as route]
            [ring.middleware.session :as ring-session]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [ring.swagger.schema :as rs]
            [hiccup.middleware :refer [wrap-base-url]]))

(defn- dev? []
  (= (:dev? env) "true"))

(defn check-authorization! [session]
  (when-not (or (dev?)
                (some #(= "oppijantunnistus-crud" %) (-> session :identity :rights)))
    (log/error "Missing user rights: " (-> session :identity :rights))
    (unauthorized!)))

(s/defschema ValidityResponse {:exists s/Bool
                               :valid s/Bool
                               (s/optional-key :email) s/Str
                               (s/optional-key :lang) s/Str
                               (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Str})})
(s/defschema OnlyTokenRequest {:url (rs/describe s/Str "Base URL for secure link.")
                               :email (rs/describe s/Str "Recipient email address.")
                               (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                               (s/optional-key :lang) (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                               (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Str})})
(s/defschema SendRequest {:url (rs/describe s/Str "Base URL for secure link.")
                          :email (rs/describe s/Str "Recipient email address.")
                          (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                          (s/optional-key :subject) (rs/describe s/Str "Email subject when template is used.")
                          (s/optional-key :template) (rs/describe s/Str "Email template in moustache format. Moustache template should contain key {{verification-link}} for 'link + token' placeholder. Other optional parameters are 'submit_time', 'expires'.")
                          (s/optional-key :lang) (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                          (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Str})})
(s/defschema TokensRequest {:url (rs/describe s/Str "Base URL for secure links.")
                            :templatename (rs/describe s/Str "Template name for email. Template with this name should exist in Viestintäpalvelu and it must have replacement with name 'securelink'")
                            :lang (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                            :applicationOidToEmailAddress (rs/describe {s/Keyword s/Str} "Map of application oids to email addresses")
                            :hakuOid (rs/describe s/Str "hakuOid for the current token")
                            :letterId (rs/describe s/Str "letter id for the letter batch for which these messages are generated for")
                            (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                            (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Str})})
(s/defschema TokenResponse {:email s/Str
                            :securelink s/Str})
(s/defschema TokensResponse {:recipients [{:email s/Str
                                           :securelink s/Str}]} )

(defn- create-wrap-database-backed-session [session-store]
  (fn [handler]
    (ring-session/wrap-session handler
                               {:root         "/oppijantunnistus"
                                :cookie-attrs {:secure (not (dev?))}
                                :store        session-store})))

(defroutes oppijan-tunnistus-routes
            "Oppijan tunnistus API"
            (GET "/token/:token" [token session]
                 :responses {200 {:schema ValidityResponse
                                  :description "Returns token validity and email in case token exists"}}
                 :summary   "Verify token"
                 (do (check-authorization! session)
                     (log/info "Verifying token" token)
                     (response (retrieve-email-and-validity-with-token token))))
            (POST "/only_token" {session :session}
                   :responses  {200 {:schema TokenResponse
                                     :description "Verification email sent.
                                    Returns verification-url that is same as callback-url+token."}}
                   :body       [s_req OnlyTokenRequest]
                   :summary    "Create verification token. Doesn't send email."
                   (do (check-authorization! session)
                       (log/info "Creating verification token to email" (s_req :email))
                       (ok (create-verification-link (s_req :email) (s_req :url) (s_req :metadata) (s_req :lang) (s_req :expires)))))
            (POST "/token" {session :session}
                   :responses  {200 {:schema s/Str
                                    :description "Verification email sent.
                                    Returns verification-url that is same as callback-url+token."}}
                  :body       [s_req SendRequest]
                  :summary    "Send verification email"
                  (do (check-authorization! session)
                      (log/info "Sending verification link to email" (s_req :email))
                      (ok (send-verification-link (s_req :email) (s_req :url) (s_req :metadata) (s_req :lang) (s_req :template) (s_req :subject) (s_req :expires)))))
            (GET "/preview/haku/:haku-oid/template/:template-name/lang/:lang" [template-name haku-oid lang session :as req]
                   :path-params [template-name :- s/Str haku-oid :- s/Str lang :- s/Str]
                   :query-params [callback-url :- s/Str]
                   :summary    "Preview verification email"
                   (do (check-authorization! session)
                       (log/info "Preview verification link email" (:params  req))
                       (let [email (ryhmasahkoposti-preview callback-url template-name lang haku-oid)]
                         {:status 200
                          :headers {"Content-Type" "message/rfc822; charset=UTF-8"
                                    "Content-Disposition" (str "inline; filename=\"example-" template-name "-" lang "-" haku-oid ".eml\"")}
                          :body email})))
            (POST "/tokens" {session :session}
                  :responses  {200 {:schema TokensResponse
                                    :description "Sends multiple verification emails using given template.
                                    Returns a list of verification-urls (callback-url+token)."}}
                  :body        [s_req TokensRequest]
                  :summary     "Send multiple verification emails"
                  (do (check-authorization! session)
                      (log/info "Sending multiple verification emails")
                      (response {:recipients (send-verification-links s_req)})))
            (route/not-found "Page not found"))

(defn auth-routes [login-cas-client
                   kayttooikeus-cas-client
                   session-store]
  (context "/auth" []
    (middleware [session-client/wrap-session-client-headers]
                (undocumented
                  (GET "/checkpermission" {session :session}
                    (ok (:superuser session)))
                  (GET "/cas" [ticket :as request]
                    (let [redirect-url (or (get-in request [:session :original-url])
                                           (urls/cas-redirect-url))
                          login-provider (auth/cas-login @login-cas-client ticket)]
                      (auth/login login-provider
                                  redirect-url
                                  @kayttooikeus-cas-client)))
                  (POST "/cas" [logoutRequest]
                    (auth/cas-initiated-logout logoutRequest session-store))
                  (GET "/logout" {session :session}
                    (crdsa-login/logout session (urls/cas-logout-url)))))))

(defn new-api []
  (let [login-cas-client (delay (delay (cas/new-cas-client)))
        session-store (create-session-store db/datasource)
        kayttooikeus-cas-client (delay (cas/new-client "/kayttooikeus-service" "j_spring_cas_security_check"
                                                       "JSESSIONID"))]
    (api {:swagger      {:spec    "/oppijan-tunnistus/swagger/swagger.json"
                         :ui      "/oppijan-tunnistus/swagger/api-docs"
                         :data    {:info {:version     "0.1.0"
                                          :title       "Oppijan tunnistus API"
                                          :description "Token verification API For OPH"}
                                   :tags [{:name "oppijantunnistus" :description "Oppijan tunnistus API"}]}
                         :options {:ui {:validatorUrl nil}}}
          :ring-swagger {:ignore-missing-mappings? true}}
         (context "/oppijan-tunnistus" []
           (route/resources "/")

           (middleware
             [(create-wrap-database-backed-session session-store)
              (when-not (dev?)
                #(crdsa-auth-middleware/with-authentication % (urls/cas-login-url)))]
             (middleware [session-client/wrap-session-client-headers
                          (session-timeout/wrap-idle-session-timeout)]
                         (context "/api/v1" [] oppijan-tunnistus-routes))
             (auth-routes login-cas-client kayttooikeus-cas-client session-store))))))
