(ns ethlance.ui.graphql
  (:require
   [district.shared.async-helpers :refer [promise->]]
   [re-frame.core :as re-frame]
   [taoensso.timbre :as log]
   [cljsjs.axios :as axios]
   ))

(defonce axios js/axios)

(re-frame/reg-fx
 ::query
 (fn [[params callback]]
   (promise-> (axios params)
              callback)))

(re-frame/reg-event-fx
 ::query
 (fn [{:keys [db]} [_ {:keys [batched-queries query variables on-success on-error]}]]
   (let [url (get-in db [:ethlance/config :graphql :url])
         access-token (get-in db [:tokens :access-token])
         params (clj->js {:url url
                          :method :post
                          :headers (merge {"Content-Type" "application/json"
                                           "Accept" "application/json"}
                                          (when access-token
                                            {"access_token" access-token}))
                          :data (js/JSON.stringify
                                 (clj->js (or batched-queries
                                              {:query query
                                               :variables variables})))})
         callback (fn [^js response]
                    (if (= 200 (.-status response))
                      ;; TODO we can still have errors even with a 200
                      ;; so we should log them or handle in some other way
                      (when on-success
                        (on-success (js->clj (if batched-queries
                                               (.-data response)
                                               (.-data (.-data response)))
                                             :keywordize-keys true)))
                      (if on-error
                        (on-error (js->clj (.-data response) :keywordize-keys true))
                        (log/error "Error during query" {:response response}))))]
     {::query [params callback]})))

;; TODO : dispatch handlers
