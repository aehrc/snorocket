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

import au.csiro.ontology.Node;
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
        Role<String> contIn = new Role<String>("cont-in");
        Role<String> partOf = new Role<String>("part-of");
        Role<String> hasLoc = new Role<String>("has-loc");
        Role<String> actsOn = new Role<String>("acts-on");

        // Create concepts
        Concept<String> endocardium = new Concept<String>("Endocardium");
        Concept<String> tissue = new Concept<String>("Tissue");
        Concept<String> heartWall = new Concept<String>("HeartWall");
        Concept<String> heartValve = new Concept<String>("HeartValve");
        Concept<String> bodyWall = new Concept<String>("BodyWall");
        Concept<String> heart = new Concept<String>("Heart");
        Concept<String> bodyValve = new Concept<String>("BodyValve");
        Concept<String> endocarditis = new Concept<String>("Endocarditis");
        Concept<String> inflammation = new Concept<String>("Inflammation");
        Concept<String> disease = new Concept<String>("Disease");
        Concept<String> heartdisease = new Concept<String>("Heartdisease");
        Concept<String> criticalDisease = new Concept<String>("CriticalDisease");

        // Create axioms
        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential<String>(contIn, heartWall),
                        new Existential<String>(contIn, heartValve) }));

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential<String>(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential<String>(partOf, heart) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential<String>(hasLoc, endocardium) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential<String>(hasLoc, heartValve) }), 
                        criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf },
                partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn },
                hasLoc);

        Set<IAxiom> axioms = new HashSet<IAxiom>();
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
        IFactory<String> factory = new CoreFactory<String>();
        NormalisedOntology<String> o = 
                new NormalisedOntology<String>(factory, axioms);
        
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

        // Build taxonomy
        o.buildTaxonomy();

        // Test results
        Node<String> bottomNode = o.getBottomNode();
        Set<Node<String>> bottomRes = bottomNode.getParents();

        //assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(o.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heart.getId())));

        Node<String> endocarditisNode = o.getEquivalents(endocarditis.getId());
        Set<Node<String>> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(o.getEquivalents(inflammation
                .getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(heartdisease
                .getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(criticalDisease
                .getId())));

        Node<String> inflammationNode = o.getEquivalents(inflammation.getId());
        Set<Node<String>> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes
                .contains(o.getEquivalents(disease.getId())));

        Node<String> endocardiumNode = o.getEquivalents(endocardium.getId());
        Set<Node<String>> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(o.getEquivalents(tissue.getId())));

        Node<String> heartdiseaseNode = o.getEquivalents(heartdisease.getId());
        Set<Node<String>> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes
                .contains(o.getEquivalents(disease.getId())));

        Node<String> heartWallNode = o.getEquivalents(heartWall.getId());
        Set<Node<String>> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(o.getEquivalents(bodyWall.getId())));

        Node<String> heartValveNode = o.getEquivalents(heartValve.getId());
        Set<Node<String>> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(o.getEquivalents(bodyValve.getId())));

        Node<String> diseaseNode = o.getEquivalents(disease.getId());
        Set<Node<String>> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(o.getTopNode()));

        Node<String> tissueNode = o.getEquivalents(tissue.getId());
        Set<Node<String>> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(o.getTopNode()));

        Node<String> heartNode = o.getEquivalents(heart.getId());
        Set<Node<String>> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(o.getTopNode()));

        Node<String> bodyValveNode = o.getEquivalents(bodyValve.getId());
        Set<Node<String>> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(o.getTopNode()));

        Node<String> bodyWallNode = o.getEquivalents(bodyWall.getId());
        Set<Node<String>> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(o.getTopNode()));

        Node<String> criticalDiseaseNode = o.getEquivalents(criticalDisease
                .getId());
        Set<Node<String>> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(o.getTopNode()));
    }

    @Test
    public void testNormalise() {
        IFactory<String> factory = new CoreFactory<String>();

        // Add roles
        Role<String> container = new Role<String>("container");
        Role<String> contains = new Role<String>("contains");

        // Add features
        Feature<String> mgPerTablet = new Feature<String>("mgPerTablet");

        // Add concepts
        Concept<String> panadol = new Concept<String>("Panadol");
        Concept<String> panadol_250mg = new Concept<String>("Panadol_250mg");
        Concept<String> panadol_500mg = new Concept<String>("Panadol_500mg");
        Concept<String> panadol_pack_250mg = 
                new Concept<String>("Panadol_pack_250mg");
        Concept<String> paracetamol = new Concept<String>("Paracetamol");
        Concept<String> bottle = new Concept<String>("Bottle");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(panadol, 
                new Existential<String>(
                contains, paracetamol));

        ConceptInclusion a2 = new ConceptInclusion(panadol_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<String>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)) }));

        ConceptInclusion a3 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<String>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)) }), panadol_250mg);

        ConceptInclusion a4 = new ConceptInclusion(panadol_500mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<String>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(500)) }));

        ConceptInclusion a5 = new ConceptInclusion(new Conjunction(
                new IConcept[] {
                        panadol,
                        new Datatype<String>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(500)) }), panadol_500mg);

        ConceptInclusion a6 = new ConceptInclusion(panadol_pack_250mg,
                new Conjunction(new IConcept[] {
                        panadol,
                        new Datatype<String>(mgPerTablet, Operator.EQUALS,
                                new IntegerLiteral(250)),
                        new Existential<String>(container, bottle) }));

        Set<IAxiom> axioms = new HashSet<IAxiom>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
        axioms.add(a6);

        NormalisedOntology<String> no = new NormalisedOntology<String>(factory);
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
        IFactory<String> factory = new CoreFactory<String>();

        // Original Endocarditis ontology axioms
        Role<String> contIn = new Role<String>("cont-in");
        Role<String> partOf = new Role<String>("part-of");
        Role<String> hasLoc = new Role<String>("has-loc");
        Role<String> actsOn = new Role<String>("acts-on");
        Concept<String> tissue = new Concept<String>("Tissue");
        Concept<String> heartWall = new Concept<String>("HeartWall");
        Concept<String> heartValve = new Concept<String>("HeartValve");
        Concept<String> bodyWall = new Concept<String>("BodyWall");
        Concept<String> heart = new Concept<String>("Heart");
        Concept<String> bodyValve = new Concept<String>("BodyValve");
        Concept<String> inflammation = new Concept<String>("Inflammation");
        Concept<String> disease = new Concept<String>("Disease");
        Concept<String> heartdisease = new Concept<String>("Heartdisease");
        Concept<String> criticalDisease = new Concept<String>("CriticalDisease");

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential<String>(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential<String>(partOf, heart) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential<String>(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential<String>(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf },
                partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn },
                hasLoc);

        // Partial ontology
        Set<IAxiom> axioms = new HashSet<IAxiom>();
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a5);
        axioms.add(a6);
        axioms.add(a7);
        axioms.add(a8);
        axioms.add(a9);
        axioms.add(a10);
        axioms.add(a11);

        NormalisedOntology<String> o = 
                new NormalisedOntology<String>(factory, axioms);
        o.classify();
        o.buildTaxonomy();
        
        // TODO: remove
        
        Node<String> bn = o.getBottomNode();
        Set<Node<String>> br = bn.getParents();
        System.out.println(br);
        
        // TODO: end remove

        // Add delta axioms and classify incrementally
        Concept<String> endocardium = new Concept<String>("Endocardium");
        Concept<String> endocarditis = new Concept<String>("Endocarditis");

        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential<String>(contIn, heartWall),
                        new Existential<String>(contIn, heartValve) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential<String>(hasLoc, endocardium) }));

        Set<IAxiom> incAxioms = new HashSet<IAxiom>();
        incAxioms.add(a1);
        incAxioms.add(a4);
        
        o.loadIncremental(incAxioms);
        o.classifyIncremental();
        o.buildTaxonomy();

        // Test results
        Node<String> bottomNode = o.getBottomNode();
        Set<Node<String>> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(o.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heart.getId())));

        Node<String> endocarditisNode = o.getEquivalents(endocarditis.getId());
        Set<Node<String>> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(o.getEquivalents(inflammation
                .getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(heartdisease
                .getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(criticalDisease
                .getId())));

        Node<String> inflammationNode = o.getEquivalents(inflammation.getId());
        Set<Node<String>> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes
                .contains(o.getEquivalents(disease.getId())));

        Node<String> endocardiumNode = o.getEquivalents(endocardium.getId());
        Set<Node<String>> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(o.getEquivalents(tissue.getId())));

        Node<String> heartdiseaseNode = o.getEquivalents(heartdisease.getId());
        Set<Node<String>> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(o.getEquivalents(disease.getId())));

        Node<String> heartWallNode = o.getEquivalents(heartWall.getId());
        Set<Node<String>> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(o.getEquivalents(bodyWall.getId())));

        Node<String> heartValveNode = o.getEquivalents(heartValve.getId());
        Set<Node<String>> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(o.getEquivalents(bodyValve.getId())));

        Node<String> diseaseNode = o.getEquivalents(disease.getId());
        Set<Node<String>> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(o.getTopNode()));

        Node<String> tissueNode = o.getEquivalents(tissue.getId());
        Set<Node<String>> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(o.getTopNode()));

        Node<String> heartNode = o.getEquivalents(heart.getId());
        Set<Node<String>> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(o.getTopNode()));

        Node<String> bodyValveNode = o.getEquivalents(bodyValve.getId());
        Set<Node<String>> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(o.getTopNode()));

        Node<String> bodyWallNode = o.getEquivalents(bodyWall.getId());
        Set<Node<String>> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(o.getTopNode()));

        Node<String> criticalDiseaseNode = o.getEquivalents(criticalDisease
                .getId());
        Set<Node<String>> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(o.getTopNode()));
    }
    
}
