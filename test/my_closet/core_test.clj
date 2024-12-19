(ns my-closet.core-test
  (:require [clojure.test :refer :all]
            [my-closet.core :refer :all]
            [midje.sweet :refer :all]))

(fact "Check if the colors match"
      (colors-match? :white :black) => true
      (colors-match? :blue :green) => false)

