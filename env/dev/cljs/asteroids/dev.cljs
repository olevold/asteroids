(ns ^:figwheel-no-load asteroids.dev
  (:require
    [asteroids.core :as core]
    [devtools.core :as devtools]))

(devtools/install!)

(enable-console-print!)

(core/init!)
