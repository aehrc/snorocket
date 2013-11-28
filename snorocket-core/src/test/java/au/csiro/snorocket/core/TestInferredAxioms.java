/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Datatype;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.IntegerLiteral;
import au.csiro.ontology.model.NamedConcept;
import au.csiro.ontology.model.NamedFeature;
import au.csiro.ontology.model.NamedRole;
import au.csiro.ontology.model.Operator;

/**
 * Unit test cases for Snorocket concrete domains functionality.
 *
 * @author Alejandro Metke
 *
 */
public class TestInferredAxioms {

    /**
     * Very simple concrete domains test that uses equality and integers. The
     * expected taxonomy is:
     *
     * -Thing
     *   -Bottle
     *   -Panadol
     *     -Panadol_250mg
     *       -Panadol_pack_250mg
     *     -Panadol_500mg
     *   -Paracetamol
     *
     */
    @Test
    public void testInferredAxioms() {

        // Add roles
        NamedRole container = new NamedRole("container");
        NamedRole contains = new NamedRole("contains");

        // Add features
        NamedFeature mgPerTablet = new NamedFeature("mgPerTablet");

        // Add concepts
        NamedConcept panadol = new NamedConcept("Panadol");
        NamedConcept panadol_250mg = new NamedConcept("Panadol_250mg");
        NamedConcept panadol_500mg = new NamedConcept("Panadol_500mg");
        NamedConcept panadol_pack_250mg = new NamedConcept("Panadol_pack_250mg");
        NamedConcept paracetamol = new NamedConcept("Paracetamol");
        NamedConcept bottle = new NamedConcept("Bottle");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, new Existential(contains, paracetamol));

        ConceptInclusion a2 = new ConceptInclusion(panadol_250mg,
                new Conjunction(new Concept[] {
                        panadol,
                        new Datatype(mgPerTablet, Operator.EQUALS, new IntegerLiteral(250)) }));

        ConceptInclusion a3 = new ConceptInclusion(new Conjunction(
                new Concept[] {
                        panadol,
                        new Datatype(mgPerTablet, Operator.EQUALS, new IntegerLiteral(250)) }), panadol_250mg);

        ConceptInclusion a4 = new ConceptInclusion(panadol_500mg,
                new Conjunction(new Concept[] {
                        panadol,
                        new Datatype(mgPerTablet, Operator.EQUALS, new IntegerLiteral(500)) }));

        ConceptInclusion a5 = new ConceptInclusion(new Conjunction(
                new Concept[] {
                        panadol,
                        new Datatype(mgPerTablet, Operator.EQUALS, new IntegerLiteral(500)) }), panadol_500mg);

        ConceptInclusion a6 = new ConceptInclusion(panadol_pack_250mg,
                new Conjunction(new Concept[] {
                        panadol,
                        new Datatype(mgPerTablet, Operator.EQUALS, new IntegerLiteral(250)),
                        new Existential(container, bottle) }));

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);
        
        for(Axiom ax : axioms) {
            System.out.println(ax);
        }
        
        System.out.println("********************");

        // Classify
        SnorocketReasoner reasoner = new SnorocketReasoner();
        reasoner.loadAxioms(axioms);
        reasoner.classify();

        // Test results
        Collection<Axiom> stated = axioms;
        Collection<Axiom> inferred = reasoner.getInferredAxioms();

        assertEquals(6, stated.size());
        assertEquals(6, inferred.size());

        ConceptInclusion probe = a2;
        System.err.println(probe);

        for (Axiom a: inferred) {
            if (a instanceof ConceptInclusion) {
                ConceptInclusion inc = (ConceptInclusion) a;
                System.out.println(inc);
                /*
                if (probe.getLhs().equals(inc.getLhs())) {
                    System.err.println(inc);
                    Conjunction rhs = (Conjunction) inc.getRhs();
                    for (Concept m: rhs.getConcepts()) {
                        System.err.println(m.getClass().getSimpleName() + "\t" + m);
                    }
                }
                */
            }
        }
    }
    
    /**
     *
     */
    @Ignore
    @Test
    public void testInferredAxioms2() {

        // Add features
        NamedFeature f = new NamedFeature("f");

        // Add concepts
        NamedConcept a = new NamedConcept("A");
        NamedConcept b = new NamedConcept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, new Datatype(f, Operator.EQUALS, new IntegerLiteral(250)));
        ConceptInclusion a2 = new ConceptInclusion(new Datatype(f, Operator.EQUALS, new IntegerLiteral(250)), b);

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);
        axioms.add(a2);
        
        // Classify
        SnorocketReasoner reasoner = new SnorocketReasoner();
        reasoner.loadAxioms(axioms);
        reasoner.classify();

        // Test results
        Collection<Axiom> stated = axioms;
        Collection<Axiom> inferred = reasoner.getInferredAxioms();


        for (Axiom ia: inferred) {
            if (ia instanceof ConceptInclusion) {
                ConceptInclusion inc = (ConceptInclusion) ia;
                System.out.println(inc);
            }
        }
        
        for (Axiom ia: stated) {
            if (ia instanceof ConceptInclusion) {
                ConceptInclusion inc = (ConceptInclusion) ia;
                System.out.println(inc);
            }
        }
    }

}
