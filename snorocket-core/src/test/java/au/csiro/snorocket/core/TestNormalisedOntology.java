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

import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.axioms.RoleInclusion;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Datatype;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.Feature;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.IntegerLiteral;
import au.csiro.ontology.model.Operator;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.axioms.Inclusion;
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
        // Create roles
        Role<String> contIn = new Role<>("cont-in");
        Role<String> partOf = new Role<>("part-of");
        Role<String> hasLoc = new Role<>("has-loc");
        Role<String> actsOn = new Role<>("acts-on");

        // Create concepts
        Concept<String> endocardium = new Concept<>("Endocardium");
        Concept<String> tissue = new Concept<>("Tissue");
        Concept<String> heartWall = new Concept<>("HeartWall");
        Concept<String> heartValve = new Concept<>("HeartValve");
        Concept<String> bodyWall = new Concept<>("BodyWall");
        Concept<String> heart = new Concept<>("Heart");
        Concept<String> bodyValve = new Concept<>("BodyValve");
        Concept<String> endocarditis = new Concept<>("Endocarditis");
        Concept<String> inflammation = new Concept<>("Inflammation");
        Concept<String> disease = new Concept<>("Disease");
        Concept<String> heartdisease = new Concept<>("Heartdisease");
        Concept<String> criticalDisease = new Concept<>("CriticalDisease");

        // Create axioms
        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential<>(contIn, heartWall),
                        new Existential<>(contIn, heartValve) }));

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential<>(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential<>(partOf, heart) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential<>(hasLoc, endocardium) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential<>(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf },
                partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn },
                hasLoc);

        Set<IAxiom> axioms = new HashSet<>();
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
        IFactory<String> factory = new CoreFactory<>();
        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        
        int total = factory.getTotalConcepts();
        for(int i = 2; i < total; i++) {
            Object ob = factory.lookupConceptId(i);
            String str = ob.toString();
            System.out.println(i+ "->"+str);
        }
        total = factory.getTotalRoles();
        for(int i = 0; i < total; i++) {
            System.out.println(i+ "->"+factory.lookupRoleId(i).toString());
        }
        total = factory.getTotalFeatures();
        for(int i = 0; i < total; i++) {
            System.out.println(i+ "->"+factory.lookupFeatureId(i).toString());
        }
        
        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        // Build taxonomy
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Test results
        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        Set<ClassNode> bottomRes = bottomNode.getParents();

        //assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heart.getId())));

        ClassNode endocarditisNode = ppd.getEquivalents(endocarditis.getId());
        Set<ClassNode> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(inflammation
                .getId())));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(heartdisease
                .getId())));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(criticalDisease
                .getId())));

        ClassNode inflammationNode = ppd.getEquivalents(inflammation.getId());
        Set<ClassNode> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes
                .contains(ppd.getEquivalents(disease.getId())));

        ClassNode endocardiumNode = ppd.getEquivalents(endocardium.getId());
        Set<ClassNode> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ppd.getEquivalents(tissue.getId())));

        ClassNode heartdiseaseNode = ppd.getEquivalents(heartdisease.getId());
        Set<ClassNode> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes
                .contains(ppd.getEquivalents(disease.getId())));

        ClassNode heartWallNode = ppd.getEquivalents(heartWall.getId());
        Set<ClassNode> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ppd.getEquivalents(bodyWall.getId())));

        ClassNode heartValveNode = ppd.getEquivalents(heartValve.getId());
        Set<ClassNode> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(ppd.getEquivalents(bodyValve.getId())));

        ClassNode diseaseNode = ppd.getEquivalents(disease.getId());
        Set<ClassNode> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode tissueNode = ppd.getEquivalents(tissue.getId());
        Set<ClassNode> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode heartNode = ppd.getEquivalents(heart.getId());
        Set<ClassNode> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyValveNode = ppd.getEquivalents(bodyValve.getId());
        Set<ClassNode> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyWallNode = ppd.getEquivalents(bodyWall.getId());
        Set<ClassNode> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode criticalDiseaseNode = ppd.getEquivalents(criticalDisease
                .getId());
        Set<ClassNode> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));
    }

    @Test
    public void testNormalise() {
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

        NormalisedOntology<String> no = new NormalisedOntology<>(factory);
        Set<Inclusion<String>> norms = no.normalise(axioms);

        for (Inclusion<String> norm : norms) {
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
        IFactory<String> factory = new CoreFactory<>();

        // Original Endocarditis ontology axioms
        Role<String> contIn = new Role<>("cont-in");
        Role<String> partOf = new Role<>("part-of");
        Role<String> hasLoc = new Role<>("has-loc");
        Role<String> actsOn = new Role<>("acts-on");
        Concept<String> tissue = new Concept<>("Tissue");
        Concept<String> heartWall = new Concept<>("HeartWall");
        Concept<String> heartValve = new Concept<>("HeartValve");
        Concept<String> bodyWall = new Concept<>("BodyWall");
        Concept<String> heart = new Concept<>("Heart");
        Concept<String> bodyValve = new Concept<>("BodyValve");
        Concept<String> inflammation = new Concept<>("Inflammation");
        Concept<String> disease = new Concept<>("Disease");
        Concept<String> heartdisease = new Concept<>("Heartdisease");
        Concept<String> criticalDisease = new Concept<>("CriticalDisease");

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential<>(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential<>(partOf, heart) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential<>(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential<>(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf },
                partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn },
                hasLoc);

        // Partial ontology
        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a5);
        axioms.add(a6);
        axioms.add(a7);
        axioms.add(a8);
        axioms.add(a9);
        axioms.add(a10);
        axioms.add(a11);

        NormalisedOntology<String> o = new NormalisedOntology<>(factory, axioms);
        o.classify();
        IConceptMap<IConceptSet> s = o.getSubsumptions();
        PostProcessedData<String> ppd = new PostProcessedData<>(factory);
        ppd.computeDag(s, false, null);

        // Add delta axioms and classify incrementally
        Concept<String> endocardium = new Concept<>("Endocardium");
        Concept<String> endocarditis = new Concept<>("Endocarditis");

        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential<>(contIn, heartWall),
                        new Existential<>(contIn, heartValve) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential<>(hasLoc, endocardium) }));

        Set<IAxiom> incAxioms = new HashSet<>();
        incAxioms.add(a1);
        incAxioms.add(a4);

        o.classifyIncremental(incAxioms);
        IConceptMap<IConceptSet> ns = o.getNewSubsumptions();
        IConceptMap<IConceptSet> as = o.getAffectedSubsumptions();
        ppd.computeDagIncremental(ns, as, false, null);

        // Test results
        ClassNode bottomNode = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        Set<ClassNode> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(ppd.getEquivalents(heart.getId())));

        ClassNode endocarditisNode = ppd.getEquivalents(endocarditis.getId());
        Set<ClassNode> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(inflammation
                .getId())));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(heartdisease
                .getId())));
        assertTrue(endocarditisRes.contains(ppd.getEquivalents(criticalDisease
                .getId())));

        ClassNode inflammationNode = ppd.getEquivalents(inflammation.getId());
        Set<ClassNode> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes
                .contains(ppd.getEquivalents(disease.getId())));

        ClassNode endocardiumNode = ppd.getEquivalents(endocardium.getId());
        Set<ClassNode> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ppd.getEquivalents(tissue.getId())));

        ClassNode heartdiseaseNode = ppd.getEquivalents(heartdisease.getId());
        Set<ClassNode> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes
                .contains(ppd.getEquivalents(disease.getId())));

        ClassNode heartWallNode = ppd.getEquivalents(heartWall.getId());
        Set<ClassNode> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ppd.getEquivalents(bodyWall.getId())));

        ClassNode heartValveNode = ppd.getEquivalents(heartValve.getId());
        Set<ClassNode> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(ppd.getEquivalents(bodyValve.getId())));

        ClassNode diseaseNode = ppd.getEquivalents(disease.getId());
        Set<ClassNode> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes
                .contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode tissueNode = ppd.getEquivalents(tissue.getId());
        Set<ClassNode> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode heartNode = ppd.getEquivalents(heart.getId());
        Set<ClassNode> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ppd.getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyValveNode = ppd.getEquivalents(bodyValve.getId());
        Set<ClassNode> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode bodyWallNode = ppd.getEquivalents(bodyWall.getId());
        Set<ClassNode> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));

        ClassNode criticalDiseaseNode = ppd.getEquivalents(criticalDisease
                .getId());
        Set<ClassNode> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ppd
                .getEquivalents(IFactory.TOP_CONCEPT)));
    }
    
}
