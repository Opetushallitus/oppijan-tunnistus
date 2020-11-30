(ns fi.vm.sade.oppijantunnistus.auth.session-timeout
  (:require [cheshire.core :as json]
            [clojure.string :as string]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [ring.middleware.session-timeout :as session-timeout]
            [ring.util.http-response :as response]))

(defn- timeout-handler [auth-url]
  (fn [{:keys [uri]}]
    (if (string/starts-with? uri "/oppijantunnistus/api")
      (response/unauthorized (json/generate-string {:redirect auth-url}))
      (response/found auth-url))))

(defn- timeout-options []
  {:timeout         (get-in cfg [:session :timeout] 28800)
   :timeout-handler (timeout-handler (-> cfg :cas.login))})

(defn wrap-idle-session-timeout []
  (fn [handler]
    (let [options (timeout-options)]
      (session-timeout/wrap-idle-session-timeout handler options))))
