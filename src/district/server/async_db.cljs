(ns district.server.async-db
  (:refer-clojure :exclude [get run!])
  (:require [mount.core :as mount :refer [defstate]]
            [taoensso.timbre :as log]
            [cljs.nodejs :as nodejs]
            [honeysql.core :as sql]
            [district.server.logging];; TODO: remove
            [honeysql.format :as sql-format]
            [district.server.config :refer [config]]
            [district.shared.async-helpers :refer [safe-go <?]]
            [clojure.string :as str]))

(def pg (nodejs/require "pg"))
(def Pool (.-Pool pg))

(declare start stop)

(def mount-state-key
  "Key defining our mount component within the district configuration"
  :district/db)

(defstate ^{:on-reload :noop} db
  :start (start (merge (clojure.core/get @config mount-state-key)
                       (mount-state-key (mount/args))))
  :stop (stop))

(defn sql-name-transform-fn [n]
  (-> n
      munge
      (str/replace "_STAR_" "*")
      (str/replace "_PERCENT_" "%")))

(def transform-result-keys-fn (comp keyword
                                    demunge
                                    #(str/replace % #"_slash_" "_SLASH_")))

(defn- map-keys [f m]
  (into {} (map (fn [[k v]] [(f k) v]) m)))

(defn get-connection
  "Returns a db connection from the pool."
  []
  (.connect (:connection-pool @db)))

(defn release-connection
  "Returns a db connection to the pool."
  [conn]
  (.release conn))

(defn run!
  "Given a db connection and a honey sql query runs it and returns its result."
  [conn statement]
  (safe-go
   (let [[query-str & values :as all] (binding [sql-format/*name-transform-fn* sql-name-transform-fn]
                                        (sql/format statement
                                                    :parameterizer :postgresql
                                                    :allow-namespaced-names? true))
         _ (log/debug "Running QUERY " {:q query-str :vals (or values [])})
         res (<? (.query conn query-str (clj->js (or values []))))
         _ (log/debug "query result" {:r res})
         ]
     (->> (js->clj (.-rows res))
          (map #(map-keys transform-result-keys-fn %))))))

(defn all
  "Given a db connection and a honey sql query runs it and returns resultset rows."
  [conn q]
  (run! conn q))

(defn get
  "Given a db connection and a honey sql query runs it and returns first resultset row."
  [conn q]
  (safe-go
   (first (<? (all conn q)))))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Transaction management ;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn begin-tx [conn]
  (.query conn "BEGIN"))

(defn commit-tx [conn]
  (.query conn "COMMIT"))

(defn rollback-tx [conn]
  (.query conn "ROLLBACK"))

;;;;;;;;;;;;;;;;;;;;;
;; Mount component ;;
;;;;;;;;;;;;;;;;;;;;;

(defn start
  "Start the ethlance-db mount component."
  [{:keys [user host database password port] :as opts}]
  (log/info "Starting DB component" {})
  (let [pool (Pool. #js {:user user
                         :host host
                         :database database
                         :password password
                         :port port})]
    (.on pool "error" (fn [err client]
                        (log/error "Unexpected error on idle client" {:err err})))

    (log/info "DB component started" {})
    {:connection-pool pool}))

(defn stop
  "Stop the db mount component."
  []
  (release-connection (:wildcard-connection @db))
  ::stopped)

(comment

  (safe-go
   (let [conn (<? (get-connection))
         res (<? (run! conn {:create-table [:usr :if-not-exists]
                             :with-columns [[[:user/address :varchar]
                                             [:user/type :varchar]

                                             ;; PK
                                             [(sql/call :primary-key :user/address)]]]}))]))

  (safe-go
   (let [conn (<? (get-connection))]
     (run! conn {:insert-into :usr
                 :columns [:user/address :user/type]
                 :values [["address1" "type1"]]})))

  (safe-go
   (let [conn (<? (get-connection))]
     (println (<? (get conn {:select [[(sql/call :last_insert_rowid) :id]]})))))

  (safe-go
   (let [conn (<? (get-connection))
         res (<? (all conn {:select [:*] :from [:usr]}))]
     (log/info "GOT " {:rows res})))
  )
