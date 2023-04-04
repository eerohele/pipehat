(ns pipehat.const
  "HL7 and MLLP constants.")

(def ^{:const true :doc "The HL7 Minimal Lower Layer Protocol (MLLP) start-of-block character."} SB 0x0B)
(def ^{:const true :doc "The HL7 Minimal Lower Layer Protocol (MLLP) end-of-block character."} EB 0x1C)
(def ^{:const true :doc "The ASCII carriage return character."} CR 0x0D)
(def ^{:const true :doc "The ASCII acknowledgement character."} ACK 0x06)
(def ^{:const true :doc "The ASCII negative acknowledgement character."} NAK 0x15)
