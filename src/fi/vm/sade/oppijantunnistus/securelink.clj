(ns fi.vm.sade.oppijantunnistus.securelink
  (:require [yesql.core :refer [defquery]]
            [fi.vm.sade.oppijantunnistus.db :as db]
            [pandect.algo.sha256 :refer :all])
  (:import [java.security SecureRandom]))

(def random (SecureRandom.))

(defn generate-token []
  (sha256 (.generateSeed random (/ 512 8))))

(defquery add-secure-link! "sql/insert.sql")

(defn add [valid_until email token callback_url]
  (->> (db/exec add-secure-link! {:valid_until valid_until
                                  :email email
                                  :token token
                                  :callback_url callback_url})))

(defquery get-secure-link "sql/get.sql")

(defn get [token]
  (->> (db/exec get-secure-link {:token token}) first))

