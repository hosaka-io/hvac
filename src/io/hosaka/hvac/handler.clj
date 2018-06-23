(ns io.hosaka.hvac.handler
  (:require [com.stuartsierra.component  :as component]
            [io.hosaka.hvac.orchestrator :as orchestrator]
            [io.hosaka.common.rabbitmq   :as rabbitmq]
            [io.hosaka.hvac.state        :as state]
            [manifold.deferred           :as d]
            [manifold.stream             :as s]))

(defn parseInteger [x]
  (try
    (Integer/parseInt x)
    (catch Exception e nil)))

(defrecord Handler [orchestrator rabbitmq state streams]
  component/Lifecycle

  (start [this]
    (do
      (rabbitmq/declare-task-queue rabbitmq "task.hvac.state.update.temp")
      (rabbitmq/declare-task-queue rabbitmq "task.hvac.state.update.mode")
      (rabbitmq/declare-task-queue rabbitmq "task.hvac.state.update")
      (let [temp-stream (rabbitmq/queue-subscription rabbitmq "task.hvac.state.update.temp")
            state-stream (rabbitmq/queue-subscription rabbitmq "task.hvac.state.update")
            mode-stream (rabbitmq/queue-subscription rabbitmq "task.hvac.state.update.mode")]
        (s/consume (fn [{:keys [response body]}]
                     (do
                       (d/success! response true)
                       (orchestrator/update-state orchestrator body))) state-stream)
        (s/consume (fn [{:keys [response body]}]
                     (do
                       (d/success! response true)
                       (when-let [temp (parseInteger body)]
                         (orchestrator/update-state orchestrator {:target body})))) temp-stream)
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
