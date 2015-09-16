(ns fi.vm.sade.oppijantunnistus.db.token
  (:require [pandect.algo.sha256 :refer :all])
  (:import [java.security SecureRandom]))

(def random (SecureRandom.))

(defn generate-token []
  (sha256 (.generateSeed random (/ 512 8))))
