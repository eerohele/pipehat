# Pipehat

[![Clojars Project](https://img.shields.io/clojars/v/me.flowthing/pipehat.svg)](https://clojars.org/me.flowthing/pipehat)

Pipehat (`|^`) is a zero-dependency Clojure library for reading and writing [HL7 version 2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185) messages encoded using vertical bar ("pipehat") encoding.

## Features

- Read, write, and round-trip HL7 v2 messages
- [Minimum Lower Layer Protocol](http://www.hl7.org/implement/standards/product_brief.cfm?product_id=55) (MLLP) support (see [`hello.repl`](https://github.com/eerohele/pipehat/blob/main/repl/hello.repl) for an example)
- Shape read messages into maps for easier (and indexed) access to message data

## Example

```clojure
(require '[pipehat.api :as hl7])
;;=> nil

(def input
  (str
    "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4" \return
    "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520" \return
    "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD" \return
    "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F" \return))
;;=> #'user/input

(hl7/read-string input)
;;=> [["MSH" ["|" "^~\\&" "GHH LAB" ...]]
;;    ["PID" [nil nil "555-44-4444" ...]]
;;    ["OBR" ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ...]]
;;    ...]

;; Round-tripping
(assert (= input (-> input hl7/read-string hl7/write-string)))
;;=> true

;; Shaping the message into a more palatable format
(-> input hl7/read-string hl7/shape)
;;=> {"MSH" [{["MSH" 1] "|", ["MSH" 2] "^~\\&", ...}],
;;    "PID" [{["PID" 3] "555-44-4444", ["PID" 5] {#, #, ...}, ...}],
;;    ...}

;; Get the value of OBX.3.3 from the shaped message
(get-in *1 ["OBX" 0 ["OBX" 3] ["OBX" 3 3]])
;;=> "POST 12H CFST:MCNC:PT:SER/PLAS:QN"
```

For more examples, see [`hello.repl`](https://github.com/eerohele/pipehat/blob/main/repl/hello.repl).

## API

See [API docs](https://eerohele.github.io/pipehat/).

## Status

Stable. There will be no breaking changes to the API. No new features are planned.

Everything apart from the `pipehat.api` namespace is internal and subject to change.

## Prior art

- [Clojure HL7 Version 2.x Message Parser](https://github.com/cmiles74/clojure-hl7-messaging-2-parser)
