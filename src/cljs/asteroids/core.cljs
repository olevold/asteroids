(ns asteroids.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent.format :refer [format]]))

(defonce app-state (atom {:asteroids [{:x 20 :y 20 :size 2 :speed-x 5 :speed-y 5}
                                      {:x 795 :y 8 :size 2 :speed-x 5 :speed-y -5}]}))
;; -------------------------
;; Views

(defn asteroid [props]
  [:polygon {:points "-20,-10 -5,-5 -10,-20 8,-20 20,-10 20,10 13,21 -11,24 -8,8 -20,10"
             :transform (format "translate (%d %d) scale(%d) rotate(%d)"
                            (:x props) (:y props)
                            (:size props)
                            (rand-int 360))}]
  )

(defn screen [asteroids]
  [:svg {:viewBox "0 0 800 600"}
    (for [astr asteroids]
      ^{:key astr} [asteroid astr]
    )]
  )

(defn home-page []
  [:div [:h2 "Welcome to asteroids"]
   [:div [:a {:href "/about"} "go to about page"]]
   [:div [screen (:asteroids @app-state)] ]])

(defn about-page []
  [:div [:h2 "About asteroids"]
   [:div [:a {:href "/"} "go to the home page"]]])


;; -------------------------
;; Routes

(defonce page (atom #'home-page))

(defn current-page []
  [:div [@page]])

(secretary/defroute "/" []
  (reset! page #'home-page))

(secretary/defroute "/about" []
  (reset! page #'about-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
