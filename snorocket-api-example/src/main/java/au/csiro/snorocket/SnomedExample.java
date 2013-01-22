/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import java.util.HashSet;
import java.util.Set;

import au.csiro.ontology.Factory;
import au.csiro.ontology.IOntology;
import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.INamedFeature;
import au.csiro.ontology.model.INamedRole;
import au.csiro.ontology.model.Operator;
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
        Factory<String> f = new Factory<>();
        
        // Create all the concepts
        IConcept bottle = f.createConcept("bottle");
        IConcept plasticBottle = f.createConcept("plasticBottle");
        IConcept glassBottle = f.createConcept("glassBottle");
        IConcept purplePlasticBottle = f.createConcept("purplePlasticBottle");
        IConcept plastic = f.createConcept("plastic");
        IConcept tallBottle = f.createConcept("tallBottle");
        IConcept wideBottle = f.createConcept("wideBottle");
        IConcept wineBottle = f.createConcept("wineBottle");
        
        // Create all the roles
        INamedRole<String> isMadeOf = f.createRole("isMadeOf");
        
        // Create all the features
        INamedFeature<String> hasHeight = f.createFeature("hasHeight");
        INamedFeature<String> hasWidth = f.createFeature("hasWidth");
        
        Set<IAxiom> axioms = new HashSet<>();
        
        // This is an example of a primitive child with no roles.
        IAxiom a0 = new ConceptInclusion(glassBottle, bottle);
        axioms.add(a0);
        
        // This is an example of a fully defined child with one role. In this
        // case two axioms are needed because the API does not support
        // equivalence directly.
        IAxiom a1 = new ConceptInclusion(
                plasticBottle, 
                f.createConjunction(
                        bottle, 
                        f.createExistential(isMadeOf, plastic)
                )
        );
        IAxiom a1b = new ConceptInclusion(
                f.createConjunction(
                        bottle, 
                        f.createExistential(isMadeOf, plastic)
                ),
                plasticBottle
        );
        axioms.add(a1);
        axioms.add(a1b);
        
        // This is an example of a primitive child with no roles
        IAxiom a2 = new ConceptInclusion(
                purplePlasticBottle, 
                plasticBottle
        );
        axioms.add(a2);
        
        // This is an example of a fully defined child with a concrete domain
        IAxiom a3 = new ConceptInclusion(
                tallBottle, 
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasHeight, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                )
        );
        
        IAxiom a3b = new ConceptInclusion(
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
        IAxiom a4 = new ConceptInclusion(
                wideBottle, 
                f.createConjunction(
                        bottle,
                        f.createDatatype(
                                hasWidth, 
                                Operator.GREATER_THAN, 
                                f.createIntegerLiteral(5))
                )
        );
        
        IAxiom a4b = new ConceptInclusion(
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
        IAxiom a5 = new ConceptInclusion(
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
        IAxiom a5b = new ConceptInclusion(
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
        IReasoner<String> r = new SnorocketReasoner<>();
        r = r.classify(axioms);
        
        // Get only the taxonomy
        IOntology<String> res = r.getClassifiedOntology();
        Utils.printTaxonomy(res.getTopNode(), res.getBottomNode());
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        SnomedExample.bottlesExample();
    }

}
