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
        NamedRole contIn = new NamedRole("cont-in");
        NamedRole partOf = new NamedRole("part-of");
        NamedRole hasLoc = new NamedRole("has-loc");
        NamedRole actsOn = new NamedRole("acts-on");

        // Create concepts
        NamedConcept endocardium = new NamedConcept("Endocardium");
        NamedConcept tissue = new NamedConcept("Tissue");
        NamedConcept heartWall = new NamedConcept("HeartWall");
        NamedConcept heartValve = new NamedConcept("HeartValve");
        NamedConcept bodyWall = new NamedConcept("BodyWall");
        NamedConcept heart = new NamedConcept("Heart");
        NamedConcept bodyValve = new NamedConcept("BodyValve");
        NamedConcept endocarditis = new NamedConcept("Endocarditis");
        NamedConcept inflammation = new NamedConcept("Inflammation");
        NamedConcept disease = new NamedConcept("Disease");
        NamedConcept heartdisease = new NamedConcept("Heartdisease");
        NamedConcept criticalDisease = new NamedConcept("CriticalDisease");

        // Create axioms
        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new Concept[] { tissue,
                        new Existential(contIn, heartWall),
                        new Existential(contIn, heartValve) }));

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new Concept[] { bodyWall, new Existential(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new Concept[] { bodyValve, new Existential(partOf, heart) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new Concept[] { inflammation, new Existential(hasLoc, endocardium) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new Concept[] { disease, new Existential(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(
                new Conjunction(new Concept[] { heartdisease, new Existential(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new Concept[] { disease, new Existential(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new Concept[] { disease, new Existential(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf }, partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn }, hasLoc);

        Set<Axiom> axioms = new HashSet<Axiom>();
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
        IFactory factory = new CoreFactory();
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        
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
        Node bottomNode = o.getBottomNode();
        Set<Node> bottomRes = bottomNode.getParents();

        //assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(o.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heart.getId())));

        Node endocarditisNode = o.getEquivalents(endocarditis.getId());
        Set<Node> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(o.getEquivalents(inflammation.getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(criticalDisease.getId())));

        Node inflammationNode = o.getEquivalents(inflammation.getId());
        Set<Node> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(o.getEquivalents(disease.getId())));

        Node endocardiumNode = o.getEquivalents(endocardium.getId());
        Set<Node> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(o.getEquivalents(tissue.getId())));

        Node heartdiseaseNode = o.getEquivalents(heartdisease.getId());
        Set<Node> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(o.getEquivalents(disease.getId())));

        Node heartWallNode = o.getEquivalents(heartWall.getId());
        Set<Node> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(o.getEquivalents(bodyWall.getId())));

        Node heartValveNode = o.getEquivalents(heartValve.getId());
        Set<Node> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(o.getEquivalents(bodyValve.getId())));

        Node diseaseNode = o.getEquivalents(disease.getId());
        Set<Node> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(o.getTopNode()));

        Node tissueNode = o.getEquivalents(tissue.getId());
        Set<Node> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(o.getTopNode()));

        Node heartNode = o.getEquivalents(heart.getId());
        Set<Node> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(o.getTopNode()));

        Node bodyValveNode = o.getEquivalents(bodyValve.getId());
        Set<Node> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(o.getTopNode()));

        Node bodyWallNode = o.getEquivalents(bodyWall.getId());
        Set<Node> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(o.getTopNode()));

        Node criticalDiseaseNode = o.getEquivalents(criticalDisease.getId());
        Set<Node> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(o.getTopNode()));
    }

    @Test
    public void testNormalise() {
        IFactory factory = new CoreFactory();

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
        IFactory factory = new CoreFactory();

        // Original Endocarditis ontology axioms
        NamedRole contIn = new NamedRole("cont-in");
        NamedRole partOf = new NamedRole("part-of");
        NamedRole hasLoc = new NamedRole("has-loc");
        NamedRole actsOn = new NamedRole("acts-on");
        NamedConcept tissue = new NamedConcept("Tissue");
        NamedConcept heartWall = new NamedConcept("HeartWall");
        NamedConcept heartValve = new NamedConcept("HeartValve");
        NamedConcept bodyWall = new NamedConcept("BodyWall");
        NamedConcept heart = new NamedConcept("Heart");
        NamedConcept bodyValve = new NamedConcept("BodyValve");
        NamedConcept inflammation = new NamedConcept("Inflammation");
        NamedConcept disease = new NamedConcept("Disease");
        NamedConcept heartdisease = new NamedConcept("Heartdisease");
        NamedConcept criticalDisease = new NamedConcept("CriticalDisease");

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new Concept[] { bodyWall, new Existential(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new Concept[] { bodyValve, new Existential(partOf, heart) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new Concept[] { disease, new Existential(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new Concept[] { heartdisease, new Existential(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new Concept[] { disease, new Existential(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new Concept[] { disease, new Existential(hasLoc, heart) }), heartdisease);

        RoleInclusion a9 = new RoleInclusion(new Role[] { partOf, partOf }, partOf);
        RoleInclusion a10 = new RoleInclusion(partOf, contIn);
        RoleInclusion a11 = new RoleInclusion(new Role[] { hasLoc, contIn }, hasLoc);

        // Partial ontology
        Set<Axiom> axioms = new HashSet<Axiom>();
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
        o.buildTaxonomy();

        // Add delta axioms and classify incrementally
        NamedConcept endocardium = new NamedConcept("Endocardium");
        NamedConcept endocarditis = new NamedConcept("Endocarditis");

        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new Concept[] { tissue,
                        new Existential(contIn, heartWall),
                        new Existential(contIn, heartValve) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new Concept[] { inflammation, new Existential(hasLoc, endocardium) }));

        Set<Axiom> incAxioms = new HashSet<Axiom>();
        incAxioms.add(a1);
        incAxioms.add(a4);
        
        o.loadIncremental(incAxioms);
        o.classifyIncremental();
        o.buildTaxonomy();

        // Test results
        Node bottomNode = o.getBottomNode();
        Set<Node> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(o.getEquivalents(endocardium.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(endocarditis.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartWall.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heartValve.getId())));
        assertTrue(bottomRes.contains(o.getEquivalents(heart.getId())));

        Node endocarditisNode = o.getEquivalents(endocarditis.getId());
        Set<Node> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(o.getEquivalents(inflammation.getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(o.getEquivalents(criticalDisease.getId())));

        Node inflammationNode = o.getEquivalents(inflammation.getId());
        Set<Node> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(o.getEquivalents(disease.getId())));

        Node endocardiumNode = o.getEquivalents(endocardium.getId());
        Set<Node> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(o.getEquivalents(tissue.getId())));

        Node heartdiseaseNode = o.getEquivalents(heartdisease.getId());
        Set<Node> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(o.getEquivalents(disease.getId())));

        Node heartWallNode = o.getEquivalents(heartWall.getId());
        Set<Node> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(o.getEquivalents(bodyWall.getId())));

        Node heartValveNode = o.getEquivalents(heartValve.getId());
        Set<Node> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(o.getEquivalents(bodyValve.getId())));

        Node diseaseNode = o.getEquivalents(disease.getId());
        Set<Node> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(o.getTopNode()));

        Node tissueNode = o.getEquivalents(tissue.getId());
        Set<Node> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(o.getTopNode()));

        Node heartNode = o.getEquivalents(heart.getId());
        Set<Node> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(o.getTopNode()));

        Node bodyValveNode = o.getEquivalents(bodyValve.getId());
        Set<Node> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(o.getTopNode()));

        Node bodyWallNode = o.getEquivalents(bodyWall.getId());
        Set<Node> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(o.getTopNode()));

        Node criticalDiseaseNode = o.getEquivalents(criticalDisease.getId());
        Set<Node> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(o.getTopNode()));
    }
    
}
