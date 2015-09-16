(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.config :refer [config]]
          [fi.vm.sade.oppijantunnistus.oppijan-tunnistus])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response]]
            [ring.util.http-response :refer [ok]]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes GET POST context]]
            [compojure.route :as route]
            [schema.core :as s]
            [hiccup.middleware :refer [wrap-base-url]]))

(s/defschema ValidityResponse {:valid s/Bool (s/optional-key :email) s/Str})
(s/defschema SendRequest {:url s/Str :email s/Str})

(defroutes* oppijan-tunnistus-routes
            "Oppijan tunnistus API"
           (GET* "/verify/:token" [token]
                 :responses {200 {:schema ValidityResponse
                                  :description "Returns token validity and email in case token exists"}}
                 :summary   "Verify token"
                 (response (retrieve-email-and-validity-with-token token)))
           (POST* "/verify" []
                  :responses  {200 {:description "Verification email sent"}}
                  :body       [sndReq SendRequest]
                  :summary    "Send verification email"
                  (send-verification-link (sndReq :email) (sndReq :url))
                  (ok))
           (route/not-found "Page not found"))

(defroutes* doc-routes
            "API documentation browser"
            (swagger-docs {:info {:title "Oppijan tunnistus API"}})
            (swagger-ui :swagger-docs "/oppijan-tunnistus/swagger/swagger.json"))

(defapi app
        (context* "/oppijan-tunnistus" []
                  (context* "/api/v1" [] oppijan-tunnistus-routes)
                  (context* "/swagger" [] doc-routes)))

