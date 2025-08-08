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

(defn insert-combination-and-feedback [combination combination-id user-id opinion]
  (jdbc/with-transaction [tx db-spec]
                         (try
                           (let [; sa fronta stize :combination-id ili :combination_id
                                 combination-id (or combination-id
                                                    (:combination-id combination)
                                                    (:combination_id combination))]

                             (if combination-id
                               ; vec postoji kombinacija
                               (do
                                 (println "combination ima ID => preskacem insert, pisem feedback za ID:" combination-id)
                                 (jdbc/execute! tx
                                                ["INSERT INTO feedback (user_id, combination_id, opinion) VALUES (?, ?, ?)"
                                                 user-id combination-id opinion])
                                 (println "feedback dodat za postojecu kombinaciju"))

                               ; treba da formiramo/nadjemo kombinaciju po komadima
                               (let [items (cond
                                             ; direktno poslata lista komada (mape sa :piece-id)
                                             (and (sequential? combination)
                                                  (map? (first combination))
                                                  (:piece-id (first combination)))
                                             (do (println "combination je lista sa piece-id") combination)

                                             ; mapa sa :pieces stringom "1,2,3" -> ucitaj ih iz baze
                                             (and (map? combination) (:pieces combination))
                                             (let [piece-ids (->> (clojure.string/split (:pieces combination) #",")
                                                                  (map clojure.string/trim)
                                                                  (map #(Integer/parseInt %)))]
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
                                               (throw (ex-info "Unsupported combination format" {:combination combination}))))

                                     description (clojure.string/join ", " (map :name items))
                                     pieces      (clojure.string/join "," (map :piece-id items))
                                     style       (determine-combination-style items)

                                     ; pokusaj da nadjes postojecu kombinaciju po 'pieces'
                                     existing    (jdbc/execute-one! tx
                                                                    ["SELECT combination_id FROM combinations WHERE pieces = ?" pieces])
                                     comb-id     (:combinations/combination_id existing)]

                                 (if comb-id
                                   (do
                                     (println "Postojeca kombinacija pronadjena, ID:" comb-id)
                                     (jdbc/execute! tx
                                                    ["INSERT INTO feedback (user_id, combination_id, opinion) VALUES (?, ?, ?)"
                                                     user-id comb-id opinion])
                                     (println "feedback sacuvan za postojecu kombinaciju"))

                                   (do
                                     (println "Spremam insert nove kombinacije:")
                                     (println "    description:" description)
                                     (println "    pieces:" pieces)
                                     (println "    style:" style)

                                     (jdbc/execute! tx
                                                    ["INSERT INTO combinations (name, pieces, style) VALUES (?, ?, ?)"
                                                     description pieces style])

                                     (let [inserted-id (-> (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS last_id"])
                                                           :last_id)]
                                       (println "INSERT kombinacije OK, id:" inserted-id)
                                       (jdbc/execute! tx
                                                      ["INSERT INTO feedback (user_id, combination_id, opinion) VALUES (?, ?, ?)"
                                                       user-id inserted-id opinion])
                                       (println "feedback sacuvan za novu kombinaciju")))))))

                           (catch Exception e
                             (println "Unexpected error while inserting combination:" (.getMessage e))
                             (throw e)))))


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

(defn get-one-user-rated-combos [user-id]
  (let [results (jdbc/execute! db-spec
                               ["SELECT pieces FROM combinations WHERE combination_id IN
                                (SELECT combination_id FROM feedback WHERE user_id = ?)"
                                user-id])]
    (println "rezultati iz baze:" results)
    (set
      (keep (fn [row]
              (let [pieces-str (or (:pieces row) (:combinations/pieces row))]
                (if (some? pieces-str)
                  (->> (str/split pieces-str #",")
                       (map parse-long)
                       sort                                 ;sortiram da bi mi uvek bio isti redosled kad proveravam
                       (str/join ","))
                  (do
                    (println "upozorenje: nije pronađen :pieces u redu:" row)
                    nil))))
            (map #(set/rename-keys % {:combinations/pieces :pieces}) results)))))

(defn update-feedback [user-id combination-id rating]
  (jdbc/execute! db-spec
                 ["UPDATE feedback SET rating=? WHERE user_id=? AND combination_id=?"
                  rating user-id combination-id]))

(defn get-combination [combination-id]
  (format-combination (jdbc/execute! db-spec
                                     ["SELECT * FROM `combinations`
                                        WHERE combination_id=?"
                                      combination-id])))

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

(defn format-liked-combinations [data]
  (map (fn [item]
         (let [renamed-item (set/rename-keys item {:combinations/combination_id :combination-id
                                                   :combinations/name           :name
                                                   :combinations/pieces         :pieces
                                                   :combinations/style          :style
                                                   :feedback/opinion            :opinion
                                                   :feedback/rating             :rating})]
           (-> renamed-item
               (update :opinion keyword))))
       data))

(defn get-liked-combinations [user-id]
  (format-liked-combinations (jdbc/execute! db-spec
                                            ["SELECT c.*, f.opinion, f.rating FROM feedback f JOIN combinations c
                                              ON f.combination_id=c.combination_id
                                              WHERE f.opinion=\"like\" AND f.user_id=?"
                                             user-id])))

(defn get-favorite-combinations [user-id]
  (format-combination (jdbc/execute! db-spec
                                     ["SELECT c.* FROM feedback f JOIN combinations c
                                              ON f.combination_id=c.combination_id
                                              WHERE f.rating=5 AND f.user_id=?"
                                      user-id])))

(defn format-users [data]
  (map (fn [item]
         (set/rename-keys item {:users/user_id  :user-id
                                :users/username :username
                                :users/password :password}))
       data))

(defn get-users [db-spec]
  (format-users (jdbc/execute! db-spec
                               ["SELECT * FROM `users`"])))