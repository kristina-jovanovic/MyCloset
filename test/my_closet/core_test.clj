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

(fact "Get all clothing items from database"
      (get-clothing-items db-spec) =not=> nil)

(fact "Insert combination, combination_items and user_feedback to database"
      (let [summer-combinations (recommendation pieces-of-clothing :summer)]
        (insert-combination-and-feedback (first summer-combinations) 1 "like") =not=> nil))

(fact "Get user feedback from database"
      (get-user-feedback db-spec) =not=> nil)

(fact "Insert user feedback to database"
      (insert-feedback 3 15 "dislike") =not=> nil)

(fact "Testing updated co-occurrence function"
      (let [ratings1 [{:user-id 1, :combination-id 15, :rating :like}
                      {:user-id 1, :combination-id 16, :rating :like}
                      {:user-id 2, :combination-id 15, :rating :like}
                      {:user-id 2, :combination-id 17, :rating :like}
                      {:user-id 2, :combination-id 18, :rating :dislike}]
            ratings2 [{:user-id 1, :combination-id 15, :rating :like}
                      {:user-id 1, :combination-id 16, :rating :like}
                      {:user-id 3, :combination-id 15, :rating :like}
                      {:user-id 3, :combination-id 17, :rating :like}
                      {:user-id 3, :combination-id 18, :rating :like}
                      {:user-id 4, :combination-id 15, :rating :like}
                      {:user-id 4, :combination-id 18, :rating :like}
                      {:user-id 1, :combination-id 34, :rating :like}]]
        (co-occurrence 1 ratings1) => {17 1}
        (co-occurrence 2 ratings1) => {16 1}
        (co-occurrence 1 ratings2) => {17 1, 18 2}))

(fact "Recommend combinations based on application logic"
      (let [ratings '({:user-id 1, :combination-id 15, :rating :like}
                      {:user-id 1, :combination-id 16, :rating :like}
                      {:user-id 1, :combination-id 31, :rating :dislike}
                      {:user-id 1, :combination-id 32, :rating :dislike}
                      {:user-id 1, :combination-id 33, :rating :dislike}
                      {:user-id 1, :combination-id 34, :rating :like}
                      {:user-id 3, :combination-id 15, :rating :dislike})
            pieces '({:id 1, :type :top, :color :white, :season :summer, :name "White T-shirt", :photo "..."}
                     {:id 2, :type :bottom, :color :black, :season :universal, :name "Black pants", :photo "..."}
                     {:id 3, :type :shoes, :color :white, :season :universal, :name "White sneakers", :photo "..."}
                     {:id 4, :type :top, :color :green, :season :summer, :name "Green T-shirt", :photo "..."}
                     {:id 8, :type :jacket, :color :black, :season :winter, :name "Black Winter Jacket", :photo "..."})]

        (recommend-combinations 2 ratings :summer pieces) => '(({:id 1, :type :top, :color :white, :season :summer, :name "White T-shirt", :photo "..."}
                                                                {:id 2, :type :bottom, :color :black, :season :universal, :name "Black pants", :photo "..."}
                                                                {:id 3, :type :shoes, :color :white, :season :universal, :name "White sneakers", :photo "..."})
                                                               ({:id 2, :type :bottom, :color :black, :season :universal, :name "Black pants", :photo "..."}
                                                                {:id 3, :type :shoes, :color :white, :season :universal, :name "White sneakers", :photo "..."}
                                                                {:id 4, :type :top, :color :green, :season :summer, :name "Green T-shirt", :photo "..."}))))

(fact "Recommend combinations based on co-occurrence matrix"
      (let [ratings '({:user-id 1, :combination-id 15, :rating :like}
                      {:user-id 1, :combination-id 16, :rating :like}
                      {:user-id 1, :combination-id 31, :rating :dislike}
                      {:user-id 3, :combination-id 32, :rating :like}
                      {:user-id 1, :combination-id 33, :rating :dislike}
                      {:user-id 1, :combination-id 34, :rating :like}
                      {:user-id 3, :combination-id 15, :rating :like})
            pieces '({:id 1, :type :top, :color :white, :season :summer, :name "White T-shirt", :photo "..."}
                     {:id 2, :type :bottom, :color :black, :season :universal, :name "Black pants", :photo "..."}
                     {:id 3, :type :shoes, :color :white, :season :universal, :name "White sneakers", :photo "..."}
                     {:id 4, :type :top, :color :green, :season :summer, :name "Green T-shirt", :photo "..."}
                     {:id 8, :type :jacket, :color :black, :season :winter, :name "Black Winter Jacket", :photo "..."})]

        (recommend-combinations 1 ratings :summer pieces) => [32]))

(fact "Checking format for return value of recommend-combinations"
      (let [ratings1 '({:user-id 1, :combination-id 15, :rating :like}
                      {:user-id 1, :combination-id 16, :rating :like}
                      {:user-id 1, :combination-id 31, :rating :dislike}
                      {:user-id 1, :combination-id 32, :rating :dislike}
                      {:user-id 1, :combination-id 33, :rating :dislike}
                      {:user-id 1, :combination-id 34, :rating :like}
                      {:user-id 3, :combination-id 15, :rating :dislike})
            ratings2 '({:user-id 1, :combination-id 15, :rating :like}
                       {:user-id 1, :combination-id 16, :rating :like}
                       {:user-id 1, :combination-id 31, :rating :dislike}
                       {:user-id 3, :combination-id 32, :rating :like}
                       {:user-id 1, :combination-id 33, :rating :dislike}
                       {:user-id 1, :combination-id 34, :rating :like}
                       {:user-id 3, :combination-id 15, :rating :like})
            pieces '({:id 1, :type :top, :color :white, :season :summer, :name "White T-shirt", :photo "..."}
                     {:id 2, :type :bottom, :color :black, :season :universal, :name "Black pants", :photo "..."}
                     {:id 3, :type :shoes, :color :white, :season :universal, :name "White sneakers", :photo "..."}
                     {:id 4, :type :top, :color :green, :season :summer, :name "Green T-shirt", :photo "..."}
                     {:id 8, :type :jacket, :color :black, :season :winter, :name "Black Winter Jacket", :photo "..."})]
        (recommend 1 ratings1 :summer pieces) => "Recommendation is a generic set of clothing combinations"
        (recommend 1 ratings2 :summer pieces) => "Recommendation is a list of combination IDs"))