(ns fi.vm.sade.oppijantunnistus.config
  (:use   [clojure.edn]
          [environ.core :refer [env]]))


(defonce config (-> (or (env :oppijantunnistus-properties) "config/reference.edn")
                    (slurp)
                    (clojure.edn/read-string)))
