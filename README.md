# Pipehat

Pipehat (`|^`) is a zero-dependency Clojure library for reading and writing [HL7 version 2](https://www.hl7.org/implement/standards/product_brief.cfm?product_id=185) messages encoded using the vertical bar ("pipehat") encoding.

> **Warning**
> Pipehat is not stable enough for public consumption.

## Example

```clojure
(require '[pipehat.api :as pipehat])
;;=> nil

(def input
  (str
    "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
    "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
    "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
    "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F\r"))
;;=> #'user/input

(pipehat/read-str input)
;;=> [[:MSH ["|" "^~\\&" "GHH LAB" ...]]
;;    [:PID [nil nil "555-44-4444" ...]]
;;    [:OBR ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ...]]
;;    ...]

;; Round-tripping
(assert (= (pipehat/write-str (pipehat/read-str input))))
;;=> true
```
