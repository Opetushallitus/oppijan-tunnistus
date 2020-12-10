(ns fi.vm.sade.oppijantunnistus.auth.cas-client
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [clj-http.client :as http-client]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log])
  (:import [fi.vm.sade.utils.cas CasClient CasParams]
           (org.http4s.client.blaze package$)))

(def csrf-value "oppijantunnistus")
(def caller-id "1.2.246.562.10.00000000001.oppijantunnistus.backend")

(defn enrich-with-mandatory-headers-and-common-settings [opts]
  (-> opts
      (update :connection-timeout (fnil identity 60000))
      (update :socket-timeout (fnil identity 60000))
      (assoc  :throw-exceptions false)
      (update :headers merge
              {"Caller-Id" caller-id}
              {"CSRF" csrf-value})
      (update :cookies merge {"CSRF" {:value csrf-value :path "/"}})))

(defrecord CasClientState [client params session-cookie-name session-id])

(defn new-cas-client []
  (new CasClient
       (-> cfg :host-virkailija)
       (.defaultClient package$/MODULE$)
       caller-id))

(defn new-client [service security-uri-suffix session-cookie-name]
  {:pre [(some? (:cas cfg))]}
  (let [username   (get-in cfg [:cas :username])
        password   (get-in cfg [:cas :password])
        cas-params (CasParams/apply service security-uri-suffix username password)
        cas-client (new-cas-client)]
    (map->CasClientState {:client              cas-client
                          :params              cas-params
                          :session-cookie-name session-cookie-name
                          :session-id          (atom nil)})))

(defn- request-with-json-body [request body]
  (-> request
      (assoc-in [:headers "Content-Type"] "application/json")
      (assoc :body (json/generate-string body))))

(defn- create-params [session-cookie-name cas-session-id body]
  (cond-> {:cookies          {session-cookie-name  {:value @cas-session-id :path "/"}}
           :redirect-strategy :none
           :throw-exceptions false}
          (some? body) (request-with-json-body body)))

(defn do-request
  [{:keys [url method] :as opts}]
  (let [method-name (string/upper-case (name method))
        opts        (enrich-with-mandatory-headers-and-common-settings opts)
        start       (System/currentTimeMillis)
        response    (http-client/request opts)
        time        (- (System/currentTimeMillis) start)
        status      (:status response 500)]
    (when (or (<= 400 status) (< 1000 time))
      (log/warn "HTTP" method-name url status (str time "ms")))
    response))

(defn- cas-http [client method url opts & [body]]
  (let [cas-client          (:client client)
        cas-params          (:params client)
        session-cookie-name (:session-cookie-name client)
        cas-session-id      (:session-id client)]
    (when (nil? @cas-session-id)
      (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name))))
    (let [resp (do-request (merge {:url url :method method}
                                            opts
                                            (create-params session-cookie-name cas-session-id body)))]
      (if (or (= 401 (:status resp))
              (= 302 (:status resp)))
        (do
          (reset! cas-session-id (.run (.fetchCasSession cas-client cas-params session-cookie-name)))
          (do-request (merge {:url url :method method}
                                       opts
                                       (create-params session-cookie-name cas-session-id body))))
        resp))))

(defn cas-authenticated-get [client url]
  (cas-http client :get url {}))
