(set-env!
 :source-paths   #{"src"}
 :resource-paths #{"resources"}
 :dependencies '[[org.clojure/clojure "1.9.0-alpha16"]
                 [org.clojure/core.async "0.3.442"]
                 [org.shredzone.acme4j/acme4j-client "0.10"]
                 [org.shredzone.acme4j/acme4j-utils "0.10"]
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.2.3"]
                 [environ "1.1.0"]
                 [boot-environ "1.1.0"]])

(require '[environ.boot :refer [environ]])

(deftask dev
  "Run a restartable system in the Repl"
  []
  (comp
   (environ :env {:certificaat-config-dir (str (System/getProperty "user.home") "/.config/certificaat/")
                  :certificaat-keypair-filename "keypair.pem"
                  :certificaat-domain-keypair-filename "teamsocial.pem"
                  :certificaat-acme-server-uri "https://acme-staging.api.letsencrypt.org/directory"
                  :certificaat-acme-uri "acme://letsencrypt.org/staging"
                  :certificaat-acme-contact "mailto:daniel.szmulewicz@gmail.com"
                  :certificaat-domain "teamsocial.me"
                  :certificaat-organization "sapiens sapiens"
                  :certificaat-challenge-type "dns-01"})
   (watch :verbose true)
   (notify :visual true)
   (repl :server true)))
