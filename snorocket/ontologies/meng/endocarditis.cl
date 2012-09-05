;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: April 5, 2006
;; Description: a toy medical ontology showing a typlical use of right-identity rule
;; Tweak&Try: disable right-identity to see that endocarditis is no longer classified as a heart disease

(define-primitive-role contained-in)
(define-primitive-role part-of :parent contained-in)
(define-primitive-role has-location :right-identity contained-in)

(define-primitive-concept Pericardium
    (and Tissue
	 (some contained-in Heart)))

(define-primitive-concept Endocardium
    (and Tissue
	 (some contained-in HeartValve)))

(define-primitive-concept HeartValve
    (and BodyValve
	 (some part-of Heart)))

(define-primitive-concept Endocarditis
    (and Inflammation
	 (some has-location Endocardium)))

(define-primitive-concept Pericarditis
    (and Inflammation
	 (some has-location Pericardium)))

(define-primitive-concept Inflammation
    (and Disease
	 (some acts-on Tissue)))

(define-concept Heartdisease
    (and Disease
	 (some has-location Heart)))

(implies (and Heartdisease
	      (some has-location HeartValve))
	 IsStateNeedsTreatment)