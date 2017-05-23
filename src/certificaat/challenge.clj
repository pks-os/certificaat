(ns certificaat.challenge
  (:refer-clojure :exclude [find])
  (:require [clojure.core.async :as a :refer [<! <!! >!! chan thread go-loop]]
            [clojure.tools.logging :as log]
            [environ.core :refer [env]])
  (:import [org.shredzone.acme4j.challenge Challenge Http01Challenge Dns01Challenge TlsSni01Challenge TlsSni02Challenge OutOfBand01Challenge]
           [org.shredzone.acme4j Status]
           [org.shredzone.acme4j.exception AcmeRetryAfterException]))

(defn http [auth domain]
  (let [challenge (.findChallenge auth Http01Challenge/TYPE)]
    (log/info "Please create a file in your web server's base directory.")
    (log/info "It must be reachable at:" (str "http://" domain  "/.well-known/acme-challenge/" (.getToken challenge)))
    (log/info "File name:" (.getToken challenge))
    (log/info "Authorization:" (.getAuthorization challenge))
    (log/info "The file must not contain any leading or trailing whitespaces or line breaks!")
    challenge))

(defn dns [auth domain]
  (let [challenge (.findChallenge auth Dns01Challenge/TYPE)]
    (log/info "Please create a TXT record:")
    (log/info (str "_acme-challenge." domain " IN TXT " (.getDigest challenge)))
    challenge))

(defn find [auth challenge-type domain]
  (case challenge-type
    Dns01Challenge/TYPE (dns auth domain)
    Http01Challenge/TYPE (http auth domain)))

(defn find2 [auth challenges]
  (.findCombination auth (into-array String challenges)))

(defn accept [challenge]
  (.trigger challenge)
  (let [c (chan)]
    (a/thread (loop [y 1
                     ms nil]
                (<!! (a/timeout (or ms 5000)))
                (log/info "Retrieving status, attempt" y)
                (let [status (log/spyf "status %s" (.getStatus challenge))]
                  (if (or (= status Status/VALID) (= status Status/INVALID) (> y 10))
                    status
                    (recur (inc y) (try
                                     (.update challenge)
                                     (catch AcmeRetryAfterException e
                                       (log/error (.getMessage e))
                                       (.getRetryAfter e))))))))))

#_ (defn restore []
     (Challenge/bind session (.getLocation challenge)))
