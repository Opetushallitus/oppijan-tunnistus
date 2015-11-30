(ns fi.vm.sade.oppijantunnistus.main
  (:use [fi.vm.sade.oppijantunnistus.server :only [oppijan-tunnistus-api]]
        [ring.adapter.jetty :only [run-jetty]]
        [fi.vm.sade.oppijantunnistus.config :refer [cfg]])
  (:require [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db])
  (:import ch.qos.logback.access.jetty.RequestLogImpl
           [org.eclipse.jetty.server.handler
            HandlerCollection
            RequestLogHandler])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn- configure-request-log [server]
  (doto server
    (.setHandler (doto (HandlerCollection.)
                   (.addHandler (.getHandler server))
                   (.addHandler (doto (RequestLogHandler.)
                                  (.setRequestLog (doto (RequestLogImpl.)
                                                    (.setFileName (:logback-access cfg))
                                                    (.start)))))))))

(defn -main [& args]
  (log/info "Using configuration: " cfg)
  (log/info "Running db migrations")
  (db/migrate "db.migration")
  (log/info "Starting server")
  (run-jetty oppijan-tunnistus-api {:port (-> cfg :server :port)
                                    :context "/api/v1"
                                    :configurator configure-request-log}))
