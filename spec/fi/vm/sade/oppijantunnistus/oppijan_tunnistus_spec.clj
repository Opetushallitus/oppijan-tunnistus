(ns fi.vm.sade.oppijantunnistus.oppijan_tunnistus_spec
    (:require [speclj.core :refer :all]
              [fi.vm.sade.oppijantunnistus.fake_server :refer [start-fake-app stop-fake-app enable_server port]]
              [fi.vm.sade.oppijantunnistus.server :refer [oppijan-tunnistus-api]]
              [ring.mock.request :refer [request content-type body]]
              [clojure.data.json :refer [write-str]]
              [cheshire.core :refer [parse-string]]
              [clj-http.client :as client]
              [ring.adapter.jetty :refer [run-jetty]]
              [fi.vm.sade.oppijantunnistus.db.db-util :as db]))

(def oppijan_port (+ 10 port))
(defn make_url_from_path [path]
  (str "http://localhost:" oppijan_port "/oppijan-tunnistus/api/v1" path))

(describe "Oppijan tunnistus API"

          (before-all
            (db/migrate "db.migration")
            (start-fake-app)
            (run-jetty oppijan-tunnistus-api {:port oppijan_port :join? false}))

          (after-all
            (stop-fake-app))

          (it "doesn't fail on unknown token query"
              (let [response (client/get (make_url_from_path "/token/smoken"))
                    json (parse-string (response :body) true)]
                (should= 200 (:status response))
                (should= false (json :valid ))
                (should= false (json :exists))))

          (it "should fail on missing email"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should= 400 (:status response))))


          (it "should return metadata in validation query"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "#"
                                                             :email "test@email.com"
                                                             :lang "hu"
                                                             :metadata {:a :b}})
                                           :content-type "application/json"})
                    body (parse-string (response :body) true)
                    token (subs body 1)]
                (let [response (client/get (make_url_from_path (str "/token/" token)))
                      body (parse-string (response :body) true)]
                  (should= 200 (:status response))
                  (should= "test@email.com" (-> body :email))
                  (should= true (-> body :valid))
                  (should= true (-> body :exists))
                  (should= "en" (-> body :lang))
                  (should (.equals {:a "b"} (-> body :metadata))))))

          (it "should fail when metadata is not map"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :metadata "not-a-map"})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should= 400 (:status response))))

          (it "should allow map metadata"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :lang "fi"
                                                             :metadata {:some :key}})
                                           :content-type "application/json" })
                    body (parse-string (response :body) true)]
                (should= 200 (:status response))))

          (it "should not allow nested map metadata"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :metadata {:some {:another :key}}})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should= 400 (:status response))))

          (it "should send verification email and return token"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :lang "fi"})
                                           :content-type "application/json" })
                    body (parse-string (response :body) true)]
                (should= 200 (:status response))
                (should (.startsWith body "http://mycallback_url#"))))

          (it "should verify valid token"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "#"
                                                             :email "test@email.com"
                                                             :lang "fi"
                                                             :subject "My Custom Subject"
                                                             :expires 1448624510000
                                                             :template "### My Custom Template {{verification-link}} ###"})
                                           :content-type "application/json"})
                    body (parse-string (response :body) true)
                    token (subs body 1)]
                (let [response (client/get (make_url_from_path (str "/token/" token)))
                      body (parse-string (response :body) true)]
                  (should= 200 (:status response))
                  (should= "test@email.com" (-> body :email))
                  (should= "fi" (-> body :lang))
                  (should= false (-> body :valid))       ; timestamp should have expired
                  (should= true (-> body :exists)))))


          (it "should send verification email and return tokens"
              (let [response (client/post (make_url_from_path "/tokens")
                                          {:body         (write-str {:url          "http://mycallback_url#"
                                                                     :templatename "my_template"
                                                                     :applicationOidToEmailAddress {
                                                                       :oid1 "test1@email.com"
                                                                       :oid2 "test2@email.com"
                                                                       :oid3 "test3@email.com"}
                                                                     :hakuOid "hakuOid1"
                                                                     :letterId "letterId"
                                                                     :lang         "fi"})
                                           :content-type "application/json"})
                    body (parse-string (response :body) true)]
                   (should= 200 (:status response))
                   (should= "test1@email.com" (-> (get ( body :recipients ) 0) :email))
                   (should= "test2@email.com" (-> (get ( body :recipients ) 1) :email ))
                   (should= "test3@email.com" (-> (get ( body :recipients ) 2) :email ))
                   (should (.startsWith (-> (get ( body :recipients ) 0) :securelink ) "http://mycallback_url#" ))
                   (should (.startsWith (-> (get ( body :recipients ) 1) :securelink ) "http://mycallback_url#" ))
                   (should (.startsWith (-> (get ( body :recipients ) 2) :securelink ) "http://mycallback_url#" ))
                   ))

          (it "should return verification email preview"
              (let [response (client/get (make_url_from_path "/preview/haku/hakuOid1/template/my_template/lang/fi")
                                          {:query-params {:callback-url          "http://mycallback_url#"}})
                    body (response :body)]
                (should= 200 (:status response))
                (should-contain "EMAIL from template" body)
                ))

          (it "should verify valid tokens"
              (let [response (client/post (make_url_from_path "/tokens")
                                          {:body         (write-str {:url          "#"
                                                                     :templatename "my_template"
                                                                     :applicationOidToEmailAddress {
                                                                       :oid1 "test1@email.com"
                                                                       :oid2 "test2@email.com"}
                                                                     :letterId "letterId"
                                                                     :hakuOid "hakuOid1"
                                                                     :lang         "fi"})
                                           :content-type "application/json"})
                    body (parse-string (response :body) true)
                    token1 (subs ((get ( body :recipients ) 0) :securelink) 1)
                    token2 (subs ((get ( body :recipients ) 1) :securelink) 1)]
                   (let [response (client/get (make_url_from_path (str "/token/" token1)))
                         body (parse-string (response :body) true)]
                        (should= 200 (:status response))
                        (should= "test1@email.com" (-> body :email))
                        (should= "fi" (-> body :lang))
                        (should= true (-> body :valid))
                        (should= true (-> body :exists)))
                   (let [response (client/get (make_url_from_path (str "/token/" token2)))
                         body (parse-string (response :body) true)]
                        (should= 200 (:status response))
                        (should= "test2@email.com" (-> body :email))
                        (should= "fi" (-> body :lang))
                        (should= true (-> body :valid))
                        (should= true (-> body :exists)))))

          (it "should fail if ryhmasahkoposti is down"
              (enable_server false)
              (try
                (client/post (make_url_from_path "/token")
                             {:body (write-str {:url "#" :email "test@email.com"})
                              :content-type "application/json"})
                (throw (RuntimeException. "No exception thrown"))
                (catch clojure.lang.ExceptionInfo e
                  (should= 500 (:status (ex-data e)))
                  (should= "{\"type\":\"unknown-exception\",\"class\":\"java.lang.RuntimeException\"}" (:body (ex-data e))))
                (finally (enable_server true)))))
(run-specs)
