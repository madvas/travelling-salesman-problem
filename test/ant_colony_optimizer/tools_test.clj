(ns travelling-salesman-problem.tools-test
  (:require [travelling-salesman-problem.tools :refer :all])
  (:use midje.sweet))

(def mylist '([0 0] [3 4] [5 12] [12 44]))
(def myset [[0 0] [3 4] [5 12] [12 44]])

(fact "get-distance works for both lists and vectors"
      (get-distance 0 1 mylist) => 5
      (get-distance 0 1 myset)  => 5)

(fact "The Euclidean distance calculation is working"
      (def mylist '([0 0] [3 4] [5 12] [12 44]))
      (get-distance 0 1 mylist) => 5
      (get-distance 1 3 mylist) => 41
      (get-distance 0 2 mylist) => 13)

(fact "Exception thrown if selected node isn't in set"
      (get-distance 1 8 '([0 0] [0 0] [1 1])) => (throws Exception))

(fact "Ensure create-keys is working properly"
  (create-keys 3) => [:01 :02 :12]
  (create-keys 5) => [:01 :02 :03 :04 :12 :13 :14 :23 :24 :34])