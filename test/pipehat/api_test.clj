(ns pipehat.api-test
  (:refer-clojure :exclude [read-string read])
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [clojure.test :refer [are deftest is]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [pipehat.api :as sut]
            [pipehat.impl.const :refer [SB EB CR]]
            [pipehat.impl.reader :refer [<<]]
            [pipehat.specs :as specs])
  (:import (clojure.lang ExceptionInfo)
           (java.io BufferedReader BufferedWriter InputStreamReader OutputStreamWriter PipedInputStream PipedOutputStream PushbackReader)))

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

(deftest mllp-roundtrip
  (let [msg-1 (sut/read-str (slurp "samples/sample-v2.3-adt-a01-1.hl7"))
        msg-2 (sut/read-str (slurp "samples/sample-v2.3-oru-r01-1.hl7"))]
    (with-open [pi (PipedInputStream.)
                po (PipedOutputStream. pi)]
      (future
        (with-open [osw (OutputStreamWriter. po)
                    writer (BufferedWriter. osw)]
          (sut/write writer msg-1 {:protocol :mllp})
          (sut/write writer msg-2 {:protocol :mllp})))

      (with-open [isr (InputStreamReader. pi)
                  bw (BufferedReader. isr)
                  reader (PushbackReader. bw)]
        (is (= msg-1 (sut/read reader)))
        (is (= msg-2 (sut/read reader)))))))

(deftest shaping
  (with-open [reader (PushbackReader. (io/reader "samples/sample-v2.4-oru-r01-1.hl7"))]
    (is (= {:OBX
            [{[:OBX 2] "NM"
              [:OBX 3] {[:OBX 3 1] "3141-9" [:OBX 3 2] "BODY WEIGHT" [:OBX 3 3] "LN"}
              [:OBX 5] "62"
              [:OBX 6] "kg"
              [:OBX 11] "F"}
             {[:OBX 2] "NM"
              [:OBX 3] {[:OBX 3 1] "3137-7" [:OBX 3 2] "HEIGHT" [:OBX 3 3] "LN"}
              [:OBX 5] "190"
              [:OBX 6] "cm"
              [:OBX 11] "F"}]
            :MSH
            [{[:MSH 1] "|"
              [:MSH 2] "^~\\&"
              [:MSH 3] "REGADT"
              [:MSH 4] "MCM"
              [:MSH 5] "IFENG"
              [:MSH 7] "199112311501"
              [:MSH 9] {[:MSH 9 1] "ADT" [:MSH 9 2] "A04" [:MSH 9 3] "ADT_A01"}
              [:MSH 10] "000001"
              [:MSH 11] "P"
              [:MSH 12] "2.4"}]
            :PID
            [{[:PID 3]
              {[:PID 3 1] "191919"
               [:PID 3 3] "GENHOS"
               [:PID 3 4] ["MR" "371-66-9256"]
               [:PID 3 7] "USSSA"
               [:PID 3 8] "SS"}
              [:PID 4] "253763"
              [:PID 5] {[:PID 5 1] "MASSIE" [:PID 5 2] "JAMES" [:PID 5 3] "A"}
              [:PID 7] "19560129"
              [:PID 8] "M"
              [:PID 11]
              {[:PID 11 1] "171 ZOBERLEIN"
               [:PID 11 3] "ISHPEMING"
               [:PID 11 4] "MI"
               [:PID 11 5] "49849"
               [:PID 11 6] "\"\""}
              [:PID 13] "(900)485-5344"
              [:PID 14] "(900)485-5344"
              [:PID 16] {[:PID 16 1] "S" [:PID 16 3] "HL70002"}
              [:PID 17] {[:PID 17 1] "C" [:PID 17 3] "HL70006"}
              [:PID 18] {[:PID 18 1] "10199925" [:PID 18 4] "GENHOS" [:PID 18 5] "AN"}
              [:PID 19] "371-66-9256"}]
            :NK1
            [{[:NK1 1] "1"
              [:NK1 2] {[:NK1 2 1] "MASSIE" [:NK1 2 2] "ELLEN"}
              [:NK1 3] {[:NK1 3 1] "SPOUSE" [:NK1 3 3] "HL70063"}
              [:NK1 4]
              {[:NK1 4 1] "171 ZOBERLEIN"
               [:NK1 4 3] "ISHPEMING"
               [:NK1 4 4] "MI"
               [:NK1 4 5] "49849"
               [:NK1 4 6] "\"\""}
              [:NK1 5] "(900)485-5344"
              [:NK1 6] ["(900)545-1234" "(900)545-1200"]
              [:NK1 7] {[:NK1 7 1] "EC1" [:NK1 7 2] "FIRST EMERGENCY CONTACT" [:NK1 7 3] "HL70131"}}
             {[:NK1 1] "2"
              [:NK1 2] {[:NK1 2 1] "MASSIE" [:NK1 2 2] "MARYLOU"}
              [:NK1 3] {[:NK1 3 1] "MOTHER" [:NK1 3 3] "HL70063"}
              [:NK1 4]
              {[:NK1 4 1] "300 ZOBERLEIN"
               [:NK1 4 3] "ISHPEMING"
               [:NK1 4 4] "MI"
               [:NK1 4 5] "49849"
               [:NK1 4 6] "\"\""}
              [:NK1 5] "(900)485-5344"
              [:NK1 6] ["(900)545-1234" "(900)545-1200"]
              [:NK1 7] {[:NK1 7 1] "EC2" [:NK1 7 2] "SECOND EMERGENCY CONTACT" [:NK1 7 3] "HL70131"}}
             {[:NK1 1] "3"}
             {[:NK1 1] "4"
              [:NK1 4]
              {[:NK1 4 1] "123 INDUSTRY WAY"
               [:NK1 4 3] "ISHPEMING"
               [:NK1 4 4] "MI"
               [:NK1 4 5] "49849"
               [:NK1 4 6] "\"\""}
              [:NK1 6] "(900)545-1200"
              [:NK1 7] {[:NK1 7 1] "EM" [:NK1 7 2] "EMPLOYER" [:NK1 7 3] "HL70131"}
              [:NK1 8] "19940605"
              [:NK1 10] "PROGRAMMER"
              [:NK1 13] "ACME SOFTWARE COMPANY"}]
            :PV1
            [{[:PV1 2] "O"
              [:PV1 3] "O/R"
              [:PV1 7] {[:PV1 7 1] "0148" [:PV1 7 2] "ADDISON,JAMES"}
              [:PV1 8] {[:PV1 8 1] "0148" [:PV1 8 2] "ADDISON,JAMES"}
              [:PV1 10] "AMB"
              [:PV1 17] {[:PV1 17 1] "0148" [:PV1 17 2] "ADDISON,JAMES"}
              [:PV1 18] "S"
              [:PV1 19] "1400"
              [:PV1 20] "A"
              [:PV1 39] "GENHOS"
              [:PV1 44] "199501101410"}]
            :PV2 [{[:PV2 8] "199901101400" [:PV2 33] "199901101400"}]
            :IN1
            [{[:IN1 1] "0"
              [:IN1 2] {[:IN1 2 1] "0" [:IN1 2 2] "HL70072"}
              [:IN1 3] "BC1"
              [:IN1 4] "BLUE CROSS"
              [:IN1 5]
              {[:IN1 5 1] "171 ZOBERLEIN" [:IN1 5 3] "ISHPEMING" [:IN1 5 4] "M149849" [:IN1 5 5] "\"\""}
              [:IN1 7] "(900)485-5344"
              [:IN1 8] "90"
              [:IN1 14] "50 OK"}]
            :EVN
            [{[:EVN 1] "A04"
              [:EVN 2] "199901101500"
              [:EVN 3] "199901101400"
              [:EVN 4] "01"
              [:EVN 6] "199901101410"}]
            :DG1
            [{[:DG1 1] "1"
              [:DG1 2] "19"
              [:DG1 4] {[:DG1 4 1] "R63.4" [:DG1 4 2] "LOSS OF WEIGHT" [:DG1 4 3] "I10"}
              [:DG1 7] "00"}]
            :GT1
            [{[:GT1 1] "1"
              [:GT1 3]
              {[:GT1 3 1] "MASSIE"
               [:GT1 3 2] "JAMES"
               [:GT1 3 3] "\"\""
               [:GT1 3 4] "\"\""
               [:GT1 3 5] "\"\""
               [:GT1 3 6] "\"\""}
              [:GT1 5]
              {[:GT1 5 1] "171 ZOBERLEIN"
               [:GT1 5 3] "ISHPEMING"
               [:GT1 5 4] "MI"
               [:GT1 5 5] "49849"
               [:GT1 5 6] "\"\""}
              [:GT1 6] "(900)485-5344"
              [:GT1 7] "(900)485-5344"
              [:GT1 11] {[:GT1 11 1] "SE" [:GT1 11 2] "SELF" [:GT1 11 3] "HL70063"}
              [:GT1 12] "371-66-925"
              [:GT1 16] "MOOSES AUTO CLINIC"
              [:GT1 17]
              {[:GT1 17 1] "171 ZOBERLEIN"
               [:GT1 17 3] "ISHPEMING"
               [:GT1 17 4] "MI"
               [:GT1 17 5] "49849"
               [:GT1 17 6] "\"\""}
              [:GT1 18] "(900)485-5344"}]
            :ROL
            [{[:ROL 2] "AD"
              [:ROL 3] {[:ROL 3 1] "CP" [:ROL 3 3] "HL70443"}
              [:ROL 4] {[:ROL 4 1] "0148" [:ROL 4 2] "ADDISON,JAMES"}}]}

          (sut/shape (sut/read reader))))))

