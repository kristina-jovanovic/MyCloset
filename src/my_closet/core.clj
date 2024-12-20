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

;defining pieces of clothing
(def white-t-shirt {:name "White T-shirt" :type :top :color :white :season :summer})
(def green-t-shirt {:name "Green T-shirt" :type :top :color :green :season :summer})
(def black-sweater {:name "Black sweater" :type :top :color :black :season :winter})
(def black-pants {:name "Black pants" :type :bottom :color :black :season :universal})
(def blue-jeans {:name "Blue jeans" :type :bottom :color :blue :season :universal})
(def white-sneakers {:name "White sneakers" :type :shoes :color :white :season :summer})
(def beige-boots {:name "Beige boots" :type :shoes :color :beige :season :winter})
(def black-winter-jacket {:name "Black winter jacket" :type :jacket :color :black :season :winter})

(def pieces-of-clothing
  [white-t-shirt
   black-pants
   white-sneakers
   green-t-shirt
   black-winter-jacket
   black-sweater
   blue-jeans
   beige-boots])

;generating a recommendation on what a user should wear
(defn recommendation [pieces-of-clothing season]
  (filter #(or (= (:season %) season)
               (= (:season %) :universal))
          pieces-of-clothing))

;checking if the combination is valid based on color, season and type
(defn combination-valid? [piece1 piece2]
  (and (colors-match? (:color piece1) (:color piece2))
       (seasons-match? piece1 piece2)
       (not= (:type piece1) (:type piece2))))
