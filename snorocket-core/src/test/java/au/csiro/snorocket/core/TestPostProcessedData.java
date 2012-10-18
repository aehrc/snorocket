/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;

import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.SparseConceptMap;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Unit test cases for the taxonomy construction process.
 * 
 * @author Alejandro Metke
 * 
 */
public class TestPostProcessedData {

    /**
     * Simple test case with the following concept-parents sets:
     * 
     * A: {A} B: {B, A} C: {C, A} D: {D, B, C} E: {E, D}
     * 
     * The resulting taxonomy should look like this:
     * 
     * TOP | A _|_ | | B C |_ _| | D | E | BOTTOM
     */
    @Test
    public void testComputeDag() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");
        int c = factory.getConcept("C");
        int d = factory.getConcept("D");
        int e = factory.getConcept("E");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(5);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        subsumptions.put(c, cSet);

        IConceptSet dSet = new SparseConceptSet();
        dSet.add(d);
        dSet.add(b);
        dSet.add(c);
        subsumptions.put(d, dSet);

        IConceptSet eSet = new SparseConceptSet();
        eSet.add(e);
        eSet.add(d);
        subsumptions.put(e, eSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 2
        Assert.assertEquals(2, aNode.getChildren().size());
        // Check children are B and C
        int[] children = new int[2];
        int i = 0;
        for (ClassNode cn : aNode.getChildren()) {
            children[i++] = cn.getEquivalentConcepts().iterator().next();
        }
        Arrays.sort(children);
        Assert.assertEquals(b, children[0]);
        Assert.assertEquals(c, children[1]);

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is D
        Assert.assertEquals(d, bNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is D
        Assert.assertEquals(d, cNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode dNode = ppd.getEquivalents(d);
        // Check node contains a single concept
        Assert.assertEquals(1, dNode.getEquivalentConcepts().size());
        // Check concept is D
        Assert.assertEquals(d, dNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, dNode.getChildren().size());
        // Check child is E
        Assert.assertEquals(e, dNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode eNode = ppd.getEquivalents(e);
        // Check node contains a single concept
        Assert.assertEquals(1, eNode.getEquivalentConcepts().size());
        // Check concept is E
        Assert.assertEquals(e, eNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, eNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, eNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of two
     * new nodes in the hierarchy.
     * 
     * Original sets: A: {A} B: {B, A} C: {C, A}
     * 
     * Original taxonomy:
     * 
     * TOP | A _|_ | | B C |_ _| | BOTTOM
     * 
     * Incremental sets: D: {D, B, C} E: {E, D}
     * 
     * New expected taxonomy:
     * 
     * TOP | A _|_ | | B C |_ _| | D | E | BOTTOM
     */
    @Test
    public void testComputeDagIncremental1() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");
        int c = factory.getConcept("C");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        subsumptions.put(c, cSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        int d = factory.getConcept("D");
        int e = factory.getConcept("E");

        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(2);

        IConceptSet dSet = new SparseConceptSet();
        dSet.add(d);
        dSet.add(b);
        dSet.add(c);
        newSubsumptions.put(d, dSet);

        IConceptSet eSet = new SparseConceptSet();
        eSet.add(e);
        eSet.add(d);
        newSubsumptions.put(e, eSet);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 2
        Assert.assertEquals(2, aNode.getChildren().size());
        // Check children are B and C
        int[] children = new int[2];
        int i = 0;
        for (ClassNode cn : aNode.getChildren()) {
            children[i++] = cn.getEquivalentConcepts().iterator().next();
        }
        Arrays.sort(children);
        Assert.assertEquals(b, children[0]);
        Assert.assertEquals(c, children[1]);

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is D
        Assert.assertEquals(d, bNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is D
        Assert.assertEquals(d, cNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode dNode = ppd.getEquivalents(d);
        // Check node contains a single concept
        Assert.assertEquals(1, dNode.getEquivalentConcepts().size());
        // Check concept is D
        Assert.assertEquals(d, dNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, dNode.getChildren().size());
        // Check child is E
        Assert.assertEquals(e, dNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode eNode = ppd.getEquivalents(e);
        // Check node contains a single concept
        Assert.assertEquals(1, eNode.getEquivalentConcepts().size());
        // Check concept is E
        Assert.assertEquals(e, eNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, eNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, eNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of two
     * new nodes, one of them immediately under TOP.
     * 
     * Original sets: A: {A} B: {B, A}
     * 
     * Original taxonomy:
     * 
     * TOP | A | B | BOTTOM
     * 
     * Incremental sets: A: {A, C} C: {C} D: {D, A, C}
     * 
     * New expected taxonomy:
     * 
     * TOP | C | A _|_ | | B D |_ _| | BOTTOM
     */
    @Test
    public void testComputeDagIncremental2() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        int c = factory.getConcept("C");
        int d = factory.getConcept("D");

        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(2);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        newSubsumptions.put(c, cSet);

        IConceptSet dSet = new SparseConceptSet();
        dSet.add(d);
        dSet.add(a);
        dSet.add(c);
        newSubsumptions.put(d, dSet);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        aSet = new SparseConceptSet();
        aSet.add(a);
        aSet.add(c);
        affectedConceptSubs.put(a, aSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is A
        Assert.assertEquals(a, cNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 2
        Assert.assertEquals(2, aNode.getChildren().size());
        // Check children are B and D
        int[] children = new int[2];
        int i = 0;
        for (ClassNode cn : aNode.getChildren()) {
            children[i++] = cn.getEquivalentConcepts().iterator().next();
        }
        Arrays.sort(children);
        Assert.assertEquals(b, children[0]);
        Assert.assertEquals(d, children[1]);

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, bNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());

        ClassNode dNode = ppd.getEquivalents(d);
        // Check node contains a single concept
        Assert.assertEquals(1, dNode.getEquivalentConcepts().size());
        // Check concept is D
        Assert.assertEquals(d, dNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, dNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, dNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of a
     * node that is equivalent to an existing node.
     * 
     * Original sets: A: {A} B: {B, A} C: {C, A} D: {D, B, C} E: {E, C}
     * 
     * Original taxonomy:
     * 
     * TOP | A _|_ | | B C_ |_ _| | D E | | BOTTOM
     * 
     * Incremental sets: C: {C, A, F} F: {F, C, B}
     * 
     * New expected taxonomy:
     * 
     * TOP | A | B | C,F _|_ | | D E |_ _| | BOTTOM
     */
    @Test
    public void testComputeDagIncremental3() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");
        int c = factory.getConcept("C");
        int d = factory.getConcept("D");
        int e = factory.getConcept("E");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        subsumptions.put(c, cSet);

        IConceptSet dSet = new SparseConceptSet();
        dSet.add(d);
        dSet.add(b);
        dSet.add(c);
        subsumptions.put(d, dSet);

        IConceptSet eSet = new SparseConceptSet();
        eSet.add(e);
        eSet.add(c);
        subsumptions.put(e, eSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        int f = factory.getConcept("F");

        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(1);

        IConceptSet fSet = new SparseConceptSet();
        fSet.add(f);
        fSet.add(c);
        fSet.add(b);
        newSubsumptions.put(f, fSet);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        cSet.add(f);
        affectedConceptSubs.put(c, cSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        // Verify taxonomy
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, aNode.getChildren().size());
        // Check child is B
        Assert.assertEquals(b, aNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());

        ClassNode cfNode = ppd.getEquivalents(c);
        // Check node contains two concepts
        Assert.assertEquals(2, cfNode.getEquivalentConcepts().size());
        // Check concepts are C and F
        int[] eqs = new int[2];
        int i = 0;
        for (IntIterator it = cfNode.getEquivalentConcepts().iterator(); it
                .hasNext();) {
            eqs[i++] = it.next();
        }
        Arrays.sort(eqs);
        Assert.assertEquals(c, eqs[0]);
        Assert.assertEquals(f, eqs[1]);
        // Check number of children is 2
        Assert.assertEquals(2, cfNode.getChildren().size());
        // Check children are D and E
        int[] children = new int[2];
        i = 0;
        for (ClassNode cn : cfNode.getChildren()) {
            children[i++] = cn.getEquivalentConcepts().iterator().next();
        }
        Arrays.sort(children);
        Assert.assertEquals(d, children[0]);
        Assert.assertEquals(e, children[1]);

        ClassNode dNode = ppd.getEquivalents(d);
        // Check node contains a single concept
        Assert.assertEquals(1, dNode.getEquivalentConcepts().size());
        // Check concept is D
        Assert.assertEquals(d, dNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, dNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, dNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());

        ClassNode eNode = ppd.getEquivalents(e);
        // Check node contains a single concept
        Assert.assertEquals(1, eNode.getEquivalentConcepts().size());
        // Check concept is E
        Assert.assertEquals(e, eNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, eNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, eNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the case where an
     * existing node becomes equivalent to another existing node.
     * 
     * Original sets: A: {A} B: {B, A}
     * 
     * Original taxonomy:
     * 
     * TOP | A | B | BOTTOM
     * 
     * Incremental sets: A: {A, B}
     * 
     * New expected taxonomy:
     * 
     * TOP | A,B | BOTTOM
     */
    @Test
    public void testComputeDagIncremental4() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(1);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        aSet = new SparseConceptSet();
        aSet.add(a);
        aSet.add(b);
        affectedConceptSubs.put(a, aSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        // Verify taxonomy
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains two concepts
        Assert.assertEquals(2, aNode.getEquivalentConcepts().size());
        // Check number of children is 1
        Assert.assertEquals(1, aNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, aNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of a new
     * relationship that modifies the hierarchy.
     * 
     * Original sets: A: {A} B: {B, A} C: {C, A}
     * 
     * Original taxonomy:
     * 
     * TOP | A _|_ | | B C |_ _| | BOTTOM
     * 
     * Incremental sets: C: {C, A, B}
     * 
     * New expected taxonomy:
     * 
     * TOP | A | B | C | BOTTOM
     */
    @Test
    public void testComputeDagIncremental5() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");
        int c = factory.getConcept("C");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        subsumptions.put(b, bSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        subsumptions.put(c, cSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(1);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        cSet.add(b);
        affectedConceptSubs.put(c, cSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, aNode.getChildren().size());
        // Check child is B
        Assert.assertEquals(b, aNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is C
        Assert.assertEquals(c, bNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, cNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of a new
     * relationship that modifies the hierarchy.
     * 
     * Original sets: A: {A} B: {B} C: {C, A, B}
     * 
     * Original taxonomy:
     * 
     * TOP _|_ | | A B |_ _| | C | BOTTOM
     * 
     * Incremental sets: B: {B, A}
     * 
     * New expected taxonomy:
     * 
     * TOP | A | B | C | BOTTOM
     */
    @Test
    public void testComputeDagIncremental6() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int b = factory.getConcept("B");
        int c = factory.getConcept("C");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(3);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        subsumptions.put(b, bSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        cSet.add(b);
        subsumptions.put(c, cSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        // Add additional subsumptions
        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(1);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);

        bSet.add(a);
        affectedConceptSubs.put(b, bSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, aNode.getChildren().size());
        // Check child is B
        Assert.assertEquals(b, aNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is C
        Assert.assertEquals(c, bNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, cNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

    /**
     * Test case for incremental DAG computation including the addition of two
     * new concepts with a relationship between them that should be discarded.
     * 
     * Original sets: A: {A} C: {C, A}
     * 
     * Original taxonomy:
     * 
     * TOP | A | C | BOTTOM
     * 
     * Incremental sets: B: {B, A} C: {C, A, B}
     * 
     * New expected taxonomy:
     * 
     * TOP | A | B | C | BOTTOM
     */
    @Test
    public void testComputeDagIncremental7() {
        PostProcessedData ppd = new PostProcessedData();
        IFactory factory = new Factory();
        int a = factory.getConcept("A");
        int c = factory.getConcept("C");

        IConceptMap<IConceptSet> subsumptions = new SparseConceptMap<>(2);
        IConceptSet aSet = new SparseConceptSet();
        aSet.add(a);
        subsumptions.put(a, aSet);

        IConceptSet cSet = new SparseConceptSet();
        cSet.add(c);
        cSet.add(a);
        subsumptions.put(c, cSet);

        ppd.computeDag(factory, subsumptions, new NullReasonerProgressMonitor());

        int b = factory.getConcept("B");
        // Add additional subsumptions
        IConceptMap<IConceptSet> newSubsumptions = new SparseConceptMap<>(1);
        IConceptSet bSet = new SparseConceptSet();
        bSet.add(b);
        bSet.add(a);
        newSubsumptions.put(b, bSet);

        IConceptMap<IConceptSet> affectedConceptSubs = new SparseConceptMap<>(1);
        cSet.add(b);
        affectedConceptSubs.put(c, cSet);

        // Compute DAG incrementally
        ppd.computeDagIncremental(factory, newSubsumptions,
                affectedConceptSubs, new NullReasonerProgressMonitor());

        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Assert.assertEquals(true, top.getParents().isEmpty());
        Assert.assertEquals(1, top.getChildren().size());

        ClassNode aNode = ppd.getEquivalents(a);
        // Check node contains a single concept
        Assert.assertEquals(1, aNode.getEquivalentConcepts().size());
        // Check concept is A
        Assert.assertEquals(a, aNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, aNode.getChildren().size());
        // Check child is B
        Assert.assertEquals(b, aNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode bNode = ppd.getEquivalents(b);
        // Check node contains a single concept
        Assert.assertEquals(1, bNode.getEquivalentConcepts().size());
        // Check concept is B
        Assert.assertEquals(b, bNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, bNode.getChildren().size());
        // Check child is C
        Assert.assertEquals(c, bNode.getChildren().iterator().next()
                .getEquivalentConcepts().iterator().next());

        ClassNode cNode = ppd.getEquivalents(c);
        // Check node contains a single concept
        Assert.assertEquals(1, cNode.getEquivalentConcepts().size());
        // Check concept is C
        Assert.assertEquals(c, cNode.getEquivalentConcepts().iterator().next());
        // Check number of children is 1
        Assert.assertEquals(1, cNode.getChildren().size());
        // Check child is BOTTOM
        Assert.assertEquals(Factory.BOTTOM_CONCEPT, cNode.getChildren()
                .iterator().next().getEquivalentConcepts().iterator().next());
    }

}
