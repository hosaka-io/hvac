(ns io.hosaka.hvac.transition)

(def threshold 0.5)

(comment
  (defn delta [f {:keys [last temp] :as st}]
    (-
     (if-let [remote (-> st :remote :temperature)]
       (f remote last)
       last)
     temp)))

(defn delta [f {:keys [target readings]}]
  (let [temps (filter number? (map #(-> % second :temperature) readings))]
    (if (empty? temps)
      nil
      (- (apply f temps) target))))

(defn heat-active? [s]
  (if-let [d (delta min s)]
    (if (= :off (:status s))
      (> (- threshold) d)
      (> threshold d))
    false))

(defn cool-active? [s]
  (if-let [d (delta max s)]
    (if (= :off (:status s))
      (< threshold d)
      (< (- threshold) d))
    false))

(defn get-new-status [s]
  (let [m (:mode s)]
    (cond
      (= :off  m) :off
      (= :fan  m) :fan
      (and (= :cool m) (cool-active? s)) m
      (and (contains? #{:eheat :heat} m) (heat-active? s)) m
      :else :off)))
