#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clj-yaml.core :as yaml])

(defn abort-with-error [msg]
  (binding [*out* *err*]
    (println "Error:" msg))
  (System/exit 1))

(defn get-env
  [var]
  (let [value (System/getenv var)]
    (when-not value
      (abort-with-error (str "environment variable " var " not defined")))
    value))

(def API-KEY (get-env "ZOTERO_API_KEY"))
(def USER-ID (get-env "ZOTERO_USER_ID"))


(defn zotero-get
  "make a get request against the zotero api, and return the response body as a clojure map"
  ([path]
   (zotero-get path {}))
  ([path query-params]
   (let [base-url "https://api.zotero.org"
         headers {"Zotero-API-Version", 3
                  "Zotero-API-Key", API-KEY}]
     (-> base-url
         (str path)
         (curl/get {:headers headers, :query-params query-params})
         :body
         (json/parse-string true)))))

(defn coll->count
  "given a Zotero collection map, return a map with the colleciton key and count"
  [{:keys [key]
    {:keys [numItems]} :meta}]
  {:key key,
   :count numItems})

(defn coll-name
  [coll]
  (get-in coll [:data :name]))

(defn collection-count
  "number of items in a named Zotero collection"
  [collection-name]
  (->> USER-ID
       (#(str "/users/" % "/collections"))
       zotero-get
       (filter #(= collection-name (coll-name %)))
       first
       coll->count))

(defn items-path [coll-key]
  (str "/users/" USER-ID "/collections/" coll-key "/items/top"))

(def collection-count-memoized (memoize collection-count))

(defn name->key
  "given a collection name `name`, return its key"
  [name]
  (-> name collection-count-memoized :key))

(defn get-paper
  "retrieve paper info for collection named `name` at index `ind`"
  [ind name]
  (let [query-params {:start ind, :limit 1}]
    (-> name
        name->key
        items-path
        (zotero-get query-params)
        first
        :data)))

(defn creator->author
  [{:keys [firstName lastName]}]
  (str firstName " " lastName))

(defn authorize
  [paper]
  (assoc paper :authors
         (->> paper :creators (map creator->author))))


(defn main
  [collection-name]
  (-> collection-name
      collection-count-memoized
      :count
      rand-int
      (get-paper collection-name)
      authorize
      (select-keys [:authors :title :url :key :publicationTitle])
      (yaml/generate-string :dumper-options {:flow-style :block})
      print))

(when (= *file* (System/getProperty "babashka.file")) (main "To read"))