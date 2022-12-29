#!/usr/bin/env bb
(require '[babashka.curl :as curl])
(require '[cheshire.core :as json])

(defn call-zotero [api-key, path, query-params]
  (let  [base-url "https://api.zotero.org"
         headers {"Zotero-API-Version", 3
                  "Zotero-API-Key", api-key}
         url (str base-url path)]
    (->
     (curl/get url {:headers headers, :query-params query-params})
     :body
     (json/parse-string true))))

(defn papers-path [user-id, collection-id]
  (str "/users/" user-id "/collections/" collection-id "/items"))

;; get the list of papers

;; We want to return a lazy sequence. We should be able to recur, terminating when there aren't any more things left to call


(defn get-papers
  ([api-key, user-id, collection-id]
   (get-papers api-key user-id collection-id 0))

  ([api-key, user-id, collection-id, start]
   (let [path (str "/users/" user-id "/collections/" collection-id "/items")]
     (call-zotero api-key path [[:limit 100, :start start]]))))


(defn main
  []
  (println "Hello, world!"))

(when (= *file* (System/getProperty "babashka.file")) (main))