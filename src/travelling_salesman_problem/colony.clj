(ns travelling-salesman-problem.colony
  [:require [travelling-salesman-problem.tools :as t]
            [clojure.math.numeric-tower :as math]
            [travelling-salesman-problem.config :refer [colony-config world-config]]])

(def ant-agents-timeout 5000)

(defrecord Ant [ant-key])

(defrecord Tour [tour-key ants])

(defrecord Colony [best-route])

(defrecord World [waypoints pher-map pher-update pher-reset
                  pher-update-reset evap-map])

(defn agent-err-handler [ag ex]
  (println "Agent error occured: " ex " value " #_@ag))

(defn create-ants [n]
  (for [i (range n)
        :let [ant (agent (->Ant i))]]
    (do (set-error-handler! ant agent-err-handler)
        ant)))

(defn create-tours [n]
  (let [ant-cnt (:ant-count colony-config)]
    (for [i (range n)]
      (map->Tour {:tour-key            (inc i)
                  :ants                (create-ants ant-cnt)}))))

(defn create-colony []
  (map->Colony {:best-route (t/new-best-route)}))

(defn create-world [waypoints]
  (let [keylist (t/create-keys (count waypoints))
        ones-map (zipmap keylist (repeat 1))
        zeros-map (zipmap keylist (repeat 0))
        evaporation-map (zipmap keylist (repeat (- 1 (:evap-rate world-config))))]
    (map->World
      {:pher-map          (ref ones-map)
       :pher-update       (ref zeros-map)
       :pher-reset        ones-map
       :pher-update-reset zeros-map
       :evap-map          evaporation-map
       :keylist           keylist
       :waypoints         waypoints})))

(defn edge-weight
  ;; Compute the numerator component of the edge-selection probability from pt1 to pt2
  [pt1 pt2 waypoints pheremone-map]
  (let [nu (/ 1 (t/get-distance pt1 pt2 waypoints))
        tau ((t/get-edge pt1 pt2) pheremone-map)]
    (* (math/expt nu (:alpha-coeff colony-config))
       (math/expt tau (:beta-coeff colony-config)))))

(defn prob-weighting
  ;; Computes the probability threshold for the next node to be selected
  ;; Note: this returns the index of the selected value in rem-nodes ; NOT the selected nodeg
  ([active-node rem-nodes waypoints pheremone-map threshold-val]

   (let [weight-num (map #(edge-weight active-node % waypoints pheremone-map) rem-nodes)
         weight-denom (reduce + weight-num)
         weight (mapv #(/ % weight-denom) weight-num)
         weight-cnt (count weight)
         threshold (loop [incremented-weight weight
                          counter 1]
                     (if (= weight-cnt counter)
                       incremented-weight
                       (recur (#(assoc %2 %1 (+ (nth %2 (dec %1)) (nth %2 %1))) counter incremented-weight)
                              (inc counter))))
         selected-threshold-index (.indexOf threshold (first (filter #(< threshold-val %) threshold)))]
     selected-threshold-index))
  ([active-node rem-nodes waypoints pheremone-map]
   (prob-weighting active-node rem-nodes waypoints pheremone-map (rand))))


(defn make-path
  ;; Generates the vector of waypoints visited for a single trip
  [waypoints pheremone-map]
  (let [open-nodes (shuffle (range 0 (count waypoints)))]
    (loop [nodes (conj [(first open-nodes)])
           remaining-nodes (t/remove-vec-element open-nodes 0)]
      (if (>= 1 (count remaining-nodes))
        (conj nodes (last remaining-nodes))

        (let [next-node-index (prob-weighting (last nodes) remaining-nodes waypoints pheremone-map)
              next-node (nth remaining-nodes next-node-index)]
          (recur (conj nodes next-node) (t/remove-vec-element remaining-nodes next-node-index)))))))

(defn path-score
  ;; Weighted score of route
  [path waypoints tour-coeff]
  (let [path2 (conj (subvec path 1) (first path))
        path-distances (reduce + (map #(t/get-distance %1 %2 waypoints) path path2))]
    (try
      (/ tour-coeff path-distances)
      (catch Exception e (str "caught exception: " (.getMessage e))))))

(defn update-pher-map [world]
  (merge-with + @(:pher-update world)
              (merge-with * @(:pher-map world) (:evap-map world))))

(defn ant-behave
  ;; 1) Stores a closed loop route touching all waypoints
  ;; 2) Generates the route score (Q/Dist)
  ;; 3) Path-Offset shifts path indices by 1 (preparatory function for next step
  ;; 4) Generates a map of all the node-edges selected
  ;; 5) (evaluate-route) If this is the best global path, the global path/score is updated
  ;; 6) Adds pheremone deposits for this tour
  ;;
  ;; **Last 3 inputs to function are atoms
  [_ waypoints best-route pher-map pheremone-update]
  (let [path (make-path waypoints @pher-map)
        score (path-score path waypoints (:tour-coeff colony-config))
        path-offset (conj (subvec path 1) (first path))
        edges-to-update (map t/get-edge path path-offset)]
    (dosync
      (when (> score (:score @best-route))
        (alter best-route (constantly {:score    score
                                       :path     path
                                       :distance (format "%.2f" (t/get-route-distance path waypoints))})))
      (dorun (map #(alter pheremone-update update-in [%] + score) edges-to-update)))))

(defn tour-behave
  ;; Has N number of ants complete a full route and update the pheremone value
  [tour colony world]
  #_(println "Tour " (:tour-key tour) " in progress " world)
  (let [ant-agents (map #(send-off % ant-behave
                                   (:waypoints world)
                                   (:best-route colony)
                                   (:pher-map world)
                                   (:pher-update world))
                        (:ants tour))]


    (if (apply await-for (cons ant-agents-timeout ant-agents))
      (let [new-pher-map (update-pher-map world)]
        (dosync (alter (:pher-map world) (constantly new-pher-map))))
      (println "timeout for ants has expired"))))


(defn execute
  ;; Function to begin running the ant-tours
  [colony world]
  (let [tour-count (:tour-count colony-config)]
    (loop [tours (create-tours tour-count)]
      (if (seq tours)
        (do
          (when (zero? (mod (count tours) 10))
            (println "Tours remaining: " (count tours)))
          (tour-behave (first tours) colony world)
          (recur (rest tours)))
        (do
          (let [best-route @(:best-route colony)]
            (println "Shortest path found: " (str (:path best-route)))
            (println "Path distance: " (str (:distance best-route)))))))))