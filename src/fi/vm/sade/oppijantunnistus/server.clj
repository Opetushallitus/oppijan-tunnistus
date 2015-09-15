(ns fi.vm.sade.oppijantunnistus.server
  (:use   [fi.vm.sade.oppijantunnistus.config :refer [config]]
          [compojure.core])
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response]]
            [compojure.route :as route]
            [compojure.handler :as handler]
            [hiccup.middleware :refer [wrap-base-url]]
            [clj-util.cas :as cas]
            [clojure.data.json :as json]
            [fi.vm.sade.oppijantunnistus.securelink :as securelink]))

(cas/set-cas (-> config :cas :url))

(defroutes oppijan-tunnistus-routes
           (GET "/verifioi/:tunniste" [tunniste]
             (let [entry (securelink/get tunniste)]
               (if entry
                 (response {:email (entry :email) :voimassa true})
                 (response {:voimassa false}))))
           (POST "/verifioi" {params :params}
             (let [email (params :email)
                   callback_url (params :url)
                   token (securelink/generate-token)]
               (println "TOKEN!" token)
               (securelink/add "1967-07-31 06:30:00 America/Caracas" email token callback_url)
               @(cas/post (apply cas/cas-params (-> config :ryhmasahkoposti :params))
                        (-> config :ryhmasahkoposti :url)
                        (json/write-str (-> config :message))
                        :content-type ["application" "json"])
               {:body params}))
           (route/not-found "Page not found"))

(def app
  (-> (handler/site oppijan-tunnistus-routes)
      (wrap-base-url)
      (wrap-json-response)
      (wrap-json-params)))
