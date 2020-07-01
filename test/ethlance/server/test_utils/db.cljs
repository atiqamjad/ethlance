(ns ethlance.server.test-utils.db
  "Includes test utilities for working with the district databases."
  (:require
   [mount.core :as mount]

   [district.server.config :refer [config]]
   [district.server.async-db]

   [ethlance.server.db :as db]
   [ethlance.server.core]))


(def test-config
  "Test configuration for the database."
  (-> ethlance.server.core/default-config
      (merge {:logging {:level "debug" :console? true}})
      (update :db merge {:opts {:memory true}})
      #_(update :db merge {:opts {:memory false}
                           :path "target/test_ethlance.db"})))


(defn fixture-start
  "Test Fixture Setup."
  [& [opts]]
  (-> (mount/with-args test-config)
      (mount/only
       [#'district.server.async-db/db
        #'ethlance.server.db/ethlance-db])
      mount/start))


(defn fixture-stop
  "Test Fixture Teardown."
  []
  (mount/stop))
