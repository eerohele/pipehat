(ns pipehat.impl.reader-test
  (:refer-clojure :exclude [read-string read])
  (:require [clojure.test :refer [are deftest is]]
            [pipehat.impl.reader :as sut :refer [<<]]
            [pipehat.impl.defaults :refer [encoding-characters]])
  (:import (clojure.lang ExceptionInfo)))

(deftest read-escape-sequence
  (letfn [(parse [in] (sut/read-string encoding-characters (<< in)))]
    (are [in out] (= out (parse in))
      "\\F\\" "|"
      "\\R\\" "~"
      "\\S\\" "^"
      "\\T\\" "&"
      "\\E\\" "\\"
      "\\X0A\\" "\f"
      "\\X0D\\" "\r"
      "\\.br\\" "\r")

    (is (thrown-with-msg? ExceptionInfo #"EOF while reading escape sequence" (parse "\\")))
    (is (thrown-with-msg? ExceptionInfo #"EOF while reading escape sequence" (parse "\\X")))
    (is (thrown-with-msg? ExceptionInfo #"EOF while reading escape sequence" (parse "\\X0")))
    (is (thrown-with-msg? ExceptionInfo #"Invalid escape sequence: \"Z\"" (parse "\\Z\\")))
    (is (thrown-with-msg? ExceptionInfo #"Invalid escape sequence: \"0X\"" (parse "\\X0X\\")))
    (is (thrown-with-msg? ExceptionInfo #"Expected escape character while reading escape sequence, got \"x\"" (parse "\\X0Dx")))))

(deftest read-string
  (letfn [(parse [in] (sut/read-string encoding-characters (<< in)))]
    (are [in out] (= out (parse in))
      "" nil
      "|" nil
      "^" nil
      "&" nil
      "~" nil
      "A" "A"
      "A \\T\\ B" "A & B"
      "A\rDG1" "A"
      "A|B" "A"
      "A^B" "A"
      "A&B" "A"
      "A~B" "A")))

(deftest read-component
  (letfn [(parse [in] (sut/read-component encoding-characters (<< in)))]
    (are [in out] (= out (parse in))
      "" nil
      "|" nil
      "^" nil
      "AA" "AA"
      "AA BB CC" "AA BB CC"
      "A \\T\\ B" "A & B"
      "AA\rDG1" "AA"
      "AA|BB" "AA"
      "AA^BB" "AA"
      "AA&" ["AA" nil]
      "&AA" [nil "AA"]
      "AA&|" ["AA" nil]
      "AA&BB" ["AA" "BB"]
      "AA&&CC" ["AA" nil "CC"]
      "AA&BB&CC" ["AA" "BB" "CC"])))

(deftest read-field
  (letfn [(parse [in] (sut/read-field encoding-characters (<< in)))]
    (are [in out] (= out (parse in))
      "" nil
      "|" nil
      "^" [nil nil]
      "AA" "AA"
      "AA BB CC" "AA BB CC"
      "A \\T\\ B" "A & B"
      "AA\rDG1" "AA"
      "AA|BB" "AA"
      "AA^BB" ["AA" "BB"]
      "^AA" [nil "AA"]
      "AA^" ["AA" nil]
      "AA^|" ["AA" nil]
      "AA^^BB" ["AA" nil "BB"]
      "AA^BB^CC" ["AA" "BB" "CC"]
      "~AA" [nil "AA"]
      "AA~" ["AA" nil]
      "AA~BB" ["AA" "BB"]
      "AA~~BB" ["AA" nil "BB"]
      "AA~BB~CC" ["AA" "BB" "CC"]
      "AA~^" ["AA" [nil nil]]
      "AA^BB~" [["AA" "BB"] nil]
      "AA^BB~CC" [["AA" "BB"] "CC"]
      "AA^BB~CC^DD" [["AA" "BB"] ["CC" "DD"]]
      "AA&BB~CC" [["AA" "BB"] "CC"]
      "AA&BB~CC&DD" [["AA" "BB"] ["CC" "DD"]]
      "AA^BB&CC~DD" [["AA" ["BB" "CC"]] "DD"])))

(deftest read-header-segment
  (letfn [(parse [in] (sut/read-header-segment (<< in)))]
    (are [in out] (= out (parse in))
      "MSH|^~\\&"
      {:encoding-characters
       {:component-separator 94
        :escape-character 92
        :field-separator 124
        :repetition-separator 126
        :sub-component-separator 38}

       :header-segment ["MSH" ["|" "^~\\&"]]})

    (is (thrown-with-msg? ExceptionInfo #"EOF while reading segment identifier" (parse "A")))
    (is (thrown-with-msg? ExceptionInfo #"EOF while reading encoding characters" (parse "MSH|")))
    (is (thrown-with-msg? ExceptionInfo #"Bad segment identifier \"ABC\"; expected \"MSH\"." (parse "ABC|^~\\&")))))

(deftest read-segments
  (letfn [(parse [in] (sut/read-segments encoding-characters (<< in)))]
    (are [in out] (= out (parse in))
      "ABC|A" [["ABC" ["A"]]]
      "ABC|A|B" [["ABC" ["A" "B"]]]
      "ABC||B" [["ABC" [nil "B"]]]
      "ABC|A^B" [["ABC" [["A" "B"]]]]
      "ABC|^A" [["ABC" [[nil "A"]]]]
      "ABC|A^B&C^D" [["ABC" [["A" ["B" "C"] "D"]]]]
      "ABC|A&B" [["ABC" [["A" "B"]]]]
      "ABC|&B" [["ABC" [[nil "B"]]]]
      "ABC|A&" [["ABC" [["A" nil]]]]
      "ABC|A~B" [["ABC" [["A" "B"]]]]
      "ABC|~B" [["ABC" [[nil "B"]]]]
      "ABC|A~" [["ABC" [["A" nil]]]]
      "ABC|A^B&C^D" [["ABC" [["A" ["B" "C"] "D"]]]]
      "ABC|A~B" [["ABC" [["A" "B"]]]]
      "ABC|A^B~C^D" [["ABC" [[["A" "B"] ["C" "D"]]]]])

    (is (thrown-with-msg? ExceptionInfo #"EOF while reading segment identifier" (parse "^")))))
