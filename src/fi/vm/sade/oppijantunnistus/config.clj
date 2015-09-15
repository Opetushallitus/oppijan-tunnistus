(ns fi.vm.sade.oppijantunnistus.config
  (:use   [clojure.edn]
          [environ.core :refer [env]]))


(defonce config (-> (or (env :config) "config/reference.edn")
                    (slurp)
                    (clojure.edn/read-string)))
