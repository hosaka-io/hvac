(ns io.hosaka.hvac.orchestrator
  (:require [com.stuartsierra.component   :as component]
            [manifold.stream              :as s]
            [java-time :as time]
            [io.hosaka.hvac.display       :as display]
            [io.hosaka.hvac.controller    :as controller]
            [io.hosaka.hvac.state         :as state]
            [io.hosaka.common.rabbitmq    :as rabbitmq]))



(defn dispaly [state]
  (vector
   (format "T: %3.1f %2d" (or (-> state :readings :local :temperature) 0.01) (-> state :target))
   (format "Mode: %s / %s" (-> state :mode name) (-> state :status name))
   (time/format "EEE dd h:mm:ssa" (-> state :now (time/local-date-time (time/zone-id))))))

(defrecord Orchestrator [state display streams rabbitmq]
  component/Lifecycle

  (start [this]
    (let [get-state (partial state/get-state (:state this))
          temp-stream (s/periodically 1250 controller/get-temp)
          controller-stream (s/periodically 15000 get-state)
          event-stream (s/periodically 30000 get-state)
          display-stream (s/periodically 1800 get-state)]
      (s/consume (partial state/update-reading (:state this) :local)
                 (s/map #(hash-map :temperature %) temp-stream))
      (s/consume (partial display/show display)
                 (s/map dispaly display-stream))
      (s/consume controller/set-status controller-stream)
        (s/consume (partial rabbitmq/publish (:rabbitmq this) "events.hvac.status")
                   (s/map (fn [st] (select-keys st (filter #(not= :now %) (keys st)))) event-stream))
        (assoc this :streams (vector temp-stream display-stream controller-stream event-stream))))

  (stop [this]
    (doall (map s/close! (:streams this)))
    (assoc this :streams nil)))


(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:state :display :rabbitmq]))

(defn update-state [{:keys [state]} {:keys [mode target]}]
  (if mode
    (state/update-mode state mode))
  (if target
    (state/update-target state target)))
