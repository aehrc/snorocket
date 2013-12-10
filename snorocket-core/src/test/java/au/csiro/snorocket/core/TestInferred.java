/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.NamedConcept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.NamedRole;

/**
 * @author Alejandro Metke
 *
 */
public class TestInferred {
    
    @Test
    public void testGetInferred() {
        NamedConcept a = new NamedConcept("A");
        ConceptInclusion ci = new ConceptInclusion(
                a, 
                new Conjunction(new Concept[] {
                        new NamedConcept("D"),
                        new Existential(new NamedRole("r"), new Conjunction(new Concept[] {
                                new Existential(new NamedRole("s"), new NamedConcept("B")),
                                new Existential(new NamedRole("t"), new NamedConcept("C"))
                        }))
                }));
        
        ConceptInclusion ci2 = new ConceptInclusion(new NamedConcept("E"), a);
        
        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(ci);
        axioms.add(ci2);
        
        System.out.println("Stated axioms:\n");
        for(Axiom ax : axioms) {
            System.err.println(ax);
        }
        
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.loadAxioms(axioms);
        sr.classify();
        
        // Retrieve inferred axioms
        Collection<Axiom> inferredAxioms = sr.getInferredAxioms();
        
        System.out.println("Inferred axioms:\n");
        for(Axiom ax : inferredAxioms) {
            System.err.println(ax);
        }
    }
    
    
    
}
