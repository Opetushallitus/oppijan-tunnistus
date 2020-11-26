(ns fi.vm.sade.oppijantunnistus.db.db-util
  (:require [clojure.tools.logging :as log]
            [clj-time.coerce :as c]
            [clojure.tools.trace :only [trace]]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [hikari-cp.core :as h]
            [clojure.java.jdbc :as jdbc]
            [cheshire.core :as json]
            [clojure.tools.logging :as log])
  (:import (java.sql PreparedStatement)
    (java.sql Date)
    (java.sql Timestamp)
    (org.flywaydb.core Flyway)
    (org.joda.time DateTime)
    (org.postgresql.jdbc PgArray)
    (org.postgresql.util PGobject)))

(extend-protocol jdbc/ISQLValue
  clojure.lang.IPersistentCollection
  (sql-value [value]
    (doto (PGobject.)
      (.setType "jsonb")
      (.setValue (json/generate-string value)))))

(extend-protocol jdbc/IResultSetReadColumn
  PGobject
  (result-set-read-column [pgobj _ _]
    (let [type  (.getType pgobj)
          value (.getValue pgobj)]
      (case type
        "json" (json/parse-string value true)
        "jsonb" (json/parse-string value true)
        :else value))))

(extend-protocol jdbc/IResultSetReadColumn
  Date
  (result-set-read-column [v _ _] (c/from-sql-date v))

  Timestamp
  (result-set-read-column [v _ _] (c/from-sql-time v))

  PgArray
  (result-set-read-column [v _ _]
    (vec (.getArray v))))

(extend-type DateTime
  jdbc/ISQLParameter
  (set-parameter [v ^PreparedStatement stmt idx]
    (.setTimestamp stmt idx (c/to-sql-time v))))

(when-not *compile-files*
  (defonce datasource (let [db-config (merge {:auto-commit        true
                                              :read-only          false
                                              :connection-timeout 30000
                                              :validation-timeout 5000
                                              :idle-timeout       600000
                                              :max-lifetime       1800000
                                              :register-mbeans    false
                                              :adapter            "postgresql"}
                                             {:database-name     (-> cfg :db :databasename)
                                              :pool-name         (str (-> cfg :db :databasename) "-pool")
                                              :username          (-> cfg :db :username)
                                              :password          (-> cfg :db :password)
                                              :server-name       (-> cfg :db :servername)
                                              :port-number       (-> cfg :db :port)
                                              :minimum-idle      20
                                              :maximum-pool-size 20})]
                        (h/make-datasource db-config))))

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
  `(jdbc/with-db-transaction [connection# {:datasource datasource}]
                             (~query ~params {:connection connection#})))
