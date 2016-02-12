(ns fi.vm.sade.oppijantunnistus.oppijan-tunnistus
  (:require [fi.vm.sade.oppijantunnistus.db.query :refer :all]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db]
            [fi.vm.sade.oppijantunnistus.token :refer [generate-token]]
            [clostache.parser :refer [render]]
            [clojure.java.io :as io]
            [org.httpkit.client :as http]
            [clojure.data.json :refer [write-str read-str]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.expiration :refer [long-to-timestamp create-expiration-timestamp now-timestamp to-date-string to-psql-timestamp is-valid]]))


(defn ^:private add-token [valid_until email token callback_url metadata lang]
  (try
    (db/exec add-secure-link! {:valid_until valid_until
                               :email email
                               :token token
                               :callback_url callback_url
                               :lang (name lang)
                               :metadata (when (some? metadata)
                                           (write-str metadata))})
    (catch Exception e
      (throw (RuntimeException.
              (str "Saving token " token " for " email " to database failed")
              e)))))

(defn ^:private get-token [token]
  (first (db/exec get-secure-link {:token token})))

(def ^:private email-template {:en (slurp (io/resource "email/email_en.mustache" ))
                               :sv (slurp (io/resource "email/email_sv.mustache" ))
                               :fi (slurp (io/resource "email/email_fi.mustache" ))})

(def ^:private email-subjects {:en "Studyinfo – login link"
                               :sv "Studieinfo – inloggningslänk"
                               :fi "Opintopolku – kirjautumislinkki"})

(defn retrieve-email-and-validity-with-token [token]
  (let [entry (get-token token)]
    (if entry
      (let [rval {:email (entry :email) :valid (is-valid (entry :valid_until)) :exists true :lang (entry :lang)}
            metadata (entry :metadata)]
        (if (some? metadata)
          (assoc rval :metadata (read-str (.getValue metadata)))
          rval))
      {:valid false :exists false})))

(defn ^:private send-ryhmasahkoposti [expires email callback_url token raw_template subject]
  (let [verification_link (str callback_url token)
        template (render raw_template {:verification-link verification_link
                                       :expires (to-date-string expires)
                                       :submit_time (to-date-string (now-timestamp))})
        ryhmasahkoposti_url (-> cfg :ryhmasahkoposti :url)
        mail_json (write-str {:email {:from "no-reply@opintopolku.fi"
                                      :subject subject
                                      :body template
                                      :isHtml true}
                              :recipient [{:email email}] })]
    (let [options {:timeout 10000
                   :headers {"Content-Type" "application/json"}
                   :body mail_json}]
      @(http/post ryhmasahkoposti_url options (fn [{:keys [status error body]}]
                                               (if (and (= 200 status) (contains? (read-str body) "id"))
                                                 verification_link
                                                 (do (log/error "Sending email failed" error)
                                                     (throw (RuntimeException.
                                                              (str "Sending email failed with status" status "and with message"))))))))))

(defn ^:private sanitize_lang [any_lang]
  (case any_lang
    "fi" :fi
    "sv" :sv
    :en))

(defn send-verification-link [email callback_url metadata some_lang some_template some_subject some_expiration]
  (let [lang (sanitize_lang some_lang)
        token (generate-token)
        template (if (some? some_template) some_template (email-template lang))
        subject (if (some? some_subject) some_subject (email-subjects lang))
        expires (if (some? some_expiration) (long-to-timestamp some_expiration) (create-expiration-timestamp))]
    (try
      (add-token (to-psql-timestamp expires) email token callback_url metadata lang)
      (send-ryhmasahkoposti expires
                            email
                            callback_url
                            token
                            template
                            subject)
      (catch Exception e
        (log/error "failed to send verification link" e)
        (throw (RuntimeException. "failed to send verification link" e))))))
