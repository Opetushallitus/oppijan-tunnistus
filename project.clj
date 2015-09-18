(defproject oppijan-tunnistus "0.1.0-SNAPSHOT"
  :description "Oppijan tunnistus"
  :url "https://github.com/Opetushallitus/email-verification-link"
  :license {:name "EUPL licence"
            :url "http://opensource.org/licenses/EUPL-1.1"}
  :deploy-repositories {"snapshots" {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"}
                        "releases" {:url "https://artifactory.oph.ware.fi/artifactory/oph-sade-release-local"}}
  :repositories [["oph-releases" "https://artifactory.oph.ware.fi/artifactory/oph-sade-release-local"]
                 ["oph-snapshots" "https://artifactory.oph.ware.fi/artifactory/oph-sade-snapshot-local"]]
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/data.json "0.2.6"]
                 ; CAS
                 [fi.vm.sade/scala-utils_2.11 "0.2.0-SNAPSHOT"]
                 [org.http4s/http4s-blaze-client_2.11 "0.10.0"]
                 [oph/clj-util "0.1.0-SNAPSHOT"]
                 ;; HTTP server
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.1.19"]
                 [clj-http "1.0.1"]
                 [ring "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-servlet "1.4.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.4.0"]
                 [hiccup "1.0.5"]

                 ;; Routing
                 [compojure "1.4.0"]
                 [metosin/compojure-api "0.23.1"]

                 ;; JSON
                 [cheshire "5.5.0"]
                 ;[prismatic/schema "0.4.4"]

                 ;; SQL + migrations
                 [yesql "0.5.0"]
                 [org.postgresql/postgresql "9.4-1202-jdbc42"]
                 ;[hikari-cp "1.3.1" :exclusions [prismatic/schema]]
                 [org.flywaydb/flyway-core "3.2.1"]

                 ;; E-mail
                 [de.ubercode.clostache/clostache "1.4.0"]

                 ;; Testing
                 [speclj "3.3.1"]
                 ;; for junit output: lein spec -f junit
                 [speclj-junit "0.0.10"]

                 ;; Configuration
                 [environ "1.0.0"]

                 ;; Logging
                 ;[org.slf4j/slf4j-log4j12 "1.7.12"]
                 [ch.qos.logback/logback-classic	"1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ring.middleware.logger "0.5.0"]
                 [ring.middleware.conditional "0.2.0"]

                 ;; Utils
                 [org.clojure/tools.trace "0.7.8"]
                 [clj-time "0.11.0"]
                 [pandect "0.5.3"]]

  :main fi.vm.sade.oppijantunnistus.main

  :target-path "target/%s"

  :plugins [[speclj "3.3.1"]
            [lein-environ "1.0.0"]
            [lein-shell "0.4.0"]
            [lein-auto "0.1.2"]
            [lein-ancient "0.6.7"]
            [lein-ring "0.8.11"]
            [lein-resource "14.10.2"]]

  :prep-tasks ["javac" "compile" "resource"]

  :resource-paths ["resources" "target/generated-resources"]

  :resource {:resource-paths ["templates"]
             :target-path "target/generated-resources/public"
             :update   false
             :extra-values {:version "0.1.0-SNAPSHOT"
                            :buildNumber ~(java.lang.System/getProperty "buildNumber")
                            :branchName ~(java.lang.System/getProperty "branchName")
                            :revisionNumber ~(java.lang.System/getProperty "revisionNumber")
                            :buildTime ~(.format
                                          (java.text.SimpleDateFormat. "yyyyMMdd-HHmm")
                                          (java.util.Date.) )}
             :silent false
             }
  :test-paths ["spec"]

  :aliases {"dbmigrate" ["run" "-m" "fi.vm.sade.oppijantunnistus.db/migrate"]}

)