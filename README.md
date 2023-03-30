# Pipehat

Pipehat (`|^`) is a zero-dependency Clojure library for reading and writing [HL7 version 2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185) messages encoded using vertical bar ("pipehat") encoding.

> **Warning**
> Pipehat is not ready for public consumption.

## Example

```clojure
user=> (require '[pipehat.api :as pipehat])
;;=> nil

user=> (def input
         (str
           "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
           "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
           "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
           "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F\r"))
;;=> #'user/input

user=> (pipehat/read-str input)
;;=> [[:MSH ["|" "^~\\&" "GHH LAB" ...]]
;;    [:PID [nil nil "555-44-4444" ...]]
;;    [:OBR ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ...]]
;;    ...]

;; Round-tripping
user=> (assert (= input (-> input pipehat/read-str pipehat/write-str)))
;;=> true

;; Shaping the message into a more palatable format (experimental)
user=> (-> input pipehat/read-str pipehat/shape)
;;=> {:MSH [{[:MSH 1] "|", [:MSH 2] "^~\\&", ...}],
;;    :PID [{[:PID 3] "555-44-4444", [:PID 5] {#, #, ...}, ...}],
;;    ...}
```

For more examples, see [`hello.repl`](https://github.com/eerohele/pipehat/blob/main/repl/hello.repl).
