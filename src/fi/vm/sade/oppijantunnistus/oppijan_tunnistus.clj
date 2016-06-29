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
        (db/exec add-secure-link! {:valid_until  valid_until
                                   :email        email
                                   :token        token
                                   :callback_url callback_url
                                   :lang         (name lang)
                                   :metadata     (when (some? metadata)
                                                       (write-str metadata))})
        (catch Exception e
          (throw (RuntimeException.
                   (str "Saving token " token " for " email " to database failed")
                   e)))))

(defn ^:private add-tokens [token-metas expires callback-url lang]
  (doseq [meta token-metas]
    (add-token (to-psql-timestamp expires)
               (:email meta)
               (:token meta)
               callback-url
               {"hakemusOid" (:application-oid meta)}
               lang)))

(defn ^:private get-token [token]
      (first (db/exec get-secure-link {:token token})))

(def ^:private email-template {:en (slurp (io/resource "email/email_en.mustache"))
                               :sv (slurp (io/resource "email/email_sv.mustache"))
                               :fi (slurp (io/resource "email/email_fi.mustache"))})

(def ^:private email-subjects {:en "Studyinfo – login link"
                               :sv "Studieinfo – inloggningslänk"
                               :fi "Opintopolku – kirjautumislinkki"})

(defn ^:private create-response [email token callback_url]
      {:email       email
       :securelink  (str callback_url token)})

(defn ^:private create-recipient [email token callback_url]
      {:email                 email
       :recipientReplacements [{:name   "securelink"
                                :value  (str callback_url token)}]})

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
                                           :expires           (to-date-string expires)
                                           :submit_time       (to-date-string (now-timestamp))})
            ryhmasahkoposti_url (-> cfg :ryhmasahkoposti :url)
            mail_json (write-str {:email     {:from    "no-reply@opintopolku.fi"
                                              :subject subject
                                              :body    template
                                              :isHtml  true}
                                  :recipient [{:email email}]})]
           (let [options {:timeout 10000
                          :headers {"Content-Type" "application/json"}
                          :body    mail_json}]
                @(http/post ryhmasahkoposti_url options (fn [{:keys [status error body]}]
                                                            (if (and (= 200 status) (contains? (read-str body) "id"))
                                                              verification_link
                                                              (do (log/error "Sending email failed" error)
                                                                  (throw (RuntimeException.
                                                                           (str "Sending email failed with status" status "and with message"))))))))))

(defn ^:private send-ryhmasahkoposti-with-tokens [recipients_data callback_url template_name lang haku_oid]
      (let [ryhmasahkoposti_url (-> cfg :ryhmasahkoposti :url)
            mail_json (write-str {:email      {:from          "no-reply@opintopolku.fi"
                                               :templateName  template_name
                                               :languageCode  lang
                                               :isHtml        true
                                               :hakuOid       haku_oid}
                                  :recipient  (for [x recipients_data] (create-recipient (nth x 0) (nth x 1) callback_url))})]
           (let [options {:timeout 10000
                          :headers {"Content-Type" "application/json"}
                          :body    mail_json}]
                @(http/post ryhmasahkoposti_url options (fn [{:keys [status error body]}]
                                                            (if (and (= 200 status) (contains? (read-str body) "id"))
                                                              (for [x recipients_data] (create-response (nth x 0) (nth x 1) callback_url))
                                                              (do (log/error "Sending email failed" error)
                                                                  (throw (RuntimeException.
                                                                           (str "Sending email failed with status " status " and with message"))))))))))

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

(defn ^:private token-meta [[application-oid email]]
  {:application-oid application-oid
   :token           (generate-token)
   :email           email})

(defn send-verification-links
  "Provided a TokensRequest, creates tokens, persists them and sends them by email"
  [{application-oid-to-email-address :applicationOidToEmailAddress
    callback-url                     :url
    lang                             :lang
    template-name                    :templatename
    expires                          :expires
    haku-oid                         :hakuOid}]
  (let [sanitized-lang (sanitize_lang lang)
        template-name (or template-name "default_template_name")
        expires (if expires
                  (long-to-timestamp expires)
                  (create-expiration-timestamp))
        token-metas (mapv token-meta application-oid-to-email-address)
        tokens (mapv (fn [meta]
                       [(:email meta) (:token meta)]) token-metas)]
    (try
      (add-tokens token-metas expires callback-url lang)
      (send-ryhmasahkoposti-with-tokens tokens callback-url template-name lang haku-oid)
      (catch Exception e
        (log/error "failed to send verification links" e)
        (throw (RuntimeException. "failed to send verification links" e))))))
