# Travelling Salesman Problem
### Multithreaded Clojure solution

This is small clojure application for solving [Travelling salesman problem](https://en.wikipedia.org/wiki/Travelling_salesman_problem) via [Ant colony optimization algorithm](https://en.wikipedia.org/wiki/Ant_colony_optimization_algorithms). As a base for my app I used [ RT-Anderson/ant-colony-optimizer](https://github.com/RT-Anderson/ant-colony-optimizer), to which I added concurrency, feature to choose arbitrary waypoints, simple UI. 

![App demo](https://cloud.githubusercontent.com/assets/3857155/13004806/33a00086-d1b0-11e5-89ea-16c181779bfb.gif "App demo")

#### How it works?
In the app, you specify number of "tours" and number of "ants" (besides other configs). Number of tours is how many times all ants will go to search for shortest route between waypoints. Each ant in a single tour is executed in a separate thread via [Clojure agents](http://clojure.org/reference/agents). Each tour is executed sequentialy, because it has to pass pheromones to another. After all tours are completed, you should see shortest route between waypoints you've chosen. 

This is as far as I understand it, I'm no expert on Ant Colony Algorithms, if you know better, please let me know ;) 

#### How to run?
`lein run`

or

`lein uberjar`

`java -jar target/travelling-salesman-problem-0.1.0-standalone.jar`

Feel free to use this code anyhow you want ;) 
