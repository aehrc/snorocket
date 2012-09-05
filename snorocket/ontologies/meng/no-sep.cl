;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: May 15, 2007
;; Description: a toy example showing a re-engineered extract of SNOMED CT dispensing with SEP-triplets. Taken from Fig.2 of paper "REplacing SEP-Triplets in SNOMED CT using Tractable Description Logic Operators."
;; Tweak&Try: Exclude all optional definitions for SEP-nodes and see that things still work properly. Try to modify slot properties of roles, such as, transitivity, right identity, and see what consequences will be missing after classification


;; =========================================================
;; Class definitions: Anatomy and Findings
;; =========================================================

(define-primitive-concept Finger 
    (and BodyPart
	 (some proper-part-of Hand)))

(define-primitive-concept Hand 
    (and BodyPart
	 (some proper-part-of UpperLimb)))

(define-primitive-concept UpperLimb BodyPart)



(define-concept Amputation-of-Finger
    (and Amputation
	 (some has-exact-location Finger)))

(define-concept Amputation-of-Hand
    (and Amputation
	 (some has-exact-location Hand)))

(define-concept Amputation-of-UpperLimb
    (and Amputation
	 (some has-exact-location UpperLimb)))



(define-concept Injury-to-Finger
    (and Injury
	 (some has-location Finger)))

(define-concept Injury-to-Hand
    (and Injury
	 (some has-location Hand)))

(define-concept Injury-to-UpperLimb
    (and Injury
	 (some has-location UpperLimb)))

;; =========================================================
;; Declarations and restrictions on relevant properties
;; =========================================================

(define-primitive-role part-of :transitive t :reflexive t)

(define-primitive-role proper-part-of :transitive t :parent part-of)

(define-primitive-role has-location :right-identity proper-part-of)

(define-primitive-role has-exact-location :parent has-location)


;; =========================================================
;; Optional class definitions to stay backward compatible with SEP
;; =========================================================

(define-concept s-Finger (some part-of Finger))
(define-concept p-Finger (some proper-part-of Finger))

(define-concept s-Hand (some part-of Hand))
(define-concept p-Hand (some proper-part-of Hand))

(define-concept s-UpperLimb (some part-of UpperLimb))
(define-concept p-UpperLimb (some proper-part-of UpperLimb))