(ns fi.vm.sade.oppijantunnistus.main
  (:use [fi.vm.sade.oppijantunnistus.server :only [app]]
        [ring.adapter.jetty :only [run-jetty]]
        [fi.vm.sade.oppijantunnistus.config :refer [config]])
  (:require [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db])
  (:gen-class))

(defn -main [& args]
  (log/info "Using configuration: " config)
  (log/info "Running db migrations")
  (db/migrate "db.migration")
  (log/info "Starting server")
  (run-jetty app {:port (-> config :server :port)
                  :context "/api/v1"}))
