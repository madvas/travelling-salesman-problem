(ns travelling-salesman-problem.config)

(def sketch-size [800 400])

(def colony-config
  {:tour-count  100
   :ant-count   200
   :alpha-coeff 1
   :beta-coeff  1.1
   :tour-coeff  1})

(def world-config
  {:evap-rate 0.6})