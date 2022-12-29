#!/usr/bin/env bb
(require '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clojure.pprint :as pprint])

(defn abort-with-error [msg]
  (binding [*out* *err*]
    (println "Error:" msg))
  (System/exit 1))

(def API-KEY (System/getenv "ZOTERO_API_KEY"))
(when (nil? API-KEY) (abort-with-error "ZOTERO_API_KEY not defined"))

(def USER-ID (System/getenv "ZOTERO_USER_ID"))
(when (nil? USER-ID) (abort-with-error "ZOTERO_USER_ID not defined"))


(def headers {"Zotero-API-Version", 3
              "Zotero-API-Key", API-KEY})
(def base-url "https://api.zotero.org")


(defn collection-count
  "number of items in a collection"
  [collection-name]
  (let [url (str base-url "/users/" USER-ID "/collections")
        name #(get-in % [:data :name])
        colls (->
               (curl/get url {:headers headers})
               :body
               (json/parse-string true))
        coll
        (->
         (filter #(= collection-name (name %)) colls)
         first)]
    {:key (:key coll)
     :count (get-in coll [:meta :numItems])}))

(defn get-paper [ind coll-key]
  (let [path (str "/users/" USER-ID "/collections/" coll-key "/items")
        url (str base-url path)]
    (->
     (curl/get url {:headers headers, :query-params {"start" ind, "limit" 1}})
     :body
     (json/parse-string true)
     first
     :data)))

(defn main
  []
  (let [coll-count (collection-count "To read")
        coll-key (:key coll-count)
        creator->author (fn [creator] (str (:firstName creator) " " (:lastName creator)))
        authorize (fn [paper] (assoc paper :authors
                                     (->> paper :creators (map creator->author))))]
    (-> coll-count
        :count
        rand-int
        (get-paper coll-key)
        authorize
        (select-keys [:authors :title :url :key :publicationTitle])
        pprint/pprint)))

(when (= *file* (System/getProperty "babashka.file")) (main))