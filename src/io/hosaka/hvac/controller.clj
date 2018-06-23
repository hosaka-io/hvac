(ns io.hosaka.hvac.controller
  (:require [clojure.string :as str]))

(def status-map {:off   {23 1, 22 1, 27 1, 17 1}
                 :fan   {23 0, 22 1, 27 1, 17 1}
                 :eheat {23 0, 22 0, 27 1, 17 1}
                 :cool  {23 0, 22 1, 27 0, 17 1}
                 :heat  {23 0, 22 1, 27 0, 17 0}})

(defn set-status [{:keys [status]}]
  (doall
   (map
    (fn [[p v]] (println (str "\tWrite: " p " -> " v))
      ;;(Gpio/digitalWrite p v)
      )
    (sort-by first
             (get status-map status)))))

;;  (def temp-file "/sys/bus/w1/devices/28-80000002cec1/w1_slave")
(def temp-file "/opt/w1_slave")


(defn get-temp []
  (->
   (let [r (slurp temp-file)
         i (str/index-of r "t=")]
     (.substring r (+ i 2)))
   str/trim-newline
   Integer/parseInt
   (* 9)
   (/ 5000.0)
   (+ 32)))
