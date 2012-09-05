;; Modeller: Boontawee Suntisrivaraporn, meng@tcs.inf.tu-dresden.de
;; Date: June 3, 2007
;; Description: a toy example ontology designed to demonstrate (most if not all) expressive means provided by the CEL reasoner. The ontology defines basic notions revolving research groups at a university and also contains an extensional component (ABox). Individuals are taken from the research group of Prof. Franz Baader at TU Dresden.
;; Disclaimer: Assertions on individuals in this ontology may not reflect the factual information about the respective persons.
;; Tweak&Try: The ontology at hand is inconsisntent, despite the inexprssive underpinning description logic. There are several interesting things one could tweak to make it consistent...


;; =========================================================
;; Role declarations and constraints
;; =========================================================

(define-primitive-role leads :domain Professor :range Project)
;; Can non-students be supervised?
(define-primitive-role supervises :range Student)
(define-primitive-role gives :range Lecture)
(define-primitive-role takes :range Lecture)


;; =========================================================
;; Concepts declarations, axioms and constraints
;; =========================================================

(define-primitive-concept Professorship Position)
(define-primitive-concept ResearchFellowship Position)
(define-primitive-concept ResearchAssistantship Position)
(define-primitive-concept WissHilfkraft ResearchAssistantship)
(define-primitive-concept StudHilfkraft ResearchAssistantship)

(equivalent StudHilfkraft Internship)

(disjoint Professorship ResearchFellowship WissHilfkraft StudHilfkraft)

(define-concept Professor
    (and Person
	 (some position Professorship)
	 (some leads Project)
	 (some gives Lecture)
	 (some supervises GradStudent)))

(define-concept ResearchFellow
    (and Person
	 (some position ResearchFellowship)
	 (some project Project)))

(define-concept ResearchAssistant
    (and Person
	 (some position ResearchAssistantship)))

(define-concept StudentAssistant
    (and Student
	 (some position ResearchAssistantship)))

(define-concept GradStudent
    (and Student
	 (some enrolled-in GradSchool)))

(define-concept UngradStudent
    (and Student
	 (some enrolled-in UngradSchool)
	 (some takes Lecture)))

(define-concept Student
    (and Person
	 (some student-id ID)
	 (some registered-at University)))

(disjoint Professor ResearchFellow Student)

(disjoint GradStudent UngradStudent)

;; =========================================================
;; ABox component featured by LAT staff at TU Dresden. Thanks!
;; =========================================================

(instance |Franz| Professor)
(instance |Carsten| ResearchFellow)
(instance |Barbara| ResearchFellow)
(instance |Jan| ResearchFellow)
(instance |Liu| ResearchFellow)
(instance |Anni| ResearchFellow)
(instance |Baris| ResearchFellow)
(instance |Meng| GradStudent)
(instance |Maja| GradStudent)
(instance |Felix| GradStudent)
(instance |Adila| UngradStudent)
(instance |Huang| UngradStudent)
(instance |Research Position on Polytime DL| ResearchFellowship)
(instance |TUD| University)
(instance |CL| UngradSchool)
(instance |LAT HIWI Pos| Internship)


(related |Franz| |DL Lecture| gives)
(related |Franz| |Polytime DL| leads)
(related |Franz| |TONES| leads)
(related |Franz| |DL + Action| leads)
(related |Franz| |Jan| supervises)
(related |Franz| |Meng| supervises)
(related |Franz| |Liu| supervises)
(related |Franz| |Baris| supervises)
(related |Franz| |Maja| supervises)
(related |Franz| |Felix| supervises)
(related |Franz| |Julia| supervises)

(related |Carsten| |DL Tutorial| gives)
(related |Carsten| |TONES| project)
(related |Carsten| |Polytime DL| project)
(related |Carsten| |DL + Action| project)
(related |Carsten| |Meng| supervises)
(related |Carsten| |Liu| supervises)
(related |Carsten| |Maja| supervises)
(related |Carsten| |Julia| supervises)
(related |Carsten| |Adila| supervises)
(related |Carsten| |Yusri| supervises)

(related |Meng| |Polytime DL| project)
(related |Meng| |Research Position on Polytime DL| position)
(related |Meng| |Huang| supervises)

(related |Yusri| |LAT HIWI Pos| position)

(related |Julia| |CL| enrolled-in)
(related |Julia| |DL Lecture| takes)
(related |Julia| |DL Tutorial| takes)