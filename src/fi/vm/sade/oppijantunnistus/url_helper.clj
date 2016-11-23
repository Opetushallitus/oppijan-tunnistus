(ns fi.vm.sade.oppijantunnistus.url-helper
  (:require [fi.vm.sade.oppijantunnistus.config :refer [cfg]])
  (:import [fi.vm.sade.properties OphProperties]))

(when-not *compile-files*
  (defonce oph-properties
    (let [host-virkailija (:host-virkailija cfg)]
      (doto (OphProperties. (into-array String ["/oppijan-tunnistus-oph.properties"]))
            (.addDefault "url-virkailija" host-virkailija)))))

(defn url
  ([key] (url key []))
  ([key params]
    (.url oph-properties (name key) (to-array params))))