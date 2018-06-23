(ns io.hosaka.hvac.state
  (:require [com.stuartsierra.component :as component]
            [io.hosaka.hvac.transition :refer [get-new-status]]
            [java-time :as time]))

(defrecord State [state-map]
  component/Lifecycle

  (start [this]
    (assoc this
           :state-map (atom {:mode :off
                             :target 72
                             :readings {:init {:temp 72 :time (time/instant)}}
                             :status :off})))
  (stop [this]
    this))

(defn new-state []
  (component/using
   (map->State {})
   []))

(defn update-reading [{:keys [state-map]} src m]
  (swap! state-map
         (fn [{:keys [readings] :as s}]
           (assoc s
                  :readings (assoc
                             readings
                             (keyword src)
                             (assoc m
                                    :time (time/instant)))))))

(defn update-mode [{:keys [state-map]} mode]
  (swap! state-map
         (fn [s]
           (assoc s :mode (keyword mode)))))

(defn update-target [{:keys [state-map]} target]
  (swap! state-map
         (fn [s]
           (assoc s :target target))))

(defn update-state [state-map]
  (let [n (time/instant)
        exp (time/minus n (time/minutes 2))
        readings (into {} (filter #(-> % second :time (time/after? exp)) (:readings state-map)))
        status (get-new-status (assoc state-map :readings readings))]
    (assoc state-map :readings readings :status status :now n)))

(defn get-state [{:keys [state-map]}]
  (swap! state-map update-state))

