(ns travelling-salesman-problem.aco-visualizer
  (:require [quil.core :as q :include-macros true]
            [quil.middleware :as m]
            [travelling-salesman-problem.colony :as c]
            [clojure.core.async :as a :refer [chan >!!]]
            [travelling-salesman-problem.tools :as t]
            [travelling-salesman-problem.config :refer [colony-config]]))

(defn setup [init-state]
  (q/frame-rate 60)
  init-state)

(defn draw-path
  ([waypoints path]
   (q/stroke 237 107 134)
   (q/stroke-weight 6)
   (q/fill 05)
   (let [path2 (conj (subvec path 1) (first path))
         get-waypt (fn [index] (nth waypoints index))]

     (doall (map #(q/line (get-waypt %1) (get-waypt %2)) path path2)))))

(defn draw-waypoints [waypoints]
  (q/fill 70 50 57)
  (q/stroke-weight 0)
  (doall (map #(q/ellipse (first %) (second %) 20 20) waypoints)))

(defn text-line [text line-number]
  (let [line-height 15
        line-base-y 20]
    (q/text text 10 (+ line-base-y (* line-number line-height)))))

(defn draw-text [state]
  (q/fill 255)
  (text-line "Press V - (Re)start route calculation" 0)
  (text-line "Press C - Clear canvas" 1)
  (text-line (str "Tours: " (:tour-count colony-config)) 2)
  (text-line (str "Ants: " (:ant-count colony-config)) 3)
  (text-line (str "Best distance: " (:distance @(:best-route state))) 4))

(defn draw [state]
  (q/background 127 178 133)
  (let [waypoints (:waypoints state)
        best-path (:path @(:best-route state))]
    (when (seq best-path)
      (draw-path waypoints best-path))
    (draw-waypoints (:waypoints state))
    (draw-text state)))

(defn update-state [state]
  state)

(defn mouse-clicked [old-state event]
  (if (and (= :left (:button event)) (not (:running old-state)))
    (update old-state :waypoints #(into [] (conj % [(:x event) (:y event)])))
    old-state))

(defn key-typed [waypoints-ch old-state event]
  (let [waypoints (:waypoints old-state)]
    (condp = (:key event)
      :c (do (t/new-best-route (:best-route old-state))
             (-> old-state
                 (dissoc :waypoints)
                 (dissoc :running)))
      :v (if (> (count waypoints) 1)
           (do (t/new-best-route (:best-route old-state))
               (>!! waypoints-ch waypoints)
               (assoc old-state :running true))
           old-state)
      old-state)))

(defn create-sketch [width height colony]
  (let [waypoints-ch (chan)]
    (q/defsketch travelling-salesman
                 :title "Travelling salesman problem"
                 :size [width height]
                 :draw draw
                 :features [:keep-on-top :resizable]
                 :setup (partial setup (select-keys colony [:best-route]))
                 :middleware [m/fun-mode]
                 :update update-state
                 :settings #(q/smooth 2)                    ;; Turn on anti-aliasing
                 :mouse-clicked mouse-clicked
                 :key-typed (partial key-typed waypoints-ch))
    waypoints-ch))