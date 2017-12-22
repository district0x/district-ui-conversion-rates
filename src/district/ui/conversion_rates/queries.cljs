(ns district.ui.conversion-rates.queries)


(defn conversion-rates
  ([db] (:district.ui.conversion-rates db))
  ([db from] (get (conversion-rates db) from)))


(defn conversion-rate [db from to]
  (get (conversion-rates db from) to))


(defn convert
  ([db from value]
   (into {} (map (fn [[k v]]
                   [k (* v value)])
                 (conversion-rates db from))))
  ([db from to value]
   (* (conversion-rate db from to) value)))


(defn merge-conversion-rates [db conversion-rates]
  (update db :district.ui.conversion-rates merge conversion-rates))


(defn dissoc-conversion-rates [db]
  (dissoc db :district.ui.conversion-rates))
