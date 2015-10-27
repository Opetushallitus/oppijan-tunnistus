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
            [hiccup.middleware :refer [wrap-base-url]]))

(s/defschema ValidityResponse {:exists s/Bool :valid s/Bool (s/optional-key :email) s/Str})
(s/defschema SendRequest {:url s/Str :email s/Str})

(defroutes* oppijan-tunnistus-routes
            "Oppijan tunnistus API"
            (GET* "/token/:token" [token]
                 :responses {200 {:schema ValidityResponse
                                  :description "Returns token validity and email in case token exists"}}
                 :summary   "Verify token"
                 (response (retrieve-email-and-validity-with-token token)))
            (POST* "/token" req
                  :responses  {200 {:schema s/Str
                                    :description "Verification email sent.
                                    Returns verification-url that is same as callback-url+token."}}
                  :body       [s_req SendRequest]
                  :summary    "Send verification email"
                  (ok (send-verification-link (s_req :email) (s_req :url))))
            (route/not-found "Page not found"))

(defroutes* doc-routes
            "API documentation browser"
            (swagger-docs {:info {:title "Oppijan tunnistus API"}})
            (swagger-ui :swagger-docs "/oppijan-tunnistus/swagger/swagger.json"))

(defapi oppijan-tunnistus-api
        (context* "/oppijan-tunnistus" []
                  (route/resources "/")
                  (context* "/api/v1" [] oppijan-tunnistus-routes)
                  (context* "/swagger" [] doc-routes)))

