(ns fi.vm.sade.oppijantunnistus.main
  (:use [fi.vm.sade.oppijantunnistus.server :only [oppijan-tunnistus-api]]
        [ring.adapter.jetty :only [run-jetty]]
        [fi.vm.sade.oppijantunnistus.config :refer [cfg]])
  (:require [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main [& args]
  (log/info "Using configuration: " cfg)
  (log/info "Running db migrations")
  (db/migrate "db.migration")
  (log/info "Starting server")
  (run-jetty oppijan-tunnistus-api {:port (-> cfg :server :port)
                                    :context "/api/v1"}))
