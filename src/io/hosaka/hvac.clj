(ns io.hosaka.hvac
  (:require [com.stuartsierra.component  :as component]
            [manifold.deferred           :as d]
            [config.core                 :refer [env]]
            [io.hosaka.common.rabbitmq   :refer [new-rabbitmq]]
            [io.hosaka.hvac.display      :refer [new-display]]
            [io.hosaka.hvac.handler      :refer [new-handler]]
            [io.hosaka.hvac.orchestrator :refer [new-orchestrator]]
            [io.hosaka.hvac.state        :refer [new-state]])
  (:import [com.pi4j.wiringpi Gpio])
  (:gen-class))



(defn init-system [env]
  (component/system-map
   :rabbitmq (new-rabbitmq env)
   :display (new-display)
   :state (new-state)
   :orchestrator (new-orchestrator)
   :handler (new-handler)
   ))

(defonce system (atom {}))

(defn -main [& args]
  (let [semaphore (d/deferred)]
;;    (Gpio/wiringPiSetupSys)

    (reset! system (init-system env))

    (swap! system component/start)

    (deref semaphore)

    (component/stop @system)

    (shutdown-agents))) 
