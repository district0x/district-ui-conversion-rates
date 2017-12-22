(ns district.ui.conversion-rates.subs
  (:require
    [district.ui.conversion-rates.queries :as queries]
    [re-frame.core :refer [reg-sub]]))


(reg-sub
  ::conversion-rates
  (fn [db [_ & args]]
    (apply queries/conversion-rates db args)))


(reg-sub
  ::conversion-rate
  (fn [db [_ & args]]
    (apply queries/conversion-rate db args)))


(reg-sub
  ::convert
  (fn [db [_ & args]]
    (apply queries/convert db args)))