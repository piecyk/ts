(ns ts.core
  (:require [reagent.core :as reagent :refer [atom]]
              [reagent.session :as session]
              [secretary.core :as secretary :include-macros true]
              [goog.events :as events]
              [goog.history.EventType :as EventType]
              [cljsjs.react :as react]
              [cljs.core.match :refer-macros [match]]
              [cljs.core.async :refer [chan <! >! put! close! timeout]]
              [ts.helpers :as h])
  (:require-macros
   [cljs.core.async.macros :refer [alt! go go-loop]])
  (:import goog.History))

(def ws (js/WebSocket. (h/ws-url "ws")))
(def send (chan))
(def receive (chan))
(def tweets (atom []))

(defn add-tweet [tweets new-tweet]
  (h/log "add-tweet" (count tweets))
  (->> (cons new-tweet tweets)
       (take 10)))

;; sad panda
(defn recive-tweet []
  (h/log "recive-tweet")
  (go (while true
        (let [msg (<! receive)]
          (h/log "recive-tweet for chan")
          ;; very sad panda...
          (let [t (h/parse (.-data msg))]
            (if (= t js/undefined)
              (h/log "t is undefined:" (.-data msg))
              (swap! tweets add-tweet t)))))))

(defn make-receiver []
  (h/log "make recevier")
  (set! (.-onmessage ws) (fn [msg] (put! receive msg)))
  (recive-tweet))

(defn render-tweets []
  [:div "Tweets stream for statuses @scala:"
   [:ul (for [tweet @tweets]
          ^{:key (.-id tweet)} [:li (.-text tweet)])]])

;; checking on heroku?
;; (go-loop []
;;   (when-let [msg (<! receive)]
;;     (h/log "we have msg")
;;     (recur)))

;; (go (while true
;;       (alt!
;;         receive ([result] (h/log "we have?")))))

;; async
(def mc (:chan (h/event-chan js/window "mousemove")))
(def kc (:chan (h/event-chan js/window "keyup")))

(defn handler [[e c]]
  (h/log e)
  (match [e]
         [{"x" x "y" y}] (h/log (str "mouse: " x ", " y))
         [{"keyCode" code}] (h/log "key:" code)
         :else (h/log "hmm?" e)))

(go
  (while true
    (handler (alts! [mc kc]) )))

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
