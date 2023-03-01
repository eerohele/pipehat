(ns pipehat.api-test
  (:refer-clojure :exclude [read-string read])
  (:require [clojure.test :refer [are deftest is]]
            [pipehat.api :as sut]
            [pipehat.impl.const :refer [SB EB CR]]
            [pipehat.impl.reader :refer [<<]])
  (:import (clojure.lang ExceptionInfo)
           (java.io BufferedWriter PipedReader PipedWriter PushbackReader)))

(deftest read
  (are [in out] (= out (sut/read (<< in)))
    ;; Minimal message
    "MSH|^~\\&" [[:MSH ["|" "^~\\&"]]]

    ;; Basic example
    (str
      "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
      "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
      "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
      "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F\r")
    [[:MSH
      ["|" "^~\\&" "GHH LAB" "ELAB-3" "GHH OE" "BLDG4" "200202150930" nil  ["ORU" "R01"] "CNTRL-3456" "P" "2.4"]]
     [:PID
      [nil nil "555-44-4444" nil ["EVERYWOMAN" "EVE" "E" nil nil nil "L"] "JONES" "196203520" "F" nil nil ["153 FERNWOOD DR." nil "STATESVILLE" "OH" "35292"] nil "(206)3345232" "(206)752-121" nil nil nil "AC555444444" nil ["67-A4335" "OH" "20030520"]]]
     [:OBR
      ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ["1554-5" "GLUCOSE"] nil nil "200202150730" nil nil nil nil nil nil nil [#_repetition ["555-55-5555" "555-66-6666-666"] "PRIMARY" "PATRICIA P" nil nil nil "MD" nil "LEVEL SEVEN HEALTHCARE, INC."] nil nil nil nil nil nil nil nil "F" nil nil nil nil nil ["444-44-4444" "HIPPOCRATES" "HOWARD H" nil nil nil "MD"]]]
     [:OBX
      ["1""SN" ["1554-5" "GLUCOSE" "POST 12H CFST:MCNC:PT:SER/PLAS:QN"] nil [nil "182"] "mg/dl" "70_105" "H" nil nil "F"]]]

    ;; MLLP
    (str
      (char SB)
      "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
      "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
      "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555~555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
      "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F\r"
      (char EB)
      (char CR))
    [[:MSH
      ["|" "^~\\&" "GHH LAB" "ELAB-3" "GHH OE" "BLDG4" "200202150930" nil  ["ORU" "R01"] "CNTRL-3456" "P" "2.4"]]
     [:PID
      [nil nil "555-44-4444" nil ["EVERYWOMAN" "EVE" "E" nil nil nil "L"] "JONES" "196203520" "F" nil nil ["153 FERNWOOD DR." nil "STATESVILLE" "OH" "35292"] nil "(206)3345232" "(206)752-121" nil nil nil "AC555444444" nil ["67-A4335" "OH" "20030520"]]]
     [:OBR
      ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ["1554-5" "GLUCOSE"] nil nil "200202150730" nil nil nil nil nil nil nil [#_repetition ["555-55-5555" "555-66-6666-666"] "PRIMARY" "PATRICIA P" nil nil nil "MD" nil "LEVEL SEVEN HEALTHCARE, INC."] nil nil nil nil nil nil nil nil "F" nil nil nil nil nil ["444-44-4444" "HIPPOCRATES" "HOWARD H" nil nil nil "MD"]]]
     [:OBX
      ["1""SN" ["1554-5" "GLUCOSE" "POST 12H CFST:MCNC:PT:SER/PLAS:QN"] nil [nil "182"] "mg/dl" "70_105" "H" nil nil "F"]]])

  (is (thrown-with-msg? ExceptionInfo #"EOF while reading segment identifier" (sut/read (<< "M"))))
  (is (thrown-with-msg? ExceptionInfo #"EOF while reading segment identifier" (sut/read (<< "MS"))))
  (is (thrown-with-msg? ExceptionInfo #"EOF while reading encoding characters" (sut/read (<< "MSH"))))
  (is (thrown-with-msg? ExceptionInfo #"EOF while reading encoding characters" (sut/read (<< "MSH|"))))
  (is (thrown-with-msg? ExceptionInfo #"EOF while reading segment identifier" (sut/read (<< "")))))

(deftest write
  ;; Basic example
  (is (= (str
           "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
           "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
           "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555^555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
           "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F"
           (char CR))

        (sut/write-str [[:MSH
                         ["|" "^~\\&" "GHH LAB" "ELAB-3" "GHH OE" "BLDG4" "200202150930" nil ["ORU" "R01"] "CNTRL-3456" "P" "2.4"]]
                        [:PID
                         [nil nil "555-44-4444" nil ["EVERYWOMAN" "EVE" "E" nil nil nil "L"] "JONES" "196203520" "F" nil nil ["153 FERNWOOD DR." nil "STATESVILLE" "OH" "35292"] nil "(206)3345232" "(206)752-121" nil nil nil "AC555444444" nil ["67-A4335" "OH" "20030520"]]]
                        [:OBR
                         ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ["1554-5" "GLUCOSE"] nil nil "200202150730" nil nil nil nil nil nil nil [#_repetition ["555-55-5555" "555-66-6666-666"] "PRIMARY" "PATRICIA P" nil nil nil "MD" nil "LEVEL SEVEN HEALTHCARE, INC."] nil nil nil nil nil nil nil nil "F" nil nil nil nil nil ["444-44-4444" "HIPPOCRATES" "HOWARD H" nil nil nil "MD"]]]
                        [:OBX
                         ["1""SN" ["1554-5" "GLUCOSE" "POST 12H CFST:MCNC:PT:SER/PLAS:QN"] nil [nil "182"] "mg/dl" "70_105" "H" nil nil "F"]]])))

  ;; MLLP
  (is (= (str
           (char SB)
           "MSH|^~\\&|GHH LAB|ELAB-3|GHH OE|BLDG4|200202150930||ORU^R01|CNTRL-3456|P|2.4\r"
           "PID|||555-44-4444||EVERYWOMAN^EVE^E^^^^L|JONES|196203520|F|||153 FERNWOOD DR.^^STATESVILLE^OH^35292||(206)3345232|(206)752-121||||AC555444444||67-A4335^OH^20030520\r"
           "OBR|1|845439^GHH OE|1045813^GHH LAB|1554-5^GLUCOSE|||200202150730||||||||555-55-5555^555-66-6666-666^PRIMARY^PATRICIA P^^^^MD^^LEVEL SEVEN HEALTHCARE, INC.|||||||||F||||||444-44-4444^HIPPOCRATES^HOWARD H^^^^MD\r"
           "OBX|1|SN|1554-5^GLUCOSE^POST 12H CFST:MCNC:PT:SER/PLAS:QN||^182|mg/dl|70_105|H|||F"
           (char EB)
           (char CR))

        (sut/write-str [[:MSH
                         ["|" "^~\\&" "GHH LAB" "ELAB-3" "GHH OE" "BLDG4" "200202150930" nil ["ORU" "R01"] "CNTRL-3456" "P" "2.4"]]
                        [:PID
                         [nil nil "555-44-4444" nil ["EVERYWOMAN" "EVE" "E" nil nil nil "L"] "JONES" "196203520" "F" nil nil ["153 FERNWOOD DR." nil "STATESVILLE" "OH" "35292"] nil "(206)3345232" "(206)752-121" nil nil nil "AC555444444" nil ["67-A4335" "OH" "20030520"]]]
                        [:OBR
                         ["1" ["845439" "GHH OE"] ["1045813" "GHH LAB"] ["1554-5" "GLUCOSE"] nil nil "200202150730" nil nil nil nil nil nil nil [#_repetition ["555-55-5555" "555-66-6666-666"] "PRIMARY" "PATRICIA P" nil nil nil "MD" nil "LEVEL SEVEN HEALTHCARE, INC."] nil nil nil nil nil nil nil nil "F" nil nil nil nil nil ["444-44-4444" "HIPPOCRATES" "HOWARD H" nil nil nil "MD"]]]
                        [:OBX
                         ["1""SN" ["1554-5" "GLUCOSE" "POST 12H CFST:MCNC:PT:SER/PLAS:QN"] nil [nil "182"] "mg/dl" "70_105" "H" nil nil "F"]]]
          {:protocol :mllp}))))

(deftest mllp
  (let [msg-1 (sut/read-str (slurp "samples/sample-v2.3-adt-a01-1.hl7"))
        msg-2 (sut/read-str (slurp "samples/sample-v2.3-oru-r01-1.hl7"))]
    (with-open [pr (PipedReader.) pw (PipedWriter. pr)]
      (future
        (with-open [writer (BufferedWriter. pw)]
          (sut/write writer msg-1 {:protocol :mllp})
          (sut/write writer msg-2 {:protocol :mllp})))

      (with-open [reader (PushbackReader. pr)]
        (is (= msg-1 (sut/read reader)))
        (is (= msg-2 (sut/read reader)))))))
