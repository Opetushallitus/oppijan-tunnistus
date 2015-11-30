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
                 [oph/clj-util "0.1.0-SNAPSHOT" :exclusions [org.scala-lang/scala-reflect
                                                             org.scala-lang.modules/scala-xml_2.11
                                                             org.scala-lang.modules/scala-parser-combinators_2.11
                                                             de.flapdoodle.embed/de.flapdoodle.embed.mongo
                                                             org.json4s/json4s-jackson_2.11
                                                             org.scala-lang/scala-compiler
                                                             org.scalatra.scalate/scalate-core_2.11
                                                             com.fasterxml.jackson.dataformat/jackson-dataformat-yaml]]

                 ;; HTTP server
                 [javax.servlet/servlet-api "2.5"]
                 [clj-http "1.0.1"]
                 [ring "1.4.0"]
                 [ring/ring-jetty-adapter "1.4.0"]
                 [ring/ring-servlet "1.4.0"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-core "1.4.0"]

                 ;; Routing
                 [compojure "1.4.0"]
                 [metosin/compojure-api "0.23.1"]

                 ;; SQL + migrations
                 [yesql "0.5.0"]
                 [org.postgresql/postgresql "9.4-1202-jdbc42"]
                 [org.flywaydb/flyway-core "3.2.1"]

                 ;; E-mail
                 [de.ubercode.clostache/clostache "1.4.0"]

                 ;; Configuration
                 [environ "1.0.0"]

                 ;; Logging
                 [org.slf4j/slf4j-api "1.7.12"]
                 [ch.qos.logback/logback-classic "1.1.3"]
                 [ch.qos.logback/logback-access "1.1.3"]
                 [org.clojure/tools.logging "0.3.1"]

                 ;; Utils
                 [org.clojure/tools.trace "0.7.8"]
                 [clj-time "0.11.0"]
                 [pandect "0.5.3"]]

  :javac-options ["-target" "1.8" "-source" "1.8" "-Xlint:-options"]

  :prep-tasks ["compile"]

  :profiles {:uberjar {:prep-tasks ["compile" "resource"]}
             :test  {:prep-tasks ["compile" "resource"]
                     :jvm-opts ["-Doppijantunnistus.properties=target/spec.edn"]
                     :dependencies [[speclj "3.3.1"]
                                    [com.cemerick/url "0.1.1"]
                                    [ring/ring-mock "0.3.0"]
                                    [speclj-junit "0.0.10"]]
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

  :plugins [[speclj "3.3.1"]
            [lein-environ "1.0.0"]
            [lein-shell "0.4.0"]
            [lein-auto "0.1.2"]
            [lein-ancient "0.6.7"]
            [lein-ring "0.8.11"]
            [lein-resource "14.10.2"]]


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
            "fakeserver" ["run" "-m" "fi.vm.sade.oppijantunnistus.fake_cas_server/start-cas-app"]}

)
