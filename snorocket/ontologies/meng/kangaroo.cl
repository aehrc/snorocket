;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: Jun 1, 2007
;; Description: a toy ontology example showing an inconsistency resulted from the interaction of range restriction on roles and disjointness on concepts.
;; Tweak&Try: disable both the domain and range restriction, and put these constraints locally in the definition of Parent

(define-primitive-role has-child :domain Human :range Human)


(disjoint Rational Irrational)

(define-primitive-concept Human (and Animal Rational))

(define-primitive-concept Beast (and Animal Irrational))

(define-concept Parent (some has-child top))
;;(define-concept Parent (and Human (some has-child Human)))

(define-primitive-concept Kangaroo Beast)

(define-primitive-concept KangarooInfant 
    (and Kangaroo
	 (some lives-in Pouch)))

(define-concept MaternityKangaroo 
    (and Kangaroo
	 (some has-body-part Pouch)
	 (some has-child KangarooInfant)))

