(ns fi.vm.sade.oppijantunnistus.db.query
  (:require [yesql.core :refer [defquery]]))

(defquery add-secure-link! "sql/insert.sql")

(defquery get-secure-link "sql/get.sql")