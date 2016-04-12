(ns statusapp.core
  (:require [reagent.core :as reagent :refer [atom]]
            [cljsjs.chartist]
            [ajax.core :as ajax]))

(enable-console-print!)

(defonce app-state (atom {:text "API Status"}))

(def response-time-class-name "response-time-chart")
(def uptime-class-name "uptime-chart")
(def api-endpoint "https://xer76gpr57.execute-api.us-east-1.amazonaws.com/prod/statuses")
(def api-key "i3x6x4eblN3HCrdEkJWeE7NsyH5WAego5gV5WX8t")

(defn on-the-five
  "Return input string if its last character is on the 10"
  [time-string]
  (let [minute (last time-string)
        ten (last (butlast time-string))]
    ;(if (and (= ten \0) (= minute \0))
    ;  time-string)))
    (if (= minute \0)
      time-string)))

(defn get-from-sequence
  "Get values by key from a list of hash-maps:
  (get-from-sequence :a [{:a 1 :b 2} {:a 2 :b 2}])
  ;;=> (1 2)"
  [lst key-name]
  (for [item lst]
    (item key-name)))

(defn strip-date
  "Strip date from a list of datetimes:
  (strip-date [\"2015-01-01T00:00\" \"2015-01-01T00:05\"
  ;;=> (\"00:00\" \"00:05\")"
  [datetimes]
  (map (fn [label]
         (let [t-index (.indexOf label \T)]
           (subs label (+ 1 t-index))))
       datetimes))

(defn attach-chartist-data
  "Attach AJAX data to chart"
  [target-class data y-axis-series]
  (let [labels (-> data
                   (get-from-sequence "ts")
                   strip-date)
        series (get-from-sequence data y-axis-series)
        ; wrap series in a vector because Chartist expects a series of series
        data (clj->js {:labels labels :series [series]})]
    (js/Chartist.Line. target-class
                       data
                       (clj->js {:low 0
                                 :showPoint false
                                 :axisX {:labelInterpolationFnc on-the-five}}))))

(defn get-data
  [url y-axis-series target-class]
  (ajax/GET url
           {:handler (fn [resp]
                         (attach-chartist-data target-class
                                               (resp "data")
                                               y-axis-series))
            :headers {:x-api-key api-key}}))

(defn chart-div [target]
  [:div {:class (str target " ct-perfect-third")}])

(defn chartist-component
  [series class-name]
  (with-meta (fn [] (chart-div class-name))
             {:component-did-mount
              (fn [_] (get-data api-endpoint
                                series
                                (str "." class-name)))}))

(defn app []
  [:div.container
   [:h1 (:text @app-state)]
   [:h3 "Uptime"]
   [(chartist-component "healthy" uptime-class-name)]
   [:h3 "Response Time"]
   [(chartist-component "execution_time" response-time-class-name)]])

(reagent/render [app] (js/document.getElementById "app"))
