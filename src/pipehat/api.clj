(ns pipehat.api
  "Read and write vertical bar encoded HL7 messages (v2.x)."
  (:refer-clojure :exclude [read read-string read+string])
  (:require [pipehat.const :refer [SB ACK NAK EB CR]]
            [pipehat.impl.reader :as reader]
            [pipehat.impl.shaper :as shaper]
            [pipehat.impl.writer :as writer])
  (:import (java.io BufferedWriter PushbackReader Reader StringReader StringWriter)))

(set! *warn-on-reflection* true)

(defmacro ^:private expect
  [c x]
  `(assert (instance? ~c ~x)
     (format "expected: %s, got: %s" (.getName ~c) (some-> ~x class .getName))))

(defn read
  "Given a `java.io.PushbackReader`, parse the HL7 message in the reader and
  return the result.

  Options:

  `:protocol`: Iff `:mllp`, enable MLLP mode. In MLLP mode, the parser discards
  every character preceding the MLLP start-of-block (0x0B) character."
  ([reader]
   (read reader {:protocol :none}))
  ([reader options]
   (expect PushbackReader reader)
   (reader/read reader options)))

(defn read-string
  "Given a string containing an HL7 message, parse the string and return the
  result."
  [s]
  (expect String s)
  (with-open [reader (-> s StringReader. PushbackReader.)]
    (read reader)))

(comment
  (read "")

  (with-open [reader (-> "samples/sample-v2.5.1-oru-r01-1.hl7" java.io.FileReader. PushbackReader.)]
    (read reader))

  (read-string (slurp "samples/sample-v2.5.1-oru-r01-1.hl7"))
  ,,,)

(defn ^:private string-capturing-pushback-reader
  "Given a `java.io.Reader`, return a `java.io.PushbackReader` that captures the
  characters it reads into a string.

  To get the string, call `.toString` on the returned object."
  ^Reader [reader]
  (let [sb (StringBuilder.)]
    (proxy [PushbackReader] [reader]
      (read []
        (let [^Reader this this ; Sidestep reflection warning.
              n (proxy-super read)]
          (when (pos? n) (.append sb (char n)))
          n))

      (toString []
        (.toString sb)))))

(defn read+string
  "Given a `java.io.Reader`, parse the HL7 message in the reader and return a
  two-element vector where the first element is the parsed message and the
  second element is a string containing the message.

  Same options as read.

  Caller must close the reader, as with `read`."
  ([reader]
   (read+string reader {}))
  ([reader options]
   (expect Reader reader)
   (let [reader (string-capturing-pushback-reader reader)]
     (try
       (let [m (read reader options)
             s (str reader)]
         [m s])
       (catch Exception ex
         (throw (ex-info (.getMessage ex) {:s (str reader)})))))))

(comment (read+string (StringReader. "MSH|^~\\&")) ,,,)

(defn shape
  "Given an HL7 message parsed by read, shape it into a map that's easy to reach
  into with Clojure core functions. For example:

  ```clojure
  user=> (def message (-> \"MSH|^~\\&|ACME\" read-string shape))
  #'user/message
  user=> (get-in message [\"MSH\" 0 [\"MSH\" 3]])
  \"ACME\"
  ```

  Shaping is a lossy operation: you CAN NOT round-trip a shaped message back
  into an HL7 string.

  Shaping is therefore only useful if you need to extract data from a message.
  It is not useful if you need to update a message."
  [message]
  (shaper/shape message))

(comment (get-in (-> "MSH|^~\\&|ACME" read-string shape) ["MSH" 0 ["MSH" 3]]) ,,,)

(defn write
  "Given a `java.io.BufferedWriter` and a vector representing an HL7 message
  (presumably parsed by read), write the message into the writer.

  Options:

  `:protocol`: Iff `:mllp`, wrap message with MLLP start-of-block (0x0B) and
  end-of-block (0x1C) characters."
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

(defn write-string
  "Given a vector representing an HL7 message (presumably parsed by `read`),
  write the message into a string and return the string."
  ([message]
   (write-string message {:protocol :none}))
  ([message options]
   (with-open [sw (StringWriter.)
               bw (BufferedWriter. sw)]
     (write bw message options)
     (.toString sw))))

(comment
  (with-open [reader (PushbackReader. (java.io.FileReader. "samples/sample-v2.5.1-oru-r01-1.hl7"))]
    (write-string (read reader)))

  (with-open [reader (PushbackReader. (java.io.FileReader. "samples/sample-v2.4-oru-r01-2.hl7"))]
    (with-open [sw (StringWriter.)
                bw (BufferedWriter. sw)]
      (write bw (read reader) {:protocol :mllp})
      (str sw)))
  ,,,)

(defn ack
  "Given a `java.io.BufferedWriter`, write a Commit Acknowledgement Block (ACK)
  into the writer and flush."
  [^BufferedWriter writer]
  (expect BufferedWriter writer)
  (.write writer SB)
  (.write writer ACK)
  (.write writer EB)
  (.write writer CR)
  (.flush writer))

(defn nak
  "Given a `java.io.BufferedWriter`, write a Negative Commit Acknowledgement
  Block (NAK) into the writer and flush."
  [^BufferedWriter writer]
  (expect BufferedWriter writer)
  (.write writer SB)
  (.write writer NAK)
  (.write writer EB)
  (.write writer CR)
  (.flush writer))
