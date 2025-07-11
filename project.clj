(defproject my-closet "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [midje "1.9.10"]
                 [seancorfield/next.jdbc "1.2.659"]
                 [mysql/mysql-connector-java "8.0.26"]
                 [ring/ring-core "1.13.0"]
                 [ring/ring-json "0.5.1"]
                 [ring/ring-jetty-adapter "1.8.2"]
                 [ring-cors "0.1.13"]
                 [metosin/reitit "0.8.0-alpha1"]
                 [metosin/muuntaja "0.6.11"]
                 [cheshire "5.13.0"]]
  :main ^:skip-aot my-closet.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}}
  :plugins [[lein-midje "3.2.1"]])
