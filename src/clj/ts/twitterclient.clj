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

(def my-creds (oauth/make-oauth-creds "ui5VI9qV8zMASQcP0zGrVHnfV"
                                      "HLIJiAfMWEvpfxsIrwzbdN9njelumMB4RghYv39zlpHyJtoX51"
                                      "11288422-a4T4T5oqgWNUj3BPh5BXgSj26PpIQMcVKUmdQIbQq"
                                      "f6QTuxeJSFeckKdM8Wgmo5tZvB3N5qkz5z7kmyUjZBGfe"))

;; (def ^:dynamic *response* (tas/user-stream :oauth-creds my-creds))

(defn my-statuses [clients]
  (tas/statuses-filter :params {:track "scala"}
                       :oauth-creds my-creds
                       :callbacks (tas/AsyncStreamingCallback.
                                   (fn [_resp payload]
                                     (go ;; TODO: refactor
                                       (if-not (clojure.string/blank? (str payload))
                                         (doseq [client (keys @clients)]
                                           (println "send new tweet")
                                           (send! client (str payload)))
                                         {})
                                       (Thread/sleep (rand-int 1000))
                                       ))
                                   (fn [_resp]
                                     (println _resp))
                                   (fn [_resp ex]
                                     (.printStackTrace ex)))))
