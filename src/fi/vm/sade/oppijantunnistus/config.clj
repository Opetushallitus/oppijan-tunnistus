(ns fi.vm.sade.oppijantunnistus.config
  (:require [environ.core :refer [env]]
            [clojure.edn :as edn]))


(when-not *compile-files*
  (def cfg (-> (or (env :oppijantunnistus-properties) "config/reference.edn")
               (slurp)
               (edn/read-string))))
