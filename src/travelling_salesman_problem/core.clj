(ns travelling-salesman-problem.core
  (:gen-class)
  [:require [travelling-salesman-problem.colony :as c]
            [travelling-salesman-problem.aco-visualizer :as vis]
            [clojure.core.async :refer [go-loop <!]]
            [travelling-salesman-problem.config :refer [sketch-size]]])

(defn -main [& args]
  (let [colony (c/create-colony)
        waypoints-ch (vis/create-sketch (first sketch-size) (second sketch-size) colony)]
    (go-loop []
             (let [waypoints (<! waypoints-ch)
                   world (c/create-world waypoints)]
               (c/execute colony world)
               (recur)))))

#_ (-main)