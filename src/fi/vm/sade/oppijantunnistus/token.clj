(ns fi.vm.sade.oppijantunnistus.token
  (:require [pandect.algo.sha256 :refer :all]
            [clojure.tools.logging :as log])
  (:import [java.security SecureRandom]))

(when-not *compile-files*
  (def ^:private random (let [rnd (SecureRandom.)]
                          (log/info "Initializing securerandom...")
                          (log/info "Done " (.generateSeed rnd 64))
                          rnd)))

(defn generate-token []
  (let [arr (byte-array 64)]
    (.nextBytes random arr)
    (sha256 arr)))
