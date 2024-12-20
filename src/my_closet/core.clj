(ns my-closet.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

;defining color rules - every color has a set of colors that matches
(def color-rules
  {:white #{:universal}
   :black #{:universal}
   :blue  #{:white :black :beige :grey}
   :beige #{:universal}})

;checking if the colors match
(defn colors-match? [color1 color2]
  (or (= (get color-rules color1 #{}) #{:universal})
      (= (get color-rules color2 #{}) #{:universal})
      (contains? (get color-rules color1 #{}) color2)))

;checking if the seasons match
(defn seasons-match? [piece1 piece2]
  (or (= (:season piece1) :universal)
      (= (:season piece2) :universal)
      (= (:season piece1) (:season piece2))))

(def pieces-of-clothing
  [{:name "White T-shirt" :type :top :color :white :season :summer}
   {:name "Black Pants" :type :bottom :color :black :season :universal}])

;generating a recommendation on what a user should wear
(defn recommendation [pieces-of-clothing season]
  (filter #(or (= (:season %) season)
               (= (:season %) :universal))
          pieces-of-clothing))

;(recommendation pieces-of-clothing :summer)
