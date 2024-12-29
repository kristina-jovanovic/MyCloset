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


;checking if the combination is valid based on color, season and type
(defn combination-valid? [piece1 piece2]
  (and (colors-match? (:color piece1) (:color piece2))
       (seasons-match? piece1 piece2)
       (not= (:type piece1) (:type piece2))))

;defining combination rules, based on types
(def allowed-combinations-of-types
  #{#{:top :bottom :shoes}
    #{:top :bottom :jacket :shoes}
    #{:dress :shoes}
    #{:dress :jacket :shoes}})

(require '[clojure.set :as set])
;this is equal to 'some' but instead of true/nil, it returns true/false
(def has-any? (complement not-any?))

;checking validity of combinations with more pieces of clothing at once
(defn combination-of-more-pieces-valid? [pieces-of-clothing]
  ;(println "Testing pieces:" pieces-of-clothes "\n")
  (let [types-in-combination (set (map :type pieces-of-clothing))]
    ;is set of these types contained in allowed combinations of types
    ;and does every pair of pieces match
    (and (has-any? #(set/subset? % types-in-combination) allowed-combinations-of-types)
         (every? (fn [[piece1 piece2]]
                   ;(println "Testing pair:" piece1 piece2)
                   (combination-valid? piece1 piece2))
                 (for [x pieces-of-clothing y pieces-of-clothing :when (not= x y)]
                   [x y])))))

;generating all combinations with k elements from the list, using clojure.math.combinatorics
(require '[clojure.math.combinatorics :as combo])
(defn all-combinations [k list]
  (combo/combinations list k))

;using all-combinations but with different number of elements, number of elements is in a range
(defn all-combinations-in-range [min-size max-size list]
  (apply concat
         (map #(all-combinations % list)
              (range min-size (inc max-size)))))

;filters pieces of clothing based on a season, takes all combinations with 2-4 elements from filtered list
;and checks if every combined pair in that combination is combined properly
;it returns every valid combination
(defn recommendation [pieces-of-clothing season]
  (let [filtered (filter #(or (= (:season %) season)
                              (= (:season %) :universal))
                         pieces-of-clothing)]
    ;(println "Filtered pieces:" filtered)
    (filter combination-of-more-pieces-valid?
            (all-combinations-in-range 2 4 filtered))))

;(def summer-combinations
;  (recommendation pieces-of-clothing :summer))
;
;;doseq = foreach
;(doseq [comb summer-combinations]
;  (println "\nRecommended summer combination:")
;  (doseq [piece comb]
;    (println (:name piece))))
;
;(def winter-combinations
;  (recommendation pieces-of-clothing :winter))
;
;;doseq = foreach
;(doseq [comb winter-combinations]
;  (println "\nRecommended winter combination:")
;  (doseq [piece comb]
;    (println (:name piece))))

;idea is to save user's preferences by saving combinations and their opinion - like or dislike
(def user-preferences (atom {}))

(defn save-user-feedback [combination rating]
  (swap! user-preferences assoc combination rating))