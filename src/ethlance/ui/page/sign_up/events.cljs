(ns ethlance.ui.page.sign-up.events
  (:require [district.parsers :refer [parse-float]]
            [ethlance.ui.util.component :refer [>evt]]
            [district.ui.web3-accounts.events :as accounts-events]
            [district.ui.web3-accounts.queries :as accounts-queries]
            [district.ui.router.effects :as router.effects]
            [ethlance.ui.event.utils :as event.utils]
            [ethlance.ui.graphql :as graphql]
            [re-frame.core :as re]
            [taoensso.timbre :as log]))

(def state-key :page.sign-up)
(def state-default
  {:candidate/full-name nil
   :candidate/professional-title nil
   :candidate/email nil
   :candidate/hourly-rate nil
   :candidate/github-key nil
   :candidate/linkedin-key nil
   :candidate/languages []
   :candidate/categories []
   :candidate/skills []
   :candidate/biography nil
   :candidate/country nil
   :candidate/ready-for-hire? false

   :employer/full-name nil
   :employer/professional-title nil
   :employer/email nil
   :employer/github-key nil
   :employer/linkedin-key nil
   :employer/languages []
   :employer/biography nil
   :employer/country nil

   :arbiter/full-name nil
   :arbiter/professional-title nil
   :arbiter/fixed-rate-per-dispute nil
   :arbiter/email nil
   :arbiter/github-key nil
   :arbiter/linkedin-key nil
   :arbiter/languages []
   :arbiter/biography nil
   :arbiter/country nil})

;;
;; Registered Events
;;
(def create-assoc-handler (partial event.utils/create-assoc-handler state-key))

(re/reg-event-fx :page.sign-up/set-candidate-full-name (create-assoc-handler :candidate/full-name))
(re/reg-event-fx :page.sign-up/set-candidate-professional-title (create-assoc-handler :candidate/professional-title))
(re/reg-event-fx :page.sign-up/set-candidate-email (create-assoc-handler :candidate/email))
(re/reg-event-fx :page.sign-up/set-candidate-hourly-rate (create-assoc-handler :candidate/hourly-rate parse-float))
(re/reg-event-fx :page.sign-up/set-candidate-github-key (create-assoc-handler :candidate/github-key))
(re/reg-event-fx :page.sign-up/set-candidate-linkedin-key (create-assoc-handler :candidate/linkedin-key))
(re/reg-event-fx :page.sign-up/set-candidate-languages (create-assoc-handler :candidate/languages))
(re/reg-event-fx :page.sign-up/set-candidate-categories (create-assoc-handler :candidate/categories))
(re/reg-event-fx :page.sign-up/set-candidate-skills (create-assoc-handler :candidate/skills))
(re/reg-event-fx :page.sign-up/set-candidate-biography (create-assoc-handler :candidate/biography))
(re/reg-event-fx :page.sign-up/set-candidate-country (create-assoc-handler :candidate/country))
(re/reg-event-fx :page.sign-up/set-candidate-ready-for-hire? (create-assoc-handler :candidate/ready-for-hire? boolean))

(re/reg-event-fx :page.sign-up/set-employer-full-name (create-assoc-handler :employer/full-name))
(re/reg-event-fx :page.sign-up/set-employer-professional-title (create-assoc-handler :employer/professional-title))
(re/reg-event-fx :page.sign-up/set-employer-email (create-assoc-handler :employer/email))
(re/reg-event-fx :page.sign-up/set-employer-github-key (create-assoc-handler :employer/github-key))
(re/reg-event-fx :page.sign-up/set-employer-linkedin-key (create-assoc-handler :employer/linkedin-key))
(re/reg-event-fx :page.sign-up/set-employer-languages (create-assoc-handler :employer/languages))
(re/reg-event-fx :page.sign-up/set-employer-biography (create-assoc-handler :employer/biography))
(re/reg-event-fx :page.sign-up/set-employer-country (create-assoc-handler :employer/country))

(re/reg-event-fx :page.sign-up/set-arbiter-full-name (create-assoc-handler :arbiter/full-name))
(re/reg-event-fx :page.sign-up/set-arbiter-professional-title (create-assoc-handler :arbiter/professional-title))
(re/reg-event-fx :page.sign-up/set-arbiter-fixed-rate-per-dispute (create-assoc-handler :arbiter/fixed-rate-per-dispute parse-float))
(re/reg-event-fx :page.sign-up/set-arbiter-email (create-assoc-handler :arbiter/email))
(re/reg-event-fx :page.sign-up/set-arbiter-github-key (create-assoc-handler :arbiter/github-key))
(re/reg-event-fx :page.sign-up/set-arbiter-linkedin-key (create-assoc-handler :arbiter/linkedin-key))
(re/reg-event-fx :page.sign-up/set-arbiter-languages (create-assoc-handler :arbiter/languages))
(re/reg-event-fx :page.sign-up/set-arbiter-biography (create-assoc-handler :arbiter/biography))
(re/reg-event-fx :page.sign-up/set-arbiter-country (create-assoc-handler :arbiter/country))

(re/reg-event-fx
 :page.sign-up/initialize-page
 (fn []
   {:forward-events
    {:register ::accounts-loaded?
     :events #{::accounts-events/accounts-changed}
     :dispatch-to [:page.sign-up/initial-query]}}))

(re/reg-event-fx
 :page.sign-up/initial-query
 (fn [{:keys [db]}]
   (let [user-address (accounts-queries/active-account db)]
     ;; TODO : query user, candidate, employer and arbiter + fields needed by components
     {:dispatch [::graphql/query {:query
                                  "query InitialQuery($address: ID!) {
                                     user(user_address: $address) {
                                       user_address
                                       user_email
                                     }
                                     candidate(user_address: $address) {
                                       user_address
                                       candidate_bio
                                     }
                                     employer(user_address: $address) {
                                       user_address
                                       employer_professionalTitle
                                     }
                                     arbiter(user_address: $address) {
                                       user_address
                                       arbiter_bio
                                     }
                                   }"
                                  :variables {:address user-address}}]})))

;; TODO : handle response
(re/reg-event-fx
 :page.sign-up/send-github-verification-code
 (fn [_ [_ code user-type]]
   {:forward-events
    {:register ::initial-query?
     :events #{:page.sign-up/initial-query}
     :dispatch-to [:page.sign-up/github-sign-up code user-type]}}))

;; TODO : response (needed?)
(re/reg-event-fx
 :page.sign-up/github-sign-up
 (fn [{:keys [db]} [_  code user-type]]
   (let [user-address (accounts-queries/active-account db)]
     {:dispatch [::graphql/query {:query
                                  "mutation GithubSignUp($githubSignUpInput: githubSignUpInput!) {
                                     githubSignUp(input: $githubSignUpInput) {
                                       todo
                                   }
                                 }"
                                  :variables {:githubSignUpInput {:code code :user_address user-address :user_type user-type}}
                                  :on-success #(>evt [::unregister-initial-query-forwarder])
                                  :on-failure #(prn "")}]})))

(re/reg-event-fx
 ::unregister-initial-query-forwarder
 (fn []
   {:forward-events {:unregister ::initial-query?}}))
