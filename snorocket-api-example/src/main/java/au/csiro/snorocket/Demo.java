/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import au.csiro.ontology.Factory;
import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.model.IConcept;
import au.csiro.snorocket.core.SnorocketReasoner;
import au.csiro.snorocket.core.util.Utils;

/**
 * Demo class for the final webinar. Shows how to load the base state for the
 * classifier and how to run an incremental classification.
 * 
 * @author Alejandro Metke
 *
 */
public class Demo {
    
    public Demo() {
        
    }
    
    @SuppressWarnings("unchecked")
    public void start() {
        
        // 1. Load the base state
        SnorocketReasoner<String> reasoner = SnorocketReasoner.load(
                this.getClass().getResourceAsStream("/classifier_uuid.state"));
        
        // 2. Load SCT to UUID map
        Map<String, String> sctToUuidMap = new HashMap<>();
        Map<String, String> uuidToDescMap = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                this.getClass().getResourceAsStream("/nid_sctid_uuid_map.txt"), 
                StandardCharsets.UTF_8))) {
            String line = null;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("[,]");
                if(parts.length < 4) {
                    System.err.println(line);
                    continue;
                }
                if(parts[1].equals("NA")) continue;
                sctToUuidMap.put(parts[1], parts[2]);
                uuidToDescMap.put(parts[2], parts[3]);
            }      
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }
        
        // 2. Add test axiom
        Factory<String> f = new Factory<>();
        String newId = UUID.randomUUID().toString();
        uuidToDescMap.put(newId, "Special Appendicits");
        
        IConcept specialAppendicitis = f.createConcept(newId);
        String appendicitsUuid = "55450fab-6786-394d-89f9-a0fd44bd7e7e";
        IConcept appendicitis = f.createConcept(appendicitsUuid);
        IAxiom a1 = f.createConceptInclusion(specialAppendicitis, appendicitis);
        Set<IAxiom> axioms = new HashSet<>();
        axioms.add(a1);
        
        // 3. Classify incrementally
        reasoner.classify(axioms);
        
        // 4. Retrieve taxonomy
        IOntology<String> ont = reasoner.getClassifiedOntology();
        
        // 5. Get node for new concept
        Node<String> specialAppendicitisNode = 
                ont.getNodeMap().get(appendicitsUuid);
        
        // 6. Print the new node
        Utils.printTaxonomy(
                specialAppendicitisNode.getParents().iterator().next(), 
                ont.getBottomNode(), 
                uuidToDescMap
        );
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Demo d = new Demo();
        d.start();
    }

}
