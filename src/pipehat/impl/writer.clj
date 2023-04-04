(ns pipehat.impl.writer
  (:require [pipehat.const :refer [SB EB CR]]
            [pipehat.impl.util :refer [hl7-type]])
  (:import (java.io BufferedWriter)))

(defn ^:private escaping-write
  [escape-character ^BufferedWriter writer ^String s]
  (.write writer ^int escape-character)
  (.write writer s)
  (.write writer ^int escape-character))

(defn ^:private write-component
  [{:keys [field-separator component-separator sub-component-separator repetition-separator escape-character]
    :as encoding-characters}
   ^BufferedWriter writer
   component]
  (if (vector? component)
    (do
      (run! (fn [sub-component]
              (write-component encoding-characters writer sub-component)

              (case (hl7-type component)
                :sub-component (.write writer ^int sub-component-separator)
                :repetition (.write writer ^int repetition-separator)
                (.write writer ^int component-separator)))
        (butlast component))

      (write-component encoding-characters writer (peek component)))

    (run! (fn [ch]
            (condp = (int ch)
              field-separator (escaping-write escape-character writer "F")
              repetition-separator (escaping-write escape-character writer "R")
              component-separator (escaping-write escape-character writer "S")
              sub-component-separator (escaping-write escape-character writer "T")
              escape-character (escaping-write escape-character writer "E")
              12 (escaping-write escape-character writer "X0A")
              13 (escaping-write escape-character writer "X0D")
              (.write writer (int ch))))
      component)))

(defn ^:private write-components
  [{:keys [^int field-separator]
    :as encoding-characters}
   ^BufferedWriter writer
   components]
  (run! (fn [component]
          (write-component encoding-characters writer component)
          (.write writer field-separator))
    (butlast components))
  (write-component encoding-characters writer (last components)))

(defn ^:private write-segment
  [{:keys [^int field-separator]
    :as encoding-characters}
   ^BufferedWriter writer
   segment]
  (when segment
    (.write writer ^String (-> segment first name))
    (.write writer ^int field-separator)

    (run! (fn [components]
            (write-components encoding-characters writer components))
      (rest segment))))

(defn ^:private write-segments
  [encoding-characters writer segments]
  (run!
    (fn [segment]
      (write-segment encoding-characters writer segment)
      (.write writer CR)
      (.flush writer))
    (butlast segments))

  (write-segment encoding-characters writer (last segments)))

(defn write
  "Given a java.io.BufferedWriter and a HL7-ER7 message, write the message into
  the writer."
  [^BufferedWriter writer message {:keys [protocol]}]
  (when (= :mllp protocol) (.write writer SB))

  (let [header-segment (first message)
        header-segment-identifier (-> header-segment first name)
        field-separator (-> header-segment second (nth 0) (nth 0) int)
        encoding-character-field (-> header-segment second (nth 1))
        header-segment-tail (-> header-segment second nnext)

        {:keys [^int field-separator]
         :as encoding-characters}
        (zipmap [:field-separator :component-separator :repetition-separator :escape-character :sub-component-separator]
          (map int (cons field-separator encoding-character-field)))]

    ;; Write header segment
    (run! #(.write writer (int %)) header-segment-identifier)
    (.write writer field-separator)

    ;; Write encoding character field as is (no escaping)
    (run! #(.write writer (int %)) encoding-character-field)

    ;; Write header segment tail
    (when (seq header-segment-tail)
      (.write writer field-separator)
      (write-components encoding-characters writer header-segment-tail))

    (.write writer CR)
    (.flush writer)

    ;; Write other segments
    (write-segments encoding-characters writer (next message)))

  (when (= :mllp protocol) (.write writer EB))
  (.write writer CR)
  (.flush writer))
