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

