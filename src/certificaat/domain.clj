(ns certificaat.domain
  (:require [clojure.spec.alpha :as s]
            [clojure.java.io :as io])
  (:import [java.net InetAddress]
           [java.net URI]))

(defn validate [spec val]
  (let [v (s/conform spec val)]
    (if (= v ::s/invalid)
      (throw (ex-info "Invalid options" (s/explain-data spec val)))
      v)))

(s/def ::path (s/and string? #(try (.exists (io/file %))
                                   (catch java.io.IOException e false))))
(s/def ::config-dir string?)
(s/def ::keypair-filename string?)
(s/def ::acme-uri (s/and string? #(.isAbsolute (URI. %))))
(s/def ::contact (s/and string? #(.isOpaque (URI. %))))
(s/def ::key-size #{1024 2048 4096})
(s/def ::key-type #{:rsa :ec})
(s/def ::domain (s/and string? #(try (.isReachable (InetAddress/getByName %) 5000)
                                     (catch java.io.IOException e false))))
(s/def ::san (s/coll-of ::domain :kind set?))
(s/def ::organisation string?)
(s/def ::challenge-type #{"http-01" "dns-01" "tls-alpn-01"})
(s/def ::challenge-url #(re-matches #"challenge\..*\.url" %))
(s/def ::authorization-url #(re-matches #"authorization\..*\.url" %))
(s/def ::order-url #(re-matches #"order.url" %))
(s/def ::account-url #(re-matches #"account.url" %))
(s/def ::certificate-url #(re-matches #"certificate.url" %))

(s/def ::hook #{:before-challenge :after-request})
(s/def ::hooks (s/* ::hook))
(s/def ::plugins (s/keys :opt-un [::dhparams ::webroot ::email ::copy-to-path]))
(s/def ::enabled boolean?)

(s/def ::webroot (s/keys :req-un [::path ::enabled]))
(s/def ::dhparams (s/keys :req-un [::enabled]))
(s/def ::email (s/keys :req-un [::enabled]))
(s/def ::copy-to-path (s/keys :req-un [::enabled ::path]))

(s/def ::cli-actions #{"init" "run" "config" "reset" "info" "cron"})
(s/def ::cli-options (s/keys :req-un [::config-dir ::domain]))
(s/def ::config (s/keys :req-un [::acme-uri ::domain ::challenge-type ::contact ::plugins]
                        :opt-un [::san]))

(defprotocol Certificaat
  (valid? [this])
  (pending? [this])
  (invalid? [this])
  (deactivated? [this])
  (expired? [this])
  (revoked? [this])
  (ready? [this])
  (processing? [this])
  (marshal [this path]))

(def realms (-> (make-hierarchy)
                (derive :config-dir ::program)
                (derive :keypair-filename ::account)
                (derive :key-size ::account)
                (derive :key-type ::account)
                (derive :contact ::domain)
                (derive :acme-uri ::domain)
                (derive :domain ::domain)
                (derive :san ::domain)
                (derive :organisation ::domain)
                (derive :challenge-type ::domain)
                (derive :plugins ::domain)))
