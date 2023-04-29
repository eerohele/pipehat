(ns ^:no-doc pipehat.impl.defaults)

(def encoding-characters
  "A map of the default, recommended HL7 encoding characters."
  {:field-separator 124
   :component-separator 94
   :repetition-separator 126
   :sub-component-separator 38
   :escape-character 92})

(comment
  (update-vals encoding-characters char)
  ,,,)
