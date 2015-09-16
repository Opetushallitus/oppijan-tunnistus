(ns fi.vm.sade.oppijantunnistus.expiration
  (:require [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-time.local :as l]
            [clj-time.coerce :as c]))

(defonce ^{:private true} timezone-id "Europe/Helsinki")

(defonce ^{:private true} formatter (f/formatter "yyyy-MM-dd HH:mm:ss" (t/time-zone-for-id timezone-id)))

(defn create-expiration-timestamp [] (str (f/unparse formatter (t/plus (l/local-now) (t/months 1))) " " timezone-id))

(defn is-valid [sql-timestamp] (t/before? (l/local-now) (c/from-sql-time sql-timestamp)))
