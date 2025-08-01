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
            [ring.util.response :refer [header response]]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [clojure.string :as str])
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

;filters
(def filters (atom {}))

(defn style-filter-passes? [item filters]
  (let [desired-styles (->> [:casual :work :formal :party]
                            (filter #(get filters %))
                            (map name)
                            (set))
        item-styles (->> (clojure.string/split (:style item) #",")
                         (map clojure.string/trim)
                         set)]
    (or (empty? desired-styles)
        (not (empty? (set/intersection desired-styles item-styles))))))


(defn combination-style-valid? [combination filters]
  (let [desired-styles (->> [:casual :work :formal :party]
                            (filter #(get filters %))
                            (map name)
                            set)
        combination-styles (->> combination
                                (mapcat #(clojure.string/split (:style %) #","))
                                set)]
    (or (empty? desired-styles)
        (every? #(or (contains? combination-styles %)
                     (contains? combination-styles "universal"))
                desired-styles))))


(defn season-filter-passes? [item filters]
  (let [season (:season item)]
    (cond
      (:summer filters) (or (= season :summer) (= season :universal))
      (:winter filters) (or (= season :winter) (= season :universal))
      :else true)))

(defn passes-filters? [item filters]
  (let [passes-style? (style-filter-passes? item filters)
        passes-season? (season-filter-passes? item filters)]
    (when (or (not passes-style?) (not passes-season?))
      ;(println "Item NE prolazi filtere:" (:name item))
      ;(println "   Style:" (:style item) "| Season:" (:season item))
      ;(println "   Style OK?:" passes-style? " | Season OK?:" passes-season?)
      )
    (and passes-style? passes-season?)))

; when we check combination that already exists in database, when we are using co-occurence matrix
(defn combination-passes-filters? [combo filters]
  (let [selected-style (some (fn [[k v]] (when (and v (#{:casual :work :formal :party} k)) k)) filters)
        selected-season (cond
                          (:summer filters) "summer"
                          (:winter filters) "winter"
                          :else nil)
        combo-style-str (str (:style combo))
        combo-styles (set (clojure.string/split combo-style-str #","))
        combo-season (str (:season combo))]

    (println "\n--- PROVERAVAM KOMBINACIJU ---")
    (println "Kombinacija ID:" (:id combo))
    (println "STYLE u kombinaciji:" combo-style-str)
    (println "SEASON u kombinaciji:" combo-season)
    (println "Trazeni stil:" (name selected-style))
    (println "Trazena sezona:" selected-season)

    (let [style-ok? (or (nil? selected-style)
                        (contains? combo-styles (name selected-style))
                        (contains? combo-styles "universal"))
          season-ok? (or (nil? selected-season)
                         (= combo-season "universal")
                         (= combo-season selected-season))]

      (println "Stil OK?" style-ok?)
      (println "Sezona OK?" season-ok?)

      (and style-ok? season-ok?))))


;filters pieces of clothing based on a season, takes all combinations with 2-4 elements from filtered list
;and checks if every combined pair in that combination is combined properly
;it returns every valid combination
(defn recommendation [user-id pieces-of-clothing season]
  (println "Svi komadi odece:" (map :name pieces-of-clothing))
  (let [filtered (filter #(passes-filters? % @filters) pieces-of-clothing)]
    (println "broj komada nakon passes-filters?:" (count filtered))
    (let [all-combos (all-combinations-in-range 2 4 filtered)]
      (println "generisanih kombinacija:" (count all-combos))
      (let [valid-combos (filter combination-of-more-pieces-valid? all-combos)]
        (println "validne kombinacije po tipu/boji/sezoni:" (count valid-combos))
        (let [style-combos (filter #(combination-style-valid? % @filters) valid-combos)
              seen-combos (get-one-user-rated-combos user-id)
              unseen-combos (remove
                              (fn [combo]
                                (let [combo-str (str/join "," (sort (map :piece-id combo)))]
                                  (println "proveravam da li je combo vec vidjen:" combo-str)
                                  (contains? seen-combos combo-str)))
                              style-combos)]
          (println "kombinacije koje korisnik NIJE video ranije:" (count unseen-combos))
          (vec unseen-combos))))))

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
                                          (= (:opinion %) :like))
                                    user-ratings)           ; filter liked ratings for our user
        target-combinations (set (map :combination-id target-user-ratings))] ; combinations that targer user liked
    ;(println "lajkovane kombinacije korisnika" user-id ":" target-combinations)

    (vec
      (->> grouped-ratings
           (reduce (fn [recommendations [other-user other-ratings]]
                     (if (= other-user user-id)
                       recommendations                      ; skipping target user
                       (reduce (fn [recs op]
                                 (let [comb-id (:combination-id op)]
                                   (if (and (= (:opinion op) :like) ; only liked combinations
                                            (not (target-combinations comb-id))) ; combinations that target user didn't evaluate
                                     (update recs comb-id (fnil inc 0))
                                     recs)))
                               recommendations
                               other-ratings)))
                   {})))))

; recommend combinations in two ways - if user does not have ratings (user feedback) yet,
; recommend him a combination based on application logic in recommendation function;
; if user has rated combinations - find 'similar' combinations from other users that
; liked combinations that our user liked - so we can assume they have similar taste
(defn recommend-combinations [user-id user-ratings season pieces-of-clothing]
  (let [user-rated (filter #(= (:user-id %) user-id) user-ratings)]
    (if (empty? user-rated)
      (do
        (println "korisnik nema ocena — koristim aplikacionu logiku.")
        (recommendation user-id pieces-of-clothing season))
      (let [co-occurrences (co-occurrence user-id user-ratings)]
        (println "CO-OCCURRENCE MAPA (RAW):" co-occurrences)

        (if (empty? co-occurrences)
          (recommendation user-id pieces-of-clothing season)
          (let [sorted-ids (->> co-occurrences
                                (sort-by val >)
                                (map key)
                                vec)
                filtered-ids (->> sorted-ids
                                  (filter (fn [id]
                                            (when-let [combo (get-combination id)]
                                              (combination-passes-filters? combo @filters))))
                                  vec)]
            filtered-ids))))))

;if combination is a seq, it is generic recommendation and we will return random combination from combinations
;if combination is a number, it is recommended through co-occurence and we will return first one,
;because they are sorted by weights and the first one is the most accurate

(defn recommend [user-id user-ratings season pieces-of-clothing]
  (let [combinations (recommend-combinations user-id user-ratings season pieces-of-clothing)]
    (println "Final recommendations:" combinations)
    (cond
      ;(empty? combinations)
      ;[] ; nema preporuka, ali to je validno

      (and (sequential? combinations)
           (sequential? (first combinations)))
      combinations        ; generička preporuka

      (and (sequential? combinations)
           (number? (first combinations)))
      (mapv get-combination combinations)

      :else
      (do
        (println "Unexpected format for recommendations:" combinations)
        []))))

;(recommend 2 user-ratings :summer pieces-of-clothing)

(defn home-response [_]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    "Welcome to My Closet!"})

(defn set-filters [{new-filters :body-params}]
  (println "Primljeni filteri:" new-filters)
  (reset! filters new-filters)
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    @filters})
;{:casual true, :work false, :formal false, :party false, :summer true, :winter false}

(defn get-recommendations-response [_]
  (let [recommendations (recommend 2 user-ratings :summer pieces-of-clothing)]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/generate-string recommendations)}))

(defn get-one-recommendation-response [req]
  (let [id (:body-params req)
        combination (get-combination id)]
    {:status  200
     :headers {"Content-Type" "application/json"}
     :body    (json/generate-string combination)}))

(defn my-clothes-response [_]
  {:status  200
   :headers {"Content-Type" "application/json"}
   :body    (json/generate-string pieces-of-clothing)})

(defn insert-feedback-response [req]
  (let [feedback (:body-params req)
        user-id (:user-id feedback)
        combination (:combination feedback)
        opinion (:opinion feedback)]
    (try
      (insert-combination-and-feedback combination user-id opinion)
      {:status  200
       :headers {"Content-Type" "application/json"}
       :body    (json/generate-string {:msg     "Feedback saved successfully."
                                       :opinion opinion})}
      (catch Exception e
        (println "Error during insert-feedback:" (.getMessage e))
        {:status  409
         :headers {"Content-Type" "application/json"}
         :body    (json/generate-string {:msg "Failed to save feedback. Possibly already exists."})}))))


(def app
  (-> (ring/ring-handler
        (ring/router
          ["/"
           ["get-recommendations" {:get  get-recommendations-response
                                   :post set-filters}]
           ["get-combination" get-one-recommendation-response]
           ["my-clothes" my-clothes-response]
           ["insert-feedback" {:post    insert-feedback-response
                               :options (fn [_] {:status 200})}]
           ["" home-response]]
          {:data {:muuntaja   m/instance
                  :middleware [muuntaja/format-middleware]}}))
      (wrap-cors
        :access-control-allow-origin [#"http://localhost:8280"]
        :access-control-allow-methods [:get :post :options]
        :access-control-allow-headers ["Content-Type"])
      wrap-json-response))

(defn start []
  (ring-jetty/run-jetty (wrap-cors app
                                   :access-control-allow-origin [#"http://localhost:8280"]
                                   :access-control-allow-methods [:get :post :options]
                                   :access-control-allow-headers ["Content-Type"])
                        {:port 3000 :join? false}))

(defn -main
  [& args]
  (start))