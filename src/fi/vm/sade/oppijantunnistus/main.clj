(ns fi.vm.sade.oppijantunnistus.main
  (:use [fi.vm.sade.oppijantunnistus.server :only [handler]]
        [ring.adapter.jetty :only [run-jetty]]
        [fi.vm.sade.oppijantunnistus.config :refer [config]])
  (:require [ring.middleware.reload :as reload]
            [ring.middleware.logger :as logger]
            [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.db :as db]
            [fi.vm.sade.oppijantunnistus.securelink :as securelink])
  (:gen-class))

(defn -main [& args]
  (log/info "Using configuration: " config)
  (log/info "Running db migrations")
  (db/migrate "db.migration")
  (log/info (securelink/listaa))
  (log/info "Starting server")
  (run-jetty handler {:port 8080}))
