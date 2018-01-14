(ns tests.all
  (:require
    [cljs.spec.alpha :as s]
    [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
    [day8.re-frame.test :refer [run-test-async wait-for run-test-sync]]
    [district.ui.conversion-rates.events :as events]
    [district.ui.conversion-rates.queries :as queries]
    [district.ui.conversion-rates.subs :as subs]
    [district.ui.conversion-rates]
    [mount.core :as mount]
    [re-frame.core :refer [reg-event-fx dispatch-sync subscribe reg-cofx reg-fx dispatch]]
    [re-frame.db :refer [app-db]]))

(s/check-asserts true)

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})

(reg-event-fx
  ::do-nothing
  (constantly nil))

(deftest tests
  (run-test-async
    (let [conversion-rates (subscribe [::subs/conversion-rates])
          conversion-rates-eth (subscribe [::subs/conversion-rates :ETH])
          conversion-rate-eth-usd (subscribe [::subs/conversion-rate :ETH :USD])
          converted-eth (subscribe [::subs/convert :ETH 2])
          converted-eth-usd (subscribe [::subs/convert :ETH :USD 2])]

      (-> (mount/with-args
            {:conversion-rates {:from-currencies [:ETH]
                                :to-currencies [:USD :EUR]}})
        (mount/start))

      (wait-for [::events/set-conversion-rates ::events/conversion-rates-load-failed]
        (let [{:keys [:USD :EUR]} (:ETH @conversion-rates)]
          (is (number? USD))
          (is (number? EUR)))

        (let [{:keys [:USD :EUR]} @conversion-rates-eth]
          (is (number? USD))
          (is (number? EUR)))

        (is (number? @conversion-rate-eth-usd))

        (let [{:keys [:USD :EUR]} @converted-eth]
          (is (number? USD))
          (is (= USD (* (:USD @conversion-rates-eth) 2)))
          (is (number? EUR))
          (is (= EUR (* (:EUR @conversion-rates-eth) 2))))

        (is (number? @converted-eth-usd))
        (is (= @converted-eth-usd (* @conversion-rate-eth-usd 2)))

        (is (true? (queries/cache-has? @app-db [:ETH :USD])))
        (is (false? (queries/cache-has? @app-db [:USD :ETH])))

        (is (true? (queries/cache-has-all-pairs? @app-db [:ETH] [:USD :EUR])))
        (is (false? (queries/cache-has-all-pairs? @app-db [:ETH] [:USD :EUR :DNT])))

        (testing "Doesn't request cached currencies again"
          (dispatch [::events/watch-conversion-rates {:from-currencies [:ETH]
                                                      :to-currencies [:USD]
                                                      :id ::my-watcher}])

          (js/setTimeout #(dispatch [::do-nothing]) 3000)
          (wait-for [::do-nothing ::events/set-conversion-rates]
            ))))))


(deftest without-cache
  (run-test-async
    (-> (mount/with-args
          {:conversion-rates {:from-currencies [:ETH]
                              :to-currencies [:USD :EUR]
                              :cache-ttl 0}})
      (mount/start))

    (wait-for [::events/set-conversion-rates ::events/conversion-rates-load-failed]
      (dispatch [::events/watch-conversion-rates {:from-currencies [:ETH]
                                                  :to-currencies [:USD]
                                                  :id ::my-watcher}])
      (js/setTimeout #(dispatch [::do-nothing]) 3000)
      (wait-for [[::events/set-conversion-rates ::events/conversion-rates-load-failed] ::do-nothing]))))


(deftest invalid-params-tests
  (run-test-sync
    (is (thrown? :default (-> (mount/with-args
                                {:conversion-rates {:from-currencies :ETH}})
                            (mount/start))))

    (-> (mount/with-args
          {})
      (mount/start))


    (is (thrown? :default (dispatch [::events/load-conversion-rates {:from-currencies :ETH}])))

    (is (thrown? :default (dispatch [::events/set-conversion-rates {:ETH 2}])))))
