#!/usr/bin/env bb
(require '[babashka.curl :as curl])
(require '[clojure.java.shell :refer [sh]])
(require '[clojure.string :as str])

(defn main
	[]
	(println "Hello, world!"))

(when (= *file* (System/getProperty "babashka.file")) (main))