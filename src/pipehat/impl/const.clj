(ns pipehat.impl.const)

(def default-encoding-characters
  "A map of the default, recommended HL7 encoding characters."
  {:field-separator 124
   :component-separator 94
   :repetition-separator 126
   :sub-component-separator 38
   :escape-character 92})

(comment
  (update-vals default-encoding-characters char)
  ,,,)

(def ^{:const true :doc "The HL7 Minimal Lower Layer Protocol (MLLP) start-of-block character."} SB 11)
(def ^{:const true :doc "The HL7 Minimal Lower Layer Protocol (MLLP) end-of-block character."} EB 28)
(def ^{:const true :doc "The ASCII carriage return character."} CR 13)
(def ^{:const true :doc "An int indicating the end of stream."} EOS -1)
(def ^{:const true :doc "An int indicating the start of element."} SOE -2)
