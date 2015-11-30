(ns fi.vm.sade.oppijantunnistus.config
  (:require [environ.core :refer [env]]
            [clojure.edn :as edn]))

(when-not *compile-files*
  (def logback-access
    (if-let [path (System/getProperty "logback.access")]
      path
      (throw (RuntimeException. "logback.access property not set"))))

  (def cfg (assoc (-> (or (env :oppijantunnistus-properties) "config/reference.edn")
                    (slurp)
                    (edn/read-string))
                  :logback-access logback-access)))
