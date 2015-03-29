(ns ts.helpers
  (:require
   [cognitect.transit :as ct]
   [cljs.core.async :as async :refer [<! >! chan close! sliding-buffer dropping-buffer put! timeout]])
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer [go alt!]]))

(defn ws-url [chan]
  (let [url (clojure.string/replace js/window.location.href #"^http" "ws")]
    (str url chan)))

(defn log [& args]
 (.log js/console (str args)))

;; use cognitect.transit
;; (def r (ct/reader :json))
(defn parse [obj]
  (try (.parse js/JSON obj)
       (catch :default e js/undefined)))

(defn event-chan
  ([type] (event-chan js/window type))
  ([el type] (event-chan (chan) el type))
  ([c el type]
    (let [writer #(put! c %)]
      (.addEventListener el type writer)
      {:chan c
       :unsubscribe #(.removeEventListener el type writer)})))
