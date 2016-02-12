(ns fi.vm.sade.oppijantunnistus.fake_server
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response created content-type header set-cookie]]
            [ring.util.http-response :refer [ok internal-server-error! internal-server-error]]
            [compojure.api.sweet :refer :all]
            [compojure.core :refer [defroutes GET POST context]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log]
            [clojure.data.json :refer [write-str read-str]]
            [schema.core :as s]
            [cemerick.url :refer [url]]
            [ring.adapter.jetty :refer [run-jetty]]))

(def port
  (-> (url (-> cfg :ryhmasahkoposti :url))
      (get :port)))

(def ^:private server_on (atom true))

(defn enable_server [enable?] (reset! server_on enable?))
;write-str {:id 3773}
(defroutes* ryhmasahkoposti_routes
            (POST "/ryhmasahkoposti-service/email/firewall" {body :body} (if @server_on
                                                                  (let [message (read-str (slurp body))]
                                                                    (log/info "Ryhmasahkoposti Received Message" message)
                                                                    (if (not (clojure.string/blank? ((message "email") "body")))
                                                                      (-> (response {:id "facebabe"})
                                                                            (content-type "application/json;charset=utf-8"))
                                                                      (-> (internal-server-error!) (header "Content-Type" "application/json;charset=utf-8"))))
                                                      (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8")))))

(defapi fake_server
        ryhmasahkoposti_routes)

(defonce ^:dynamic *fake_app* (atom nil))

(defn start-fake-app []
  (log/info "Starting fake ryhmasahkoposti in URL" (-> cfg :ryhmasahkoposti :url))
  (if (not (nil? @*fake_app*))
    (.stop @*fake_app*))
  (reset! *fake_app* (run-jetty fake_server {:port port :join? false} )))

(defn stop-fake-app []
  (.stop @*fake_app*))
