/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Arrays;
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
import au.csiro.ontology.model.Role;
import au.csiro.ontology.model.RoleInclusion;

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

        Set<Axiom> axioms = new HashSet<>();
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
        assertEquals(4, inferred.size());

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

        Set<Axiom> axioms = new HashSet<>();
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

    @Test
    public void testRoleChainingProduct() {
        final Set<Axiom> axioms = new HashSet<>();

        final Role rg = new NamedRole("roleGroup");
        final Role im = new NamedRole("isModificationOf");

        axioms.add(new RoleInclusion(new Role[] {im, im}, im)); // transitive
        axioms.add(new RoleInclusion(new Role[] {}, im));       // reflexive

        NamedConcept pct = null;
        NamedConcept pcte = null;

        // ----
        final Role ai = new NamedRole("hasActiveIngredient");

        final Concept mp = new NamedConcept("Medicinal product");
        pct = new NamedConcept("Product containing testosterone");
        pcte = new NamedConcept("Product containing testosterone enantate");
        final Concept t = new NamedConcept("Testosterone");
        final Concept te = new NamedConcept("Testosterone enantate");
        final Concept a = new NamedConcept("Androstane");

        axioms.add(new RoleInclusion(new Role[] {ai, im}, ai)); // role chain

        axioms.add(new ConceptInclusion(t, a));
        axioms.add(new ConceptInclusion(te, a));
        axioms.add(new ConceptInclusion(te, new Existential(im, t)));

        equiv(axioms, pct,  new Conjunction(new Concept[] {mp, new Existential(rg, new Existential(ai, t))}));
        equiv(axioms, pcte, new Conjunction(new Concept[] {mp, new Existential(rg, new Existential(ai, te))}));

        // ----

        // Classify
        SnorocketReasoner reasoner = new SnorocketReasoner();
        reasoner.loadAxioms(axioms);
        reasoner.classify();

        // Test results
        Collection<Axiom> stated = axioms;
        Collection<Axiom> inferred = reasoner.getInferredAxioms();

        printAxioms(inferred);

        boolean pcteIsaPctFound = false;
        boolean teIsModTeFound = false;
        for (Axiom i: inferred) {
            if (i instanceof ConceptInclusion) {
                ConceptInclusion inc = (ConceptInclusion) i;
                Concept lhs = inc.getLhs();
                if (lhs instanceof NamedConcept) {
                    final NamedConcept named = (NamedConcept) lhs;
                    if (named.equals(pcte)) {
                        final Concept rhs = inc.getRhs();
                        if (rhs instanceof NamedConcept) {
                            pcteIsaPctFound |= pct.equals(rhs);
                        } else if (rhs instanceof Conjunction) {
                            pcteIsaPctFound |= Arrays.asList(((Conjunction) rhs).getConcepts()).contains(pct);
                        } else {
                            fail("Unexpected rhs found: " + rhs);
                        }
                    } else if (named.equals(te)) {
                        for (Concept conj: ((Conjunction) inc.getRhs()).getConcepts()) {
                            if (conj instanceof Existential) {
                                Existential ex = (Existential) conj;
                                if (im.equals(ex.getRole())) {
                                    teIsModTeFound |= te.equals(ex.getConcept());
                                    if (te.equals(ex.getConcept())) {
                                        System.err.println("****** " + i);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        assertTrue("PCTE is not a child of PCT", pcteIsaPctFound);
        assertTrue("TE is not a modification of itself", teIsModTeFound);

    }

    @Test
    public void testRoleChainingMeasure() {
        final Set<Axiom> axioms = new HashSet<>();

        final Role rg = new NamedRole("roleGroup");
        final Role im = new NamedRole("isModificationOf");

        axioms.add(new RoleInclusion(new Role[] {im, im}, im)); // transitive
        axioms.add(new RoleInclusion(new Role[] {}, im));       // reflexive

        // ----
        final Role meth = new NamedRole("method");
        final Role comp = new NamedRole("component");

        final Concept evalAct = new NamedConcept("Evaluation action");
        final Concept measAct = new NamedConcept("Measurement action");

        final Concept subst = new NamedConcept("Substance");
        final Concept dOrM = new NamedConcept("Drug or Medicament");
        final NamedConcept amOrAmD = new NamedConcept("Amphetamine or amphetamine derivative");
        final Concept sam = new NamedConcept("Substituted amphetamine");
        final Concept amph = new NamedConcept("Amphetamine");
        final Concept mam = new NamedConcept("Methamphetamine");
        final Concept dam = new NamedConcept("Dimetamphetamine");

        final NamedConcept damMeas = new NamedConcept("Dimetamphetamine measurement");
        final Concept amMeas = new NamedConcept("Amphetamine measurement");
        final Concept mamMeas = new NamedConcept("Methamphetamine measurement");
        final Concept drugMeas = new NamedConcept("Drug measurement");
        final Concept measSubs = new NamedConcept("Measurement of substance");
        final Concept measProc = new NamedConcept("Measurement proc");
        final Concept evalProc = new NamedConcept("Evaluation proc");
        final Concept proc = new NamedConcept("Procedure");


        axioms.add(new RoleInclusion(new Role[] {comp, im}, comp)); // role chain

        axioms.add(new ConceptInclusion(measAct, evalAct));

        equiv(axioms, evalProc, proc, group(rg, meth, evalAct));
        equiv(axioms, measProc, evalProc, group(rg, meth, measAct));
        equiv(axioms, measSubs, measProc, group(rg, meth, measAct), group(rg, comp, subst));
        equiv(axioms, drugMeas, measSubs, group(rg, meth, measAct), group(rg, comp, dOrM));
        equiv(axioms, amMeas, drugMeas, group(rg, comp, amph));
        equiv(axioms, damMeas, amMeas, group(rg, meth, measAct), group(rg, comp, dam));
        equiv(axioms, mamMeas, amMeas, group(rg, meth, measAct), group(rg, comp, mam));

        axioms.add(new ConceptInclusion(dam, sam));
        axioms.add(new ConceptInclusion(dam, new Existential(im, mam)));
        axioms.add(new ConceptInclusion(sam, amOrAmD));
        axioms.add(new ConceptInclusion(amOrAmD, dOrM));       // SIMPLE
        axioms.add(new ConceptInclusion(dOrM, subst));
        axioms.add(new ConceptInclusion(mam, sam));

        // Classify
        SnorocketReasoner reasoner = new SnorocketReasoner();
        reasoner.loadAxioms(axioms);
        reasoner.classify();

        // Test results
        Collection<Axiom> stated = axioms;
        Collection<Axiom> inferred = reasoner.getInferredAxioms();

        printAxioms(inferred);

        System.err.println();
        boolean found = false;
        for (Axiom i: inferred) {
            if (i instanceof ConceptInclusion) {
                final ConceptInclusion inc = (ConceptInclusion) i;
                final Concept lhs = inc.getLhs();
                final Concept rhs = inc.getRhs();
                if (lhs instanceof NamedConcept && ((NamedConcept) lhs).equals(damMeas)) {
                    System.err.println(lhs + " [");
                    if (rhs instanceof NamedConcept) {
                        System.err.println("\t" + rhs);
                    } else if (rhs instanceof Conjunction) {
                        for (Concept conj: ((Conjunction) rhs).getConcepts()) {
                            System.err.println("\t" + conj);
                        }
//                        found |= Arrays.asList(((Conjunction) rhs).getConcepts()).contains(pct);
                    } else {
                        System.err.println("\t" + rhs);
                    }
                }
                if (rhs instanceof NamedConcept && ((NamedConcept) rhs).equals(damMeas)) {
                    System.err.println(rhs + " ]");
                    if (lhs instanceof NamedConcept) {
                        System.err.println("\t" + lhs);
                    } else if (lhs instanceof Conjunction) {
                        for (Concept conj: ((Conjunction) lhs).getConcepts()) {
                            System.err.println("\t" + conj);
                        }
//                        found |= Arrays.asList(((Conjunction) lhs).getConcepts()).contains(pct);
                    } else {
                        System.err.println("\t" + lhs);
                    }
                }
            }
        }
        //            assertTrue("PCTE is not a child of PCT", found);

    }

    private void equiv(Set<Axiom> axioms, Concept lhs, Concept... rhs) {
        if (rhs.length > 1) {
            final Conjunction conj = new Conjunction(rhs);
            axioms.add(new ConceptInclusion(lhs, conj));
            axioms.add(new ConceptInclusion(conj, lhs));
        } else {
            axioms.add(new ConceptInclusion(lhs, rhs[0]));
            axioms.add(new ConceptInclusion(rhs[0], lhs));
        }
    }

    private Existential group(Role rg, Role role, Concept value) {
        return new Existential(rg, new Existential(role, value));
    }

    private void printAxioms(Collection<Axiom> axioms) {
        for (Axiom a: axioms) {
            if (a instanceof RoleInclusion) {
                RoleInclusion inc = (RoleInclusion) a;
                System.out.println("RI: " + inc);
            }
        }
        System.out.println("----");
        for (Axiom a: axioms) {
            if (a instanceof ConceptInclusion) {
                ConceptInclusion inc = (ConceptInclusion) a;
                System.out.println("CI: " + inc);
            }
        }
    }

}
