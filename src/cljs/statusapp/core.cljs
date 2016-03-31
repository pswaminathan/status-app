(ns statusapp.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.chartist]
            [ajax.core :refer [GET]]))

(enable-console-print!)

(defonce app-state (atom {:text "API Status"}))

(def repsonse-time-class-name "response-time-chart")
(def uptime-class-name "uptime-chart")
(def api-endpoint "https://xer76gpr57.execute-api.us-east-1.amazonaws.com/prod/statuses")
(def api-key "i3x6x4eblN3HCrdEkJWeE7NsyH5WAego5gV5WX8t")

(defn on-the-hour [time-string]
  (let [minute (subs time-string 4 5)]
    (if (or (= minute "5") (= minute "0"))
      time-string)))

(defn get-from-sequence
  [keywd lst]
  (for [item lst]
    (item keywd)))

(defn strip-date
  [labels]
  (map (fn [i] (subs i 11))
    labels))

(defn chartist-line
  [resp y-axis-series target-class]
  (let [data (clj->js {:labels (strip-date (get-from-sequence "ts" resp))
                       :series [(get-from-sequence y-axis-series resp)]})]
    (println data)
    (js/Chartist.Line. target-class
                       data
                       (clj->js {:low 0 :showPoint false
                                 :axisX {:labelInterpolationFnc on-the-hour}}))))

(defn get-data
   [url y-axis-series target-class]
   (GET url
        {:handler (fn [resp]
                      (chartist-line (resp "data") y-axis-series target-class))
         :headers {:x-api-key api-key}}))

(defn chart-div [target]
  [:div {:class (str target " ct-perfect-third")}])

(def response-time-component
  (with-meta (fn [] (chart-div repsonse-time-class-name))
    {:component-did-mount (fn [_]
                            (get-data api-endpoint
                                      "execution_time"
                                      (str "." repsonse-time-class-name)))}))

(def uptime-component
  (with-meta (fn [] (chart-div uptime-class-name))
    {:component-did-mount (fn [_]
                            (get-data api-endpoint
                                      "healthy"
                                      (str "." uptime-class-name)))}))

(defn app []
  [:div.container
    [:h1 (:text @app-state)]
    [:h3 "Uptime"]
    [uptime-component]
    [:h3 "Response Time"]
    [response-time-component]])

(reagent/render [app] (js/document.getElementById "app"))
