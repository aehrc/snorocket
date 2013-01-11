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

import org.junit.Test;

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
    @SuppressWarnings("unchecked")
    //@Test
    public void testSave() {

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
        
        SnorocketReasoner<String> sr = new SnorocketReasoner<>();
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

        sr.classify(incAxioms);

        // Test results
        IOntology<String> ont = sr.getClassifiedOntology();
        
        Node<String> bottom = ont.getBottomNode();
        Set<Node<String>> bottomRes = bottom.getParents();
        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ont.getNode(endocardium.getId())));
        assertTrue(bottomRes.contains(ont.getNode(endocarditis.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartWall.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartValve.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heart.getId())));

        Node<String> endocarditisNode = ont.getNode(endocarditis.getId());
        Set<Node<String>> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ont.getNode(inflammation.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(criticalDisease.getId())));

        Node<String> inflammationNode = ont.getNode(inflammation.getId());
        Set<Node<String>> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ont.getNode(disease.getId())));

        Node<String> endocardiumNode = ont.getNode(endocardium.getId());
        Set<Node<String>> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ont.getNode(tissue.getId())));

        Node<String> heartdiseaseNode = ont.getNode(heartdisease.getId());
        Set<Node<String>> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ont.getNode(disease.getId())));

        Node<String> heartWallNode = ont.getNode(heartWall.getId());
        Set<Node<String>> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ont.getNode(bodyWall.getId())));

        Node<String> heartValveNode = ont.getNode(heartValve.getId());
        Set<Node<String>> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes
                .contains(ont.getNode(bodyValve.getId())));

        Node<String> diseaseNode = ont.getNode(disease.getId());
        Set<Node<String>> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(ont.getTopNode()));

        Node<String> tissueNode = ont.getNode(tissue.getId());
        Set<Node<String>> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ont.getTopNode()));

        Node<String> heartNode = ont.getNode(heart.getId());
        Set<Node<String>> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ont.getTopNode()));

        Node<String> bodyValveNode = ont.getNode(bodyValve.getId());
        Set<Node<String>> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ont.getTopNode()));

        Node<String> bodyWallNode = ont.getNode(bodyWall.getId());
        Set<Node<String>> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ont.getTopNode()));

        Node<String> criticalDiseaseNode = ont.getNode(criticalDisease.getId());
        Set<Node<String>> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ont.getTopNode()));
    }
    
    @Test
    public void testEndocarditis() {
        org.apache.log4j.LogManager.getRootLogger().setLevel((org.apache.log4j.Level)org.apache.log4j.Level.TRACE);
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
        SnorocketReasoner<String> sr = new SnorocketReasoner<>();
        sr.classify(axioms);
        
        IOntology<String> ont = sr.getClassifiedOntology();
        
        Utils.printTaxonomy(ont.getTopNode(), ont.getBottomNode());
        
        // Test taxonomy results
        Node<String> bottomNode = ont.getBottomNode();
        Set<Node<String>> bottomRes = bottomNode.getParents();

        assertTrue(bottomRes.size() == 5);
        assertTrue(bottomRes.contains(ont.getNode(endocardium.getId())));
        assertTrue(bottomRes.contains(ont.getNode(endocarditis.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartWall.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heartValve.getId())));
        assertTrue(bottomRes.contains(ont.getNode(heart.getId())));

        Node<String> endocarditisNode = ont.getNode(endocarditis.getId());
        Set<Node<String>> endocarditisRes = endocarditisNode.getParents();
        assertTrue(endocarditisRes.size() == 3);
        assertTrue(endocarditisRes.contains(ont.getNode(inflammation.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(heartdisease.getId())));
        assertTrue(endocarditisRes.contains(ont.getNode(criticalDisease.getId())));

        Node<String> inflammationNode = ont.getNode(inflammation.getId());
        Set<Node<String>> inflammationRes = inflammationNode.getParents();
        assertTrue(inflammationRes.size() == 1);
        assertTrue(inflammationRes.contains(ont.getNode(disease.getId())));

        Node<String> endocardiumNode = ont.getNode(endocardium.getId());
        Set<Node<String>> endocardiumRes = endocardiumNode.getParents();
        assertTrue(endocardiumRes.size() == 1);
        assertTrue(endocardiumRes.contains(ont.getNode(tissue.getId())));

        Node<String> heartdiseaseNode = ont.getNode(heartdisease.getId());
        Set<Node<String>> heartdiseaseRes = heartdiseaseNode.getParents();
        assertTrue(heartdiseaseRes.size() == 1);
        assertTrue(heartdiseaseRes.contains(ont.getNode(disease.getId())));

        Node<String> heartWallNode = ont.getNode(heartWall.getId());
        Set<Node<String>> heartWallRes = heartWallNode.getParents();
        assertTrue(heartWallRes.size() == 1);
        assertTrue(heartWallRes.contains(ont.getNode(bodyWall.getId())));

        Node<String> heartValveNode = ont.getNode(heartValve.getId());
        Set<Node<String>> heartValveRes = heartValveNode.getParents();
        assertTrue(heartValveRes.size() == 1);
        assertTrue(heartValveRes.contains(ont.getNode(bodyValve.getId())));

        Node<String> diseaseNode = ont.getNode(disease.getId());
        Set<Node<String>> diseaseRes = diseaseNode.getParents();
        assertTrue(diseaseRes.size() == 1);
        assertTrue(diseaseRes.contains(ont.getTopNode()));

        Node<String> tissueNode = ont.getNode(tissue.getId());
        Set<Node<String>> tissueRes = tissueNode.getParents();
        assertTrue(tissueRes.size() == 1);
        assertTrue(tissueRes.contains(ont.getTopNode()));

        Node<String> heartNode = ont.getNode(heart.getId());
        Set<Node<String>> heartRes = heartNode.getParents();
        assertTrue(heartRes.size() == 1);
        assertTrue(heartRes.contains(ont.getTopNode()));

        Node<String> bodyValveNode = ont.getNode(bodyValve.getId());
        Set<Node<String>> bodyValveRes = bodyValveNode.getParents();
        assertTrue(bodyValveRes.size() == 1);
        assertTrue(bodyValveRes.contains(ont.getTopNode()));

        Node<String> bodyWallNode = ont.getNode(bodyWall.getId());
        Set<Node<String>> bodyWallRes = bodyWallNode.getParents();
        assertTrue(bodyWallRes.size() == 1);
        assertTrue(bodyWallRes.contains(ont.getTopNode()));

        Node<String> criticalDiseaseNode = ont.getNode(criticalDisease.getId());
        Set<Node<String>> criticalDiseaseRes = criticalDiseaseNode.getParents();
        assertTrue(criticalDiseaseRes.size() == 1);
        assertTrue(criticalDiseaseRes.contains(ont.getTopNode()));
    }

}
