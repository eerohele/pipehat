(ns pipehat.impl.util)

(defn element-type
  "Given an IMeta, return its element type."
  [x]
  (-> x meta :pipehat.api/element-type))
