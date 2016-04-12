(ns statusapp.get-statuses
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f])
  (:import [java.io PrintWriter]
           [com.amazonaws.services.dynamodbv2 AmazonDynamoDBClient]
           [com.amazonaws.services.dynamodbv2.document DynamoDB Item]
           (com.amazonaws.services.dynamodbv2.document.internal ScanCollection))
  (:gen-class
   :methods [^:static [handle [java.io.InputStream
                               java.io.OutputStream
                               com.amazonaws.services.lambda.runtime.Context]
                       void]]))

(def table-name "myapp-status")

(extend Item json/JSONWriter
  {:-write (fn [in ^PrintWriter out]
               (.print out (.toJSON in)))})

(defn- write-array [s ^PrintWriter out]
  (.print out \[)
  (loop [x s]
    (when (seq x)
      (let [fst (first x)
            nxt (next x)]
        (json/-write fst out)
        (when (seq nxt)
          (.print out \,)
          (recur nxt)))))
  (.print out \]))

(extend ScanCollection json/JSONWriter
  {:-write write-array})

(defn get-ts [item]
  (.getString item "ts"))

(defn sort-by-ts [scan-collection]
  (sort-by get-ts scan-collection))

(defn get-dynamo []
  (DynamoDB. (AmazonDynamoDBClient.)))

(defn -handle
  [in out ctx]
  (let [low (t/ago (t/hours 12))
        ts (f/unparse (f/formatters :date-hour-minute) low)
        dynamo (get-dynamo)
        table (.getTable dynamo table-name)
        items (.scan table "ts >= :ts" nil {":ts" ts})]
    (with-open [out-stream (io/writer out)]
      (.write out-stream
              (json/write-str {:data (sort-by-ts items)})))))

(defn -main
  [in out context]
    (-handle in out context))
