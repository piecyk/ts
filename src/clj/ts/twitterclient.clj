(ns ts.twitterclient
  (:use org.httpkit.server)
  (:require
   [clojure.data.json :as json]
   [http.async.client :as ac]
   [clojure.core.async :refer [chan go go-loop >!! <! >!]]
   [twitter.oauth :as oauth]
   [twitter.api.streaming :as tas]
   [twitter.callbacks.handlers :as tch]
   [cheshire.core :as cheshire])
  (:import (twitter.callbacks.protocols AsyncStreamingCallback)
           (java.io ByteArrayOutputStream)))

(def my-creds (oauth/make-oauth-creds "ui5VI9qV8zMASQcP0zGrVHnfV"
                                      "HLIJiAfMWEvpfxsIrwzbdN9njelumMB4RghYv39zlpHyJtoX51"
                                      "11288422-a4T4T5oqgWNUj3BPh5BXgSj26PpIQMcVKUmdQIbQq"
                                      "f6QTuxeJSFeckKdM8Wgmo5tZvB3N5qkz5z7kmyUjZBGfe"))

;; (def ^:dynamic *response* (tas/user-stream :oauth-creds my-creds))

(defn parse [json-str]
  (try
    (cheshire/parse-string json-str)
    (catch Exception e (println (str "caught exception: " (.getMessage e)))
           nil)))

(defn send-to-clients [clients, data]
  (if-not (nil? data)
    (doseq [client (keys @clients)]
      (println "send new tweet" client)
      (send! client (str data)))
    {}))

(defn my-statuses [clients]
  (println "my-statuses")
  (tas/statuses-filter :params {:track "scala"}
                       :oauth-creds my-creds
                       :callbacks (tas/AsyncStreamingCallback. ;; (send-to-clients clients (parse #(str %2)))
                                                               (fn [_resp payload]
                                                                 ;; (go ;; TODO: refactor
                                                                   ;;(parse payload)
                                                                   (send-to-clients clients (parse (str payload)))
                                                                   ;; (if-not (clojure.string/blank? (str payload))
                                                                   ;;   (doseq [client (keys @clients)]
                                                                   ;;     (println "send new tweet" client)
                                                                   ;;     (send! client (str payload)))
                                                                   ;;   {})
                                                                   ;; (Thread/sleep (rand-int 1000))
                                                                   )
                                                               (fn [_resp]
                                                                 (println _resp))
                                                               (fn [_resp ex]
                                                                 (.printStackTrace ex)))))
