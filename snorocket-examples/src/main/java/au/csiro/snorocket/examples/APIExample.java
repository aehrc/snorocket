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
        
        // The factory returns IConcepts - in this case the actual type is
        // INamedConcept<String>
        Concept endocardium = Factory.createNamedConcept("Endocardium");
        Concept tissue = Factory.createNamedConcept("Tissue");
        Concept heartWall = Factory.createNamedConcept("HeartWall");
        Concept heartValve = Factory.createNamedConcept("HeartValve");
        Concept bodyWall = Factory.createNamedConcept("BodyWall");
        Concept heart = Factory.createNamedConcept("Heart");
        Concept bodyValve = Factory.createNamedConcept("BodyValve");
        Concept endocarditis = Factory.createNamedConcept("Endocarditis");
        Concept inflammation = Factory.createNamedConcept("Inflammation");
        Concept disease = Factory.createNamedConcept("Disease");
        Concept heartDisease = Factory.createNamedConcept("HeartDisease");
        Concept criticalDisease = Factory.createNamedConcept("CriticalDisease");
        
        // The factory can also be used to create roles
        Role actsOn = Factory.createNamedRole("acts-on");
        Role partOf = Factory.createNamedRole("part-of");
        Role contIn = Factory.createNamedRole("cont-in");
        Role hasLoc = Factory.createNamedRole("has-loc");
        
        // Finally, the factory can be used to create axioms
        Concept lhs = endocardium;
        Concept rhs = Factory.createConjunction(
                tissue,
                Factory.createExistential((NamedRole) contIn, heartWall),
                Factory.createExistential((NamedRole) contIn, heartValve)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        lhs = heartWall;
        rhs = Factory.createConjunction(
                bodyWall,
                Factory.createExistential((NamedRole) partOf, heart)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        lhs = heartValve;
        rhs = Factory.createConjunction(
                bodyValve,
                Factory.createExistential((NamedRole) partOf, heart)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        lhs = endocarditis;
        rhs = Factory.createConjunction(
                inflammation,
                Factory.createExistential((NamedRole) hasLoc, endocardium)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));

        lhs = inflammation;
        rhs = Factory.createConjunction(
                disease,
                Factory.createExistential(actsOn, tissue)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        lhs = Factory.createConjunction(
                heartDisease,
                Factory.createExistential(hasLoc, heartValve)
        );
        rhs = criticalDisease;
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        // There is no direct support in the API to create an equivalence axiom
        // so it has to be created using two concept inclusion axioms
        lhs = heartDisease;
        rhs = Factory.createConjunction(
                disease,
                Factory.createExistential(hasLoc, heart)
        );
        baseAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        baseAxioms.add(Factory.createConceptInclusion(rhs, lhs));
        
        Role[] rlhs = new Role[]{partOf, partOf};
        Role rrhs = partOf;
        baseAxioms.add(Factory.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new Role[]{partOf};
        rrhs = contIn;
        baseAxioms.add(Factory.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new Role[]{hasLoc, contIn};
        rrhs = hasLoc;
        baseAxioms.add(Factory.createRoleInclusion(rlhs, rrhs));
        
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
        
        Concept heartInflammation = Factory.createNamedConcept("HeartInflammation");
        
        lhs = heartInflammation;
        rhs = inflammation;
        additionalAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
        lhs = endocarditis;
        rhs = Factory.createConjunction(
                heartInflammation,
                Factory.createExistential(hasLoc, endocardium)
        );
        additionalAxioms.add(Factory.createConceptInclusion(lhs, rhs));
        
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
