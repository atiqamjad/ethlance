(ns ethlance.ui.events
  "Main entry point for all registered events within re-frame for ethlance."
  (:require ethlance.ui.page.arbiters.events
            ethlance.ui.page.candidates.events
            ethlance.ui.page.employers.events
            ethlance.ui.page.invoices.events
            ethlance.ui.page.job-contract.events
            ethlance.ui.page.job-detail.events
            ethlance.ui.page.jobs.events
            ethlance.ui.page.me.events
            ethlance.ui.page.new-invoice.events
            ethlance.ui.page.new-job.events
            ethlance.ui.page.profile.events
            [re-frame.core :as re]))

(def forwarded-events
  "Forwarded Events.

   Notes:

   - district.ui.router/watch-active-page effect handler uses forwarded events
   - Additional info: https://github.com/day8/re-frame-forward-events-fx"
  (list
   [:page.jobs/initialize-page
    :page.sign-up/initialize-page
    :page.candidates/initialize-page
    :page.arbiters/initialize-page
    :page.employers/initialize-page
    :page.profile/initialize-page
    :page.job-contract/initialize-page
    :page.job-detail/initialize-page
    :page.new-job/initialize-page
    :page.invoices/initialize-page
    :page.new-invoice/initialize-page]))


(defn initialize
  "Sets initial db state for local components, local pages, and site-wide events."
  [{:keys [db] :as cofx} [_ config]]
  (let [new-db
        (assoc db
               ;; Component Events
               ;; /Nothing here, yet/
               :ethlance/config config

               ;; Page Events
               ethlance.ui.page.me.events/state-key
               ethlance.ui.page.me.events/state-default
               ethlance.ui.page.jobs.events/state-key
               ethlance.ui.page.jobs.events/state-default
               ethlance.ui.page.candidates.events/state-key
               ethlance.ui.page.candidates.events/state-default
               ethlance.ui.page.arbiters.events/state-key
               ethlance.ui.page.arbiters.events/state-default
               ethlance.ui.page.employers.events/state-key
               ethlance.ui.page.employers.events/state-default
               ethlance.ui.page.profile.events/state-key
               ethlance.ui.page.profile.events/state-default
               ethlance.ui.page.job-contract.events/state-key
               ethlance.ui.page.job-contract.events/state-default
               ethlance.ui.page.job-detail.events/state-key
               ethlance.ui.page.job-detail.events/state-default
               ethlance.ui.page.new-job.events/state-key
               ethlance.ui.page.new-job.events/state-default
               ethlance.ui.page.invoices.events/state-key
               ethlance.ui.page.invoices.events/state-default
               ethlance.ui.page.new-invoice.events/state-key
               ethlance.ui.page.new-invoice.events/state-default)]

               ;; Main Events
               ;; /Nothing here, yet/
    {:db new-db
     ;; Initialize Forwarded FX Events
     :dispatch-n forwarded-events
     :log/info ["Initialized re-frame app state" (clj->js new-db)]}))

;;
;; Registered Events
;;

(re/reg-event-fx :ethlance/initialize initialize)
