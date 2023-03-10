(ns pipehat.roundtrip-test
  (:require [clojure.java.io :as io]
            [clojure.spec.alpha :as spec]
            [clojure.string :as string]
            [clojure.test :refer [deftest is]]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [pipehat.api :as sut]
            [pipehat.specs :as specs]))

(defn sample-files
  []
  (filter #(string/ends-with? (.getName %) ".hl7") (.listFiles (io/file "samples"))))

(deftest samples
  (doseq [file (sample-files)]
    (let [input (slurp (io/reader file))]
      (is (= input (sut/write-str (sut/read-str input)))))))

(defspec generative 25
  (prop/for-all [message (spec/gen ::specs/message)]
    (= message (sut/read-str (sut/write-str message)))))

(comment
  (-> [[:MSH [" " ""]]] sut/write-str sut/read-str)
  ,,,)