#!/usr/bin/env bb
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                                 :git/sha "1d9df099be4fbfd30b9b903642ad376373c16298"}}})
(require '[clojure.spec.alpha :as s]
         '[babashka.curl :as curl]
         '[cheshire.core :as json])

(defn abort-with-error [msg]
  (binding [*out* *err*]
    (println "Error:" msg))
  (System/exit 1))

(def api-key (System/getenv "ZOTERO_API_KEY"))
(when (nil? api-key) (abort-with-error "ZOTERO_API_KEY not defined"))

(def user-id (System/getenv "ZOTERO_USER_ID"))
(when (nil? user-id) (abort-with-error "ZOTERO_USER_ID not defined"))


(def headers {"Zotero-API-Version", 3
              "Zotero-API-Key", api-key})
(def base-url "https://api.zotero.org")

(s/def :zotero/collection (s/keys :req [:zotero/key :zotero/meta]))
(s/def :zotero/key string?)
(s/def :zotero/meta (s/keys :req [:zotero/numItems]))
(s/def :zotero/numItems int?)


  (defn collection-count
    "number of items in a collection"
    [collection-name]
    (let [url (str base-url "/users/" user-id "/collections")
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

(s/def :zotero/collection-count (s/keys :req [:zotero/key :zotero/count]))
(s/def :zotero/key string?)
(s/def :zotero/count int?)


(collection-count "To read")

(defn get-paper [coll-key ind]
  (let [path (str "/users/" user-id "/collections/" coll-key "/items")
        url (str base-url path)]
    (->
     (curl/get url {:headers headers, :query-params {"start" ind, "limit" 1}})
     :body
     (json/parse-string true)
     first
     :data
     :title)))

(s/def ::paper (s/keys :req [::data]))
(s/def ::data (s/keys :req [::title]))

(defn main
  []
  (let [coll-count (collection-count "To read")
        coll-key (:key coll-count)
        ind (rand-int (:count coll-count))]
    (->> ind
         (get-paper coll-key)
         println)))



(when (= *file* (System/getProperty "babashka.file")) (main))