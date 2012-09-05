;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: March 5, 2007
;; Description: a toy example illustrating the use of reflexivity and transitivity assertions on roles. In particular, part-of MUST be declared transitive so that the classification system can take care of transitivity reasoning. Part and whole of some entity could be referred to by the new role, part-whole-of, which is designed to replace the needs for S-nodes in the SEP-triplet modelling technique.
;; Tweak&Try: Compare the inferred concept hierarchy with that without the reflexitivity assertion.


(define-primitive-role part-of :transitive t
		               :parent part-whole-of)
;;(define-primitive-role part-whole-of :reflexive t)

(define-primitive-concept Appendix (some part-of Large-Intestine))
(define-primitive-concept Large-Intestine (some part-of Intestine))
(define-primitive-concept Small-Intestine (some part-of Intestine))
(define-primitive-concept Intestine (some part-of Gastrointestinal-Tract))
(define-primitive-concept Stomach (some part-of Gastrointestinal-Tract))

(define-concept |All Anatomical Parts of Gastrointestinal Tract|
    (some part-of Gastrointestinal-Tract))

(define-concept |All Anatomical Parts or Whole of Gastrointestinal Tract|
    (some part-whole-of Gastrointestinal-Tract))