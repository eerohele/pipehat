(ns ^:no-doc pipehat.impl.shaper
  (:require [clojure.string :as string]
            [pipehat.impl.util :refer [element-type]]))

(defn ^:private not-blank
  [x]
  (if (string? x)
    (-> x string/trim not-empty)
    (not-empty x)))

(defn ^:private shape-sub-components
  [id field-index component-index x]
  (case (element-type x)
    :sub-component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when-some [item (not-blank item)]
            [[id field-index component-index (inc index)] item])))
      x)

    x))

(defn ^:private shape-components
  [id field-index x]
  (case (element-type x)
    :component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when-some [item (not-blank item)]
            [[id field-index (inc index)] (shape-sub-components id field-index (inc index) item)])))
      x)

    ;; The first component of the field has sub-components.
    :sub-component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when-some [item (not-blank item)]
            [[id field-index 1 (inc index)] item])))
      x)

    x))

(defn ^:private shape-repetitions
  [id field-index x]
  (case (element-type x)
    :repetition
    (into []
      (keep
        (fn [item]
          (shape-components id field-index item)))
      x)

    :component
    (into (sorted-map)
      (keep-indexed
        (fn [index item]
          (when-some [item (not-blank item)]
            [[id field-index (inc index)] (shape-components id field-index item)])))
      x)

    x))

(defn ^:private shape-fields
  [id fields]
  (into (sorted-map)
    (keep-indexed
      (fn [index item]
        (when-some [item (not-blank item)]
          [[id (inc index)] (shape-repetitions id (inc index) item)])))
    fields))

(defn shape
  [message]
  (reduce
    (fn [m [id fields]]
      (update m id (fnil conj []) (shape-fields id fields)))
    {}
    message))

(comment
  (set! *print-meta* true)

  (shape [["MSH" [^{:pipehat.api/element-type :component} ["ADT" "A04"]]]])
  (shape [["ABC" [^{:pipehat.api/element-type :repetition} [["A" "B"] ["C" "D"]]]]])

  (shape [["ABC" [^{:pipehat.api/element-type :repetition}
                  [^{:pipehat.api/element-type :component} ["A" "B"]
                   ^{:pipehat.api/element-type :component} ["C" "D"]]]]])
  ,,,)
