(ns tests.all
  (:require
    [cljs.spec.alpha :as s]
    [cljs.test :refer [deftest is testing run-tests async use-fixtures]]
    [day8.re-frame.test :refer [run-test-async wait-for run-test-sync]]
    [district.ui.conversion-rates.events :as events]
    [district.ui.conversion-rates.subs :as subs]
    [district.ui.conversion-rates]
    [mount.core :as mount]
    [re-frame.core :refer [reg-event-fx dispatch-sync subscribe reg-cofx reg-fx dispatch]]))

(s/check-asserts true)

(use-fixtures
  :each
  {:after
   (fn []
     (mount/stop))})


(deftest tests
  (run-test-async
    (let [conversion-rates (subscribe [::subs/conversion-rates])
          conversion-rates-eth (subscribe [::subs/conversion-rates :ETH])
          conversion-rate-eth-usd (subscribe [::subs/conversion-rate :ETH :USD])
          converted-eth (subscribe [::subs/convert :ETH 2])
          converted-eth-usd (subscribe [::subs/convert :ETH :USD 2])]

      (-> (mount/with-args
            #_ {:conversion-rates {:from-currencies [:ETH]
                                :to-currencies [:USD :EUR]}}
            {:conversion-rates {:from-currencies [:ETH :BTC]
                                :to-currencies [:USD :EUR :DNT]}})
        (mount/start))

      (wait-for [::events/set-conversion-rates ::events/conversion-rates-load-failed]
        (#(do (println %) %) @converted-eth-usd)



        #_ (let [{:keys [:USD :EUR]} (:ETH @conversion-rates)]
          (is (number? USD))
          (is (number? EUR)))

        #_ (let [{:keys [:USD :EUR]} @conversion-rates-eth]
          (is (number? USD))
          (is (number? EUR)))

        #_ (is (number? @conversion-rate-eth-usd))

        #_ (let [{:keys [:USD :EUR]} @converted-eth]
          (is (number? USD))
          (is (= USD (* (:USD @conversion-rates-eth) 2)))
          (is (number? EUR))
          (is (= EUR (* (:EUR @conversion-rates-eth) 2))))

        #_ (is (number? @converted-eth-usd))
        #_ (is (= @converted-eth-usd (* @conversion-rate-eth-usd 2)))))))


(deftest invalid-params-tests
  (run-test-sync
    (is (thrown? :default (-> (mount/with-args
                                {:conversion-rates {:from-currencies :ETH}})
                            (mount/start))))

    (-> (mount/with-args
          {:conversion-rates {:disable-loading-at-start? true}})
      (mount/start))

    (is (thrown? :default (dispatch [::events/load-conversion-rates {:from-currencies :ETH}])))

    (is (thrown? :default (dispatch [::events/set-conversion-rates {:ETH 2}])))))


