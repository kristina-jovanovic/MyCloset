(ns my-closet.db
  (:gen-class)
  (:require [next.jdbc :as jdbc]))

(def db-spec
  {:dbtype   "mysql"
   :dbname   "my_closet_db"
   :host     "localhost"
   :port     3306
   :user     "root"
   :password ""})

(defn get-clothing-items [db-spec]
  (jdbc/execute! db-spec
                 ["SELECT * FROM `clothing_items`"]))

