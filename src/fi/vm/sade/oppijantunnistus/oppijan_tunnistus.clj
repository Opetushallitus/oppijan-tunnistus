(ns fi.vm.sade.oppijantunnistus.oppijan-tunnistus
  (:require [fi.vm.sade.oppijantunnistus.db.query :refer :all]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db]
            [fi.vm.sade.oppijantunnistus.token :refer [generate-token]]
            [clostache.parser :refer [render]]
            [clojure.java.io :as io]
            [clj-util.cas :refer [set-cas cas-params post]]
            [clojure.data.json :refer [write-str]]
            [fi.vm.sade.oppijantunnistus.config :refer [config]]
            [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.expiration :refer [create-expiration-timestamp is-valid]]))


(defn ^:private add-token [valid_until email token callback_url]
  (->> (db/exec add-secure-link! {:valid_until valid_until
                                  :email email
                                  :token token
                                  :callback_url callback_url})))

(defn ^:private get-token [token]
  (->> (db/exec get-secure-link {:token token}) first))

(def ^:private email-template (slurp (io/resource "email/email.mustache" )))

(defn retrieve-email-and-validity-with-token [token]
  (let [entry (get-token token)]
    (println entry)
    (if entry
      {:email (entry :email) :valid (is-valid (entry :valid_until))}
      {:valid false})))

(set-cas (-> config :cas :url))

(defn ^:private send-ryhmasahkoposti [email callback_url token]
  (let [verification_link (str callback_url token)
        template (render email-template {:verification-link verification_link})]
    @(post (apply cas-params (-> config :ryhmasahkoposti :params))
             (-> config :ryhmasahkoposti :url)
             (write-str {:email {:from "no-reply@opintopolku.fi"
                                 :subject "Verification link"
                                 :body template
                                 :isHtml true}
                         :recipient [{:email email}] })
             :content-type ["application" "json"])
    verification_link
    ))

(defn send-verification-link [email callback_url]
  (let [token (generate-token)]
    (if (add-token (create-expiration-timestamp) email token callback_url)
      (send-ryhmasahkoposti email callback_url token)
      (log/error "Saving token to database failed!"))
    ))
