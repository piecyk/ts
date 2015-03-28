(ns ts.handler
  (:use org.httpkit.server)
  (:require [compojure.core :refer [GET defroutes]]
            [compojure.route :refer [not-found resources]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [selmer.parser :refer [render-file]]
            [prone.middleware :refer [wrap-exceptions]]
            [environ.core :refer [env]]
            [clojure.data.json :as json]
            [clojure.core.async :refer [<! >! >!! put! close! go-loop go]]
            [ts.twitterclient :as tst]
            ))

(def clients (atom {}))

(tst/my-statuses clients)

(defn ws-handler [req]
  (with-channel req channel
    (println "WebSocket channel connected")
    (swap! clients assoc channel true)
    (on-receive channel (fn [data]
                          (println "got data: " data)
                          (send! channel data)))
    (on-close channel (fn [status]
                        (swap! clients dissoc channel)
                        (println "closed, status")))))

(defroutes routes
  (GET "/" [] (render-file "templates/index.html" {:dev (env :dev?)}))
  (GET "/ws" [] ws-handler)
  (resources "/")
  (not-found "Not Found"))

(def app
  (let [handler (wrap-defaults routes site-defaults)]
    (if (env :dev?) (wrap-exceptions handler) handler)))
