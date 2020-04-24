(ns cljdoc-analyzer.test-helper
  (:require [clojure.test :as t]
            [clojure.java.io :as io]
            [clojure.string :as string]
            [cljdoc-analyzer.analysis-edn :as analysis-edn]))

(defn edn-filename [prefix project version]
  (let [project (if (string/index-of project "/")
                  project
                  (str project "/" project))]
    (str prefix "/" project "/" version "/cljdoc.edn")))

(defn- filter-namespace
  "Filter undesired namespace in a analysis map.
  Useful when arguments of function are generated by gensyms: no reproducible analysis"
  [analysis-map language remove-ns?]
  (update-in analysis-map
             [:analysis language]
             ;; % is the list of namespaces
             #(remove (fn [ns] (remove-ns? (str (:name ns)))) %)))

(defn verify-analysis-result [ project version edn-out-filename {:keys [exit out err]} ]
  (println "analysis exit code:" exit)
  (println "analysis stdout:")
  (println out)
  (println "analysis stderr:")
  (println err)
  (t/is (zero? exit))
  (let [expected-f (io/resource (edn-filename "expected-edn" project version))]
    (when-not expected-f
      (throw (ex-info "expected edn file missing"
                      {:project project
                       :version version
                       :path (edn-filename "expected-edn" project version)})))
    (let [expected-analysis (analysis-edn/read expected-f)
          actual-analysis (analysis-edn/read edn-out-filename)]
      (cond
        ;; For specter package, filter the "com.rpl.specter.impl" namespace
        (and (= project "com.rpl/specter")
             (= version "1.1.3"))
        (t/is (= (filter-namespace expected-analysis "cljs" #{"com.rpl.specter.impl"})
                 (filter-namespace actual-analysis "cljs" #{"com.rpl.specter.impl"})))

        :else
        (t/is (= expected-analysis
                 actual-analysis))
        ))))
