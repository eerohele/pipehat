(ns pipehat.specs
  (:require [clojure.string :as string]
            [clojure.spec.alpha :as spec]
            [clojure.spec.gen.alpha :as gen]
            [pipehat.impl.const :refer [SB EB CR]]
            [pipehat.impl.reader :refer [unwrap1]]))

(spec/def ::segment-identifier
  (spec/with-gen (spec/and keyword? #(= 3 (count (name %))))
    (fn []
      (gen/fmap #(keyword (string/join (map string/upper-case %))) (gen/vector (gen/char-alpha) 3)))))

(spec/def ::non-empty-string
  (spec/and string? #(not= % "")))

(spec/def ::sub-component
  (spec/coll-of ::non-empty-string :kind vector?))

(spec/def ::repetition
  (spec/coll-of ::non-empty-string :kind vector?))

(spec/def ::component*
  (spec/or :string ::non-empty-string :sub-component ::sub-component :repetition ::repetition))

(spec/def ::component
  (spec/with-gen
    (spec/nilable ::component*)
    #(gen/fmap (comp unwrap1 not-empty) (spec/gen ::component*))))

(spec/def ::field
  (spec/or :string ::non-empty-string :component ::component))

(defmacro ^:private catvec
  "Like clojure.spec.alpha/cat, but expects and generates a vectors."
  [& args]
  `(let [spec# (spec/cat ~@args)]
     (spec/with-gen (spec/and vector? spec#)
       #(gen/fmap vec (spec/gen spec#)))))

(spec/def ::fields
  (spec/* ::field))

(spec/def ::segment
  (spec/tuple
    ::segment-identifier
    (spec/with-gen (catvec :fields (spec/* ::field))
      #(gen/fmap not-empty (spec/gen (catvec :fields ::fields))))))

(spec/def ::field-separator
  (spec/with-gen
    (spec/and string? #(= 1 (count %)))
    (fn [] (gen/fmap (partial apply str) (gen/vector (gen/char) 1)))))

(spec/def ::encoding-characters
  (spec/with-gen
    (spec/and string? #(= 4 (count %)))
    (fn [] (gen/fmap (partial apply str) (gen/vector (gen/char) 4)))))

(def ^:private header-segment-gen
  (gen/tuple
    (gen/return :MSH)
    (gen/fmap (fn [[header segments]] (into header segments))
      (gen/tuple
        (gen/bind
          (gen/such-that #(not (#{(char SB) (char EB) (char CR)} %)) (gen/char))
          (fn [field-separator]
            (gen/tuple
              (gen/return (str field-separator))
              (gen/fmap (partial apply str)
                (gen/vector-distinct
                  (gen/such-that #(not (#{(char SB) (char EB) (char CR) field-separator} %)) (gen/char))
                  {:num-elements 4})))))
        (spec/gen ::fields)))))

(spec/def ::header-segment
  (spec/with-gen
    (spec/tuple
      #{:MSH}
      (catvec
        :field-separator ::field-separator
        :encoding-characters ::encoding-characters
        :fields ::fields))
    (fn [] header-segment-gen)))

(spec/def ::message
  (catvec :header-segment ::header-segment ::segments (spec/* ::segment)))

(comment
  (gen/generate (spec/gen ::header-segment))
  (gen/generate (spec/gen ::message))
  ,,,)