(ns asteroids.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent.format :refer [format]]))

(defonce app-state (atom {:asteroids '({:x 20 :y 20 :size 4 :speed-x 2.2 :speed-y 1.1 :rotation 13 :key 1}
                                      {:x 795 :y 8 :size 4 :speed-x -1.9 :speed-y 1.3 :rotation 184 :key 2})}))

(defn wraparound [val increment max-val min-val]
  (let [new-val (+ val increment)]
    (cond
      (> new-val max-val) min-val
      (< new-val min-val) max-val
      :else new-val
      )
    )
  )

(defn update-asteroid [roid]
  (-> roid
    (assoc :x (wraparound (:x roid) (:speed-x roid) 820 -20))
    (assoc :y (wraparound (:y roid) (:speed-y roid) 620 -20))
    )
  )

(defn split-asteroid [roid]
  (if (> (:size roid) 1)
    (list {:x (:x roid) , :y (:y roid) , :speed-x (:speed-x roid) ,
         :speed-y (* 1.5 (:speed-y roid)) , :size (/ (:size roid) 2) , :rotation (rand-int 360) , :key (rand-int 1000000)}
        {:x (:x roid) , :y (:y roid) , :speed-y (:speed-y roid) ,
         :speed-x (* 1.5 (:speed-x roid)) , :size (/ (:size roid) 2) , :rotation (rand-int 360) , :key (rand-int 1000000)})
    '()
    )

  )

(defn split-destroyed-asteroids [roids]
  (let [destroyed (filter #(:destroyed %) roids)]
    (reduce #(concat %1 (split-asteroid %2)) roids destroyed)
    )
  )

(defn update-all-asteroids! []
  (swap! app-state assoc :asteroids (->> (:asteroids @app-state)
                                        (map update-asteroid)
                                        split-destroyed-asteroids
                                        (filter #(not (:destroyed %)))
                                      )
                                    )
  )

;; -------------------------
;; Views

(defn asteroid [props]
  [:polygon {:points "-18,-8 -3,-5 -8,-18 6,-18 18,-8 18,8 11,19 -9,22 -6,6 -18,8"
             :fill "none"
             :transform (format "translate (%d %d) scale(%d) rotate(%d)"
                            (:x props) (:y props)
                            (:size props)
                            (:rotation props))}]
  )

(defn asteroid-debug-info [props]
  [:div.entityDebug
    [:div [:strong "Id: "] (:key props) ]
    [:div [:strong "Location: "] (format "%.2f , %.2f" (:x props) (:y props))]
    [:button {:on-click (fn [] (swap! app-state assoc :asteroids (map #(if (= (:key %) (:key props)) (assoc % :destroyed true) %) (:asteroids @app-state))))} "Destroy" ]
  ]
  )

(defn screen [asteroids]
  (js/setTimeout update-all-asteroids! 80)
  [:svg {:viewBox "0 0 800 600"}
    (for [astr asteroids]
      ^{:key (:key astr)} [asteroid astr]
    )]
  )

(defn entity-list [entities]
  [:div#entitylist
  (for [entity entities]
    ^{:key (:key entity)} [asteroid-debug-info entity]
    )]
  )

(defn home-page []
  [:div [:h2 "Asteroids in SVG"]
   [:div [:a {:href "/about"} "go to about page"]]
   [:div [screen (:asteroids @app-state)] [entity-list (:asteroids @app-state)]]
   ])

(defn about-page []
  [:div [:h2 "About asteroids"]
    "This will gradually become a clone of the arcade classic Asteroids. It's written in clojurescript/reagent and uses SVG to draw the game screen."
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
