(ns fi.vm.sade.oppijantunnistus.fake_cas_server
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response created header set-cookie]]
            [ring.util.http-response :refer [ok internal-server-error]]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes GET POST context]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log]
            [cemerick.url :refer [url]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def port
  (-> (url (-> cfg :cas :url))
      (get :port)))

(def cas_url (str "http://localhost:" port))

(def ^:private server_on (atom true))

(defn enable_server [enable?] (reset! server_on enable?))

(defroutes* ryhmasahkoposti_cas_routes
            (POST "/cas/v1/tickets" [] (created (str cas_url "/cas/v1/tickets/TGT-123")))
            (POST "/cas/v1/tickets/TGT-123" [] (ok "ST-123"))
            (POST "/ryhmasahkoposti-service/email" [] (if @server_on
                                                        (-> (ok) (header "Content-Type" "application/json;charset=utf-8"))
                                                        (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8"))))
            (GET "/ryhmasahkoposti-service/j_spring_cas_security_check" []
              (-> (ok)
                  (header "Set-Cookie" "JSESSIONID=foobar-123"))))

(defapi fake_cas_server
        ryhmasahkoposti_cas_routes)

(defonce ^:dynamic *cas_app* (atom nil))

(defn start-cas-app []
  (log/info "Starting fake ryhmasahkoposti with mocked CAS in URL" cas_url)
  (if (not (nil? @*cas_app*))
    (.stop @*cas_app*))
  (reset! *cas_app* (run-jetty fake_cas_server {:port port :join? false} )))

(defn stop-cas-app []
  (.stop @*cas_app*))
