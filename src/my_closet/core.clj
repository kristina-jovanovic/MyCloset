(ns my-closet.core
  (:require [clojure.set :as set]
            [clojure.math.combinatorics :as combo]
            [my-closet.db :refer :all]
            [ring.util.response :refer [header response]]
            ;[ring.adapter.jetty :refer [run-jetty]]
            [ring.adapter.jetty :as ring-jetty]
            [reitit.ring :as ring]
            [muuntaja.core :as m]
            [reitit.ring.middleware.muuntaja :as muuntaja]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.util.response :refer [header response]])
  (:gen-class))

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

;(recommendation pieces-of-clothing :summer)

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
(def user-ratings (get-user-feedback db-spec))

;co-occurrence matrix shows how often particular combinations appear together
;in user-ratings, key is combination that user likes, and values are combinations that
;often appear with it, along with number that shows how many times they are liked together
;using this, system will recommend combinations that are often liked by users that also like
;combination that our user likes, we could say they have similar taste in fashion

;co-occurrence shows combinations that we assume our user will like, based on fact
;that our user liked particular combination, and so did some other user - and now
;combinations that other user liked are being recommended to our user, because
;we can say they have similar taste in fashion
;co-occurrence contains of pairs: combination-id and 'weight' that represents
;how many times that combinations appears liked together with combinations that our user likes
(defn co-occurrence [user-id user-ratings]
  (let [grouped-ratings (group-by :user-id user-ratings)    ;we group user-ratings by user
        target-user-ratings (filter #(and (= (:user-id %) user-id)
                                          (= (:rating %) :like))
                                    user-ratings)           ; filter liked ratings for our user
        target-combinations (set (map :combination-id target-user-ratings))] ; combinations that targer user liked
    (->> grouped-ratings
         (reduce (fn [recommendations [other-user other-ratings]]
                   (if (= other-user user-id)
                     recommendations                        ; skipping target user
                     (reduce (fn [recs rating]
                               (let [comb-id (:combination-id rating)]
                                 (if (and (= (:rating rating) :like) ; only liked combinations
                                          (not (target-combinations comb-id))) ; combinations that target user didn't evaluate
                                   (update recs comb-id (fnil inc 0))
                                   recs)))
                             recommendations
                             other-ratings)))
                 {}))))

; recommend combinations in two ways - if user does not have ratings (user feedback) yet,
; recommend him a combination based on application logic in recommendation function;
; if user has rated combinations - find 'similar' combinations from other users that
; liked combinations that our user liked - so we can assume they have similar taste
(defn recommend-combinations [user-id user-ratings season pieces-of-clothing]
  (let [user-rated (filter #(= (:user-id %) user-id) user-ratings)] ; target user's ratings
    (if (empty? user-rated)
      (do
        ;(println "User has no ratings, returning generic recommendations.")
        (recommendation pieces-of-clothing season))         ; generic recommendation
      ;it returns seq of combinations that contain pieces of clothing
      (let [co-occurrences (co-occurrence user-id user-ratings)
            recommendations (->> co-occurrences
                                 (sort-by val >)            ; sort by value descending
                                 (map key))]                ; combination-id only
        (if (empty? recommendations)
          (do
            ;(println "No recommendations from co-occurrence matrix, returning generic recommendations.")
            (recommendation pieces-of-clothing season))     ; generic recommendation
          ;it returns seq of combinations that contain pieces of clothing

          (do
            ;(println "Sorted recommendations by weight:" recommendations)
            recommendations)
          ;it returns seq of combination-ids sorted by weight
          ))
      )
    ))

;if combination is a seq, it is generic recommendation and we will return random combination from combinations
;if combination is a number, it is recommended through co-occurence and we will return first one,
;because they are sorted by weights and the first one is the most accurate
(defn recommend [user-id user-ratings season pieces-of-clothing]
  (let [combinations
        (recommend-combinations user-id user-ratings season pieces-of-clothing)]
    (cond
      (and (seq? combinations)
           (seq? (first combinations)))
      (do
        (rand-nth combinations))
      (and (seq? combinations)
           (number? (first combinations)))
      (do
        (get-combination (first combinations)))
      :else
      (do
        (str "Unexpected format for recommendations")))))

;(recommend 2 user-ratings :summer pieces-of-clothing)

(defn home-response [_]
  {:status 200
   :body   "Welcome to My Closet!"})

(def filters (atom {}))

(defn set-filters [{new-filters :body-params}]
  (reset! filters new-filters)
  {:status 200
   :body   @filters})
;{:casual true, :work false, :formal false, :party false, :summer true, :winter false}

(defn get-recommendations-response [_]
  {:status 200
   :body (recommend-combinations 2 user-ratings :summer pieces-of-clothing)})

(defn my-clothes-response [_]
  {:status 200
   :body   pieces-of-clothing})

(def app
  (ring/ring-handler
    (ring/router
      ["/"
       ["get-recommendations" {:get  get-recommendations-response
                               :post set-filters}]
       ["my-clothes" my-clothes-response]
       ["" home-response]]
      {:data {:muuntaja   m/instance
              :middleware [muuntaja/format-middleware]}})))

(defn start []
  (ring-jetty/run-jetty (wrap-cors app
                                   :access-control-allow-origin [#"http://localhost:8280"]
                                   :access-control-allow-methods [:get :post]) {:port 3000 :join? false}))

(defn -main
  [& args]
  (start))