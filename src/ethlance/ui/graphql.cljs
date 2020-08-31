(ns ethlance.ui.graphql
  (:require
   [district.shared.async-helpers :refer [promise->]]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [cljsjs.axios :as axios]
   [ethlance.ui.util.component :refer [>evt]]
   ))

(defonce axios js/axios)

(defmulti handler
  (fn [_ key value]
    (cond
      (:error value) :api/error
      (vector? key) (first key)
      :else key)))

(defn- update-db [cofx fx]
  (if-let [db (:db fx)]
    (assoc cofx :db db)
    cofx))

(defn- safe-merge [fx new-fx]
  (reduce (fn [merged-fx [k v]]
            (if (= :db k)
              (assoc merged-fx :db v)))
          fx
          new-fx))

(defn- do-reduce-handlers
  [{:keys [db] :as cofx} f coll]
  (reduce (fn [fxs element]
            (let [updated-cofx (update-db cofx fxs)]
              (if element
                (safe-merge fxs (f updated-cofx element))
                fxs)))
          {:db db}
          coll))

(defn reduce-handlers
  [cofx response]
  (do-reduce-handlers cofx
                      (fn [fxs [k v]]
                        (log/debug "@ calling handler" {:k k :v v :fx fxs})
                        (handler fxs k v))
                      response))

(re-frame/reg-event-fx
 ::response
 (fn [{:keys [db] :as cofx} [_ response]]
   (reduce-handlers cofx response)))

(re-frame/reg-fx
 ::query
 (fn [[params callback]]
   (promise-> (axios params)
              callback)))

(re-frame/reg-event-fx
 ::query
 (fn [{:keys [db]} [_ {:keys [query variables on-success on-error]}]]
   (let [url (get-in db [:ethlance/config :graphql :url])
         access-token (get-in db [:tokens :access-token])
         params (clj->js {:url url
                          :method :post
                          :headers (merge {"Content-Type" "application/json"
                                           "Accept" "application/json"}
                                          (when access-token
                                            {"access_token" access-token}))
                          :data (js/JSON.stringify
                                 (clj->js {:query query
                                           :variables variables}))})
         callback (fn [^js response]
                    (if (= 200 (.-status response))
                      ;; TODO we can still have errors even with a 200
                      ;; so we should log them or handle in some other way
                      (let [response (js->clj (.-data (.-data response))
                                              :keywordize-keys true)]
                        (>evt [::response response]))
                      (log/error "Error during query" {:error (js->clj (.-data response) :keywordize-keys true)})))]
     {::query [params callback]})))

(defmethod handler :default
  [{:keys [db] :as cofx} k values]
  ;; NOTE: this is the default handler that is intented for queries and mutations
  ;; that have nothing to do besides reducing over their response values
  (log/debug "default handler" {:k k
                                :cofx cofx})
  (reduce-handlers cofx values))

;; TODO
(defmethod handler :user
  [{:keys [db] :as cofx} user-ident {:user/keys [email] :as user}]

  (log/debug "user handler" {:u user
                             :ident user-ident})

  {:db (-> db (assoc :fubar :bar))}

  )


(defmethod handler :user_address
  [{:keys [db] :as cofx} user-ident address]

  (log/debug "user_address handler" {:a address
                                     :ident user-ident})

  {:db (-> db (assoc :fu :bar))}

  )

(defmethod handler :user_email
  [{:keys [db] :as cofx} user-ident {:user/keys [email] :as user}]
  (log/debug "user_email handler" {:cofx cofx})
  {:db db})

(defmethod handler :api/error
  [_ _ _]
  ;; NOTE: this handler is here only to catch errors
  )
