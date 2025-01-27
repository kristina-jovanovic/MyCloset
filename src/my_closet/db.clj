(ns my-closet.db
  (:gen-class)
  (:require [next.jdbc :as jdbc]
            [clojure.set :as set]
            [clojure.string :as str]))

(def db-spec
  {:dbtype   "mysql"
   :dbname   "my_closet_db"
   :host     "localhost"
   :port     3306
   :user     "root"
   :password ""})

(defn format-clothing-items [data]
  (map (fn [item]
         (let [renamed-item (set/rename-keys item {:clothing_items/clothing_item_id :id
                                                   :clothing_items/type             :type
                                                   :clothing_items/color            :color
                                                   :clothing_items/season           :season
                                                   :clothing_items/name             :name
                                                   :clothing_items/photo            :photo})]
           (-> renamed-item
               (update :type keyword)
               (update :color keyword)
               (update :season keyword))))
       data))

(defn get-clothing-items [db-spec]
  (format-clothing-items (jdbc/execute! db-spec
                                        ["SELECT * FROM `clothing_items`"])))

;insert new combination and feedback
(defn insert-combination-and-feedback [combination user-id rating]
  (jdbc/with-transaction [tx db-spec]
                         (let [description (str/join ", " (map :name combination))
                               _ (jdbc/execute! tx
                                                ["INSERT INTO combinations (description) VALUES (?)"
                                                 description])
                               inserted-id (-> (jdbc/execute-one! tx ["SELECT LAST_INSERT_ID() AS last_id"])
                                               :last_id)]
                           (doseq [piece combination]
                             (jdbc/execute! tx
                                            ["INSERT INTO combination_items (clothing_item_id, combination_id) VALUES (?, ?)"
                                             (:id piece) inserted-id]))
                           (jdbc/execute! tx
                                          ["INSERT INTO user_feedback (user_id, combination_id, rating) VALUES (?, ?, ?)"
                                           user-id inserted-id rating]))))

(defn format-user-feedback [data]
  (map (fn [item]
         (let [renamed-item (set/rename-keys item {:user_feedback/user_id        :user-id
                                                   :user_feedback/combination_id :combination-id
                                                   :user_feedback/rating         :rating})]
           (-> renamed-item
               (update :rating keyword))))
       data))

(defn get-user-feedback [db-spec]
  (format-user-feedback (jdbc/execute! db-spec
                                        ["SELECT * FROM `user_feedback`"])))

(defn insert-feedback [user-id combination-id rating]
  (jdbc/execute! db-spec
                 ["INSERT INTO user_feedback (user_id, combination_id, rating) VALUES (?, ?, ?)"
                  user-id combination-id rating]))

(defn get-combination [combination-id]
  (format-clothing-items (jdbc/execute! db-spec
                                       ["SELECT cl.*  FROM `combination_items` ci JOIN `clothing_items` cl ON ci.clothing_item_id=cl.clothing_item_id
                                        WHERE combination_id=?"
                                        combination-id])))



