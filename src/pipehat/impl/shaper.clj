(ns pipehat.impl.shaper
  (:require [pipehat.impl.util :refer [hl7-type]]))

(defn ^:private shape-sub-components
  [id field-index component-index x]
  (case (hl7-type x)
    :sub-component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when (seq item)
            [[id field-index component-index (inc index)] item]))
        x))

    x))

(defn ^:private shape-components
  [id field-index x]
  (case (hl7-type x)
    :component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when (seq item)
            [[id field-index (inc index)] (shape-sub-components id field-index (inc index) item)]))
        x))

    ;; The first component of the field has sub-components.
    :sub-component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when (seq item)
            [[id field-index 1 (inc index)] item]))
        x))

    x))

(defn ^:private shape-fields
  [id fields]
  (into (sorted-map)
    (keep-indexed
      (fn [index item]
        (when (seq item)
          [[id (inc index)] (shape-components id (inc index) item)]))
      fields)))

(defn shape
  [message]
  (reduce
    (fn [m [id fields]]
      (update m id (fnil conj []) (shape-fields id fields)))
    {}
    message))
