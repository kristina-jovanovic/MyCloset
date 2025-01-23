(ns my-closet.core-test
  (:require [clojure.test :refer :all]
            [my-closet.core :refer :all]
            [midje.sweet :refer :all]
            [clojure.string :as str]
            [my-closet.db :refer :all]))

(fact "Check if the colors match"
      (colors-match? :white :black) => true
      (colors-match? :blue :green) => false)

(fact "Check if the seasons match"
      (seasons-match? {:season :summer} {:season :summer}) => true
      (seasons-match? {:season :summer} {:season :winter}) => false
      (seasons-match? {:season :universal} {:season :summer}) => true
      (seasons-match? {:season :winter} {:season :universal}) => true)

(fact "Generate recommendation"
      (recommendation pieces-of-clothing :summer) =not=> nil)

(fact "Check if the combination is valid based on color, season and types"
      (combination-valid? {:name "White T-shirt" :type :top :color :white :season :summer} {:name "Black pants" :type :bottom :color :black :season :universal}) => true
      (combination-valid? {:name "White T-shirt" :type :top :color :white :season :summer} {:name "Green T-shirt" :type :top :color :green :season :summer}) => false)

(fact "Check if the combination of more pieces is valid"
      (combination-of-more-pieces-valid?
        [{:name "Black sweater" :type :top :color :black :season :winter}
         {:name "Blue jeans" :type :bottom :color :blue :season :universal}
         {:name "Beige boots" :type :shoes :color :beige :season :winter}]) => true
      (combination-of-more-pieces-valid?
        [{:name "White T-shirt" :type :top :color :white :season :summer}
         {:name "Black pants" :type :bottom :color :black :season :universal}
         {:name "White sneakers" :type :shoes :color :white :season :summer}]) => true
      (combination-of-more-pieces-valid?
        [{:name "White T-shirt" :type :top :color :white :season :summer}
         {:name "Black pants" :type :bottom :color :black :season :universal}
         {:name "Beige boots" :type :shoes :color :beige :season :winter}
         {:name "Black winter jacket" :type :jacket :color :black :season :winter}]) => false
      (combination-of-more-pieces-valid?
        [{:name "White T-shirt" :type :top :color :white :season :summer}
         {:name "Black pants" :type :bottom :color :black :season :universal}
         {:name "Blue jeans" :type :bottom :color :blue :season :universal}]) => false)

(fact "Generate a co-occurrence matrix based on user ratings"
      (co-occurrence {:user1 {[:a] :like, [:b] :like}
                      :user2 {[:a] :like, [:c] :like}})
      => {[:a] {[:b] 1, [:c] 1}
          [:b] {[:a] 1}
          [:c] {[:a] 1}})

;(fact "Recommend combinations that are not rated by the user but co-occurring with liked ones"
;      (let [user-ratings (atom {:user1 {[:a] :like}
;                                :user2 {[:a] :like, [:b] :like}})
;            cooc {[:a] {[:b] 1}}
;            output (with-out-str (recommend :user1 user-ratings cooc pieces-of-clothing :summer
;                                            :input-fn (fn [] "like")
;                                            :output-fn println))]
;        (str/includes? output "[:b]") => true))
;
;(fact "Recommend a combination, ask for feedback, and update ratings"
;      (let [user-ratings (atom {:user1 {[:a] :like}})
;            cooc {[:a] {[:b] 1}}]
;
;        (recommend :user1 user-ratings cooc pieces-of-clothing :summer
;                   :input-fn (fn [] "like")
;                   :output-fn println)
;
;        ;check if the user-ratings is updated properly (we simulate user entering 'like')
;        @user-ratings => {:user1 {[:a] :like, [:b] :like}}
;
;        (let [printed-output (with-out-str (recommend :user1 user-ratings cooc pieces-of-clothing :summer
;                                                      :input-fn (fn [] "like")
;                                                      :output-fn println))]
;          (or (str/includes? printed-output "Initial recommendations: ")
;              (str/includes? printed-output "Recommendations based on your preferences: {[:b] 1}")
;              (str/includes? printed-output "Do you like this combination?")
;              (str/includes? printed-output "Ok, I will recommend another combination...")
;              (str/includes? printed-output "Thanks! This combination will be added to favorites.")) => true
;          )))

;(fact "Recommend combinations for a new user, update ratings, and verify output"
;      (let [user-ratings (atom {})
;            cooc {[:a] {[:b] 1}}]
;
;        ;simulate the recommendation process and capture output
;        (let [printed-output (with-out-str
;                               (recommend :user1 user-ratings cooc pieces-of-clothing :summer
;                                          :input-fn (fn [] "like")
;                                          :output-fn println))]
;
;          ;check that the user-ratings is updated for :user1
;          (get @user-ratings :user1) =not=> nil
;          (some #(= :like (val %)) (get @user-ratings :user1)) => true
;
;          ;verify that specific strings are present in the output
;          (str/includes? printed-output "Initial recommendations:") => true
;          (str/includes? printed-output "Do you like this combination?") => true
;          (str/includes? printed-output "Thanks! This combination will be added to favorites.") => true)))

(fact "Get all clothing items from database"
      (get-clothing-items db-spec) =not=> nil)

(fact "Insert combination, combination_items and user_feedback to database"
      (let [summer-combinations (recommendation pieces-of-clothing :summer)]
        (insert-combination-and-feedback (first summer-combinations) 1 "like") =not=> nil))

(fact "Get user feedback from database"
      (get-user-feedback db-spec) =not=> nil)

(fact "Insert user feedback to database"
      (insert-feedback 3 15 "dislike") =not=> nil)