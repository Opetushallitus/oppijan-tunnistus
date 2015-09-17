(ns fi.vm.sade.oppijantunnistus.db.db-util
  (:gen-class)
  (:use [clojure.tools.trace :only [trace]]
        [fi.vm.sade.oppijantunnistus.config :refer [config]]
        [clojure.java.jdbc])
  (:require [clojure.tools.logging :as log]
            [clojure.tools.logging :as log])
  (import [org.flywaydb.core Flyway]
          [org.flywaydb.core.api.migration.jdbc JdbcMigration]
          [org.flywaydb.core.api.migration MigrationInfoProvider]
          [org.flywaydb.core.api MigrationVersion]
          [javax.sql.DataSource]
          [org.postgresql.ds PGPoolingDataSource]))

(defonce datasource (doto (new PGPoolingDataSource)
                      (.setServerName   (-> config :db :servername))
                      (.setDatabaseName (-> config :db :databasename))
                      (.setUser         (-> config :db :username))
                      (.setPassword     (-> config :db :password))
                      (.setPortNumber   (-> config :db :port))
                      (.setMaxConnections 3)))

(defn migrate [& migration-paths]
  (let [schema-name (-> config :db :schema)
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