(ns statusapp.status-job
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            [clj-http.client :as client]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [clj-uuid :as uuid])
  (:import [com.amazonaws.services.dynamodbv2 AmazonDynamoDBClient]
           [com.amazonaws.services.dynamodbv2.document DynamoDB Item])
  (:gen-class
   :methods [^:static [handle [java.io.InputStream
                               com.amazonaws.services.lambda.runtime.Context]
                       void]]))

(def table-name "myapp-status")
(def api-key "i3x6x4eblN3HCrdEkJWeE7NsyH5WAego5gV5WX8t")
(def api-endpoint "https://xer76gpr57.execute-api.us-east-1.amazonaws.com/prod/healthcheck")

(defn get-data [url]
  (println (str "getting " url))
  (client/get url
    {:headers {:x-api-key api-key}
     :as :json}))

(defn convert-for-dynamodb
  [maps]
  (-> (Item.)
      (.withPrimaryKey "event_id" (maps "event_id") "ts" (maps "ts"))
      (.withInt "healthy" (maps "healthy"))
      (.withInt "execution_time" (maps "execution_time"))))

(defn send-to-dynamodb
  [table healthy etime start]
    (let [data {"event_id" (str (uuid/v1))
                "ts" (f/unparse (f/formatters :date-hour-minute)
                                start)
                "healthy" (if healthy 1 0)
                "execution_time" etime}]
      (println data)
      (.putItem table (convert-for-dynamodb data))))

(defn -main []
  (println "starting!")
  (let [start (t/now)
        response (get-data api-endpoint)
        healthy (:healthy (:body response))
        exec-time (:request-time response)
        conn (AmazonDynamoDBClient.)
        table (.getTable (DynamoDB. conn) table-name)]
    (send-to-dynamodb table
                      healthy
                      exec-time
                      start)))

(defn -handle [in ctx]
  (-main))
