/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.axioms.RI;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;

/**
 * Main unit tests for Snorocket.
 * 
 * @author Alejandro Metke
 * 
 */
public class TestNormalisedOntology {

    /**
     * Tests the simple example found in the paper "Efficient Reasoning in EL+".
     */
    @Test
    public void testEndocarditis() {
        IFactory factory = new Factory();

        // Add roles
        int contIn = factory.getRole("cont-in");
        int partOf = factory.getRole("part-of");
        int hasLoc = factory.getRole("has-loc");
        int actsOn = factory.getRole("acts-on");

        // Add concepts
        int endocardium = factory.getConcept("Endocardium");
        int tissue = factory.getConcept("Tissue");
        int heartWall = factory.getConcept("HeartWall");
        int heartValve = factory.getConcept("HeartValve");
        int bodyWall = factory.getConcept("BodyWall");
        int heart = factory.getConcept("Heart");
        int bodyValve = factory.getConcept("BodyValve");
        int endocarditis = factory.getConcept("Endocarditis");
        int inflammation = factory.getConcept("Inflammation");
        int disease = factory.getConcept("Disease");
        int heartdisease = factory.getConcept("Heartdisease");
        int criticalDisease = factory.getConcept("CriticalDisease");

        // Add axioms
        GCI a1 = new GCI(endocardium, new Conjunction(new AbstractConcept[] {
                new Concept(tissue),
                new Existential(contIn, new Concept(heartWall)),
                new Existential(contIn, new Concept(heartValve)) }));

        GCI a2 = new GCI(heartWall, new Conjunction(new AbstractConcept[] {
                new Concept(bodyWall),
                new Existential(partOf, new Concept(heart)) }));

        GCI a3 = new GCI(heartValve, new Conjunction(new AbstractConcept[] {
                new Concept(bodyValve),
                new Existential(partOf, new Concept(heart)) }));

        GCI a4 = new GCI(endocarditis, new Conjunction(new AbstractConcept[] {
                new Concept(inflammation),
                new Existential(hasLoc, new Concept(endocardium)) }));

        GCI a5 = new GCI(inflammation, new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(actsOn, new Concept(tissue)) }));

        GCI a6 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(heartdisease),
                new Existential(hasLoc, new Concept(heartValve)) }),
                criticalDisease);

        GCI a7 = new GCI(heartdisease, new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(hasLoc, new Concept(heart)) }));

        GCI a8 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(hasLoc, new Concept(heart)) }), heartdisease);

        RI a9 = new RI(new int[] { partOf, partOf }, partOf);
        RI a10 = new RI(partOf, contIn);
        RI a11 = new RI(new int[] { hasLoc, contIn }, hasLoc);

        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);
        axioms.add(a7);
        axioms.add(a8);
        axioms.add(a9);
        axioms.add(a10);
        axioms.add(a11);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Test results
        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        Set<ClassNode> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocardium)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocarditis)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartWall)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartValve)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heart)));

        ClassNode endocarditisNode = ppd.getEquivalents(endocarditis);
        Set<ClassNode> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(inflammation)));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(heartdisease)));
        assertTrue(endocarditisRes
                .contains(ppd.getEquivalents(criticalDisease)));

        ClassNode inflammationNode = ppd.getEquivalents(inflammation);
        Set<ClassNode> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ppd.getEquivalents(disease)));

        ClassNode endocardiumNode = ppd.getEquivalents(endocardium);
        Set<ClassNode> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ppd.getEquivalents(tissue)));

        ClassNode heartdiseaseNode = ppd.getEquivalents(heartdisease);
        Set<ClassNode> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ppd.getEquivalents(disease)));

        ClassNode heartWallNode = ppd.getEquivalents(heartWall);
        Set<ClassNode> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ppd.getEquivalents(bodyWall)));

        ClassNode heartValveNode = ppd.getEquivalents(heartValve);
        Set<ClassNode> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(ppd.getEquivalents(bodyValve)));

        ClassNode diseaseNode = ppd.getEquivalents(disease);
        Set<ClassNode> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode tissueNode = ppd.getEquivalents(tissue);
        Set<ClassNode> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode heartNode = ppd.getEquivalents(heart);
        Set<ClassNode> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyValveNode = ppd.getEquivalents(bodyValve);
        Set<ClassNode> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyWallNode = ppd.getEquivalents(bodyWall);
        Set<ClassNode> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode criticalDiseaseNode = ppd.getEquivalents(criticalDisease);
        Set<ClassNode> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));
    }

    @Test
    public void testNormalise() {
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

        NormalisedOntology no = new NormalisedOntology(factory);
        Set<Inclusion> norms = no.normalise(axioms);

        for (Inclusion norm : norms) {
            System.out.println(norm.getNormalForm().toString());
        }

        // Not much of a test ;)
        assertEquals(12, norms.size());
    }

    /**
     * Tests incremental classification functionality for correctness by doing
     * the following:
     * 
     * <ol>
     * <li>Two axioms are removed from the Endocarditis ontology (see axioms
     * below).</li>
     * <li>This ontology is classified.</li>
     * <li>The axioms that were removed are added programmatically to the
     * ontology.</li>
     * <li>The new ontology is reclassified.</li>
     * <li>The results are compared to the original ground truth.</li>
     * </ol>
     * 
     * Declaration(Class(:Endocardium)) Declaration(Class(:Endocarditis))
     * 
     * SubClassOf( :Endocardium ObjectIntersectionOf( :Tissue
     * ObjectSomeValuesFrom(:cont-in :HeartWall) ObjectSomeValuesFrom(:cont-in
     * :HeartValve) ) )
     * 
     * SubClassOf( :Endocarditis ObjectIntersectionOf( :Inflammation
     * ObjectSomeValuesFrom(:has-loc :Endocardium) ) )
     */
    @Test
    public void testEndocarditisIncremental() {
        IFactory factory = new Factory();

        // Original Endocarditis ontology axioms
        int contIn = factory.getRole("cont-in");
        int partOf = factory.getRole("part-of");
        int hasLoc = factory.getRole("has-loc");
        int actsOn = factory.getRole("acts-on");
        int tissue = factory.getConcept("Tissue");
        int heartWall = factory.getConcept("HeartWall");
        int heartValve = factory.getConcept("HeartValve");
        int bodyWall = factory.getConcept("BodyWall");
        int heart = factory.getConcept("Heart");
        int bodyValve = factory.getConcept("BodyValve");
        int inflammation = factory.getConcept("Inflammation");
        int disease = factory.getConcept("Disease");
        int heartdisease = factory.getConcept("Heartdisease");
        int criticalDisease = factory.getConcept("CriticalDisease");

        GCI a2 = new GCI(heartWall, new Conjunction(new AbstractConcept[] {
                new Concept(bodyWall),
                new Existential(partOf, new Concept(heart)) }));

        GCI a3 = new GCI(heartValve, new Conjunction(new AbstractConcept[] {
                new Concept(bodyValve),
                new Existential(partOf, new Concept(heart)) }));

        GCI a5 = new GCI(inflammation, new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(actsOn, new Concept(tissue)) }));

        GCI a6 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(heartdisease),
                new Existential(hasLoc, new Concept(heartValve)) }),
                criticalDisease);

        GCI a7 = new GCI(heartdisease, new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(hasLoc, new Concept(heart)) }));

        GCI a8 = new GCI(new Conjunction(new AbstractConcept[] {
                new Concept(disease),
                new Existential(hasLoc, new Concept(heart)) }), heartdisease);

        RI a9 = new RI(new int[] { partOf, partOf }, partOf);
        RI a10 = new RI(partOf, contIn);
        RI a11 = new RI(new int[] { hasLoc, contIn }, hasLoc);

        // Partial ontology
        Set<Inclusion> axioms = new HashSet<Inclusion>();
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a5);
        axioms.add(a6);
        axioms.add(a7);
        axioms.add(a8);
        axioms.add(a9);
        axioms.add(a10);
        axioms.add(a11);

        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        IConceptMap<IConceptSet> s = o.getSubsumptions();
        PostProcessedData ppd = new PostProcessedData();
        ppd.computeDag(factory, s, null);

        // Add delta axioms and classify incrementally
        int endocardium = factory.getConcept("Endocardium");
        int endocarditis = factory.getConcept("Endocarditis");

        GCI a1 = new GCI(endocardium, new Conjunction(new AbstractConcept[] {
                new Concept(tissue),
                new Existential(contIn, new Concept(heartWall)),
                new Existential(contIn, new Concept(heartValve)) }));

        GCI a4 = new GCI(endocarditis, new Conjunction(new AbstractConcept[] {
                new Concept(inflammation),
                new Existential(hasLoc, new Concept(endocardium)) }));

        Set<Inclusion> incAxioms = new HashSet<Inclusion>();
        incAxioms.add(a1);
        incAxioms.add(a4);

        o.classifyIncremental(incAxioms);
        IConceptMap<IConceptSet> ns = o.getNewSubsumptions();
        IConceptMap<IConceptSet> as = o.getAffectedSubsumptions();
        ppd.computeDagIncremental(factory, ns, as, null);

        // Test results
        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        Set<ClassNode> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocardium)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocarditis)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartWall)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartValve)));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heart)));

        ClassNode endocarditisNode = ppd.getEquivalents(endocarditis);
        Set<ClassNode> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(inflammation)));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(heartdisease)));
        assertTrue(endocarditisRes
                .contains(ppd.getEquivalents(criticalDisease)));

        ClassNode inflammationNode = ppd.getEquivalents(inflammation);
        Set<ClassNode> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ppd.getEquivalents(disease)));

        ClassNode endocardiumNode = ppd.getEquivalents(endocardium);
        Set<ClassNode> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ppd.getEquivalents(tissue)));

        ClassNode heartdiseaseNode = ppd.getEquivalents(heartdisease);
        Set<ClassNode> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ppd.getEquivalents(disease)));

        ClassNode heartWallNode = ppd.getEquivalents(heartWall);
        Set<ClassNode> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ppd.getEquivalents(bodyWall)));

        ClassNode heartValveNode = ppd.getEquivalents(heartValve);
        Set<ClassNode> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(ppd.getEquivalents(bodyValve)));

        ClassNode diseaseNode = ppd.getEquivalents(disease);
        Set<ClassNode> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode tissueNode = ppd.getEquivalents(tissue);
        Set<ClassNode> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode heartNode = ppd.getEquivalents(heart);
        Set<ClassNode> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyValveNode = ppd.getEquivalents(bodyValve);
        Set<ClassNode> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyWallNode = ppd.getEquivalents(bodyWall);
        Set<ClassNode> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode criticalDiseaseNode = ppd.getEquivalents(criticalDisease);
        Set<ClassNode> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));
    }

}
