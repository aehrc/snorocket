/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */

package au.csiro.snorocket.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import au.csiro.ontology.Node;
import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.AbstractAxiom;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

/**
 * This class represents an instance of the reasoner. It uses the internal
 * ontology model. If you need to use an OWL model refer to the 
 * {@link SnorocketOWLReasoner} class.
 * 
 * @author Alejandro Metke
 *
 */
final public class SnorocketReasoner implements IReasoner {
    
    private NormalisedOntology no = null;
    private IFactory factory = null;
    
    /**
     * Creates an instance of Snorocket using the given base ontology.
     * 
     * @param ontology The base ontology to classify.
     */
    public SnorocketReasoner() {
        
    }
    
    @Override
    public void classify(Set<AbstractAxiom> axioms) {
        factory = new Factory();
        no = new NormalisedOntology(factory);
        no.loadAxioms(new HashSet<AbstractAxiom>(axioms));
        no.classify();
    }
    
    @Override
    public void classifyIncremental(Set<AbstractAxiom> axioms) {
        no.classifyIncremental(axioms);
    }
    
    @Override
    public Taxonomy getTaxonomy() {
        if(no == null)
            return null;
        
        PostProcessedData ppd = new PostProcessedData(factory);
        ppd.computeDag(no.getSubsumptions(), null);
        
        Map<String, Node> res = new HashMap<String, Node>();
        
        // Two pass approach - first create the map with the new nodes without
        // connections and then add the connections
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Queue<ClassNode> todo = new LinkedList<>();
        todo.add(top);
        
        Map<ClassNode, Node> nodeToNodeMap = new HashMap<>();
        
        while(!todo.isEmpty()) {
            ClassNode node = todo.poll();
            Node newNode = new Node();
            nodeToNodeMap.put(node, newNode);
            IConceptSet equivs = node.getEquivalentConcepts();
            for(IntIterator it = equivs.iterator(); it.hasNext(); ) {
                newNode.getEquivalentConcepts().add(
                        factory.lookupConceptId(it.next()));
            }
            
            for(String key : newNode.getEquivalentConcepts()) {
                res.put(key, newNode);
            }
            todo.addAll(node.getChildren());
        }
        
        for(ClassNode key : nodeToNodeMap.keySet()) {
            Node node = nodeToNodeMap.get(key);
            for(ClassNode parent : key.getParents()) {
                node.getParents().add(nodeToNodeMap.get(parent));
            }
            
            for(ClassNode child : key.getChildren()) {
                node.getChildren().add(nodeToNodeMap.get(child));
            }
        }
        
        return new Taxonomy(res);
    }
    
}
