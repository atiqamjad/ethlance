(ns ethlance.server.contract.ethlance-comment
  "EthlanceComment contract methods"
  (:refer-clojure :exclude [count last])
  (:require
   [bignumber.core :as bn]
   [cljs-web3.eth :as web3-eth]
   [district.server.smart-contracts :as contracts]
   [ethlance.shared.enum.user-type :as enum.user-type]))


(def ^:dynamic *comment-key*
  "Comment Key"
  nil) ;; [:ethlance-comment "0x0"]


(defn- requires-comment-key
  "Asserts the correct use of the comment functions."
  []
  (assert *comment-key* "Given function needs to be wrapped in 'with-ethlance-comment"))


(defn call
  "Call the bound EthlanceComment contract with the given
  `method-name` and `args`."
  [method-name & args]
  (requires-comment-key)
  (apply contracts/contract-call *comment-key* method-name args))


(defn user-type [] (enum.user-type/val->kw (call :user_type)))
(defn user-address [] (call :user_address))
(defn date-created [] (call :date_created))
(defn date-updated [] (call :date_updated))


(defn update!
  "Update the comment with a revised comment contained in the provided `metahash`"
  [metahash]
  (call :update metahash))


(defn count
  "Get the number of comment revisions."
  []
  (when-let [n (call :get-count)]
    (bn/number n)))


(defn revision-by-index
  "Get the comment revision at the given index"
  [index]
  (call :get-revision-by-index index))


(defn last
  "Get the current comment metahash"
  []
  (call :get-last))
