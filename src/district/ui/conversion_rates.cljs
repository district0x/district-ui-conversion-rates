(ns district.ui.conversion-rates
  (:require
    [cljs.spec.alpha :as s]
    [district.ui.conversion-rates.events :as events]
    [mount.core :as mount :refer [defstate]]
    [re-frame.core :refer [dispatch-sync]]))

(declare start)
(declare stop)
(defstate conversion-rates
  :start (start (:conversion-rates (mount/args)))
  :stop (stop))

(s/def ::from-currencies (s/coll-of keyword?))
(s/def ::to-currencies (s/coll-of keyword?))
(s/def ::request-timeout number?)
(s/def ::disable-loading-at-start? boolean?)
(s/def ::polling-interval-ms number?)
(s/def ::disable-polling? boolean?)
(s/def ::opts (s/keys :opt-un [::from-currencies ::to-currencies ::request-timeout
                               ::disable-loading-at-start? ::polling-interval-ms ::disable-polling?]))

(defn start [opts]
  (s/assert ::opts opts)
  (dispatch-sync [::events/start opts])
  opts)


(defn stop []
  (dispatch-sync [::events/stop]))

