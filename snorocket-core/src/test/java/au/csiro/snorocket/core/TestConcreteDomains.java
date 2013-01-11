/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Datatype;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.Feature;
import au.csiro.ontology.model.FloatLiteral;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.IntegerLiteral;
import au.csiro.ontology.model.Operator;
import au.csiro.ontology.model.Role;
import au.csiro.ontology.model.StringLiteral;
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
    public void testConcreteDomainsEqualityInts() {
        IFactory<String> factory = new CoreFactory<>();

        // Add roles
        Role<String> container = new Role<>("container");
        Role<String> contains = new Role<>("contains");

        // Add features
        Feature<String> mgPerTablet = new Feature<>("mgPerTablet");

        // Add concepts
        Concept<String> panadol = new Concept<>("Panadol");
        Concept<String> panadol_250mg = new Concept<>("Panadol_250mg");
        Concept<String> panadol_500mg = new Concept<>("Panadol_500mg");
        Concept<String> panadol_pack_250mg = new Concept<>("Panadol_pack_250mg");
        Concept<String> paracetamol = new Concept<>("Paracetamol");
        Concept<String> bottle = new Concept<>("Bottle");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, new Existential<>(
                contains, paracetamol));

        ConceptInclusion a2 = new ConceptInclusion(panadol_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)) }));

        ConceptInclusion a3 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)) }), panadol_250mg);

        ConceptInclusion a4 = new ConceptInclusion(panadol_500mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(500)) }));

        ConceptInclusion a5 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(500)) }), panadol_500mg);

        ConceptInclusion a6 = new ConceptInclusion(panadol_pack_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)),
                        new Existential<>(container, bottle) }));

        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol.getId());
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg.getId());
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg.getId());
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg.getId());
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg.getId())));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol.getId());
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle.getId());
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));
    }

    /**
     * Very simple concrete domains test that uses equality and floats. The
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
    public void testConcreteDomainsEqualityFloats() {
        IFactory<String> factory = new CoreFactory<>();

        // Add roles
        Role<String> container = new Role<>("container");
        Role<String> contains = new Role<>("contains");

        // Add features
        Feature<String> mgPerTablet = new Feature<>("mgPerTablet");

        // Add concepts
        Concept<String> panadol = new Concept<>("Panadol");
        Concept<String> panadol_250mg = new Concept<>("Panadol_250mg");
        Concept<String> panadol_500mg = new Concept<>("Panadol_500mg");
        Concept<String> panadol_pack_250mg = new Concept<>("Panadol_pack_250mg");
        Concept<String> paracetamol = new Concept<>("Paracetamol");
        Concept<String> bottle = new Concept<>("Bottle");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, new Existential<>(
                contains, paracetamol));

        ConceptInclusion a2 = new ConceptInclusion(panadol_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new FloatLiteral(250.0f)) }));

        ConceptInclusion a3 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new FloatLiteral(250.0f)) }), panadol_250mg);

        ConceptInclusion a4 = new ConceptInclusion(panadol_500mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new FloatLiteral(500.0f)) }));

        ConceptInclusion a5 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new FloatLiteral(500.0f)) }), panadol_500mg);

        ConceptInclusion a6 = new ConceptInclusion(panadol_pack_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new FloatLiteral(250.0f)),
                        new Existential<>(container, bottle) }));

        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol.getId());
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg.getId());
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg.getId());
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg.getId());
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg.getId())));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol.getId());
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle.getId());
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));
    }

    /**
     * Very simple concrete domains test that uses equality and strings. The
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
    public void testConcreteDomainsEqualityStrings() {
        IFactory<String> factory = new CoreFactory<>();

        // Add roles
        Role<String> container = new Role<>("container");
        Role<String> contains = new Role<>("contains");

        // Add features
        Feature<String> mgPerTablet = new Feature<>("mgPerTablet");

        // Add concepts
        Concept<String> panadol = new Concept<>("Panadol");
        Concept<String> panadol_250mg = new Concept<>("Panadol_250mg");
        Concept<String> panadol_500mg = new Concept<>("Panadol_500mg");
        Concept<String> panadol_pack_250mg = new Concept<>("Panadol_pack_250mg");
        Concept<String> paracetamol = new Concept<>("Paracetamol");
        Concept<String> bottle = new Concept<>("Bottle");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, new Existential<>(
                contains, paracetamol));

        ConceptInclusion a2 = new ConceptInclusion(panadol_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new StringLiteral("250")) }));

        ConceptInclusion a3 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new StringLiteral("250")) }), panadol_250mg);

        ConceptInclusion a4 = new ConceptInclusion(panadol_500mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new StringLiteral("500")) }));

        ConceptInclusion a5 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new StringLiteral("500")) }), panadol_500mg);

        ConceptInclusion a6 = new ConceptInclusion(panadol_pack_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new StringLiteral("250")),
                        new Existential<>(container, bottle) }));

        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        // Classify
        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol.getId());
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode panadol_250mgNode = ppd.getEquivalents(panadol_250mg.getId());
        Set<ClassNode> panadol_250mgRes = panadol_250mgNode.getParents();
        assertTrue(panadol_250mgRes.size() == 1);
        assertTrue(panadol_250mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_500mgNode = ppd.getEquivalents(panadol_500mg.getId());
        Set<ClassNode> panadol_500mgRes = panadol_500mgNode.getParents();
        assertTrue(panadol_500mgRes.size() == 1);
        assertTrue(panadol_500mgRes
                .contains(ppd.getEquivalents(panadol.getId())));

        ClassNode panadol_pack_250mgNode = ppd
                .getEquivalents(panadol_pack_250mg.getId());
        Set<ClassNode> panadol_pack_250mgRes = panadol_pack_250mgNode
                .getParents();
        assertTrue(panadol_pack_250mgRes.size() == 1);
        assertTrue(panadol_pack_250mgRes.contains(ppd
                .getEquivalents(panadol_250mg.getId())));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol.getId());
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(CoreFactory.TOP_CONCEPT)));

        ClassNode bottleNode = ppd.getEquivalents(bottle.getId());
        Set<ClassNode> bottleRes = bottleNode.getParents();
        assertTrue(bottleRes.size() == 1);
        assertTrue(bottleRes.contains(ppd.getEquivalents(CoreFactory.TOP_CONCEPT)));
    }

    /**
     * Test for concrete domains taken from the paper "Tractable Extensions of
     * the Description Logic EL with Numerical Datatypes". It uses integer
     * values and the operators less than, equals, and greater than.
     */
    @Test
    public void testConcreteDomainsOperators() {
        IFactory<String> factory = new CoreFactory<>();

        // Add roles
        Role<String> contains = new Role<>("contains");
        Role<String> hasPrescription = new Role<>("hasPrescription");

        // Add features
        Feature<String> mgPerTablet = new Feature<>("mgPerTablet");
        Feature<String> hasAge = new Feature<>("hasAge");

        // Add concepts
        Concept<String> panadol = new Concept<>("Panadol");
        Concept<String> paracetamol = new Concept<>("Paracetamol");
        Concept<String> patient = new Concept<>("Patient");
        Concept<String> X = new Concept<>("X");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, new Existential<>(
                contains, new Conjunction(new IConcept[] {
                        paracetamol,
                        new Datatype<>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(500)) })));

        ConceptInclusion a2 = new ConceptInclusion(
                new Conjunction(new IConcept[] {
                        patient,
                        new Datatype<>(hasAge, Operator.LESS_THAN,
                                new IntegerLiteral(6)),
                        new Existential<>(hasPrescription, new Existential<>(
                                contains,
                                new Conjunction(new IConcept[] {
                                        paracetamol,
                                        new Datatype<>(mgPerTablet,
                                                Operator.GREATER_THAN,
                                                new IntegerLiteral(250)) }))) }),
                Concept.BOTTOM);

        ConceptInclusion a3 = new ConceptInclusion(X, new Conjunction(
                new IConcept[] {
                        patient,
                        new Datatype<>(hasAge, Operator.EQUALS,
                                new IntegerLiteral(3)),
                        new Existential<>(hasPrescription, panadol) }));

        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);

        // Classify
        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Test results
        ClassNode panadolNode = ppd.getEquivalents(panadol.getId());
        Set<ClassNode> panadolRes = panadolNode.getParents();
        assertTrue(panadolRes.size() == 1);
        assertTrue(panadolRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode paracetamolNode = ppd.getEquivalents(paracetamol.getId());
        Set<ClassNode> paracetamolRes = paracetamolNode.getParents();
        assertTrue(paracetamolRes.size() == 1);
        assertTrue(paracetamolRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode patientNode = ppd.getEquivalents(patient.getId());
        Set<ClassNode> patientRes = patientNode.getParents();
        assertTrue(patientRes.size() == 1);
        assertTrue(patientRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        assertTrue(bottomNode.getEquivalentConcepts().contains(
                factory.getConcept(X.getId())));
        Set<ClassNode> bottomRes = bottomNode.getParents();
        assertTrue(bottomRes.size() == 3);
        assertTrue(bottomRes.contains(ppd.getEquivalents(panadol.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(paracetamol.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(patient.getId())));
    }

}
