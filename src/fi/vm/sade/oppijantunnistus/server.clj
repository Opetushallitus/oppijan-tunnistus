(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.oppijan-tunnistus])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response]]
            [ring.util.http-response :refer [ok]]
            [cheshire.core :refer [parse-string]]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.route :as route]
            [clojure.tools.logging :as log]
            [schema.core :as s]
            [ring.swagger.schema :as rs]
            [hiccup.middleware :refer [wrap-base-url]]))

(s/defschema ValidityResponse {:exists s/Bool
                               :valid s/Bool
                               (s/optional-key :email) s/Str
                               (s/optional-key :lang) s/Str
                               (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Keyword})})
(s/defschema SendRequest {:url (rs/describe s/Str "Base URL for secure link.")
                          :email (rs/describe s/Str "Recipient email address.")
                          (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                          (s/optional-key :subject) (rs/describe s/Str "Email subject when template is used.")
                          (s/optional-key :template) (rs/describe s/Str "Email template in moustache format. Moustache template should contain key {{verification-link}} for 'link + token' placeholder. Other optional parameters are 'submit_time', 'expires'.")
                          (s/optional-key :lang) (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                          (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Keyword})})
(s/defschema TokensRequest {:url (rs/describe s/Str "Base URL for secure links.")
                            :templatename (rs/describe s/Str "Template name for email. Template with this name should exist in Viestintäpalvelu and it must have replacement with name 'securelink'")
                            :lang (rs/describe s/Str "Email language in ISO-639-1 format. E.g. 'en','fi','sv'.")
                            :applicationOidToEmailAddress (rs/describe {s/Keyword s/Str} "Map of application oids to email addresses")
                            :hakuOid (rs/describe s/Str "hakuOid for the current token")
                            (s/optional-key :expires) (rs/describe Long "Expiration date as unix timestamp (long milliseconds).")
                            (s/optional-key :metadata) (s/conditional map? {s/Keyword s/Keyword})})
(s/defschema TokensResponse {:recipients [{:email s/Str
                                           :securelink s/Str}]} )

(defroutes* oppijan-tunnistus-routes
            "Oppijan tunnistus API"
            (GET* "/token/:token" [token]
                 :responses {200 {:schema ValidityResponse
                                  :description "Returns token validity and email in case token exists"}}
                 :summary   "Verify token"
                 (do (log/info "Verifying token" token)
                     (response (retrieve-email-and-validity-with-token token))))
            (POST* "/token" req
                  :responses  {200 {:schema s/Str
                                    :description "Verification email sent.
                                    Returns verification-url that is same as callback-url+token."}}
                  :body       [s_req SendRequest]
                  :summary    "Send verification email"
                  (do (log/info "Sending verification link to email" (s_req :email))
                      (ok (send-verification-link (s_req :email) (s_req :url) (s_req :metadata) (s_req :lang) (s_req :template) (s_req :subject) (s_req :expires)))))
            (POST* "/tokens" req
                  :responses  {200 {:schema TokensResponse
                                    :description "Sends multiple verification emails using given template.
                                    Returns a list of verification-urls (callback-url+token)."}}
                  :body        [s_req TokensRequest]
                  :summary     "Send multiple verification emails"
                  (do (log/info "Sending multiple verification emails")
                      (response {:recipients (send-verification-links s_req)})))
            (route/not-found "Page not found"))

(defroutes* doc-routes
            "API documentation browser"
            (swagger-docs {:info {:title "Oppijan tunnistus API"}})
            (swagger-ui :swagger-docs "/oppijan-tunnistus/swagger/swagger.json"))

(defapi oppijan-tunnistus-api
        {:ring-swagger {:ignore-missing-mappings? true}}
        (context* "/oppijan-tunnistus" []
                  (route/resources "/")
                  (context* "/api/v1" [] oppijan-tunnistus-routes)
                  (context* "/swagger" [] doc-routes)))
