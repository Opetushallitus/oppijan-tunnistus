(ns fi.vm.sade.oppijantunnistus.expiration
  (:require [clj-time.core :as t]
            [fi.vm.sade.oppijantunnistus.config :refer [cfg]]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.coerce :as c]))

(def ^:private timezone-id "Europe/Helsinki")

(def ^:private formatter (f/formatter "yyyy-MM-dd HH:mm:ss" (t/time-zone-for-id timezone-id)))

(def ^:private template-formatter (f/formatter "dd.MM.yyyy" (t/time-zone-for-id timezone-id)))

(defn long-to-timestamp [lval] (c/from-long lval))
(defn create-expiration-timestamp [] (t/plus (l/local-now) (t/days (-> cfg :expires-in :days))))
(defn now-timestamp [] (l/local-now))

(defn to-date-string [timestamp] (f/unparse template-formatter timestamp))
(defn from-sql-time [timestamp] (c/from-sql-time timestamp))
(defn to-psql-timestamp [timestamp] (str (f/unparse formatter timestamp) " " timezone-id))

(defn is-valid [sql-timestamp] (t/before? (l/local-now) (c/from-sql-time sql-timestamp)))
