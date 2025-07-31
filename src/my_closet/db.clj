(ns my-closet.db
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.string :as str]))

(def db-spec
  {:dbtype   "mysql"
   :dbname   "my_closet_database"
   :host     "localhost"
   :port     3306
   :user     "root"
   :password ""})

(defn format-clothing-items [data]
  (map (fn [item]
         (let [renamed-item (set/rename-keys item {:pieces_of_clothing/piece_id :piece-id
                                                   :pieces_of_clothing/name     :name
                                                   :pieces_of_clothing/type     :type
                                                   :pieces_of_clothing/color    :color
                                                   :pieces_of_clothing/season   :season
                                                   :pieces_of_clothing/style    :style
                                                   :pieces_of_clothing/photo    :photo})]
           (-> renamed-item
               (update :type keyword)
               (update :color keyword)
               (update :season keyword))))
       data))

(defn format-combination [data]
  (map (fn [item]
         (set/rename-keys item {:combinations/combination_id :combination-id
                                :combinations/name           :name
                                :combinations/pieces         :pieces
                                :combinations/style          :style}))
       data))


(defn get-clothing-items [db-spec]
  (format-clothing-items (jdbc/execute! db-spec
                                        ["SELECT * FROM `pieces_of_clothing`"])))

;logic in the beggining
(defn determine-combination-style [pieces]
  (let [styles (map #(set (remove str/blank?
                                  (str/split (or (:style %) "") #",")))
                    pieces)
        all-styles (apply set/union styles)
        has-formal? (contains? all-styles "formal")
        has-party? (contains? all-styles "party")
        has-strict-work? (some #(= #{"work"} %) styles)
        has-work? (contains? all-styles "work")
        has-casual? (contains? all-styles "casual")
        all-universal? (every? #(= #{"universal"} %) styles)]
    (cond
      has-formal? "formal"
      has-party? "party"
      has-strict-work? "work"
      all-universal? "universal"
      (and has-casual? has-work?) "casual,work"
      :else "casual")))

;insert new combination and feedback
(defn insert-feedback [user-id combination-id opinion]
  (jdbc/execute! db-spec
                 ["INSERT INTO feedback (user_id, combination_id, opinion) VALUES (?, ?, ?)"
                  user-id combination-id opinion]))

(defn insert-combination-and-feedback [combination user-id opinion]
  (println "📥 [insert-combination-and-feedback] RAW combination data:" combination)
  (jdbc/with-transaction [tx db-spec]
                         (do
                           (try
                             (if (:combination-id combination)
                               (do
                                 (println "combination ima :combination-id => vec postoji u bazi, preskacem insert u combinations")
                                 (insert-feedback user-id (:combination-id combination) opinion)
                                 (println "feedback dodat za postojecu kombinaciju sa ID:" (:combination-id combination)))

                               (let [items (cond
                                             (and (sequential? combination)
                                                  (map? (first combination))
                                                  (:piece-id (first combination)))
                                             (do (println "combination je lista sa piece-id") combination)

                                             (and (map? combination) (:pieces combination))
                                             (let [piece-ids (map #(Integer/parseInt (clojure.string/trim %))
                                                                  (clojure.string/split (:pieces combination) #","))]
                                               (println "combination je mapa sa :pieces stringom -> piece-ids:" piece-ids)
                                               (let [resolved (format-clothing-items
                                                                (jdbc/execute! tx
                                                                               (into [(str "SELECT * FROM `pieces_of_clothing` WHERE piece_id IN ("
                                                                                           (clojure.string/join "," (repeat (count piece-ids) "?")) ")")]
                                                                                     piece-ids)))]
                                                 (println "ucitani podaci iz baze za piece-ids:" resolved)
                                                 resolved))

                                             :else
                                             (do
                                               (println "combination je u neocekivanom formatu:" (type combination))
                                               (throw (ex-info "Unsupported combination format"
                                                               {:combination combination}))))

                                     description (str/join ", " (map :name items))
                                     pieces (str/join "," (map :piece-id items))
                                     style (determine-combination-style items)]

                                 (println "spremam insert nove kombinacije:")
                                 (println "    description:" description)
                                 (println "    pieces:" pieces)
                                 (println "    style:" style)

                                 (jdbc/execute! tx
                                                ["INSERT INTO combinations (name, pieces, style) VALUES (?, ?, ?)"
                                                 description pieces style])

                                 (let [inserted-id (-> (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS last_id"])
                                                       :last_id)]
                                   (println "INSERT kombinacije ok, id:" inserted-id)
                                   (insert-feedback user-id inserted-id opinion)
                                   (println "feedback sacuvan za novu kombinaciju"))))

                             (catch Exception e
                               (println "Unexpected error while inserting combination:" (.getMessage e))
                               (throw e))))))


(defn format-user-feedback [data]
  (map (fn [item]
         (let [renamed-item (set/rename-keys item {:feedback/user_id        :user-id
                                                   :feedback/combination_id :combination-id
                                                   :feedback/opinion        :opinion
                                                   :feedback/rating         :rating})]
           (-> renamed-item
               (update :opinion keyword)
               (update :rating keyword))))
       data))

(defn get-user-feedback [db-spec]
  (format-user-feedback (jdbc/execute! db-spec
                                       ["SELECT * FROM `feedback`"])))


(defn update-feedback [user-id combination-id rating]
  (jdbc/execute! db-spec
                 ["UPDATE feedback SET rating=? WHERE user_id=? AND combination_id=?"
                  rating user-id combination-id]))

(defn get-combination [combination-id]
  (format-combination (jdbc/execute! db-spec
                                     ["SELECT * FROM `combinations`
                                        WHERE combination_id=?"
                                      combination-id])))

;(defn get-combination [combination-id]
;  (let [result (jdbc/execute! db-spec
;                              ["SELECT * FROM `combinations` WHERE combination_id=?" combination-id])
;        raw (first result)]
;    (when raw
;      (set/rename-keys raw {:combinations/combination_id :combination-id
;                            :combinations/name           :name
;                            :combinations/pieces         :pieces
;                            :combinations/style          :style
;                            ;:combinations/season         :season
;                            }))))


(defn get-combination-items [combination-id]
  (let [result (jdbc/execute! db-spec
                              ["SELECT pieces FROM combinations WHERE combination_id=?" combination-id])
        pieces-str (:combinations/pieces (first result))]
    (if (nil? pieces-str)
      []
      (let [piece-ids (map #(Integer/parseInt (str %)) (clojure.string/split pieces-str #","))
            items (format-clothing-items (jdbc/execute! db-spec
                                                        (into [(str "SELECT * FROM `pieces_of_clothing` WHERE piece_id IN ("
                                                                    (clojure.string/join "," (repeat (count piece-ids) "?")) ")")]
                                                              piece-ids)))]
        (seq items)))))

