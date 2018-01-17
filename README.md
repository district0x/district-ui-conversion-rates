# district-ui-conversion-rates

[![Build Status](https://travis-ci.org/district0x/district-ui-conversion-rates.svg?branch=master)](https://travis-ci.org/district0x/district-ui-conversion-rates)

Clojurescript [mount](https://github.com/tolitius/mount) + [re-frame](https://github.com/Day8/re-frame) module for a district UI, that handles conversion rates between fiat currencies and cryptocurrencies.  
This module currently uses [cryptocompare.com](https://www.cryptocompare.com/) API to obtain rates. 

It also uses internal caching, so other modules don't load same rates unnecessarily many times.


## Installation
Add `[district0x/district-ui-conversion-rates "1.0.1"]` into your project.clj  
Include `[district.ui.conversion-rates]` in your CLJS file, where you use `mount/start`

## API Overview

**Warning:** district0x modules are still in early stages, therefore API can change in a future.

- [district.ui.conversion-rates](#districtuiconversion-rates)
- [district.ui.conversion-rates.subs](#districtuiconversion-ratessubs)
  - [::conversion-rates](#conversion-rates-sub)
  - [::conversion-rate](#conversion-rate-sub)
  - [::convert](#convert-sub)
- [district.ui.conversion-rates.events](#districtuiconversion-ratesevents)
  - [::watch-conversion-rates](#watch-conversion-rates)
  - [::stop-watching-conversion-rates](#stop-watching-conversion-rates)
  - [::load-conversion-rates](#load-conversion-rates)
  - [::set-conversion-rates](#set-conversion-rates)
  - [::conversion-rates-load-failed](#conversion-rates-load-failed)
- [district.ui.conversion-rates.queries](#districtuiconversion-ratesqueries)
  - [conversion-rates](#conversion-rates)
  - [conversion-rate](#conversion-rate)
  - [convert](#convert)
  - [merge-conversion-rates](#merge-conversion-rates)
  - [cache-ttl](#cache-ttl)
  - [assoc-cache-ttl](#assoc-cache-ttl)


## district.ui.conversion-rates
This namespace contains conversion-rates [mount](https://github.com/tolitius/mount) module.
In case you pass initial configuration, the module will load rates at mount start. 

You can pass following args to initiate this module: 
* `:from-currencies` Currencies you'll be converting from
* `:to-currencies` Currencies you'll be converting to
* `:request-timeout` Timeout of request to obtain rates, before throwing error (ms). Default: 10000 (10s)
* `:request-interval-ms` How often rates should be reloaded. Default: 300000 (5 min.). Pass 0 to disable reloading.
* `:cache-ttl` Time-to-live of the cache. A rate won't be loaded again until this duration passes since last load of that rate. 
Default: 180000 (3 min.). 

```clojure
  (ns my-district.core
    (:require [mount.core :as mount]
              [district.ui.conversion-rates]))

  (-> (mount/with-args
        {:conversion-rates {:from-currencies [:ETH :BTC]
                            :to-currencies [:USD :EUR :DNT]}})
    (mount/start))
```

## district.ui.conversion-rates.subs
re-frame subscriptions provided by this module:

#### <a name="conversion-rates-sub">`::conversion-rates`
Returns conversion rates. You can use 2 forms:
```clojure
@(subscribe [::subs/conversion-rates])
;; {:ETH {:USD 664.31, :EUR 570.81, :DNT 8873.63}, :BTC {:USD 13601.62, :EUR 11714.62, :DNT 183150.18}}

@(subscribe [::subs/conversion-rates :ETH])
;; {:USD 663.98, :EUR 570.87, :DNT 8871.79}
```

#### <a name="conversion-rate-sub">`::conversion-rate`
Returns conversion rate of a pair
```clojure
@(subscribe [::subs/conversion-rate :ETH :USD])
;; 664.92
```

#### <a name="convert-sub">`::convert`
Converts passed value according to rate
```clojure
@(subscribe [::subs/convert :ETH 2])
;; {:USD 1327.96, :EUR 1141.24, :DNT 17779.82}

@(subscribe [::subs/convert :ETH :USD 2])
;; 1327.9
```
Complete example might be:
```clojure
(ns my-district.home-page
  (:require [district.ui.conversion-rates.subs :as rates-subs]
            [re-frame.core :refer [subscribe]]))

(defn home-page []
  (let [converted-eth-usd (subscribe [::rates-subs/convert :ETH :USD 2])]
    (fn []
      [:div "2 ETH is currently " @converted-eth-usd " USD"])))
```

## district.ui.conversion-rates.events
re-frame events provided by this module:

#### <a name="watch-conversion-rates">`::watch-conversion-rates [opts]`
Loads conversion rates and sets up reloading. Use this event from other modules, that need specific conversion rates.   
opts:   
- `:from-currencies`  
- `:to-currencies`  
- `:request-interval-ms` (optional)  
- `:request-timeout` (optional)  
- `:id` Id of reloading interval, so you can stop it later.  

#### <a name="stop-watching-conversion-rates">`::stop-watching-conversion-rates [id]`
Stops reloading rates by id. 

#### <a name="load-conversion-rates">`::load-conversion-rates [opts]`
Loads conversion rates from external API.   
opts: 
- `:from-currencies`  
- `:to-currencies`  
- `:request-timeout` (optional)  

#### <a name="set-conversion-rates">`::set-conversion-rates [conversion-rates]`
Set conversion rates into re-frame db. Also, this event is fired after `::load-conversion-rates`. You can use it to hook into
event flow.

#### <a name="conversion-rates-load-failed">`::conversion-rates-load-failed`
Event fired when loading failed 

## district.ui.conversion-rates.queries
DB queries provided by this module:  
*You should use them in your events, instead of trying to get this module's 
data directly with `get-in` into re-frame db.*

#### <a name="conversion-rates">`conversion-rates [db]`
Works the same way as sub `::conversion-rates`

#### <a name="conversion-rate">`conversion-rate [db from to]`
Works the same way as sub `::conversion-rate`

#### <a name="convert">`convert [db from to value]`
Works the same way as sub `::convert`

```clojure
(ns my-district.events
    (:require [district.ui.conversion-rates.queries :as rates-queries]
              [re-frame.core :refer [reg-event-fx]]))

(reg-event-fx
  ::my-event
  (fn [{:keys [:db]}]
    (if (< 1500 (rates-queries/convert db :ETH :USD 2))
      {:dispatch [::do-something]}
      {:dispatch [::do-other-thing]})))
```

#### <a name="merge-conversion-rates">`merge-conversion-rates [db conversion-rates]`
Merges in new conversion rates and returns new re-frame db.

#### <a name="cache-ttl">`cache-ttl [db]`
Returns current cache-ttl

#### <a name="assoc-cache-ttl">`assoc-cache-ttl [db cache-ttl]`
Associates new cache-ttl and returns new re-frame db.

## Development
```bash
lein deps

# To run tests and rerun on changes
lein doo chrome tests
```