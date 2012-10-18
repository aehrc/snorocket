/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.model.FloatLiteral;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.model.StringLiteral;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;

/**
 * Unit test cases for Snorocket concrete domains functionality.
 * 
 * @author Alejandro Metke
 *
 */
public class TestConcreteDomains {

    /**
     * Very simple concrete domains test that uses equality and integers. The
     * expected taxonomy is:
     * 
     * -Thing -Bottle -Panadol -Panadol_250mg -Panadol_pack_250mg -Panadol_500mg
     * -Paracetamol
     */
    @Test
    public void testConcreteDomainsEqualityInts() {
        IFactory factory = new Factory();

        // Add roles
        int container = factory.getRole("container");
        int contains = factory.getRole("contains");

        // Add features
        int mgPerTablet = factory.getFeature("mgPerTablet");

        // Add concepts
        int panadol = factory.getConcept("Panadol");
        int panadol_250mg = factory.getConcept("Panadol_250mg");
        int panadol_500mg = factory.getConcept("Panadol_500mg");
        int panadol_pack_250mg = factory.getConcept("Panadol_pack_250mg");
        int paracetamol = factory.getConcept("Paracetamol");
        int bottle = factory.getConcept("Bottle");

        // Add axioms
        GCI a1 = new GCI(panadol, new Existential(contains, new Concept(
                paracetamol)));

        GCI a2 = new GCI(panadol_250mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new IntegerLiteral(250)) }));

        GCI a3 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new IntegerLiteral(250)) }), panadol_250mg);

        GCI a4 = new GCI(panadol_500mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new IntegerLiteral(500)) }));

        GCI a5 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new IntegerLiteral(500)) }), panadol_500mg);

        GCI a6 = new GCI(panadol_pack_250mg, new Conjunction(
                new AbstractConcept[] {
                        new Concept(panadol),
                        new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                                new IntegerLiteral(250)),
                        new Existential(container, new Concept(bottle)) }));

        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol);
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg);
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg);
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg);
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg)));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol);
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle);
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));
    }

    /**
     * Very simple concrete domains test that uses equality and floats. The
     * expected taxonomy is:
     * 
     * -Thing -Bottle -Panadol -Panadol_250mg -Panadol_pack_250mg -Panadol_500mg
     * -Paracetamol
     */
    @Test
    public void testConcreteDomainsEqualityFloats() {
        IFactory factory = new Factory();

        // Add roles
        int container = factory.getRole("container");
        int contains = factory.getRole("contains");

        // Add features
        int mgPerTablet = factory.getFeature("mgPerTablet");

        // Add concepts
        int panadol = factory.getConcept("Panadol");
        int panadol_250mg = factory.getConcept("Panadol_250mg");
        int panadol_500mg = factory.getConcept("Panadol_500mg");
        int panadol_pack_250mg = factory.getConcept("Panadol_pack_250mg");
        int paracetamol = factory.getConcept("Paracetamol");
        int bottle = factory.getConcept("Bottle");

        // Add axioms
        GCI a1 = new GCI(panadol, new Existential(contains, new Concept(
                paracetamol)));

        GCI a2 = new GCI(panadol_250mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new FloatLiteral(250.0f)) }));

        GCI a3 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new FloatLiteral(250.0f)) }), panadol_250mg);

        GCI a4 = new GCI(panadol_500mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new FloatLiteral(500.0f)) }));

        GCI a5 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new FloatLiteral(500.0f)) }), panadol_500mg);

        GCI a6 = new GCI(panadol_pack_250mg, new Conjunction(
                new AbstractConcept[] {
                        new Concept(panadol),
                        new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                                new FloatLiteral(250.0f)),
                        new Existential(container, new Concept(bottle)) }));

        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol);
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg);
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg);
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg);
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg)));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol);
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle);
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));
    }

    /**
     * Very simple concrete domains test that uses equality and strings. The
     * expected taxonomy is:
     * 
     * -Thing -Bottle -Panadol -Panadol_250mg -Panadol_pack_250mg -Panadol_500mg
     * -Paracetamol
     */
    @Test
    public void testConcreteDomainsEqualityStrings() {
        IFactory factory = new Factory();

        // Add roles
        int container = factory.getRole("container");
        int contains = factory.getRole("contains");

        // Add features
        int mgPerTablet = factory.getFeature("mgPerTablet");

        // Add concepts
        int panadol = factory.getConcept("Panadol");
        int panadol_250mg = factory.getConcept("Panadol_250mg");
        int panadol_500mg = factory.getConcept("Panadol_500mg");
        int panadol_pack_250mg = factory.getConcept("Panadol_pack_250mg");
        int paracetamol = factory.getConcept("Paracetamol");
        int bottle = factory.getConcept("Bottle");

        // Add axioms
        GCI a1 = new GCI(panadol, new Existential(contains, new Concept(
                paracetamol)));

        GCI a2 = new GCI(panadol_250mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new StringLiteral("250")) }));

        GCI a3 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new StringLiteral("250")) }), panadol_250mg);

        GCI a4 = new GCI(panadol_500mg, new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new StringLiteral("500")) }));

        GCI a5 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(panadol),
                new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                        new StringLiteral("500")) }), panadol_500mg);

        GCI a6 = new GCI(panadol_pack_250mg, new Conjunction(
                new AbstractConcept[] {
                        new Concept(panadol),
                        new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                                new StringLiteral("250")),
                        new Existential(container, new Concept(bottle)) }));

        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol);
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg);
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg);
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes.contains(ppd.getEquivalents(panadol)));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg);
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg)));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol);
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(Factory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle);
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(Factory.TOP_CONCEPT)));
    }

    /**
     * Test for concrete domains taken from the paper "Tractable Extensions of
     * the Description Logic EL with Numerical Datatypes". It uses integer
     * values and the operators less than, equals, and greater than.
     */
    @Test
    public void testConcreteDomainsOperators() {
        IFactory factory = new Factory();

        // Add roles
        int contains = factory.getRole("contains");
        int hasAge = factory.getRole("hasAge");
        int hasPrescription = factory.getRole("hasPrescription");

        // Add features
        int mgPerTablet = factory.getFeature("mgPerTablet");

        // Add concepts
        int panadol = factory.getConcept("Panadol");
        int paracetamol = factory.getConcept("Paracetamol");
        int patient = factory.getConcept("Patient");
        int X = factory.getConcept("X");

        // Add axioms
        GCI a1 = new GCI(panadol, new Existential(contains, new Conjunction(
                new AbstractConcept[] {
                        new Concept(paracetamol),
                        new Datatype(mgPerTablet, Datatype.OPERATOR_EQUALS,
                                new IntegerLiteral(500)) })));

        GCI a2 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(patient),
                new Datatype(hasAge, Datatype.OPERATOR_LESS_THAN,
                        new IntegerLiteral(6)),
                new Existential(hasPrescription, new Existential(contains,
                        new Conjunction(new AbstractConcept[] {
                                new Concept(paracetamol),
                                new Datatype(mgPerTablet,
                                        Datatype.OPERATOR_GREATER_THAN,
                                        new IntegerLiteral(250)) }))) }),
                new Concept(IFactory.BOTTOM_CONCEPT));

        GCI a3 = new GCI(new Concept(X), new Conjunction(new AbstractConcept[] {
                new Concept(patient),
                new Datatype(hasAge, Datatype.OPERATOR_EQUALS,
                        new IntegerLiteral(3)),
                new Existential(hasPrescription, new Concept(panadol)) }));

        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol);
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol);
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode patientNode = ppd.getEquivalents(patient);
        Set<ClassNode> patientRes = patientNode.getParents();
        assertTrue(patientRes.size() == 1);
        assertTrue(patientRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        assertTrue(bottomNode.getEquivalentConcepts().contains(X));
        Set<ClassNode> bottomRes = bottomNode.getParents();
        assertTrue(bottomRes.size() == 3);
        assertTrue(bottomRes.contains(ppd.getEquivalents(panadol)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(paracetamol)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(patient)));
    }

}
