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
import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.axioms.RoleInclusion;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.IConcept;
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
        Role contIn = new Role("cont-in");
        Role partOf = new Role("part-of");
        Role hasLoc = new Role("has-loc");
        Role actsOn = new Role("acts-on");
        Concept tissue = new Concept("Tissue");
        Concept heartWall = new Concept("HeartWall");
        Concept heartValve = new Concept("HeartValve");
        Concept bodyWall = new Concept("BodyWall");
        Concept heart = new Concept("Heart");
        Concept bodyValve = new Concept("BodyValve");
        Concept inflammation = new Concept("Inflammation");
        Concept disease = new Concept("Disease");
        Concept heartdisease = new Concept("Heartdisease");
        Concept criticalDisease = new Concept("CriticalDisease");

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential(partOf, heart) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential(hasLoc, heartValve) }), criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential(hasLoc, heart) }), heartdisease);

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
        
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.classify(axioms);
        
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
        Concept endocardium = new Concept("Endocardium");
        Concept endocarditis = new Concept("Endocarditis");

        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential(contIn, heartWall),
                        new Existential(contIn, heartValve) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential(hasLoc, endocardium) }));

        Set<IAxiom> incAxioms = new HashSet<IAxiom>();
        incAxioms.add(a1);
        incAxioms.add(a4);

        sr.classify(incAxioms);

        // Test results
        IOntology ont = sr.getClassifiedOntology();
        
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
        Role rg = new Role("RoleGroup");
        Role fs = new Role("site");
        Role am = new Role("morph");
        Role lat = new Role("lat");
        
        Concept finding = new Concept("Finding");
        Concept fracfind = new Concept("FractureFinding");
        Concept limb = new Concept("Limb");
        Concept arm = new Concept("Arm");
        Concept left = new Concept("Left");
        Concept fracture = new Concept("Fracture");
        Concept burn = new Concept("Burn");
        Concept right = new Concept("Right");
        Concept multi = new Concept("Multiple");
        
        IConcept[] larm = {
                arm, new Existential(lat, left)
        };
        IConcept[] rarm = {
                arm, new Existential(lat, right)
        };
        IConcept[] g1 = {
                new Existential(fs, new Conjunction(rarm)),
                new Existential(fs, arm),
                new Existential(am, fracture),
        };
        IConcept[] g2 = {
                new Existential(fs, new Conjunction(larm)),
                new Existential(am, burn),
        };
        IConcept[] rhs = {
                finding,
                new Existential(rg, new Conjunction(g1)),
                new Existential(rg, new Conjunction(g2)),
        };
        IConcept[] rhs2 = {
                finding,
                new Existential(rg, new Existential(am, fracture)),
        };
        IAxiom[] inclusions = {
                new ConceptInclusion(multi, new Conjunction(rhs)),
                new ConceptInclusion(arm, limb),
                new ConceptInclusion(fracfind, new Conjunction(rhs2)),
                new ConceptInclusion(new Conjunction(rhs2), fracfind),
        };
        
        Set<IAxiom> axioms = new HashSet<IAxiom>();
        for (IAxiom a : inclusions) {
            axioms.add(a);
        }

        // Classify
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.classify(axioms);
        
        IOntology ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        try {
            for (IAxiom a: axioms) {
                System.out.println("Stated: " + a);
            }
            for (IAxiom a: sr.getInferredAxioms()) {
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
        Role contIn = new Role("cont-in");
        Role partOf = new Role("part-of");
        Role hasLoc = new Role("has-loc");
        Role actsOn = new Role("acts-on");

        // Create concepts
        Concept endocardium = new Concept("Endocardium");
        Concept tissue = new Concept("Tissue");
        Concept heartWall = new Concept("HeartWall");
        Concept heartValve = new Concept("HeartValve");
        Concept bodyWall = new Concept("BodyWall");
        Concept heart = new Concept("Heart");
        Concept bodyValve = new Concept("BodyValve");
        Concept endocarditis = new Concept("Endocarditis");
        Concept inflammation = new Concept("Inflammation");
        Concept disease = new Concept("Disease");
        Concept heartdisease = new Concept("Heartdisease");
        Concept criticalDisease = new Concept("CriticalDisease");

        // Create axioms
        ConceptInclusion a1 = new ConceptInclusion(endocardium,
                new Conjunction(new IConcept[] { tissue,
                        new Existential(contIn, heartWall),
                        new Existential(contIn, heartValve) }));

        ConceptInclusion a2 = new ConceptInclusion(heartWall, new Conjunction(
                new IConcept[] { bodyWall,
                        new Existential(partOf, heart) }));

        ConceptInclusion a3 = new ConceptInclusion(heartValve, new Conjunction(
                new IConcept[] { bodyValve,
                        new Existential(partOf, heart) }));

        ConceptInclusion a4 = new ConceptInclusion(endocarditis,
                new Conjunction(new IConcept[] { inflammation,
                        new Existential(hasLoc, endocardium) }));

        ConceptInclusion a5 = new ConceptInclusion(inflammation,
                new Conjunction(new IConcept[] { disease,
                        new Existential(actsOn, tissue) }));

        ConceptInclusion a6 = new ConceptInclusion(new Conjunction(
                new IConcept[] { heartdisease,
                        new Existential(hasLoc, heartValve) }), 
                        criticalDisease);

        ConceptInclusion a7 = new ConceptInclusion(heartdisease,
                new Conjunction(new IConcept[] { disease,
                        new Existential(hasLoc, heart) }));

        ConceptInclusion a8 = new ConceptInclusion(
                new Conjunction(new IConcept[] { disease,
                        new Existential(hasLoc, heart) }), 
                        heartdisease);

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
        SnorocketReasoner sr = new SnorocketReasoner();
        sr.classify(axioms);
        
        IOntology ont = sr.getClassifiedOntology();
        
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
            for (IAxiom a: sr.getInferredAxioms()) {
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
    	
    	Factory fac = new Factory();
    	IConcept a = fac.createConcept("A");
    	IConcept b = fac.createConcept("B");
    	IConcept c = fac.createConcept("C");
    	IConcept d = fac.createConcept("D");
    	IConcept e = fac.createConcept("E");
    	IConcept f = fac.createConcept("F");
    	IConcept g = fac.createConcept("G");
    	
    	IAxiom a1 = fac.createConceptInclusion(b, a);
    	IAxiom a2 = fac.createConceptInclusion(c, b);
    	IAxiom a3 = fac.createConceptInclusion(d, c);
    	IAxiom a4 = fac.createConceptInclusion(e, a);
    	IAxiom a5 = fac.createConceptInclusion(f, e);
    	
    	Set<IAxiom> axioms = new HashSet<IAxiom>();
        axioms.add(a1);
        axioms.add(a2);
        axioms.add(a3);
        axioms.add(a4);
        axioms.add(a5);
    	
    	SnorocketReasoner sr = new SnorocketReasoner();
        sr.classify(axioms);
        
        IOntology ont = sr.getClassifiedOntology();
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        IAxiom a6 = fac.createConceptInclusion(g, e);
        IAxiom a7 = fac.createConceptInclusion(f, g);
        
        axioms.clear();
        axioms.add(a6);
        axioms.add(a7);
        
        sr.classify(axioms);
        ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        Set<Node> affectedNodes = ont.getAffectedNodes();
        Set<String> affectedIds = new HashSet<String>();
        for(Node affectedNode : affectedNodes) {
        	affectedIds.addAll(affectedNode.getEquivalentConcepts());
        }
        
        System.out.println("Affected node ids: "+affectedIds);
        
        Assert.assertTrue("Node G was not found in affected nodes", 
        		affectedIds.contains("G"));
        
        Assert.assertTrue("Node F was not found in affected nodes", 
        		affectedIds.contains("F"));
    }
    
    @Test
    public void testBottom() {
        IFactory factory = new CoreFactory();

        // Add concepts
        Concept a = new Concept("A");
        Concept b = new Concept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, Concept.BOTTOM_CONCEPT);
        ConceptInclusion a2 = new ConceptInclusion(b, Concept.TOP_CONCEPT);

        Set<IAxiom> axioms = new HashSet<IAxiom>();
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
        Concept a = new Concept("A");
        Concept b = new Concept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(a, Concept.BOTTOM_CONCEPT);

        Set<IAxiom> axioms = new HashSet<IAxiom>();
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
        Concept a = new Concept("A");
        Concept b = new Concept("B");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(a, Concept.BOTTOM_CONCEPT);

        Set<IAxiom> axioms = new HashSet<IAxiom>();
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
        Concept a = new Concept("A");
        Concept b = new Concept("B");
        Concept c = new Concept("C");

        // Add axioms
        ConceptInclusion a1 = new ConceptInclusion(a, b);
        ConceptInclusion a2 = new ConceptInclusion(c, b);
        ConceptInclusion a3 = new ConceptInclusion(c, Concept.BOTTOM_CONCEPT);

        Set<IAxiom> axioms = new HashSet<IAxiom>();
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
