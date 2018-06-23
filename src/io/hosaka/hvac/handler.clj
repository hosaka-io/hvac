(ns io.hosaka.hvac.handler
  (:require [com.stuartsierra.component  :as component]
            [io.hosaka.hvac.orchestrator :as orchestrator]
            [io.hosaka.common.rabbitmq   :as rabbitmq]
            [io.hosaka.hvac.state        :as state]
            [manifold.deferred           :as d]
            [manifold.stream             :as s]))

(defrecord Handler [orchestrator rabbitmq state streams]
  component/Lifecycle

  (start [this]
    (do
      (rabbitmq/declare-task-queue rabbitmq "task.hvac.state.update.temp")
      (rabbitmq/declare-task-queue rabbitmq "task.hvac.state.update.mode")
      (let [temp-stream (rabbitmq/queue-subscription rabbitmq "task.hvac.state.update.temp")
            mode-stream (rabbitmq/queue-subscription rabbitmq "task.hvac.state.update.mode")]
        (s/consume (fn [{:keys [response body]}]
                     (do
                       (d/success! response true)
                       (orchestrator/update-state orchestrator {:target body}))) temp-stream)
        (s/consume (fn [{:keys [response body]}]
                     (do
                       (d/success! response true)
                       (orchestrator/update-state orchestrator {:mode   body}))) mode-stream)



        (assoc this :streams (vector temp-stream mode-stream)))))

  (stop [this]
    this))

(defn new-handler []
  (component/using
   (map->Handler {})
   [:orchestrator :rabbitmq :state]))
