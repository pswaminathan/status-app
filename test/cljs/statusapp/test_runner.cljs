(ns statusapp.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [statusapp.core-test]))

(enable-console-print!)

(doo-tests 'statusapp.core-test)
