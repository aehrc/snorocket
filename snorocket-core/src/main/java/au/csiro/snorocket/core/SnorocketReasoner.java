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

import java.util.Collection;
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
import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.concurrent.Context;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.RoleMap;
import au.csiro.snorocket.core.util.RoleSet;

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
                (IConcept)res.factory.lookupConceptId(Factory.TOP_CONCEPT), 
                (IConcept)res.factory.lookupConceptId(Factory.BOTTOM_CONCEPT));
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
            factory = new Factory<T>();
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
    public void prune() {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    @Override
    public IOntology<T> getClassifiedOntology(boolean includeTaxonomy,
            boolean includeStatedAxioms, boolean includeInferredAxioms) {
        // Check ontology is classified
        if(!isClassified) return null;
        
        if(!includeTaxonomy && !includeStatedAxioms && !includeInferredAxioms) {
            throw new IllegalArgumentException("The method must be called" +
            		"with at least one flag set to true");
        }
        
        log.info("Building taxonomy");
        Map<T, Node<T>> t = getInternalTaxonomy();
        
        // Optimisation for the scenario where only the taxonomy is needed
        if(includeTaxonomy && !includeStatedAxioms && !includeInferredAxioms) {
            return new Ontology<T>(null, null, t);
        }
        
        Collection<IAxiom> statedAxioms = null;
        Collection<IAxiom> inferredAxioms = null;
        
        if(includeStatedAxioms) {
            log.info("Building stated axioms");
            statedAxioms = getStatedAxioms();
        }
        
        if(includeInferredAxioms) {
            log.info("Building inferred axioms");
            inferredAxioms = getInferredAxioms(t);
        }
        
        if(!includeTaxonomy) {
            t = null;
        }
        
        IOntology<T> res = new Ontology<T>(statedAxioms, inferredAxioms, t);
        return res;
    }
    
    private Map<T, Node<T>> getInternalTaxonomy() {
        assert(no != null);
        
        PostProcessedData<T> ppd = new PostProcessedData<T>(factory);
        ppd.computeDag(no.getSubsumptions(), null);
        
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
        
        return res;
    }
    
    private Collection<IAxiom> getStatedAxioms() {
        return no.getStatedAxioms();
    }
    
    @SuppressWarnings("unchecked")
    private Collection<IAxiom> getInferredAxioms(Map<T, Node<T>> t) {
        // Get the is-a relationships that correspond to the proximal super type
        // view
        Node<T> top = t.get(factory.lookupConceptId(IFactory.TOP_CONCEPT));
        
        Queue<Node<T>> todo = new LinkedList<>();
        todo.add(top);
        
        Set<IAxiom> axioms = new HashSet<>();
        
        Set<Node<T>> processed = new HashSet<>();
        
        Node<T> node = todo.poll();
        while(node != null) {
            processed.add(node);
            
            Set<T> equivs = node.getEquivalentConcepts();
            Object[] equivsArray = equivs.toArray();
            
            // Add equivalence axioms
            if(equivs.size() > 1) {
                for(int i = 0; i < equivsArray.length; i++) {
                    for(int j = i+1; j < equivsArray.length; j++) {
                        IConcept lhs = new Concept<T>((T)equivsArray[i]);
                        IConcept rhs = new Concept<T>((T)equivsArray[j]);
                        axioms.add(new ConceptInclusion(lhs, rhs));
                        axioms.add(new ConceptInclusion(rhs, lhs));
                    }
                }
            }
            
            // Add is-a relationships
            Set<Node<T>> parents = node.getParents();
            for(Node<T> parent : parents) {
                Object[] parentsEquivsArray = 
                        parent.getEquivalentConcepts().toArray();
                // Create an is-a relationship between each equivalent concept
                // and each equivalent parent
                for(int i = 0; i < equivsArray.length; i++) {
                    T equiv = (T)equivsArray[i];
                    for(int j = 0; j < parentsEquivsArray.length; j++) {
                        T equivParent = (T)parentsEquivsArray[j];
                        IConcept lhs = new Concept<T>(equiv);
                        IConcept rhs = new Concept<T>(equivParent);
                        axioms.add(new ConceptInclusion(lhs, rhs));
                    }
                }
            }
            
            for(Node<T> child : node.getChildren()) {
                if(!processed.contains(child)) {
                    todo.add(child);
                }
            }
            node = todo.poll();
        }
        
        processed.clear();
        processed = null;
        
        // Get all the other relationships that correspond to the distribution
        // view
        int numRoles = factory.getTotalRoles();
        R r = no.getRelationships();
        RoleMap<RoleSet> rc = no.getRoleClosureCache();
        
        // We need the inverted role closure to make sure redundant 
        // relationships are filtered
        RoleMap<RoleSet> irc = getInvertedRoleClosure(rc, numRoles);
        
        int numConcepts = factory.getTotalConcepts();
        for(int i = 0; i < numRoles; i++) {
            for(int j = 0; j < numConcepts; j++) {
                IConceptSet tgts = r.lookupB(j, i);
                for(IntIterator it = tgts.iterator(); it.hasNext(); ) {
                    // Check if this relationship has a more specific one and
                    // if not then add it
                    int b = it.next();
                    RoleSet childRoles = irc.get(i);
                    boolean skip = false;
                    
                    if(childRoles != null)  {
                        for (int k = childRoles.nextSetBit(0); k >= 0; 
                                k = childRoles.nextSetBit(k+1)) {
                            IConceptSet ics = r.lookupB(j, k);
                            if(ics != null && ics.contains(b)) {
                                skip = true;
                                break;
                            }
                        }
                    }
                    
                    if(!skip) {
                        T role = factory.lookupRoleId(i);
                        T A = factory.lookupConceptId(j);
                        T B = factory.lookupConceptId(b);
                        axioms.add(buildAxiom(A, B, role));
                    }
                }
            }
        }
        
        return axioms;
    }
    
    private IAxiom buildAxiom(T a, T b, T r) {
        return new ConceptInclusion(no.transform(a), 
                new Existential<>(new Role<T>(r), no.transform(b)));
    }
    
    private RoleMap<RoleSet> getInvertedRoleClosure(RoleMap<RoleSet> rc, 
            int numRoles) {
        RoleMap<RoleSet> irc = new RoleMap<RoleSet>(numRoles);
        for(int i = 0; i < numRoles; i++) {
            if(rc.containsKey(i)) {
                RoleSet parentRoles = rc.get(i);
                for (int j = parentRoles.nextSetBit(0); j >= 0; 
                        j = parentRoles.nextSetBit(j+1)) {
                    if(j == i) continue;
                    RoleSet children = irc.get(j);
                    if(children == null) {
                        children = new RoleSet();
                        irc.put(j, children);
                    }
                    children.add(i);
                }
            }
        }
        return irc;
    }

    @Override
    public IOntology<T> getClassifiedOntology() {
        return getClassifiedOntology(true,  true,  true);
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
        ppd.computeDag(no.getSubsumptions(), null);
        
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
