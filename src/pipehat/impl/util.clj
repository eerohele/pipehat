(ns pipehat.impl.util)

(defn hl7-type
  "Given an IMeta, return its HL7 type."
  [x]
  (-> x meta :hl7/type))
