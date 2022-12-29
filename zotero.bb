#!/usr/bin/env bb
(require '[babashka.deps :as deps])
(deps/add-deps '{:deps {org.babashka/spec.alpha {:git/url "https://github.com/babashka/spec.alpha"
                                                 :git/sha "1d9df099be4fbfd30b9b903642ad376373c16298"}}})
(require '[clojure.spec.alpha :as s]
         '[babashka.curl :as curl]
         '[cheshire.core :as json]
         '[clojure.pprint :as pprint])

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

(s/def ::collection (s/keys :req [::key ::meta]))
(s/def ::key string?)
(s/def ::meta (s/keys :req [::numItems]))
(s/def ::numItems int?)


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

(s/def ::collection-count (s/keys :req [::key ::count]))
(s/def ::key string?)
(s/def ::count int?)

(defn get-paper [ind coll-key]
  (let [path (str "/users/" user-id "/collections/" coll-key "/items")
        url (str base-url path)]
    (->
     (curl/get url {:headers headers, :query-params {"start" ind, "limit" 1}})
     :body
     (json/parse-string true)
     first
     :data)))

(s/def ::paper (s/keys :req [::data]))
(s/def ::data (s/keys :req [::creators ::title ::url ::key]))
(s/def ::creators (s/coll-of ::creator))
(s/def ::creator (s/keys :req [::creatorType ::firstName ::lastName]))

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