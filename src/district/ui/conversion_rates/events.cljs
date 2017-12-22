(ns district.ui.conversion-rates.events
  (:require
    [ajax.core :as ajax]
    [clojure.string :as string]
    [day8.re-frame.http-fx]
    [district.ui.conversion-rates.queries :as queries]
    [district0x.re-frame.interval-fx]
    [district0x.re-frame.spec-interceptors :refer [validate-first-arg]]
    [goog.string :as gstring]
    [goog.string.format]
    [re-frame.core :refer [reg-event-fx trim-v]]
    [cljs.spec.alpha :as s]))

(def interceptors [trim-v])

(s/def ::conversion-rates (s/map-of keyword? (s/map-of keyword? number?)))

(reg-event-fx
  ::start
  interceptors
  (fn [{:keys [:db]} [{:keys [:disable-loading-at-start? :disable-polling? :polling-interval-ms]
                       :or {polling-interval-ms 300000}
                       :as opts}]]
    (merge
      {:db (queries/merge-conversion-rates db {})}
      (when-not disable-loading-at-start?
        {:dispatch [::load-conversion-rates opts]})
      (when (and (not disable-loading-at-start?)
                 (not disable-polling?))
        {:dispatch-interval {:dispatch [::load-conversion-rates opts]
                             :id ::load-conversion-rates
                             :ms polling-interval-ms}}))))


(reg-event-fx
  ::load-conversion-rates
  [interceptors (validate-first-arg :district.ui.conversion-rates/opts)]
  (fn [{:keys [:db]} [{:keys [:from-currencies :to-currencies :request-timeout]
                       :or {request-timeout 10000}}]]
    {:http-xhrio {:method :get
                  :uri (gstring/format "https://min-api.cryptocompare.com/data/pricemulti?fsyms=%s&tsyms=%s"
                                       (string/join "," (map name from-currencies))
                                       (string/join "," (map name to-currencies)))
                  :timeout request-timeout
                  :response-format (ajax/json-response-format {:keywords? true})
                  :on-success [::set-conversion-rates]
                  :on-failure [::conversion-rates-load-failed]}}))


(reg-event-fx
  ::set-conversion-rates
  [interceptors (validate-first-arg ::conversion-rates)]
  (fn [{:keys [:db]} [conversion-rates]]
    {:db (queries/merge-conversion-rates db conversion-rates)}))


(reg-event-fx
  ::conversion-rates-load-failed
  (constantly nil))


(reg-event-fx
  ::stop
  interceptors
  (fn [{:keys [:db]}]
    {:db (queries/dissoc-conversion-rates db)
     :clear-interval {:id ::load-conversion-rates}}))






