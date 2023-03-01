(ns pipehat.api
  "Read and write vertical bar encoded HL7 messages (v2.x)."
  (:refer-clojure :exclude [read])
  (:require [pipehat.impl.reader :as reader]
            [pipehat.impl.writer :as writer])
  (:import (java.io BufferedWriter PushbackReader StringReader StringWriter)))

(set! *warn-on-reflection* true)

(defmacro ^:private expect
  [c x]
  `(assert (instance? ~c ~x)
     (format "expected: %s, got: %s" (.getName ~c) (some-> ~x class .getName))))

(defn read
  "Given a java.io.PushbackReader, parse the HL7 message in the reader and
  return the result."
  [reader]
  (expect PushbackReader reader)
  (reader/read reader))

(defn read-str
  "Given a HL7 message string, parse the message in the string and return the
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

(defn write
  "Given a java.io.BufferedWriter and a vector representing a HL7 message
  (presumably parsed by read), write the message into the writer.

  Options:

    :protocol
      Iff :mllp, wrap message with MLLP start block (0x0B) and end block (0x1C)
      characters."
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

(comment
  (let [s (str
            "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
            "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
            "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
            "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F\r")
        message (read-str s)]
    (tap> message)
    (= s (write-str message)))
  ,,,)
