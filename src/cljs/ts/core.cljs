(ns ts.core
  (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [cognitect.transit :as ct]
              [cljs.core.async :refer [chan <! >! put! close! timeout]])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]])
  (:import goog.History))

;; (def r (ct/reader :json))

(def send (chan))
(def receive (chan))
(def tweets (atom []))

(def ws-url
  (let [url (clojure.string/replace js/window.location.href #"^http" "ws")]
    (str url "ws")))
(def ws (js/WebSocket. ws-url))

(defn add-tweet [tweets new-tweet]
  (.log js/console "add-tweet" (count tweets))
  (->> (cons new-tweet tweets)
       (take 10)))

(defn recive-tweet []
  (.log js/console "recive-tweet")
  (go-loop []
    (when-let [msg (<! receive)]
      (.log js/console "recive-tweet for chan")
      ;; sad panda
      (swap! tweets add-tweet (.-data msg))
      (recur))))

(defn make-receiver []
  (.log js/console "make recevier")
  (set! (.-onmessage ws) (fn [msg] (put! receive msg)))
  (recive-tweet))

(defn parse [obj]
  (try
    (.parse js/JSON obj)
    (catch :default e
      js/undefined)))

(defn render-tweets []
  [:div "Tweets stream for statuses @scala:"
   [:ul
    (for [tweet @tweets]
      (let [el (parse tweet)]
        (if (= el js/undefined)
          (.log js/console "el is undefined")
          ^{:key (.-id el)} [:li (.-text el)]
          ))
          )]])

;; -------------------------
;; Views

(defn home-page []
  [:div [:h3 "Welcome and wait few sec..."]
   (render-tweets)])

;; (defn about-page []
;;   [:div [:h2 "About ts"]
;;    [:div [:a {:href "#/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes
(secretary/set-config! :prefix "#")

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

;; (secretary/defroute "/about" []
;;   (session/put! :current-page #'about-page))

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
  (make-receiver)
  (mount-root))
