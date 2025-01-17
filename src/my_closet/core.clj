(ns my-closet.core
  (:gen-class)
  (:require [clojure.set :as set]
            [clojure.math.combinatorics :as combo]
            [my-closet.db :refer :all]))

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
  (get-clothing-items db-spec))

;checking if the combination is valid based on color, season and type
(def combination-valid?
  (memoize
    (fn [piece1 piece2]
      (and (colors-match? (:color piece1) (:color piece2))
           (seasons-match? piece1 piece2)
           (not= (:type piece1) (:type piece2))))))

;defining combination rules, based on types
(def allowed-combinations-of-types
  #{#{:top :bottom :shoes}
    #{:top :bottom :jacket :shoes}
    #{:dress :shoes}
    #{:dress :jacket :shoes}})

(defn types-allowed? [types-in-combination]
  (some #(set/subset? % types-in-combination) allowed-combinations-of-types))

(defn all-pairs-valid? [pieces-of-clothing]
  (every? (fn [[piece1 piece2]] (combination-valid? piece1 piece2))
          (for [x pieces-of-clothing y pieces-of-clothing :when (not= x y)] [x y])))

;checking validity of combinations with more pieces of clothing at once
(defn combination-of-more-pieces-valid? [pieces-of-clothing]
  (let [types-in-combination (set (map :type pieces-of-clothing))]
    (boolean
      (and (types-allowed? types-in-combination)
           (all-pairs-valid? pieces-of-clothing)))))

;generating all combinations with k elements from the list, using clojure.math.combinatorics
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

;idea is to save user's ratings by saving combinations and their opinion - like or dislike
(def user-ratings (atom {}))

;(defn save-user-feedback [combination rating]
;  (swap! user-ratings assoc combination rating))
(defn save-user-feedback [user-id combination rating]
  (swap! user-ratings update user-id
         (fn [user-data]
           (assoc (or user-data {}) combination rating))))

;co-occurrence matrix shows how often particular combinations appear together
;in user-ratings, key is combination that user likes, and values are combinations that
;often appear with it, along with number that shows how many times they are liked together
;using this, system will recommend combinations that are often liked by users that also like
;combination that our user likes, we could say they have similar taste in fashion
(defn co-occurrence [user-ratings]
  (reduce (fn [cooc [user ratings]]
            (reduce (fn [c [combo1 rating1]]
                      (reduce (fn [c [combo2 rating2]]
                                (if (and (not= combo1 combo2)
                                         (= rating1 rating2))
                                  (update-in c [combo1 combo2] (fnil inc 0))
                                  c))
                              c
                              ratings))
                    cooc
                    ratings))
          {}
          user-ratings))

;extracts recommendations that are relevant for our user based on co-ocurence matrix
;if user dislikes combination, another one is being presented, and so on
;if he likes it, that is it, and the results are being updated in user-ratings.
;if user is new and does not have any liked combinations, then a combination will be generated
;through recommendation function
(defn recommend
  [user-id user-ratings co-occurrence-matrix pieces-of-clothing season & {:keys [input-fn output-fn]
                                                                          :or   {input-fn  read-line
                                                                                 output-fn println}}]
  (let [user-rated (get @user-ratings user-id)]
    (if (empty? user-rated)
      (let [initial-recommendations (recommendation pieces-of-clothing season)]
        (output-fn (str "Initial recommendations: " initial-recommendations))
        (loop [remaining-recommendations initial-recommendations
               updated-ratings {}]
          (if-let [current (first remaining-recommendations)]
            (do
              (output-fn (str "Do you like this combination? " current " (like/dislike)"))
              (let [feedback (input-fn)]
                (cond
                  (= feedback "like")
                  (do
                    (output-fn "Thanks! This combination will be added to favorites.")
                    (swap! user-ratings assoc user-id (assoc updated-ratings current :like))
                    (save-combination-and-feedback current user-id feedback)
                    nil)

                  (= feedback "dislike")
                  (do
                    (output-fn "Ok, I will recommend another combination...")
                    (save-combination-and-feedback current user-id feedback)
                    (recur (rest remaining-recommendations)
                           (assoc updated-ratings current :dislike)))
                  :else
                  (do
                    (output-fn "Please enter 'like' or 'dislike'.")
                    (recur remaining-recommendations updated-ratings)))))))
        (output-fn "No remaining recommendations. Thanks for the feedback!")))
    (let [liked-combos (set (keys (filter #(= :like (val %)) user-rated)))
          recommendations (reduce (fn [rec combo]
                                    (merge-with + rec (get co-occurrence-matrix combo {})))
                                  {}
                                  liked-combos)]
      (output-fn (str "Recommendations based on your preferences: " recommendations))

      (loop [remaining-recommendations (->> recommendations
                                            (remove #(contains? user-rated (key %)))
                                            (sort-by val >)
                                            (map key))
             updated-ratings user-rated]
        (if-let [current (first remaining-recommendations)]
          (do
            (output-fn (str "Do you like this combination? " current " (like/dislike)"))
            (let [feedback (input-fn)]
              (cond
                (= feedback "like")
                (do
                  (output-fn "Thanks! This combination will be added to favorites.")
                  (swap! user-ratings assoc user-id (assoc updated-ratings current :like))
                  nil)

                (= feedback "dislike")
                (do
                  (output-fn "Ok, I will recommend another combination...")
                  (recur (rest remaining-recommendations)
                         (assoc updated-ratings current :dislike)))

                :else
                (do
                  (output-fn "Please enter 'like' or 'dislike'.")
                  (recur remaining-recommendations updated-ratings)))))))
      (output-fn "No remaining recommendations. Thanks for the feedback!"))))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  ;(recommend 1 user-ratings (co-occurrence user-ratings) pieces-of-clothing :summer)
  (println "Hello, World!")
  )