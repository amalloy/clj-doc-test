(ns doc-test.core
  (:use clojure.test)
  (:use [clojure.contrib.str-utils :only (re-split)]))

(defn- read-expr-pair
  "Read two expressions from expr-string and return a tuple of them.

  => (read-expr-pair \"(+ 1 2) 3\")
  [(+ 1 2) 3]"
  [expr-string]
  (with-open [sreader (new java.io.StringReader expr-string)
              pbreader (new java.io.PushbackReader sreader)]
    [(read pbreader) (read pbreader)]))

(defn- find-expression-strings
  "Finds expressions that they belong in a REPL. Namely, the => arrow
  beginning a line followed by 2 expressions.

  => (find-expression-strings (str \\newline \"=> ((adder 1) 2) 3\"))
  (\" ((adder 1) 2) 3\")"
  [docstr]
  (drop 1 (re-split #"\n\s*=>" docstr)))

(defn to-is
  "Converts a doc-test to forms using clojure.test/is.

  => (to-is (:doc (meta (var read-expr-pair))))
  ((clojure.test/is (clojure.core/= (read-expr-pair \"(+ 1 2) 3\")
                                    '[(+ 1 2) 3])))"
  [doc]
  (let [expr-strs (find-expression-strings doc)
        exprs (map read-expr-pair expr-strs)]
    (map (fn [[expr result]] `(is (= ~expr '~result)))
         exprs)))

(defmacro doc-test
  "Creates a (deftest ...) form based upon the examples in f's doc."
  [f]
  (let [f-meta (eval `(meta (var ~f)))
        is-statments (to-is (:doc f-meta))]
    (if (seq is-statments) ; only make a test if there are doc-tests
      `(deftest ~(gensym (str (:name f-meta) "__doc-test__"))
         ~@is-statments))))

; what the doc-test macro output is shooting for, approximately
;(deftest adder__doc-test__...
;  (is (= ((adder 1) 4) 3)))

(doc-test read-expr-pair)
(doc-test find-expression-strings)
(doc-test to-is)
