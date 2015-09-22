(ns fi.vm.sade.oppijantunnistus.oppijan_tunnistus_spec
    (:require [speclj.core :refer :all]
              [fi.vm.sade.oppijantunnistus.fake_cas_server :refer [cas_url start-cas-app stop-cas-app]]
              [fi.vm.sade.oppijantunnistus.server :refer [oppijan-tunnistus-routes]]
              [ring.mock.request :refer [request content-type body]]
              [clojure.data.json :refer [write-str]]
              [fi.vm.sade.oppijantunnistus.db.db-util :as db]))

(describe "Oppijan tunnistus API"

          (before-all
            (db/migrate)
            (start-cas-app))

          (after-all
            (stop-cas-app))

          (it "doesn't fail on unknown token query"
              (let [response (oppijan-tunnistus-routes (request :get "/token/smoken"))]
                (should (= 200 (:status response)))
                (should (= false (-> response :body :valid)))))

          (it "should send verification email and return token"
              (let [response (oppijan-tunnistus-routes (-> (request :post "/token")
                                                           (body (write-str {:url "http://mycallback_url#"
                                                                             :email "test@email.com"}))
                                                           (content-type "application/json")))]
                (should (= 200 (:status response)))
                (should (.startsWith (:body response) "http://mycallback_url#"))))

          (it "should verify valid token"
              (let [response (oppijan-tunnistus-routes (-> (request :post "/token")
                                                           (body (write-str {:url "#"
                                                                             :email "test@email.com"}))
                                                           (content-type "application/json")))]
                (let [response (oppijan-tunnistus-routes (request :get (str "/token/" (subs (:body response) 1))))]
                  (should (= 200 (:status response)))
                  (should (= "test@email.com" (-> response :body :email)))
                  (should (= true (-> response :body :valid))))))

          )

(run-specs)
