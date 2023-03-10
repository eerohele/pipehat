(ns pipehat.impl.reader
  (:refer-clojure :exclude [read read-string])
  (:require [clojure.string :as string]
            [pipehat.impl.const :refer [default-encoding-characters CR SB EB EOS SOE]])
  (:import (java.io PushbackReader StringReader)))

(defn <<
  [s]
  (-> s StringReader. PushbackReader.))

(defn read-line-break-escape-sequence
  "Given a reader, read an escape sequence representing a line break (one of
  \\br.\\, \\X0A.\\, or \\X0D.\\)."
  [^PushbackReader reader]
  (let [n1 (.read reader)]
    (if (#{EOS EB} n1)
      (throw (ex-info "EOF while reading escape sequence" {}))
      (let [n2 (.read reader)]
        (if (#{EOS EB} n2)
          (throw (ex-info "EOF while reading escape sequence" {}))
          (condp = [n1 n2]
            [98 114] 13 ; \.br\ -> \return
            [48 65] 12 ; \X0A\ -> \formfeed
            [48 68] 13 ; \X0D\ -> \return
            (throw
              (ex-info (format "Invalid escape sequence: \"%s%s\"" (char n1) (char n2))
                {:chs [(char n1) (char n2)]}))))))))

(comment
  (read-line-break-escape-sequence (<< "0A\\"))
  ,,,)

(defn read-escape-sequence
  "Given a map of encoding characters and a reader, read an escape sequence."
  [{:keys [field-separator
           repetition-separator
           component-separator
           sub-component-separator
           escape-character]}
   ^PushbackReader reader]
  (let [n (.read reader)]
    (cond
      (#{EOS EB} n)
      (throw (ex-info "EOF while reading escape sequence" {}))

      :else
      (let [ret (case n
                  70 field-separator ;; F
                  82 repetition-separator ;; R
                  83 component-separator ;; S
                  84 sub-component-separator ;; T
                  69 escape-character ;; E
                  (88 46) (read-line-break-escape-sequence reader) ;; X
                  (throw (ex-info (format "Invalid escape sequence: \"%s\"" (char n)) {:char (char n)})))
            terminator (.read reader)]

        (when (not= escape-character terminator)
          (throw (ex-info (format "Expected escape character while reading escape sequence, got \"%s\"" (char terminator)) {:char (char terminator)})))

        ret))))

(comment
  (read-escape-sequence default-encoding-characters (<< "F\\"))
  ,,,)

(defn read-string
  "Given a map of encoding characters and a reader, read a string."
  [{:keys [field-separator repetition-separator component-separator sub-component-separator escape-character]
    :as encoding-characters}
   ^PushbackReader reader]
  (let [sb (StringBuilder.)]
    (loop []
      (let [n (.read reader)]
        (cond
          (= EOS n)
          (not-empty (.toString sb))

          (#{EB CR field-separator component-separator sub-component-separator repetition-separator} n)
          (do (.unread reader n) (not-empty (.toString sb)))

          (= escape-character n)
          (do
            (.append sb ^char (char (read-escape-sequence encoding-characters reader)))
            (recur))

          :else
          (do (.append sb ^char (char n))
            (recur)))))))

(comment
  (read-string default-encoding-characters (<< "Parker \\T\\ Sons"))
  (read-string default-encoding-characters (<< "A\r"))
  ,,,)

(defn unwrap1
  "Given a coll, if the coll has one element, return the element, else coll."
  [xs]
  (if (next xs) xs (first xs)))

(comment
  (unwrap1 nil)
  (unwrap1 [])
  (unwrap1 [:a])
  (unwrap1 [:a :b])
  ,,,)

(defn read-repetition
  "Given a map of encoding characters and a reader, read a repetition element."
  [{:keys [field-separator repetition-separator component-separator sub-component-separator]
    :as encoding-characters}
   ^PushbackReader reader]
  (loop [xs []]
    (let [n (.read reader)]
      (cond
        (= EOS n)
        (unwrap1 xs)

        (#{EB CR field-separator component-separator sub-component-separator repetition-separator} n)
        (do (.unread reader n) (unwrap1 xs))

        :else
        (do (.unread reader n)
          (recur (conj xs (read-string encoding-characters reader))))))))

(defn read-sub-component
  "Given a map of encoding characters and a reader, read a sub-component."
  [{:keys [field-separator repetition-separator component-separator sub-component-separator]
    :as encoding-characters}
   ^PushbackReader reader]
  (loop [xs []]
    (let [n (.read reader)]
      (cond
        (= EOS n)
        (unwrap1 xs)

        (#{EB CR field-separator component-separator sub-component-separator} n)
        (do (.unread reader n) (unwrap1 xs))

        (= repetition-separator n)
        (recur (with-meta (conj xs (read-repetition encoding-characters reader)) {:hl7/type :repetition}))

        :else
        (do (.unread reader n)
          (recur (conj xs (read-string encoding-characters reader))))))))

(defn read-component
  "Given a map of encoding characters and a reader, read a component."
  [{:keys [field-separator repetition-separator component-separator sub-component-separator]
    :as encoding-characters}
   ^PushbackReader reader]
  (loop [xs [] n SOE]
    (cond
      (= EOS n)
      (unwrap1 xs)

      (#{EB CR field-separator component-separator} n)
      (do (.unread reader n) (unwrap1 xs))

      (= repetition-separator n)
      (recur
        (with-meta (conj xs (read-repetition encoding-characters reader)) {:hl7/type :repetition})
        (.read reader))

      (= sub-component-separator n)
      (recur
        (with-meta (conj xs (read-sub-component encoding-characters reader)) {:hl7/type :sub-component})
        (.read reader))

      :else
      (recur
        (conj xs (read-string encoding-characters reader))
        (.read reader)))))

(comment
  (read-component default-encoding-characters (<< "A"))
  ,,,)

(defn read-field
  "Given a map of encoding characters and a reader, read a field."
  [{:keys [field-separator]
    :as encoding-characters}
   ^PushbackReader reader]
  (loop [xs [] n SOE]
    (cond
      (= EOS n)
      (unwrap1 xs)

      (#{EB CR field-separator} n)
      (do (.unread reader n) (unwrap1 xs))

      :else
      (recur (with-meta (conj xs (read-component encoding-characters reader)) {:hl7/type :component}) (.read reader)))))

(comment
  (read-field default-encoding-characters (<< "^"))
  ,,,)

(defn read-fields
  "Given a map of encoding characters and a reader, read all fields until a
  terminator (EB, EOS, or CR)."
  [encoding-characters ^PushbackReader reader]
  (loop [xs []]
    (let [n (.read reader)]
      (cond
        (= EB n)
        (do (.unread reader n) xs)

        (#{EOS CR} n) xs

        :else
        (recur (conj xs (read-field encoding-characters reader)))))))

(defn read-segment-identifier
  "Given a reader, read a segment identifier."
  [^PushbackReader reader]
  (let [sb (StringBuilder.)]
    (loop []
      (if (= 3 (.length sb))
        (.toString sb)
        (let [n (.read reader)]
          (cond
            (#{EOS EB} n)
            (throw (ex-info "EOF while reading segment identifier" {:n n}))

            :else
            (do (.append sb (char n)) (recur))))))))

(comment
  (read-segment-identifier (<< "D|"))
  (read-segment-identifier (<< "DG1|1||786.50^CHEST PAIN^I9|||A"))
  ,,,)

(defn read-segments
  "Given a map of encoding characters and a reader, read all message segments in
  the reader."
  [encoding-characters ^PushbackReader reader]
  (loop [xs []]
    (let [n (.read reader)]
      (cond
        ;; Read the CR that trails EB
        (= EB n) (do (.read reader) xs)

        (= EOS n) xs

        (= CR n) (recur xs)

        :else
        (do (.unread reader n)
          (let [id (read-segment-identifier reader)
                fields (read-fields encoding-characters reader)]
            (recur (conj xs [(keyword id) fields]))))))))

(comment
  (read-segments default-encoding-characters (<< "AL1|1||^ASPIRIN\rDG1|1||786.50^CHEST PAIN, UNSPECIFIED^I9|||A"))
  ,,,)

(defn read-encoding-character
  "Given a reader, read an encoding character defined in the MSH.2 HL7 field."
  [^PushbackReader reader]
  (let [n (.read reader)]
    (if (#{EOS EB} n)
      (throw (ex-info "EOF while reading encoding characters" {:n n}))
      n)))

(defn read-header-segment-identifier
  "Given a map of encoding characters and a reader, read a message header
  segment (MSH) identifier."
  [^PushbackReader reader]
  (let [sb (StringBuilder.)]
    (loop []
      (if (= 3 (.length sb))
        (let [id (.toString sb)]
          (if (not= id "MSH")
            (throw (ex-info (format "Bad segment identifier \"%s\"; expected \"MSH\"." id) {:id id}))
            id))
        (let [n (.read reader)]
          (if (#{EOS EB} n)
            (throw (ex-info "EOF while reading segment identifier" {:n n}))
            (do (.append sb (char n)) (recur))))))))

(defn read-header-segment
  "Given a java.io.PushbackReader, parse a HL7 message header segment (MSH).

  Return a map with:

    :encoding-characters -- a map of the encoding characters specified in the header.
    :header-segment -- the header segment (a map)."
  [^PushbackReader reader]
  (let [id (read-header-segment-identifier reader)

        {:keys [field-separator] :as encoding-characters}
        (array-map
          :field-separator (read-encoding-character reader)
          :component-separator (read-encoding-character reader)
          :repetition-separator (read-encoding-character reader)
          :escape-character (read-encoding-character reader)
          :sub-component-separator (read-encoding-character reader))]

    {:encoding-characters encoding-characters

     :header-segment
     (let [fields (read-fields encoding-characters reader)]
       [(keyword id)
        (into [(-> field-separator char str)
               (string/join (map char (vals (rest encoding-characters))))]
          fields)])}))

(comment
  (read-header-segment (<< "MSH|^~\\&|MegaReg|XYZHospC|SuperOE|XYZImgCtr|20060529090131-0500||ADT^A01^ADT_A01|01052901|P|2.5"))
  ,,,)

(defn read
  "Given a java.io.PushbackReader on a vertical bar encoded HL7 message, parse
  the message and return it."
  [reader]
  (let [n (.read reader)
        ;; Maybe throw away MLLP start block character.
        _ (when-not (= SB n) (.unread reader n))
        {:keys [header-segment encoding-characters]} (read-header-segment reader)]
    (into [header-segment] (read-segments encoding-characters reader))))
