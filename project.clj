(defproject oppijan-tunnistus "0.1.0-SNAPSHOT"
  :description "Oppijan tunnistus"
  :url "https://github.com/Opetushallitus/oppijan-tunnistus"
  :license {:name "EUPL licence"
            :url "http://opensource.org/licenses/EUPL-1.1"}
  :deploy-repositories {"snapshots" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"}
                        "releases" {:url "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"}}
  :repositories [["oph-releases" "https://artifactory.opintopolku.fi/artifactory/oph-sade-release-local"]
                 ["oph-snapshots" "https://artifactory.opintopolku.fi/artifactory/oph-sade-snapshot-local"]
                 ["ext-snapshots" "https://artifactory.opintopolku.fi/artifactory/ext-snapshot-local"]]
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/data.json "2.4.0"]
                 ;; HTTP server
                 [javax.servlet/servlet-api "2.5"]
                 [http-kit "2.5.3"]
                 [clj-http "3.12.3"]
                 [ring "1.9.4"]
                 [ring/ring-jetty-adapter "1.9.4"]
                 [ring/ring-servlet "1.9.4"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-core "1.9.4"]

                 ;; Routing
                 [metosin/compojure-api "1.1.13"]

                 ;; SQL + migrations
                 [yesql "0.5.3"]
                 [org.postgresql/postgresql "42.2.23"]
                 [org.flywaydb/flyway-core "7.15.0"]
                 [hikari-cp "2.13.0"]

                 ;; E-mail
                 [de.ubercode.clostache/clostache "1.4.0"]

                 ;; Configuration
                 [environ "1.2.0"]
                 [fi.vm.sade.java-utils/java-properties "0.1.0-SNAPSHOT"]
                 [propertea "1.3.1"]

                 ;; Logging
                 [org.slf4j/slf4j-api "1.7.32"]
                 [ch.qos.logback/logback-classic "1.2.5"]
                 [ch.qos.logback/logback-access "1.2.5"]
                 [org.clojure/tools.logging "1.1.0"]

                 ;; Utils
                 [org.clojure/tools.trace "0.7.11"]
                 [clj-time "0.15.2"]
                 [pandect "1.0.1"]


                 [fi.vm.sade/scala-cas_2.12 "2.2.1-SNAPSHOT"]
                 [ring/ring-session-timeout "0.2.0"]
                 [oph/clj-ring-db-cas-session "0.3.0-SNAPSHOT"]]

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]

  :prep-tasks ["compile"]

  :profiles {:uberjar {:prep-tasks ["compile" "resource"]}
             :dev   {:env {:dev? "true"}
                     :jvm-opts ["-Dlogback.access=does-not-exist.xml"]}
             :test  {:prep-tasks ["compile" "resource"]
                     :jvm-opts ["-Doppijantunnistus.properties=target/spec.edn"
                                "-Dlogback.access=does-not-exist.xml"]
                     :env {:dev? "true"}
                     :dependencies [[speclj "3.3.2"]
                                    [com.cemerick/url "0.1.1"]
                                    [ring/ring-mock "0.4.0"]
                                    [speclj-junit "0.0.11"]]
                     :resource {:resource-paths ["config"]
                                :target-path "target"
                                :includes [ #".*spec.edn" ]
                                :update false ;; if true only process files with src newer than dest
                                :extra-values {:fakeServerPort ~(str (+ 10000 (rand-int 50000)))}
                                :silent false
                                }}}

  :main fi.vm.sade.oppijantunnistus.main
  :aot [fi.vm.sade.oppijantunnistus.main]

  :target-path "target/%s"


  :plugins [[speclj "3.3.2"]
            [lein-environ "1.1.0"]
            [lein-shell "0.4.0"]
            [lein-auto "0.1.2"]
            [lein-ancient "0.7.0"]
            [lein-ring "0.8.11"]
            [lein-resource "14.10.2"]
            [lein-deploy-artifacts "0.1.0"]]

  :resource-paths ["resources" "target/generated-resources"]

  :resource {:resource-paths ["templates"]
             :target-path "target/generated-resources/public"
             :update false ;; if true only process files with src newer than dest
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

  :aliases {"dbmigrate" ["run" "-m" "fi.vm.sade.oppijantunnistus.db/migrate"]
            "fakeserver" ["run" "-m" "fi.vm.sade.oppijantunnistus.fake_server/start-fake-app"]}

)
