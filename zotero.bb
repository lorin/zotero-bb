#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clj-yaml.core :as yaml])

(defn abort-with-error [msg]
  (binding [*out* *err*]
    (println "Error:" msg))
  (System/exit 1))

(def API-KEY (System/getenv "ZOTERO_API_KEY"))
(when (nil? API-KEY) (abort-with-error "ZOTERO_API_KEY not defined"))

(def USER-ID (System/getenv "ZOTERO_USER_ID"))
(when (nil? USER-ID) (abort-with-error "ZOTERO_USER_ID not defined"))

(defn zotero-get
  "make a get request against the zotero api, and return the response body as a clojure map"
  ([path query-params]
   (let [base-url "https://api.zotero.org"
         headers {"Zotero-API-Version", 3
                  "Zotero-API-Key", API-KEY}]
     (-> base-url
         (str path)
         (curl/get {:headers headers, :query-params query-params})
         :body
         (json/parse-string true))))
  ([path] (zotero-get path {})))

(defn collection-count
  "number of items in a collection"
  [collection-name]
  (let [path (str "/users/" USER-ID "/collections")
        name #(get-in % [:data :name])
        colls (zotero-get path)
        coll (->> colls (filter #(= collection-name (name %))) first)]
    {:key (:key coll)
     :count (get-in coll [:meta :numItems])}))

(defn items-path [coll-key]
  (str "/users/" USER-ID "/collections/" coll-key "/items/top"))

(defn get-paper [ind coll-key]
    (-> coll-key
        items-path
        (zotero-get {"start" ind, "limit" 1})
        first
        :data))


(defn creator->author
  [creator]
  (str (:firstName creator) " " (:lastName creator)))

(defn authorize
  [paper]
  (assoc paper :authors
         (->> paper :creators (map creator->author))))

(defn main
  []
  (let [coll-count (collection-count "To read")
        coll-key (:key coll-count)]
    (-> coll-count
        :count
        rand-int
        (get-paper coll-key)
        authorize
        (select-keys [:authors :title :url :key :publicationTitle])
        ( yaml/generate-string :dumper-options {:flow-style :block})
        print)))

(when (= *file* (System/getProperty "babashka.file")) (main))