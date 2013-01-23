/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import java.util.HashSet;
import java.util.Set;

import au.csiro.ontology.Factory;
import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.INamedRole;
import au.csiro.snorocket.core.SnorocketReasoner;

/**
 * @author Alejandro Metke
 *
 */
public class LegoExample {

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("Running Chest Pain Lego Example");
        
        // Create the reasoner instance - the parametrised type refers to the
        // type of the external identifiers. In this case String is used and the
        // concepts are identified by sctid (but UUIDs could also be used).
        IReasoner<String> reasoner = new SnorocketReasoner<>();
        
        // This set will contain the axioms
        Set<IAxiom> axioms = new HashSet<>();
        
        // The factory can be used to create axioms using the default 
        // implementing classes
        Factory<String> f = new Factory<>();
        
        // The factory returns IConcepts - in this case the actual type is
        // INamedConcept<String>
        IConcept nonCardiacChestPain = f.createConcept("274668005");
        IConcept duringExcersice = f.createConcept("309604004");
        IConcept interview = f.createConcept("108217004");
        IConcept present = f.createConcept("52101004");
        
        // The factory can also be used to create roles
        INamedRole<String> associatedWith = f.createRole("47429007");
        
        // We need to define exactly what the identifier for the new concept 
        // will be
        IConcept historyCardioStandardNonAnginalChestPainExertion = 
                f.createConcept("pce_24220");
        
        // This is the axiom created for the discernable
        axioms.add(
            f.createConceptInclusion(
                historyCardioStandardNonAnginalChestPainExertion, 
                f.createConjunction(
                        nonCardiacChestPain,
                        f.createExistential(associatedWith, duringExcersice)
                )
        ));
        
        // This is the axiom created for the qualifier
        axioms.add(
            f.createConceptInclusion(
                historyCardioStandardNonAnginalChestPainExertion, 
                interview
            )
        );
        
        // This is the axiom created for the value
        axioms.add(
            f.createConceptInclusion(
                historyCardioStandardNonAnginalChestPainExertion, 
                present
            )
        );
        
        // The first time the classify method is called it runs a full 
        // classification. In this example there is no base state loaded so
        // a full (and not very useful) classification will be excuted.
        reasoner.classify(axioms);
        
        // The taxonomy contains the inferred hierarchy
        IOntology<String> t = reasoner.getClassifiedOntology();
        
        // We can look for nodes using the concept ids.
        Node<String> newNode = t.getNode("pce_24220");
        System.out.println("Node for HISTORY_CARDIO_Standard_Non_Anginal_" +
        		"Chest_Pain_Exertion:\n  "+
                newNode.getEquivalentConcepts());
        
        
        // We can now look for the parent and child nodes
        Set<Node<String>> parentNodes = newNode.getParents();
        System.out.println("Parents:");
        for(Node<String> parentNode : parentNodes) {
            System.out.println("  "+parentNode.getEquivalentConcepts());
        }
        
        Set<Node<String>> childNodes = newNode.getChildren();
        System.out.println("Children:");
        for(Node<String> childNode : childNodes) {
            System.out.println("  "+childNode.getEquivalentConcepts());
        }

    }

}