(defn sample-files
  []
  (filter #(string/ends-with? (.getName %) ".hl7") (.listFiles (io/file "samples"))))

(deftest samples
  (doseq [file (sample-files)]
    (let [input (slurp (io/reader file))]
      (is (= input (sut/write-str (sut/read-str input)))))))

(defspec roundtrip 25
  (prop/for-all [message (spec/gen ::specs/message)]
    (= message (sut/read-str (sut/write-str message)))))

(comment
  (-> [[:MSH ["" ""]]] sut/write-str sut/read-str)
  ,,,)

(spec/def ::identifier
  (specs/catvec :identifier ::specs/segment-identifier :indices (spec/+ pos-int?)))

(spec/def ::repetition
  (spec/coll-of ::specs/non-empty-string :kind vector? :gen-max 2))

(spec/def ::element
  (spec/map-of ::identifier
    (spec/or
      :string ::specs/non-empty-string
      :repetition ::repetition
      :sub-component (spec/map-of ::identifier
                       (spec/or
                         :string ::specs/non-empty-string
                         :repetition ::repetition
                         :sub-component ::element)
                       :gen-max 2))
    :gen-max 3))

(spec/def ::shaped
  (spec/map-of
    ::specs/segment-identifier
    (spec/coll-of ::element :kind vector? :gen-max 3)))

(defspec shaping-gen 25
  (prop/for-all [message (spec/gen ::specs/message)]
    (spec/valid? ::shaped (-> message sut/write-str sut/read-str sut/shape))))
