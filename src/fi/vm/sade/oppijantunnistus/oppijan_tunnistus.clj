(ns fi.vm.sade.oppijantunnistus.oppijan-tunnistus
  (:require [fi.vm.sade.oppijantunnistus.db.query :refer :all]
            [fi.vm.sade.oppijantunnistus.db.db-util :as db]
            [fi.vm.sade.oppijantunnistus.token :refer [generate-token]]
            [clostache.parser :refer [render]]
            [clojure.java.io :as io]
            [clj-util.cas :refer [set-cas cas-params post]]
            [clojure.data.json :refer [write-str read-str]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clojure.tools.logging :as log]
            [fi.vm.sade.oppijantunnistus.expiration :refer [create-expiration-timestamp now-timestamp to-date-string to-psql-timestamp is-valid]]))


(defn ^:private add-token [valid_until email token callback_url metadata lang template]
  (->> (db/exec add-secure-link! {:valid_until valid_until
                                  :email email
                                  :token token
                                  :callback_url callback_url
                                  :lang (name lang)
                                  :template (name template)
                                  :metadata (if (some? metadata) (write-str metadata) nil) })))

(defn ^:private get-token [token]
  (->> (db/exec get-secure-link {:token token}) first))

(def ^:private email-template {:email {:en (slurp (io/resource "email/email_en.mustache" ))
                                       :sv (slurp (io/resource "email/email_sv.mustache" ))
                                       :fi (slurp (io/resource "email/email_fi.mustache" ))}
                               :maksulinkki {:en (slurp (io/resource "email/maksulinkki_en.mustache" ))
                                             :sv (slurp (io/resource "email/maksulinkki_sv.mustache" ))
                                             :fi (slurp (io/resource "email/maksulinkki_fi.mustache" ))}})
(def ^:private email-subjects {:email {:en "Studyinfo – login link"
                                       :sv "Studieinfo – inloggningslänk"
                                       :fi "Opintopolku – kirjautumislinkki"}
                               :maksulinkki {:en "Studyinfo – maksulinkki"
                                             :sv "Studieinfo – maksulinkki"
                                             :fi "Opintopolku – maksulinkki"}})

(defn retrieve-email-and-validity-with-token [token]
  (let [entry (get-token token)]
    (if entry
      (let [rval {:email (entry :email) :valid (is-valid (entry :valid_until)) :exists true :lang (entry :lang)}
            metadata (entry :metadata)]
        (if (some? metadata)
          (assoc rval :metadata (read-str (.getValue metadata)))
          rval))
      {:valid false :exists false})))

(defn ^:private send-ryhmasahkoposti [expires email callback_url token lang template_name]
  (let [verification_link (str callback_url token)
        template (render ((email-template template_name) lang) {:verification-link verification_link
                                         :expires (to-date-string expires)
                                         :submit_time (to-date-string (now-timestamp))})
        cas_url (-> cfg :cas :url)
        ryhmasahkoposti_params (-> cfg :ryhmasahkoposti :params)
        ryhmasahkoposti_url (-> cfg :ryhmasahkoposti :url)
        mail_json (write-str {:email {:from "no-reply@opintopolku.fi"
                                      :subject ((email-subjects template_name) lang)
                                      :body template
                                      :isHtml true}
                              :recipient [{:email email}] })]
    (set-cas cas_url)
    (let [response (try
                     @(post (apply cas-params ryhmasahkoposti_params) ryhmasahkoposti_url mail_json :content-type ["application" "json"])
                     (catch Throwable t (log/error "Sending ryhmasahkoposti failed!" t)))]
    (if (= 200 (-> response :status :code))
      verification_link
      (do (log/error "Sending email failed!" response)
          (throw (Exception. "Sending email failed!")))
      ))))

(defn ^:private sanitize_lang [any_lang]
  (case any_lang
    "fi" :fi
    "sv" :sv
    :en))

(defn ^:private sanitize_template [any_template]
  (case any_template
    "email" :email
    "maksulinkki" :maksulinkki
    :email))

(defn send-verification-link [email callback_url metadata any_lang any_template]
  (let [lang (sanitize_lang any_lang)
        template (sanitize_template any_template)
        token (generate-token)
        expires (create-expiration-timestamp)]
    (if (add-token (to-psql-timestamp expires) email token callback_url metadata lang template)
      (send-ryhmasahkoposti expires email callback_url token lang template)
      (log/error "Saving token to database failed!"))
    ))
