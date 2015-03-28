(ns ts.server
  (:require [ts.handler :refer [app]]
            [org.httpkit.server :as http-kit])
  (:gen-class))

 (defn -main [& args]
   (let [port (Integer/parseInt (or (System/getenv "PORT") "3000"))]
     (println "Starting http-kit..." port)
     ;;(run-jetty app {:port port :join? false})
     (http-kit/run-server app {:port port})
     ))
