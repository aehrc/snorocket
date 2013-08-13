/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.examples;

import java.util.HashSet;
import java.util.Set;

import au.csiro.ontology.Factory;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.Node;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.NamedRole;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.SnorocketReasoner;

/**
 * This class shows how to use Snorocket's classification API.
 * 
 * @author Alejandro Metke
 *
 */
public class APIExample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        
        System.out.println("Running Snorocket API Example");
        
        // Create the reasoner instance - the parametrised type refers to the
        // type of the external identifiers
        IReasoner reasoner = new SnorocketReasoner();
        
        // This set will contain the base axioms
        Set<Axiom> baseAxioms = new HashSet<Axiom>();
        
        // The factory can be used to create axioms using the default 
        // implementing classes
        Factory f = new Factory();
        
        // The factory returns IConcepts - in this case the actual type is
        // INamedConcept<String>
        Concept endocardium = f.createNamedConcept("Endocardium");
        Concept tissue = f.createNamedConcept("Tissue");
        Concept heartWall = f.createNamedConcept("HeartWall");
        Concept heartValve = f.createNamedConcept("HeartValve");
        Concept bodyWall = f.createNamedConcept("BodyWall");
        Concept heart = f.createNamedConcept("Heart");
        Concept bodyValve = f.createNamedConcept("BodyValve");
        Concept endocarditis = f.createNamedConcept("Endocarditis");
        Concept inflammation = f.createNamedConcept("Inflammation");
        Concept disease = f.createNamedConcept("Disease");
        Concept heartDisease = f.createNamedConcept("HeartDisease");
        Concept criticalDisease = f.createNamedConcept("CriticalDisease");
        
        // The factory can also be used to create roles
        Role actsOn = f.createNamedRole("acts-on");
        Role partOf = f.createNamedRole("part-of");
        Role contIn = f.createNamedRole("cont-in");
        Role hasLoc = f.createNamedRole("has-loc");
        
        // Finally, the factory can be used to create axioms
        Concept lhs = endocardium;
        Concept rhs = f.createConjunction(
                tissue,
                f.createExistential((NamedRole) contIn, heartWall),
                f.createExistential((NamedRole) contIn, heartValve)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = heartWall;
        rhs = f.createConjunction(
                bodyWall,
                f.createExistential((NamedRole) partOf, heart)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = heartValve;
        rhs = f.createConjunction(
                bodyValve,
                f.createExistential((NamedRole) partOf, heart)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = endocarditis;
        rhs = f.createConjunction(
                inflammation,
                f.createExistential((NamedRole) hasLoc, endocardium)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));

        lhs = inflammation;
        rhs = f.createConjunction(
                disease,
                f.createExistential(actsOn, tissue)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = f.createConjunction(
                heartDisease,
                f.createExistential(hasLoc, heartValve)
        );
        rhs = criticalDisease;
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        // There is no direct support in the API to create an equivalence axiom
        // so it has to be created using two concept inclusion axioms
        lhs = heartDisease;
        rhs = f.createConjunction(
                disease,
                f.createExistential(hasLoc, heart)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        baseAxioms.add(f.createConceptInclusion(rhs, lhs));
        
        Role[] rlhs = new Role[]{partOf, partOf};
        Role rrhs = partOf;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new Role[]{partOf};
        rrhs = contIn;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new Role[]{hasLoc, contIn};
        rrhs = hasLoc;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        // The first time the classify method is called it runs a full 
        // classification
        reasoner.loadAxioms(baseAxioms);
        reasoner.classify();
        
        // If classification worked properly then Endocarditis should be 
        // classified not only as an Inflammation but also as a HeartDisease 
        // and a CriticalDisease
        Ontology t = reasoner.getClassifiedOntology();
        
        // We use the same id that was used to create the concept to look for
        // the corresponding node in the taxonomy
        Node endocarditisNode = t.getNode("Endocarditis");
        System.out.println("Node for endocarditis:\n  "+
                endocarditisNode.getEquivalentConcepts());
        
        
        // We can now print the equivalent concepts in the node and the parent
        // nodes
        Set<Node> parentNodes = endocarditisNode.getParents();
        System.out.println("Parents of endocarditis:");
        for(Node parentNode : parentNodes) {
            System.out.println("  "+parentNode.getEquivalentConcepts());
        }
        
        // We can now add more axioms to the ontology and re-run the 
        // classification
        Set<Axiom> additionalAxioms = new HashSet<Axiom>();
        
        Concept heartInflammation = f.createNamedConcept("HeartInflammation");
        
        lhs = heartInflammation;
        rhs = inflammation;
        additionalAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = endocarditis;
        rhs = f.createConjunction(
                heartInflammation,
                f.createExistential(hasLoc, endocardium)
        );
        additionalAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        // Subsequent invocations will trigger an incremental classification
        System.out.println("Running incremental classification:");
        reasoner.loadAxioms(additionalAxioms);
        reasoner.classify();
        
        // Now Endocarditis should be a HeartInflammation instead of an 
        // Inflammation
        t = reasoner.getClassifiedOntology();
        endocarditisNode = t.getNode("Endocarditis");
        System.out.println("Node for endocarditis:\n  "+
                endocarditisNode.getEquivalentConcepts());

        parentNodes = endocarditisNode.getParents();
        System.out.println("Parents of endocarditis:");
        for(Node parentNode : parentNodes) {
            System.out.println("  "+parentNode.getEquivalentConcepts());
        }
    }

}
