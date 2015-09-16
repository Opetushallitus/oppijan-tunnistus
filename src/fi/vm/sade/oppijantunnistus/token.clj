(ns fi.vm.sade.oppijantunnistus.token
  (:require [pandect.algo.sha256 :refer :all])
  (:import [java.security SecureRandom]))

(def random (SecureRandom.))

(defn generate-token []
  (sha256 (.generateSeed random (/ 512 8))))
