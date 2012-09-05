;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: Jun 4, 2007
;; Description: featuring a left-identity rule.
;; Tweak&Try: disable the transitivity and/or left-identity axioms, and see what consequences will be missing.

(define-primitive-role has-part :transitive t)

;; "has-part" is defined as a left-identity role of "has-color"
;; aximatization variant: 
;; (define-primitive-role has-color :left-identity has-part)
(role-inclusion (compose has-part has-color) has-color)


(define-primitive-concept green color)
(define-primitive-concept brown color)
(define-primitive-concept black color)
(define-primitive-concept yellow color)
(define-primitive-concept blue color)
(define-primitive-concept red color)

(define-primitive-concept forest
    (some has-part tree))
(define-primitive-concept tree 
    (and (some has-part leaf)
	 (some has-part trunk)))
(define-primitive-concept leaf 
    (some has-color green))
(define-primitive-concept trunk 
    (some has-color brown))

(implies (some has-color green)
	 green-thing)
(implies (some has-color brown)
	 brown-thing)