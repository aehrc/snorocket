/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.examples;

import java.util.HashSet;
import java.util.Set;

import au.csiro.ontology.Factory;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.Feature;
import au.csiro.ontology.model.Operator;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.SnorocketReasoner;
import au.csiro.snorocket.core.util.Utils;

/**
 * This class contains several examples of how to use the Snorocket API for
 * people familiar with SNOMED.
 *  
 * @author Alejandro Metke
 *
 */
public class SnomedExample {
    
    /**
     * Shows how to create the following axioms:
     * 
     * <ol>
     *   <li>Primitive child with no roles</li>
     *   <li>Fully defined child with one or more roles</li>
     *   <li>Fully defined child with a concrete domain</li>
     * </ol>
     * 
     */
    public static void bottlesExample() {
        // A factory is used to create expressions and axioms
        Factory f = new Factory();
        
        // Create all the concepts
        Concept bottle = f.createNamedConcept("bottle");
        Concept plasticBottle = f.createNamedConcept("plasticBottle");
        Concept glassBottle = f.createNamedConcept("glassBottle");
        Concept purplePlasticBottle = f.createNamedConcept("purplePlasticBottle");
        Concept plastic = f.createNamedConcept("plastic");
        Concept tallBottle = f.createNamedConcept("tallBottle");
        Concept wideBottle = f.createNamedConcept("wideBottle");
        Concept wineBottle = f.createNamedConcept("wineBottle");
        
        // Create all the roles
        Role isMadeOf = f.createNamedRole("isMadeOf");
        
        // Create all the features
        Feature hasHeight = f.createNamedFeature("hasHeight");
        Feature hasWidth = f.createNamedFeature("hasWidth");
        
        Set<Axiom> axioms = new HashSet<Axiom>();
        
        // This is an example of a primitive child with no roles.
        Axiom a0 = new ConceptInclusion(glassBottle, bottle);
        axioms.add(a0);
        
        // This is an example of a fully defined child with one role. In this
        // case two axioms are needed because the API does not support
        // equivalence directly.
        Axiom a1 = new ConceptInclusion(
                plasticBottle, 
                f.createConjunction(
                        bottle, 
                        f.createExistential(isMadeOf, plastic)
                )
        );
        Axiom a1b = new ConceptInclusion(
                f.createConjunction(
                        bottle, 
                        f.createExistential(isMadeOf, plastic)
                ),
                plasticBottle
        );
        axioms.add(a1);
        axioms.add(a1b);
        
        // This is an example of a primitive child with no roles
        Axiom a2 = new ConceptInclusion(
                purplePlasticBottle, 
                plasticBottle
        );
        axioms.add(a2);
        
        // This is an example of a fully defined child with a concrete domain
        Axiom a3 = new ConceptInclusion(
                tallBottle, 
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasHeight, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                )
        );
        
        Axiom a3b = new ConceptInclusion(
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasHeight, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                ),
                tallBottle
        );
        axioms.add(a3);
        axioms.add(a3b);
        
        // This is another example of a fully defined child with a concrete 
        // domain
        Axiom a4 = new ConceptInclusion(
                wideBottle, 
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasWidth, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                )
        );
        
        Axiom a4b = new ConceptInclusion(
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasWidth, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                ),
                wideBottle
        );
        axioms.add(a4);
        axioms.add(a4b);
        
        // Yet another example of a fully defined child with a concrete domain
        Axiom a5 = new ConceptInclusion(
                wineBottle, 
                f.createConjunction(
                        glassBottle,
                        f.createDatatype(
                                hasWidth, 
                                Operator.EQUALS, 
                                f.createIntegerLiteral(2)),
                        f.createDatatype(
                                hasHeight, 
                                Operator.EQUALS, 
                                f.createIntegerLiteral(6))
                )
        );
        Axiom a5b = new ConceptInclusion(
                f.createConjunction(
                        glassBottle,
                        f.createDatatype(
                                hasWidth, 
                                Operator.EQUALS, 
                                f.createIntegerLiteral(2)),
                        f.createDatatype(
                                hasHeight, 
                                Operator.EQUALS, 
                                f.createIntegerLiteral(6))
                ),
                wineBottle
        );
        axioms.add(a5);
        axioms.add(a5b);
        
        // Create a classifier and classify the axioms
        IReasoner r = new SnorocketReasoner();
        r.loadAxioms(axioms);
        r = r.classify();
        
        // Get only the taxonomy
        Ontology res = r.getClassifiedOntology();
        Utils.printTaxonomy(res.getTopNode(), res.getBottomNode());
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        SnomedExample.bottlesExample();
    }

}
