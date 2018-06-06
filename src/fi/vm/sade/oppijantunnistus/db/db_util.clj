(ns fi.vm.sade.oppijantunnistus.db.db-util
  (:gen-class)
  (:use [clojure.tools.trace :only [trace]]
        [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
        [clojure.java.jdbc])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging :as log])
  (import [org.flywaydb.core Flyway]
          [com.zaxxer.hikari HikariDataSource]))

(when-not *compile-files*
  (def datasource (doto (new HikariDataSource)
                    (.setJdbcUrl (str "jdbc:postgresql://" (-> cfg :db :servername) ":" (-> cfg :db :port) "/" (-> cfg :db :databasename)))
                    (.setUsername     (-> cfg :db :username))
                    (.setPassword     (-> cfg :db :password))
                    )))

(defn migrate [& migration-paths]
  (let [schema-name (-> cfg :db :schema)
        flyway (doto (Flyway.)
                 (.setSchemas (into-array String [schema-name]))
                 (.setDataSource datasource)
                 (.setLocations (into-array String migration-paths)))]
    (try (.migrate flyway)
         (catch Throwable e
           (log/error e)
           (throw e)))))

(defmacro exec [query params]
  `(with-db-transaction [connection# {:datasource datasource}]
                         (~query ~params {:connection connection#})))
