(ns fi.vm.sade.oppijantunnistus.oppijan_tunnistus_spec
    (:require [speclj.core :refer :all]
              [fi.vm.sade.oppijantunnistus.fake_cas_server :refer [cas_url start-cas-app stop-cas-app enable_server port]]
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
            (start-cas-app)
            (run-jetty oppijan-tunnistus-api {:port oppijan_port :join? false}))

          (after-all
            (stop-cas-app))

          (it "doesn't fail on unknown token query"
              (let [response (client/get (make_url_from_path "/token/smoken"))
                    json (parse-string (response :body) true)]
                (should (= 200 (:status response)))
                (should (= false (json :valid )))
                (should (= false (json :exists)))))

          (it "should fail on missing email"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should (= 400 (:status response)))))

          (it "should fail when metadata is not map"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :metadata "not-a-map"})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should (= 400 (:status response)))))

          (it "should allow map metadata"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :metadata {:some :key}})
                                           :content-type "application/json" })
                    body (parse-string (response :body) true)]
                (should (= 200 (:status response)))))

          (it "should not allow nested map metadata"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"
                                                             :metadata {:some {:another :key}}})
                                           :content-type "application/json"
                                           :throw-exceptions false})
                    body (parse-string (response :body) true)]
                (should (= 400 (:status response)))))

          (it "should send verification email and return token"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "http://mycallback_url#"
                                                             :email "test@email.com"})
                                           :content-type "application/json" })
                    body (parse-string (response :body) true)]
                (should (= 200 (:status response)))
                (should (.startsWith body "http://mycallback_url#"))))

          (it "should verify valid token"
              (let [response (client/post (make_url_from_path "/token")
                                          {:body (write-str {:url "#"
                                                             :email "test@email.com"
                                                             :lang "fi"})
                                           :content-type "application/json"})
                    body (parse-string (response :body) true)
                    token (subs body 1)]
                (let [response (client/get (make_url_from_path (str "/token/" token)))
                      body (parse-string (response :body) true)]
                  (should (= 200 (:status response)))
                  (should (= "test@email.com" (-> body :email)))
                  (should (= "fi" (-> body :lang)))
                  (should (= true (-> body :valid)))
                  (should (= true (-> body :exists))))))

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
                  (should (= 200 (:status response)))
                  (should (= "test@email.com" (-> body :email)))
                  (should (= true (-> body :valid)))
                  (should (= true (-> body :exists)))
                  (should (= "en" (-> body :lang)))
                  (should (.equals {:a "b"} (-> body :metadata))))))

          (it "should fail if ryhmasahkoposti is down"
              (enable_server false)
              (try
                (should-throw (client/post (make_url_from_path "/token")
                                           {:body (write-str {:url "#" :email "test@email.com"})
                                            :content-type "application/json"}))
                (finally (enable_server true))))

          )
(run-specs)
