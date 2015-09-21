(ns fi.vm.sade.oppijantunnistus.fake_cas_server
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response created header set-cookie]]
            [ring.util.http-response :refer [ok]]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes GET POST context]]
            [clojure.tools.logging :as log]
            [ring.adapter.jetty :refer [run-jetty]]))

(def ^:private port 3000)
;(+ 10000 (rand-int 50000)))

(def cas_url (str "http://localhost:" port))

(log/info "Starting fake ryhmasahkoposti with mocked CAS in URL" cas_url)

(defroutes* ryhmasahkoposti_cas_routes
            (POST "/cas/v1/tickets" [] (created "/cas/v1/tickets/TGT-123"))
            (POST "/cas/v1/tickets/TGT-123" [] (ok "ST-123"))
            (POST "/ryhmasahkoposti-service/email" [] (ok))
            (GET "/ryhmasahkoposti-service/j_spring_cas_security_check" [] (->
                                                                             (ok)
                                                                             (header "Set-Cookie" "JSESSIONID=foobar-123"))))
(defapi fake_cas_server
        ryhmasahkoposti_cas_routes)

(defonce ^:dynamic *cas_app* (atom nil))

(defn start-cas-app []
  (if (not (nil? @*cas_app*))
    (.stop @*cas_app*))
  (reset! *cas_app* (run-jetty fake_cas_server {:port port})))

(defn stop-cas-app []
  (.stop @*cas_app*))
