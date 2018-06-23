(ns io.hosaka.hvac
  (:require [com.stuartsierra.component  :as component]
            [config.core                 :refer [env]]
            [io.hosaka.common.rabbitmq   :refer [new-rabbitmq]]
            [io.hosaka.hvac.display      :refer [new-display]]
            [io.hosaka.hvac.handler      :refer [new-handler]]
            [io.hosaka.hvac.orchestrator :refer [new-orchestrator]]
            [io.hosaka.hvac.state        :refer [new-state]])
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
  (reset! system (init-system env))

  (swap! system component/start)


  )
