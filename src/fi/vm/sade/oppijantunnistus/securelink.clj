(ns fi.vm.sade.oppijantunnistus.securelink
  (:require [yesql.core :refer [defquery]]
            [fi.vm.sade.oppijantunnistus.db :as db]))

(defquery list-securelinks "sql/list.sql")

(defn listaa []
  (->> (db/exec list-securelinks {}) first))
