(ns my-closet.core-test
  (:require [clojure.test :refer :all]
            [my-closet.core :refer :all]
            [midje.sweet :refer :all]))

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
      (combination-valid? white-t-shirt black-pants) => true
      (combination-valid? white-t-shirt green-t-shirt) => false)

(fact "Check if the combination of more pieces is valid"
      (combination-of-more-pieces-valid? [black-sweater blue-jeans beige-boots]) => true
      (combination-of-more-pieces-valid? [white-t-shirt black-pants white-sneakers]) => true
      (combination-of-more-pieces-valid? [white-t-shirt black-pants beige-boots black-winter-jacket]) => false
      (combination-of-more-pieces-valid? [white-t-shirt black-pants blue-jeans]) => false)

(fact "Store feedback from user"
      (reset! user-ratings {})
      (save-user-feedback :user1 [:white-t-shirt :black-pants :white-sneakers] :like)
      @user-ratings => { :user1 {[:white-t-shirt :black-pants :white-sneakers] :like} })

(fact "Generate a co-occurrence matrix based on user ratings"
      (cooccurrence {:user1 {[:a] :like, [:b] :like}
                     :user2 {[:a] :like, [:c] :like}})
      => {[:a] {[:b] 1, [:c] 1}
          [:b] {[:a] 1}
          [:c] {[:a] 1}})

(fact "Recommend combinations that are not rated by the user but co-occurring with liked ones"
      (recommend :user1 {:user1 {[:a] :like}
                         :user2 {[:a] :like, [:b] :like}}
                 {[:a] {[:b] 1}})
      => '([:b]))

