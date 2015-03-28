(ns ts.core
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [cljs.core.async :refer [chan <! >! put! close! timeout]])
    (:import goog.History))


(def create-ws-url
  (let [url (clojure.string/replace js/window.location.href #"^http" "ws")]
    (str url "ws")))

(def conn
  (js/WebSocket. create-ws-url))

(defn send-to-server [msg]
  (do
    (.send conn (.stringify js/JSON (js-obj "msg" msg)))))

(set! (.-onload js/window)
      (fn []
        (go
          (send-to-server "test"))))

(set! (.-onopen conn)
  (fn [e]
    (.send conn
      (.stringify js/JSON (js-obj "command" "getall")))))

(set! (.-onerror conn)
  (fn []
    (.log js/console js/arguments)))

(set! (.-onmessage conn)
  (fn [e]
    (let [msgs (.parse js/JSON (.-data e))]
      (.log js/console msgs))))

;; -------------------------
;; Views

(defn home-page []
  [:div [:h2 "Welcome to ts"]
   [:div [:a {:href "#/about"} "go to about page"]]])

(defn about-page []
  [:div [:h2 "About ts"]
   [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

;; -------------------------
;; History
;; must be called after routes have been defined
(defn hook-browser-navigation! []
  (doto (History.)
    (events/listen
     EventType/NAVIGATE
     (fn [event]
       (secretary/dispatch! (.-token event))))
    (.setEnabled true)))

;; -------------------------
;; Initialize app
(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (hook-browser-navigation!)
  (mount-root))
