(ns fi.vm.sade.oppijantunnistus.fake_server
  (:require [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.util.response :refer [response created content-type header set-cookie]]
            [ring.util.http-response :refer [ok internal-server-error! internal-server-error]]
            [compojure.api.sweet :refer [defapi undocumented middleware api defroutes GET POST context]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log]
            [clojure.data.json :refer [write-str read-str]]
            [schema.core :as s]
            [cemerick.url :refer [url]]
            [ring.adapter.jetty :refer [run-jetty]]
            [fi.vm.sade.oppijantunnistus.url-helper :as url-helper]))

(def port
  (-> (url (:host-virkailija cfg))
      (get :port)))

(def ^:private server_on (atom true))

(defn enable_server [enable?] (reset! server_on enable?))

(defn ^:private fake-email-sender [body headers cookies]
  (if (and @server_on)
    (let [message (read-str (slurp body))]
      (log/info "Ryhmasahkoposti Received Message" message)
      (if (not (clojure.string/blank? ((message "email") "body")))
        (-> (response {:id "facebabe"})
            (content-type "application/json;charset=utf-8"))
        (if (not (clojure.string/blank? ((message "email") "templateName")))
          (-> (response {:id "facebabe"})
              (content-type "application/json;charset=utf-8"))
          (-> (internal-server-error!) (header "Content-Type" "application/json;charset=utf-8"))
          )))
    (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8"))))

;write-str {:id 3773}
(defroutes ryhmasahkoposti_routes
  (POST "/ryhmasahkoposti-service/email/firewall" {:keys [headers params body cookies] :as request}
    (fake-email-sender body headers cookies))
  (POST "/ryhmasahkoposti-service/email/async/firewall" {:keys [headers params body cookies] :as request}
    (fake-email-sender body headers cookies))
  (POST "/ryhmasahkoposti-service/email/preview/firewall" {:keys [headers params body cookies] :as request}
    (if (not (= "CSRF" ((get cookies "CSRF") :value)))
      (do (
           (log/error "CSRF cookie not set")
           (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8")))
          ))
    (if (not (= "CSRF" (get headers "csrf")))
      (do ((log/error "CSRF header not set")
           (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8")))))
    (if (and @server_on (and (contains? headers "clientsubsystemcode")
                             (contains? headers "caller-id")))
      (let [message (read-str (slurp body))]
        (log/info "Ryhmasahkoposti Received Preview Message" message)
        (if (not (clojure.string/blank? ((message "email") "body")))
          (-> (response (str "Message-ID: EMAIL from body"))
              (content-type "plain/text;charset=utf-8"))
          (if (not (clojure.string/blank? ((message "email") "templateName")))
            (-> (response (str "Message-ID: EMAIL from template"))
                (content-type "plain/text;charset=utf-8"))
            (-> (internal-server-error!) (header "Content-Type" "application/json;charset=utf-8"))
            )))
      (-> (internal-server-error) (header "Content-Type" "application/json;charset=utf-8")))))

(use 'ring.middleware.cookies)
(defapi fake_server
        (wrap-cookies ryhmasahkoposti_routes))

(defonce ^:dynamic *fake_app* (atom nil))

(defn start-fake-app []
  (log/info "Starting fake ryhmasahkoposti in URL"
    (url-helper/url "ryhmasahkoposti-service.email.firewall"))
  (if (not (nil? @*fake_app*))
    (.stop @*fake_app*))
  (reset! *fake_app* (run-jetty fake_server {:port port :join? false} )))

(defn stop-fake-app []
  (.stop @*fake_app*))
