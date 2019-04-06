(ns inkscape-animation-assistant.core
    (:require
      [reagent.core :as r]
      [inkscape-animation-assistant.animation :refer [animate! flip-layers layers-get-all]]))

(def initial-state
  {:playing false
   :svg nil
   :last nil
   :file nil})

(defn read-file [file cb]
  (let [reader (js/FileReader.)]
    (aset reader "onload" #(cb (.. % -target -result)))
    (.readAsText reader file "utf-8")))

(defn get-file-time [file]
  (.getTime (.-lastModifiedDate file)))

(defn filewatcher [state]
  (let [file (@state :file)
        last-mod (@state :last)]
    (when (and file (not= (.getTime (.-lastModifiedDate file)) (.getTime last-mod)))
      (read-file file (fn [content]
                        (swap! state assoc :svg content))))))

(defn file-selected! [state ev]
  (let [input (.. ev -target)
        file (and (.-files input) (aget (.-files input) 0))]
    (read-file file (fn [content]
                      (when (= (.indexOf content "viewBox") -1)
                        (js/alert "Warning: SVG has no viewBox.\nScale will be fixed at original size."))
                      (swap! state assoc :file file :last (.-lastModifiedDate file) :svg content)))))

(defn play! [state ev]
  (let [timer (js/setTimeout (partial animate! #(@state :playing) 0) 1)]
    (swap! state #(-> %
                      (assoc :timer timer)
                      (update-in [:playing] not)))))

(defn pause! [state ev]
  (swap! state update-in [:playing] not))

;; -------------------------
;; Views

(defn component-folder-icon []
  [:svg {:viewBox "0 -256 1950 1950" :width 256 :height 256}
   [:g {:transform "matrix(1,0,0,-1,30.372881,1443.4237)"}
    [:path {:fill "#888" :d "m1781,605q0,35-53,35h-1088q-40,0-85.5-21.5t-71.5-52.5l-294-363q-18-24-18-40,0-35,53-35h1088q40,0,86,22t71,53l294,363q18,22,18,39zm-1141,163h768v160q0,40-28,68t-68,28h-576q-40,0-68,28t-28,68v64q0,40-28,68t-68,28h-320q-40,0-68-28t-28-68v-853l256,315q44,53,116,87.5t140,34.5zm1269-163q0-62-46-120l-295-363q-43-53-116-87.5t-140-34.5h-1088q-92,0-158,66t-66,158v960q0,92,66,158t158,66h320q92,0,158-66t66-158v-32h544q92,0,158-66t66-158v-160h192q54,0,99-24.5t67-70.5q15-32,15-68z"}]]])

(defn component-play-pause [state]
  [:svg#play-pause.icon {:viewBox "0 0 1792 1792"
         :on-click (partial (if (@state :playing) pause! play!) state)}
   [:path {:d
           (if (@state :playing)
             "M1216 1184v-576q0-14-9-23t-23-9h-576q-14 0-23 9t-9 23v576q0 14 9 23t23 9h576q14 0 23-9t9-23zm448-288q0 209-103 385.5t-279.5 279.5-385.5 103-385.5-103-279.5-279.5-103-385.5 103-385.5 279.5-279.5 385.5-103 385.5 103 279.5 279.5 103 385.5z"
             "M896 128q209 0 385.5 103t279.5 279.5 103 385.5-103 385.5-279.5 279.5-385.5 103-385.5-103-279.5-279.5-103-385.5 103-385.5 279.5-279.5 385.5-103zm384 823q32-18 32-55t-32-55l-544-320q-31-19-64-1-32 19-32 56v640q0 37 32 56 16 8 32 8 17 0 32-9z")}]])

(defn component-close [state]
  [:svg#close.icon {:viewBox "0 0 1792 1792"
                    :on-click #(reset! state initial-state)}
   [:path {:d "M1277 1122q0-26-19-45l-181-181 181-181q19-19 19-45 0-27-19-46l-90-90q-19-19-46-19-26 0-45 19l-181 181-181-181q-19-19-45-19-27 0-46 19l-90 90q-19 19-19 46 0 26 19 45l181 181-181 181q-19 19-19 45 0 27 19 46l90 90q19 19 46 19 26 0 45-19l181-181 181 181q19 19 45 19 27 0 46-19l90-90q19-19 19-46zm387-226q0 209-103 385.5t-279.5 279.5-385.5 103-385.5-103-279.5-279.5-103-385.5 103-385.5 279.5-279.5 385.5-103 385.5 103 279.5 279.5 103 385.5z"}]])

(defn component-app [state]
  (if (@state :file)
    [:div#container
     [:div#animation {:dangerouslySetInnerHTML {:__html (@state :svg)}
                      :ref #(when % (flip-layers (fn [i l] (or (= i 0))) (layers-get-all)))}]
     [:div#interface
      [component-close state]
      [component-play-pause state]]]
    [:div#choosefile
     [:div
      [:label 
       [:input#fileinput {:type "file"
                          :accept ".svg"
                          :on-change (partial file-selected! state)}]
       [component-folder-icon]
       [:p "Choose SVG"]]]]))

;; -------------------------
;; Initialize app

(defonce state (r/atom initial-state))

(defn mount-root []
  (r/render [component-app state] (.getElementById js/document "app")))

(defn init! []
  (js/setInterval (partial #'filewatcher state) 250)
  (mount-root))