(ns fi.vm.sade.oppijantunnistus.main
  (:use [fi.vm.sade.oppijantunnistus.server :only [handler]]
        [ring.adapter.jetty :only [run-jetty]])
  (:require [ring.middleware.reload :as reload])
  (:gen-class))

(defn -main [& args]
  (run-jetty handler {:port 8080}))
