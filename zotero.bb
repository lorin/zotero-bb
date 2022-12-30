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
  [coll]
  (let [{:keys [key meta]} coll]
    {:key key
     :count (:numItems meta)}))

(defn coll-name
  [coll]
  (get-in coll [:data :name]))

(defn collection-count
  "number of items in a collection"
  [collection-name]
  (->> USER-ID
       (#(str "/users/" % "/collections"))
       zotero-get
       (filter #(= collection-name (coll-name %)))
       first
       coll->count))

(defn items-path [coll-key]
  (str "/users/" USER-ID "/collections/" coll-key "/items/top"))

(defn get-paper
  "retrieve paper info for collection `coll-key` at index `ind`"
  [ind coll-key]
  (-> coll-key
      items-path
      (zotero-get {"start" ind, "limit" 1})
      first
      :data))

(defn creator->author
  "take a map that "
  [creator]
  (str (:firstName creator) " " (:lastName creator)))

(defn authorize
  [paper]
  (assoc paper :authors
         (->> paper :creators (map creator->author))))

(def collection-count-memoized (memoize collection-count))

(defn name->key
  "given a collection name `name`, return its key"
  [name]
  (-> name collection-count-memoized :key))


(defn main
  []
  (let [collection-name "To read"
        collection-key (name->key collection-name)]
    (-> collection-name
        collection-count-memoized
        :count
        rand-int
        (get-paper collection-key)
        authorize
        (select-keys [:authors :title :url :key :publicationTitle])
        (yaml/generate-string :dumper-options {:flow-style :block})
        print)))

(when (= *file* (System/getProperty "babashka.file")) (main))