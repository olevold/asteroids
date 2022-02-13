(ns asteroids.core
    (:require [reagent.core :as reagent :refer [atom]]
              [secretary.core :as secretary :include-macros true]
              [accountant.core :as accountant]
              [reagent.format :refer [format]]))

(def app-state (atom {:asteroids '(
                            {:x 20 :y 20 :size 4 :speed-x 1.8 :speed-y 1.1 :rotation 13 :key 1}
                            {:x 795 :y 8 :size 4 :speed-x -1.3 :speed-y 1.3 :rotation 184 :key 2}
                            )
                          :ship {:x 400 :y 300 :rotation 0}
                          :destroyed #{}
                          :bullets '()
                          :fire false
                    }
                  )
                )

(.addEventListener
  js/document
  "keydown"
  (fn [e]
    (cond
      (= "ArrowRight" (.-key e)) (swap! app-state assoc :right true)
      (= "ArrowLeft" (.-key e)) (swap! app-state assoc :left true)
      (= "Control" (.-key e)) (swap! app-state assoc :fire true)
      )
    )
  )

  (.addEventListener
    js/document
    "keyup"
    (fn [e]
      (cond
        (= "ArrowRight" (.-key e)) (swap! app-state assoc :right false)
        (= "ArrowLeft" (.-key e)) (swap! app-state assoc :left false)
        )
      )
    )

(defn degrees-to-radians [degrees] (-> degrees (* 3.14) (/ 180)))

(defn wraparound [val increment max-val min-val]
  (let [new-val (+ val increment)]
    (cond
      (> new-val max-val) min-val
      (< new-val min-val) max-val
      :else new-val
      )
    )
  )

(defn update-entity [entity]
  (-> entity
    (assoc :x (wraparound (:x entity) (:speed-x entity) 820 -20))
    (assoc :y (wraparound (:y entity) (:speed-y entity) 620 -20))
    )
  )

(defn split-asteroid [roid]
  (if (> (:size roid) 0.3)
    (list {:x (:x roid) , :y (:y roid) , :speed-x (:speed-x roid) ,
         :speed-y (* 1.5 (:speed-y roid)) , :size (/ (:size roid) 2) , :rotation (rand-int 360) , :key (rand-int 1000000)}
        {:x (:x roid) , :y (:y roid) , :speed-y (:speed-y roid) ,
         :speed-x (* 1.5 (:speed-x roid)) , :size (/ (:size roid) 2) , :rotation (rand-int 360) , :key (rand-int 1000000)})
    '()
    )

  )

(defn destroyed? [roid] (contains? (:destroyed @app-state) (:key roid)))

(defn split-destroyed-asteroids [roids]
  (let [destroyed (filter destroyed? roids)]
    (reduce #(concat %1 (split-asteroid %2)) roids destroyed)
    )
  )

(defn update-all-entities! []
  (let [ship           (:ship @app-state)
        destroyed-list (:destroyed @app-state)
        fire           (:fire @app-state)
        left           (:left @app-state)
        right          (:right @app-state)
        asteroids      (:asteroids @app-state)
        bullets        (->>
                          @app-state
                          :bullets
                          (map update-entity)
                          (map #(update % :ttl dec))
                          (filter #(> (:ttl %) 0)))
        ]
      (reset! app-state {
        :ship (assoc ship :rotation
            (cond
                left (-> ship (get :rotation) (- 12))
                right (-> ship (get :rotation) (+ 12))
                :else (:rotation ship)
              )
          )
        :destroyed (:destroyed @app-state)
        :fire false
        :left left
        :right right
        :asteroids (->> asteroids
                        (map update-entity)
                        split-destroyed-asteroids
                        (filter #(not (destroyed? %)))
                        doall
                    )
        :bullets (if (and fire (< (count bullets) 5))
          (conj bullets {:x (:x ship)
                         :y (:y ship)
                         :ttl 14
                         :speed-x (->> ship :rotation degrees-to-radians Math/sin (* 18))
                         :speed-y (->> ship :rotation degrees-to-radians Math/cos (* -18))
                         :key (rand-int 1000000)
                         })
          bullets)
        })
    )
  )

;; -------------------------
;; Views

(defn ship [props]
  [:polygon {:points "0 -15 -9 15 9 15"
             :fill "none"
             :transform (format "translate (%d %d) rotate(%d)"
                            (:x props) (:y props)  (:rotation props))
              }
        ]
  )

(defn bullet [props]
  [:circle {:r 1
            :fill "white"
            :transform (format "translate (%d %d)" (:x props) (:y props))}]
  )

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
    [:button {:on-click (fn [] (swap! app-state assoc :destroyed (conj (:destroyed @app-state) (:key props))))} "Destroy" ]
  ]
  )

(defn screen [asteroids bullets ship_]
  (js/setTimeout update-all-entities!  60)
  [:svg {:viewBox "0 0 800 600"}
    (for [astr asteroids]
      ^{:key (:key astr)} [asteroid astr]
    )
    (for [bullet_ bullets]
      ^{:key (:key bullet_)} [bullet bullet_]
    )
    (ship ship_)]
  )

(defn entity-list [entities]
  [:div#entitylist
  (for [entity entities]
    ^{:key (:key entity)} [asteroid-debug-info entity]
    )]
  )

(defn home-page []
  [:div
   [:h2]
   [:div [:a {:href "/about"} "go to about page"]]
   [:div [screen (:asteroids @app-state) (:bullets @app-state) (:ship @app-state) ] [entity-list (:asteroids @app-state)]]
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
