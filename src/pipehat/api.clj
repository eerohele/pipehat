(ns pipehat.api
  "Read and write vertical bar encoded HL7 messages (v2.x)."
  (:refer-clojure :exclude [read])
  (:require [pipehat.impl.reader :as reader]
            [pipehat.impl.shaper :as shaper]
            [pipehat.impl.writer :as writer])
  (:import (java.io BufferedWriter PushbackReader StringReader StringWriter)))

(set! *warn-on-reflection* true)

(defmacro ^:private expect
  [c x]
  `(assert (instance? ~c ~x)
     (format "expected: %s, got: %s" (.getName ~c) (some-> ~x class .getName))))

(defn read
  "Given a java.io.PushbackReader, parse the HL7 message in the reader and
  return the result.

  Options:

    :protocol
      Iff :mllp, enable MLLP mode. In MLLP mode, the parser discards every
      character preceding the MLLP start-of-block (0x0B) character."
  ([reader]
   (read reader {:protocol :none}))
  ([reader options]
   (expect PushbackReader reader)
   (reader/read reader options)))

(defn read-str
  "Given a string containing a HL7 message, parse the string and return the
  result."
  [s]
  (expect String s)
  (with-open [reader (-> s StringReader. PushbackReader.)]
    (read reader)))

(comment
  (read "")

  (with-open [reader (-> "samples/sample-v2.5.1-oru-r01-1.hl7" java.io.FileReader. PushbackReader.)]
    (read reader))

  (read-str (slurp "samples/sample-v2.5.1-oru-r01-1.hl7"))
  ,,,)

(defn ^:experimental shape
  "EXPERIMENTAL; subject to change.

  Given a HL7 message parsed by read, shape it into a map compatible with
  Clojure core functions.

  You CAN NOT write a shaped message back into a HL7 string."
  [message]
  (shaper/shape message))

(defn write
  "Given a java.io.BufferedWriter and a vector representing a HL7 message
  (presumably parsed by read), write the message into the writer.

  Options:

    :protocol
      Iff :mllp, wrap message with MLLP start-of-block (0x0B) and end-of-block
      (0x1C) characters."
  ([writer message]
   (write writer message {:protocol :none}))
  ([writer message options]
   (expect BufferedWriter writer)
   (assert (some? message) "message must not be nil")
   (writer/write writer message options)))

(comment
  (write nil nil)
  (write (java.io.StringWriter.) nil)
  ,,,)

(defn write-str
  "Given a vector representing a HL7 message (presumably parsed by read), write
  the message into a string and return the string."
  ([message]
   (write-str message {:protocol :none}))
  ([message options]
   (with-open [sw (StringWriter.)
               bw (BufferedWriter. sw)]
     (write bw message options)
     (.toString sw))))

(comment
  (with-open [reader (PushbackReader. (java.io.FileReader. "samples/sample-v2.5.1-oru-r01-1.hl7"))]
    (write-str (read reader)))

  (with-open [reader (PushbackReader. (java.io.FileReader. "samples/sample-v2.4-oru-r01-2.hl7"))]
    (with-open [sw (StringWriter.)
                bw (BufferedWriter. sw)]
      (write bw (read reader) {:protocol :mllp})
      (str sw)))
  ,,,)
