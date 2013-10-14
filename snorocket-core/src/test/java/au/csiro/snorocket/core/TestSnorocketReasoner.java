/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Assert;

import org.junit.Test;

import au.csiro.ontology.Factory;
import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.NamedConcept;
import au.csiro.ontology.model.NamedRole;
import au.csiro.ontology.model.RoleInclusion;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.util.Utils;

/**
 * @author Alejandro Metke
 *
 */
public class TestSnorocketReasoner {
    
    /**
     * 
     */
    @Test
    public void testSave() {

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
        
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.loadAxioms(axioms);
        sr.classify();
        
        try {
            // Save to temp file
            File temp = File.createTempFile("temp",".ser");
            temp.deleteOnExit();
            sr.save(new FileOutputStream(temp));
            
            sr = null;
            sr = SnorocketReasoner.load(new FileInputStream(temp));
        } catch(Exception ex) {
            ex.printStackTrace();
            assertTrue(false);
        }

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
        
        sr.loadAxioms(incAxioms);
        sr.classify();

        // Test results
        Ontology ont = sr.getClassifiedOntology();
        
        Node bottom = ont.getBottomNode();
        Set<Node> bottomRes = bottom.getParents();
        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ont.getNode(endocardium.getId())));
        assertTrue(bottomRes.contains(ont.getNode(endocarditis.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartWall.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartValve.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heart.getId())));

        Node endocarditisNode = ont.getNode(endocarditis.getId());
        Set<Node> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ont.getNode(inflammation.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(criticalDisease.getId())));

        Node inflammationNode = ont.getNode(inflammation.getId());
        Set<Node> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ont.getNode(disease.getId())));

        Node endocardiumNode = ont.getNode(endocardium.getId());
        Set<Node> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ont.getNode(tissue.getId())));

        Node heartdiseaseNode = ont.getNode(heartdisease.getId());
        Set<Node> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ont.getNode(disease.getId())));

        Node heartWallNode = ont.getNode(heartWall.getId());
        Set<Node> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ont.getNode(bodyWall.getId())));

        Node heartValveNode = ont.getNode(heartValve.getId());
        Set<Node> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(ont.getNode(bodyValve.getId())));

        Node diseaseNode = ont.getNode(disease.getId());
        Set<Node> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(ont.getTopNode()));

        Node tissueNode = ont.getNode(tissue.getId());
        Set<Node> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ont.getTopNode()));

        Node heartNode = ont.getNode(heart.getId());
        Set<Node> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ont.getTopNode()));

        Node bodyValveNode = ont.getNode(bodyValve.getId());
        Set<Node> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ont.getTopNode()));

        Node bodyWallNode = ont.getNode(bodyWall.getId());
        Set<Node> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ont.getTopNode()));

        Node criticalDiseaseNode = ont.getNode(criticalDisease.getId());
        Set<Node> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ont.getTopNode()));
    }
    
    @Test
    public void testNesting() {
        NamedRole rg = new NamedRole("RoleGroup");
        NamedRole fs = new NamedRole("site");
        NamedRole am = new NamedRole("morph");
        NamedRole lat = new NamedRole("lat");
        
        NamedConcept finding = new NamedConcept("Finding");
        NamedConcept fracfind = new NamedConcept("FractureFinding");
        NamedConcept limb = new NamedConcept("Limb");
        NamedConcept arm = new NamedConcept("Arm");
        NamedConcept left = new NamedConcept("Left");
        NamedConcept fracture = new NamedConcept("Fracture");
        NamedConcept burn = new NamedConcept("Burn");
        NamedConcept right = new NamedConcept("Right");
        NamedConcept multi = new NamedConcept("Multiple");
        
        Concept[] larm = {
                arm, new Existential(lat, left)
        };
        Concept[] rarm = {
                arm, new Existential(lat, right)
        };
        Concept[] g1 = {
                new Existential(fs, new Conjunction(rarm)),
                new Existential(fs, arm),
                new Existential(am, fracture),
        };
        Concept[] g2 = {
                new Existential(fs, new Conjunction(larm)),
                new Existential(am, burn),
        };
        Concept[] rhs = {
                finding,
                new Existential(rg, new Conjunction(g1)),
                new Existential(rg, new Conjunction(g2)),
        };
        Concept[] rhs2 = {
                finding,
                new Existential(rg, new Existential(am, fracture)),
        };
        Axiom[] inclusions = {
                new ConceptInclusion(multi, new Conjunction(rhs)),
                new ConceptInclusion(arm, limb),
                new ConceptInclusion(fracfind, new Conjunction(rhs2)),
                new ConceptInclusion(new Conjunction(rhs2), fracfind),
        };
        
        Set<Axiom> axioms = new HashSet<Axiom>();
        for (Axiom a : inclusions) {
            axioms.add(a);
        }

        // Classify
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.loadAxioms(axioms);
        sr.classify();
        
        Ontology ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        try {
            for (Axiom a: axioms) {
                System.out.println("Stated: " + a);
            }
            for (Axiom a: sr.getInferredAxioms()) {
                System.out.println("Axiom:  " + a);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    @Test
    public void testEndocarditis() {
        org.apache.log4j.LogManager.getRootLogger().setLevel((org.apache.log4j.Level)org.apache.log4j.Level.TRACE);
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
                new Concept[] { bodyWall,
                        new Existential(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new Concept[] { bodyValve,
                        new Existential(partOf, heart) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new Concept[] { inflammation,
                        new Existential(hasLoc, endocardium) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new Concept[] { disease,
                        new Existential(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new Concept[] { heartdisease,
                        new Existential(hasLoc, heartValve) }), 
                        criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new Concept[] { disease,
                        new Existential(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new Concept[] { disease,
                        new Existential(hasLoc, heart) }), 
                        heartdisease);

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
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.loadAxioms(axioms);
        sr.classify();
        
        Ontology ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        // Test taxonomy results
        Node bottomNode = ont.getBottomNode();
        Set<Node> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ont.getNode(endocardium.getId())));
        assertTrue(bottomRes.contains(ont.getNode(endocarditis.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartWall.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartValve.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heart.getId())));

        Node endocarditisNode = ont.getNode(endocarditis.getId());
        Set<Node> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ont.getNode(inflammation.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(criticalDisease.getId())));

        Node inflammationNode = ont.getNode(inflammation.getId());
        Set<Node> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ont.getNode(disease.getId())));

        Node endocardiumNode = ont.getNode(endocardium.getId());
        Set<Node> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ont.getNode(tissue.getId())));

        Node heartdiseaseNode = ont.getNode(heartdisease.getId());
        Set<Node> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ont.getNode(disease.getId())));

        Node heartWallNode = ont.getNode(heartWall.getId());
        Set<Node> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ont.getNode(bodyWall.getId())));

        Node heartValveNode = ont.getNode(heartValve.getId());
        Set<Node> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(ont.getNode(bodyValve.getId())));

        Node diseaseNode = ont.getNode(disease.getId());
        Set<Node> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(ont.getTopNode()));

        Node tissueNode = ont.getNode(tissue.getId());
        Set<Node> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ont.getTopNode()));

        Node heartNode = ont.getNode(heart.getId());
        Set<Node> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ont.getTopNode()));

        Node bodyValveNode = ont.getNode(bodyValve.getId());
        Set<Node> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ont.getTopNode()));

        Node bodyWallNode = ont.getNode(bodyWall.getId());
        Set<Node> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ont.getTopNode()));

        Node criticalDiseaseNode = ont.getNode(criticalDisease.getId());
        Set<Node> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ont.getTopNode()));
        
        try {
            for (Axiom a: sr.getInferredAxioms()) {
                System.out.println("Axiom: " + a);
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * Tests the identification of possibly affected concepts after an
     * incremental taxonomy calculation.
     */
    @Test
    public void testIncrementalTaxonomy() {
    	
    	Concept a = Factory.createNamedConcept("A");
    	Concept b = Factory.createNamedConcept("B");
    	Concept c = Factory.createNamedConcept("C");
    	Concept d = Factory.createNamedConcept("D");
    	Concept e = Factory.createNamedConcept("E");
    	Concept f = Factory.createNamedConcept("F");
    	Concept g = Factory.createNamedConcept("G");
    	
    	Axiom a1 = Factory.createConceptInclusion(b, a);
    	Axiom a2 = Factory.createConceptInclusion(c, b);
    	Axiom a3 = Factory.createConceptInclusion(d, c);
    	Axiom a4 = Factory.createConceptInclusion(e, a);
    	Axiom a5 = Factory.createConceptInclusion(f, e);
    	
    	Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
    	
    	SnorocketReasoner sr = new SnorocketReasoner();
    	sr.loadAxioms(axioms);
        sr.classify();
        
        Ontology ont = sr.getClassifiedOntology();
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        Axiom a6 = Factory.createConceptInclusion(g, e);
        Axiom a7 = Factory.createConceptInclusion(f, g);
        
        axioms.clear();
        axioms.add(a6);
        axioms.add(a7);
        
        sr.loadAxioms(axioms);
        sr.classify();
        ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        Set<Node> affectedNodes = ont.getAffectedNodes();
        Set<String> affectedIds = new HashSet<String>();
        for(Node affectedNode : affectedNodes) {
        	affectedIds.addAll(affectedNode.getEquivalentConcepts());
        }
        
        System.out.println("Affected node ids: "+affectedIds);
        
        Assert.assertTrue("Node G was not found in affected nodes", affectedIds.contains("G"));
        
        Assert.assertTrue("Node F was not found in affected nodes", affectedIds.contains("F"));
    }
    
    @Test
    public void testBottom() {
        IFactory factory = new CoreFactory();

        // Add concepts
        NamedConcept a = new NamedConcept("A");
        NamedConcept b = new NamedConcept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, NamedConcept.BOTTOM_CONCEPT);
        ConceptInclusion a2 = new ConceptInclusion(b, NamedConcept.TOP_CONCEPT);

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);
        axioms.add(a2);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        
        // Build taxonomy
        o.buildTaxonomy();

        // Test results
        Node bNode = o.getEquivalents(b.getId());
        Set<Node> bParents = bNode.getParents();
        assertTrue(bParents.size() == 1);
        assertTrue(bParents.contains(o.getTopNode()));

        Node bottomNode = o.getBottomNode();
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        bottomNode.getEquivalentConcepts().contains(a.getId());
        Set<Node> bottomParents = bottomNode.getParents();
        assertTrue(bottomParents.size() == 1);
        assertTrue(bottomParents.contains(o.getEquivalents(b.getId())));
    }
    
    @Test
    public void testBottom2() {
        IFactory factory = new CoreFactory();

        // Add concepts
        NamedConcept a = new NamedConcept("A");
        NamedConcept b = new NamedConcept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(a, NamedConcept.BOTTOM_CONCEPT);

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);
        axioms.add(a2);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        
        // Build taxonomy
        o.buildTaxonomy();

        // Test results
        Node bNode = o.getEquivalents(b.getId());
        Set<Node> bParents = bNode.getParents();
        assertTrue(bParents.size() == 1);
        assertTrue(bParents.contains(o.getTopNode()));

        Node bottomNode = o.getBottomNode();
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        bottomNode.getEquivalentConcepts().contains(a.getId());
        Set<Node> bottomParents = bottomNode.getParents();
        assertTrue(bottomParents.size() == 1);
        assertTrue(bottomParents.contains(o.getEquivalents(b.getId())));
    }
    
    @Test
    public void testBottomIncremental() {
        IFactory factory = new CoreFactory();

        // Add concepts
        NamedConcept a = new NamedConcept("A");
        NamedConcept b = new NamedConcept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(a, NamedConcept.BOTTOM_CONCEPT);

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        
        // Build taxonomy
        o.buildTaxonomy();
        
        axioms.clear();
        axioms.add(a2);
        o.loadIncremental(axioms);
        o.classifyIncremental();
        o.buildTaxonomy();

        // Test results
        Node bNode = o.getEquivalents(b.getId());
        Set<Node> bParents = bNode.getParents();
        assertTrue(bParents.size() == 1);
        assertTrue(bParents.contains(o.getTopNode()));

        Node bottomNode = o.getBottomNode();
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        bottomNode.getEquivalentConcepts().contains(a.getId());
        Set<Node> bottomParents = bottomNode.getParents();
        assertTrue(bottomParents.size() == 1);
        assertTrue(bottomParents.contains(o.getEquivalents(b.getId())));
    }

    @Test
    public void testBottomIncremental2() {
        IFactory factory = new CoreFactory();

        // Add concepts
        NamedConcept a = new NamedConcept("A");
        NamedConcept b = new NamedConcept("B");
        NamedConcept c = new NamedConcept("C");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(c, b);
        ConceptInclusion a3 = new ConceptInclusion(c, NamedConcept.BOTTOM_CONCEPT);

        Set<Axiom> axioms = new HashSet<Axiom>();
        axioms.add(a1);

        // Classify
        NormalisedOntology o = new NormalisedOntology(factory, axioms);
        o.classify();
        
        // Build taxonomy
        o.buildTaxonomy();
        
        axioms.clear();
        axioms.add(a2);
        axioms.add(a3);
        o.loadIncremental(axioms);
        o.classifyIncremental();
        o.buildTaxonomy();

        // Test results
        Node bNode = o.getEquivalents(b.getId());
        Set<Node> bParents = bNode.getParents();
        assertTrue(bParents.size() == 1);
        assertTrue(bParents.contains(o.getTopNode()));
        Set<Node> bChildren = bNode.getChildren();
        assertTrue(bChildren.size() == 1);
        assertTrue(bChildren.contains(o.getEquivalents(a.getId())));
        
        Node bottomNode = o.getBottomNode();
        Node aNode = o.getEquivalents(a.getId());
        Set<Node> aParents = aNode.getParents();
        assertTrue(aParents.size() == 1);
        assertTrue(aParents.contains(bNode));
        Set<Node> aChildren = aNode.getChildren();
        assertTrue(aChildren.size() == 1);
        assertTrue(aChildren.contains(o.getBottomNode()));
        assertTrue(bottomNode.getEquivalentConcepts().size() == 2);
        assertTrue(bottomNode.getEquivalentConcepts().contains(c.getId()));
        Set<Node> bottomParents = bottomNode.getParents();
        assertTrue(bottomParents.size() == 1);
        assertTrue(bottomParents.contains(aNode));
    }
    
}
