/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import java.util.HashSet;
import java.util.Set;

import au.csiro.ontology.Factory;
import au.csiro.ontology.Node;
import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.INamedRole;
import au.csiro.ontology.model.IRole;
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
        IReasoner<String> reasoner = new SnorocketReasoner<>();
        
        // This set will contain the base axioms
        Set<IAxiom> baseAxioms = new HashSet<>();
        
        // The factory can be used to create axioms using the default 
        // implementing classes
        Factory<String> f = new Factory<>();
        
        // The factory returns IConcepts - in this case the actual type is
        // INamedConcept<String>
        IConcept endocardium = f.createConcept("Endocardium");
        IConcept tissue = f.createConcept("Tissue");
        IConcept heartWall = f.createConcept("HeartWall");
        IConcept heartValve = f.createConcept("HeartValve");
        IConcept bodyWall = f.createConcept("BodyWall");
        IConcept heart = f.createConcept("Heart");
        IConcept bodyValve = f.createConcept("BodyValve");
        IConcept endocarditis = f.createConcept("Endocarditis");
        IConcept inflammation = f.createConcept("Inflammation");
        IConcept disease = f.createConcept("Disease");
        IConcept heartDisease = f.createConcept("HeartDisease");
        IConcept criticalDisease = f.createConcept("CriticalDisease");
        
        // The factory can also be used to create roles
        INamedRole<String> actsOn = f.createRole("acts-on");
        INamedRole<String> partOf = f.createRole("part-of");
        INamedRole<String> contIn = f.createRole("cont-in");
        INamedRole<String> hasLoc = f.createRole("has-loc");
        
        // Finally, the factory can be used to create axioms
        IConcept lhs = endocardium;
        IConcept rhs = f.createConjunction(
                tissue,
                f.createExistential(contIn, heartWall),
                f.createExistential(contIn, heartValve)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = heartWall;
        rhs = f.createConjunction(
                bodyWall,
                f.createExistential(partOf, heart)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = heartValve;
        rhs = f.createConjunction(
                bodyValve,
                f.createExistential(partOf, heart)
        );
        baseAxioms.add(f.createConceptInclusion(lhs, rhs));
        
        lhs = endocarditis;
        rhs = f.createConjunction(
                inflammation,
                f.createExistential(hasLoc, endocardium)
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
        
        IRole[] rlhs = new IRole[]{partOf, partOf};
        IRole rrhs = partOf;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new IRole[]{partOf};
        rrhs = contIn;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        rlhs = new IRole[]{hasLoc, contIn};
        rrhs = hasLoc;
        baseAxioms.add(f.createRoleInclusion(rlhs, rrhs));
        
        // The first time the classify method is called it runs a full 
        // classification
        reasoner.classify(baseAxioms);
        
        // If classification worked properly then Endocarditis should be 
        // classified not only as an Inflammation but also as a HeartDisease 
        // and a CriticalDisease
        Taxonomy<String> t = reasoner.getTaxonomy();
        
        // We use the same id that was used to create the concept to look for
        // the corresponding node in the taxonomy
        Node<String> endocarditisNode = t.getNode("Endocarditis");
        System.out.println("Node for endocarditis:\n  "+
                endocarditisNode.getEquivalentConcepts());
        
        
        // We can now print the equivalent concepts in the node and the parent
        // nodes
        Set<Node<String>> parentNodes = endocarditisNode.getParents();
        System.out.println("Parents of endocarditis:");
        for(Node<String> parentNode : parentNodes) {
            System.out.println("  "+parentNode.getEquivalentConcepts());
        }
        
        // We can now add more axioms to the ontology and re-run the 
        // classification
        Set<IAxiom> additionalAxioms = new HashSet<>();
        
        IConcept heartInflammation = f.createConcept("HeartInflammation");
        
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
        reasoner.classify(additionalAxioms);
        
        // Now Endocarditis should be a HeartInflammation instead of an 
        // Inflammation
        t = reasoner.getTaxonomy();
        endocarditisNode = t.getNode("Endocarditis");
        System.out.println("Node for endocarditis:\n  "+
                endocarditisNode.getEquivalentConcepts());

        parentNodes = endocarditisNode.getParents();
        System.out.println("Parents of endocarditis:");
        for(Node<String> parentNode : parentNodes) {
            System.out.println("  "+parentNode.getEquivalentConcepts());
        }
    }

}
