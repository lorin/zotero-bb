#!/usr/bin/env bb
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                                 :git/sha "1d9df099be4fbfd30b9b903642ad376373c16298"}}})
(require '[clojure.spec.alpha :as s]
         '[babashka.curl :as curl]
         '[cheshire.core :as json])

(def api-key (System/getenv "ZOTERO_API_KEY"))
(def user-id (System/getenv "ZOTERO_USER_ID"))

(s/def ::collection (s/keys :req [::key ::meta]))
(s/def ::key string?)
(s/def ::meta (s/keys :req [::numItems]))
(s/def ::numItems int?)

(do
  (defn collection-count
    "number of items in a collection"
    [collection]
    (let [base-url "https://api.zotero.org"
          headers {"Zotero-API-Version", 3
                   "Zotero-API-Key", api-key}
          url (str base-url "/users/" user-id "/collections")
          name #(get-in % [:data :name])
          colls (->
                 (curl/get url {:headers headers})
                 :body
                 (json/parse-string true))]
      (->
       (filter #(= collection (name %)) colls)
       first
:meta
:numItems
)))

  (collection-count "To read"))




  (defn call-zotero [path, query-params]
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
  ([user-id, collection-id]
   (get-papers user-id collection-id 0))

  ([user-id, collection-id, start]
   (let [path (str "/users/" user-id "/collections/" collection-id "/items")]
     (call-zotero path [[:limit 100, :start start]]))))


(defn main
  []
  (println "Hello, world!"))

(when (= *file* (System/getProperty "babashka.file")) (main))