(ns ts.twitterclient
  (:use org.httpkit.server)
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
   [clojure.core.async :refer [chan go go-loop >!! <! >!]]
   [twitter.oauth :as oauth]
   [twitter.api.streaming :as tas]
   [twitter.callbacks.handlers :as tch])
  (:import (twitter.callbacks.protocols AsyncStreamingCallback)))

(def my-creds (oauth/make-oauth-creds "YqxzmJQXggdZqtQJ32zhuXXyl"
                                      "lqIX7svDLRwV1ButIuHhdJPMQ4TA9tt4gKT6ddBQNXZ6REjhW7"
                                      "11288422-CkpsJFt1Bx3CXq74TXrC0rQxqudiwzzhevwWTmnSq"
                                      "4PpQEGFrKM2pRnCxgwlDCmKbw6eVqnUikL8QrQmSn6Fjh"))

;;(def ^:dynamic *response* (tas/user-stream :oauth-creds my-creds))

(defn my-statuses [clients]
  (tas/statuses-filter :params {:track "scala"}
                       :oauth-creds my-creds
                       :callbacks (tas/AsyncStreamingCallback.
                                   (fn [_resp payload]
                                     (go
                                       (if-not (clojure.string/blank? (str payload))
                                         (doseq [client (keys @clients)]
                                           (println "send new status")
                                           (send! client (str payload)))
                                         {})
                                       (Thread/sleep (rand-int 1500))
                                       ))
                                   (fn [_resp]
                                     (println _resp))
                                   (fn [_resp ex]
                                     (.printStackTrace ex)))))
