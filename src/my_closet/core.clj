(ns my-closet.core
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))

(def color-rules
  {:white #{:universal}
   :black #{:universal}})

(defn colors-match? [color1 color2]
  (or (= (get color-rules color1 #{}) #{:universal})
      (= (get color-rules color2 #{}) #{:universal})
      (contains? (get color-rules color1 #{}) color2)))
