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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.apache.log4j.Logger;

import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.util.Statistics;
import au.csiro.snorocket.core.concurrent.Context;
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
@SuppressWarnings("deprecation")
final public class SnorocketReasoner<T extends Comparable<T>> implements IReasoner<T>, Serializable {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(SnorocketReasoner.class);
    
    public static final int BUFFER_SIZE = 10;
    
    private NormalisedOntology<T> no = null;
    private IFactory<T> factory = null;
    private boolean isClassified = false;
    private IOntology<T> ont = null;
    
    /**
     * Loads a saved instance of a {@link SnorocketReasoner} from an input
     * stream.
     * 
     * @param in
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static SnorocketReasoner load(InputStream in) {
        SnorocketReasoner res; 
        try(ObjectInputStream ois = new ObjectInputStream(in); ) { 
            res = (SnorocketReasoner)ois.readObject();  
        } 
        catch(Exception e) { 
            log.error("Problem loading reasoner." + e);
            throw new RuntimeException(e);
        }
        Concept.reconnectTopBottom(
                (IConcept)res.factory.lookupConceptId(CoreFactory.TOP_CONCEPT), 
                (IConcept)res.factory.lookupConceptId(CoreFactory.BOTTOM_CONCEPT));
        Context.init(res.no);
        return res;
    }
    
    /**
     * Creates an instance of Snorocket using the given base ontology.
     * 
     * @param ontology The base ontology to classify.
     */
    public SnorocketReasoner() {
        
    }

    @Override
    public IReasoner<T> classify(Set<IAxiom> axioms) {
        if(!isClassified) {
            factory = new CoreFactory<T>();
            no = new NormalisedOntology<T>(factory);
            no.loadAxioms(new HashSet<IAxiom>(axioms));
            no.classify();
            isClassified = true;
        } else {
            no.classifyIncremental(axioms);
        }
        return this;
    }
    
    @Override
    public IReasoner<T> classify(Iterator<IAxiom> axioms) {
        IReasoner<T> res = null;
        Set<IAxiom> axiomSet = new HashSet<>();
        while(axioms.hasNext()) {
            IAxiom axiom = axioms.next();
            if(axiom == null) continue;
            axiomSet.add(axiom);
            if(axiomSet.size() == BUFFER_SIZE) {
                res = classify(axiomSet);
                axiomSet.clear();
            }
        }
        
        if(!axiomSet.isEmpty()) {
            res = classify(axiomSet);
        }
        
        return res;
    }
    
    @Override
    public IReasoner<T> classify(IOntology<T> ont) {
        return classify(new HashSet<>(ont.getStatedAxioms()));
    }

    @Override
    public void prune() {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    @Override
    public IOntology<T> getClassifiedOntology() {
        // Check ontology is classified
        if(!isClassified) throw new RuntimeException(
                "Ontology is not classified!");
        
        log.info("Building taxonomy");
        Map<T, Node<T>> t = getInternalTaxonomy(false);
        
        if(ont == null) {
            return new Ontology<T>(null, t);
        } else {
            ont.setNodeMap(t);
            return ont;
        }
    }
    
    private Map<T, Node<T>> getInternalTaxonomy(
            boolean includeVirtualConcepts) {
        assert(no != null);
        
        PostProcessedData<T> ppd = new PostProcessedData<T>(factory);
        ppd.computeDag(no.getSubsumptions(), includeVirtualConcepts, null);
        
        long start = System.currentTimeMillis();
        
        Map<T, Node<T>> res = new HashMap<>();
        
        // Two pass approach - first create the map with the new nodes without
        // connections and then add the connections
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Queue<ClassNode> todo = new LinkedList<>();
        todo.add(top);
        
        Map<ClassNode, Node<T>> nodeToNodeMap = new HashMap<>();
        
        while(!todo.isEmpty()) {
            ClassNode node = todo.poll();
            Node<T> newNode = new Node<T>();
            nodeToNodeMap.put(node, newNode);
            IConceptSet equivs = node.getEquivalentConcepts();
            for(IntIterator it = equivs.iterator(); it.hasNext(); ) {
                newNode.getEquivalentConcepts().add(
                        factory.lookupConceptId(it.next()));
            }
            
            for(T key : newNode.getEquivalentConcepts()) {
                res.put(key, newNode);
            }
            todo.addAll(node.getChildren());
        }
        
        for(ClassNode key : nodeToNodeMap.keySet()) {
            Node<T> node = nodeToNodeMap.get(key);
            for(ClassNode parent : key.getParents()) {
                node.getParents().add(nodeToNodeMap.get(parent));
            }
            
            for(ClassNode child : key.getChildren()) {
                node.getChildren().add(nodeToNodeMap.get(child));
            }
        }
        
        Statistics.INSTANCE.setTime("taxonomy transformation",
                System.currentTimeMillis() - start);
        return res;
    }
    
    /**
     * @deprecated Use {@link SnorocketReasoner#getClassifiedOntology()} 
     * instead.
     */
    @Override
    public Taxonomy<T> getTaxonomy() {
        if(no == null)
            return null;
        
        PostProcessedData<T> ppd = new PostProcessedData<T>(factory);
        ppd.computeDag(no.getSubsumptions(), false, null);
        
        Map<T, Node<T>> res = new HashMap<>();
        
        // Two pass approach - first create the map with the new nodes without
        // connections and then add the connections
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        Queue<ClassNode> todo = new LinkedList<>();
        todo.add(top);
        
        Map<ClassNode, Node<T>> nodeToNodeMap = new HashMap<>();
        
        while(!todo.isEmpty()) {
            ClassNode node = todo.poll();
            Node<T> newNode = new Node<T>();
            nodeToNodeMap.put(node, newNode);
            IConceptSet equivs = node.getEquivalentConcepts();
            for(IntIterator it = equivs.iterator(); it.hasNext(); ) {
                newNode.getEquivalentConcepts().add(
                        factory.lookupConceptId(it.next()));
            }
            
            for(T key : newNode.getEquivalentConcepts()) {
                res.put(key, newNode);
            }
            todo.addAll(node.getChildren());
        }
        
        for(ClassNode key : nodeToNodeMap.keySet()) {
            Node<T> node = nodeToNodeMap.get(key);
            for(ClassNode parent : key.getParents()) {
                node.getParents().add(nodeToNodeMap.get(parent));
            }
            
            for(ClassNode child : key.getChildren()) {
                node.getChildren().add(nodeToNodeMap.get(child));
            }
        }
        
        return new Taxonomy<T>(res);
    }

    @Override
    public void save(OutputStream out) {
        try (ObjectOutputStream oos = new ObjectOutputStream(out)){
            oos.writeObject(this); 
            oos.flush();
        } catch(Exception e) {
            log.error("Problem saving reasoner.", e);
            throw new RuntimeException(e);
        }
    }
    
}
