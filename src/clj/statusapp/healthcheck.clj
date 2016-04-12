(ns statusapp.healthcheck
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json])
  (:gen-class
   :methods [^:static [handle [java.io.InputStream
                               java.io.OutputStream
                               com.amazonaws.services.lambda.runtime.Context]
                       void]]))

(defn server-up? 
  "Not a very stable server.
  Stub that returns a random 5% chance of server being down"
  []
  (>= (rand) 0.05))

(defn -handle 
  [in out context]
  (with-open [out-stream (io/writer out)]
    (let [out-json (json/write-str {:healthy (server-up?)})]
      (.write out-stream out-json))))

(defn -main
  [in out context]
    (-handle in out context))
