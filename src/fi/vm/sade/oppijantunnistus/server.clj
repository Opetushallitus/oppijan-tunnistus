(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.config :refer [config]]
          [compojure.core]
          [fi.vm.sade.oppijantunnistus.oppijan-tunnistus])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response]]
            [ring.util.http-response :refer [ok]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [hiccup.middleware :refer [wrap-base-url]]))

(defroutes oppijan-tunnistus-routes
           (GET  (str (-> config :server :base-url) "/verify/:token") [token]
             (response (retrieve-email-and-validity-with-token token)))
           (POST (str (-> config :server :base-url) "/verify") {params :params}
             (send-verification-link (params :email) (params :url))
             (ok))
           (route/not-found "Page not found"))

(def app
  (-> (handler/site oppijan-tunnistus-routes)
      (wrap-json-response)
      (wrap-json-params)
      (wrap-base-url)))
