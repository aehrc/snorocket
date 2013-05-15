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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.apache.log4j.Logger;

import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.IConjunction;
import au.csiro.ontology.model.IExistential;
import au.csiro.ontology.model.INamedConcept;
import au.csiro.ontology.model.INamedRole;
import au.csiro.ontology.model.Role;
import au.csiro.snorocket.core.concurrent.CR;
import au.csiro.snorocket.core.concurrent.Context;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
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
final public class SnorocketReasoner<T extends Comparable<T>> 
    implements IReasoner<T>, Serializable {
    
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
        ObjectInputStream ois = null; 
        try {
            ois = new ObjectInputStream(in); 
            res = (SnorocketReasoner)ois.readObject();  
        } catch(Exception e) { 
            log.error("Problem loading reasoner." + e);
            throw new RuntimeException(e);
        } finally {
            if(ois != null) {
                try { ois.close(); } catch(Exception e) {}
            }
        }
        Concept.reconnectTopBottom(
                (IConcept)res.factory.lookupConceptId(CoreFactory.TOP_CONCEPT), 
                (IConcept)res.factory.lookupConceptId(CoreFactory.BOTTOM_CONCEPT));
        Context.init(res.no);
        res.no.buildTaxonomy();
        return res;
    }
    
    /**
     * Creates an instance of Snorocket using the given base ontology.
     * 
     * @param ontology The base ontology to classify.
     */
    public SnorocketReasoner() {
        factory = new CoreFactory<T>();
        no = new NormalisedOntology<T>(factory);
    }
    
    @Override
    public IReasoner<T> classify(Set<IAxiom> axioms) {
        if(!isClassified) {
            factory = new CoreFactory<T>();
            no = new NormalisedOntology<T>(factory);
            no.loadAxioms(axioms);
            no.classify();
            isClassified = true;
        } else {
            no.loadIncremental(axioms);
            no.classifyIncremental();
        }
        return this;
    }
    
    @Override
    public IReasoner<T> classify(Iterator<IAxiom> axioms) {
        IReasoner<T> res = null;
        Set<IAxiom> axiomSet = new HashSet<IAxiom>();
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
        IReasoner<T> res = classify(new HashSet<IAxiom>(ont.getStatedAxioms()));
        return res;
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
        no.buildTaxonomy();
        Map<T, Node<T>> taxonomy = no.getTaxonomy();
        Set<Node<T>> affectedNodes = no.getAffectedNodes();
        
        return new Ontology<T>(null, null, null, taxonomy, affectedNodes);
    }
    
    @Override
    public IOntology<T> getClassifiedOntology(IOntology<T> ont) {
        // Check ontology is classified
        if(!isClassified) throw new RuntimeException(
                "Ontology is not classified!");
        
        log.info("Building taxonomy");
        no.buildTaxonomy();
        Map<T, Node<T>> nodeMap = no.getTaxonomy();
        Set<Node<T>> affectedNodes = no.getAffectedNodes();
        
        ont.setNodeMap(nodeMap);
        ont.setAffectedNodes(affectedNodes);
        
        return ont;
    }
    
    /**
     * Ideally we'd return some kind of normal form axioms here.  However, in 
     * the presence of GCIs this is not well defined (as far as I know - 
     * Michael).
     * <p>
     * Instead, we will return stated form axioms for Sufficient conditions (
     * i.e. for INamedConcept on the RHS),  and SNOMED CT DNF-based axioms for 
     * Necessary conditions. The former is just a filter over the stated axioms,
     * the latter requires looking at the Taxonomy and inferred relationships.
     * <p>
     * Note that there will be <i>virtual</i> INamedConcepts that need to be 
     * skipped/expanded and redundant IExistentials that need to be filtered.
     * 
     * @return
     */
    public Collection<IAxiom> getInferredAxioms() {
        final Collection<IAxiom> inferred = new HashSet<IAxiom>();
        
        if(!isClassified) {
            classify();  
        }
        
        if (!no.isTaxonomyComputed()) {
            log.info("Building taxonomy");
            no.buildTaxonomy();
        }
        
        final Map<T, Node<T>> taxonomy = no.getTaxonomy();
        final IConceptMap<Context> contextIndex = no.getContextIndex();
        final IntIterator itr = contextIndex.keyIterator();
        while (itr.hasNext()) {
            final int key = itr.next();
            final T id = factory.lookupConceptId(key);
            
            if (factory.isVirtualConcept(key) || Concept.BOTTOM == id) {
                continue;
            }
            
            IConcept rhs = getNecessary(contextIndex, taxonomy, key);

            final Concept<T> lhs = new Concept<T>(factory.lookupConceptId(key));
            if (!lhs.equals(rhs) && rhs != Concept.TOP) { // skip trivial axioms
                inferred.add(new ConceptInclusion(lhs, rhs));
            }
        }
        
        return inferred;
    }
    
    protected IConcept getNecessary(IConceptMap<Context> contextIndex, Map<T, Node<T>> taxonomy, int key) {
        final T id = factory.lookupConceptId(key);
        final List<IConcept> result = new ArrayList<IConcept>();

        final Node<T> node = taxonomy.get(id);
        if (node != null) {
            for (final Node<T> parent: node.getParents()) {
                final T parentId = parent.getEquivalentConcepts().iterator().next();
                if (Concept.TOP != parentId) {      // Top is redundant
                    result.add(Concept.createFrom(parentId));
                }
            }
        } else if (id instanceof au.csiro.snorocket.core.model.Conjunction) {
            // In this case, we have a result of normalisation so we reach inside and grab out the parents
            for (AbstractConcept conjunct: ((au.csiro.snorocket.core.model.Conjunction) id).getConcepts()) {
                if (conjunct instanceof au.csiro.snorocket.core.model.Concept) {
                    final int conjunctInt = ((au.csiro.snorocket.core.model.Concept) conjunct).hashCode();
                    final T conjunctId = factory.lookupConceptId(conjunctInt);
                    result.add(Concept.createFrom(conjunctId));
                }
            }
        }

        final Context ctx = contextIndex.get(key);
        CR succ = ctx.getSucc();
        for (int roleId: succ.getRoles()) {
            INamedRole<T> role = new Role<T>(factory.lookupRoleId(roleId));
            IConceptSet values = getLeaves(succ.lookupConcept(roleId));
            for (IntIterator itr2 = values.iterator(); itr2.hasNext(); ) {
                int valueInt = itr2.next();
                if (!factory.isVirtualConcept(valueInt)) {
                    final T valueId = factory.lookupConceptId(valueInt);
                    final Existential<T> x = new Existential<T>(role, new Concept<T>(valueId));
                    result.add(x);
                } else {
                    final IConcept valueConcept = getNecessary(contextIndex, taxonomy, valueInt);
                    final Existential<T> x = new Existential<T>(role, Builder.build(no, valueConcept));
                    result.add(x);
                }
            }
        }
        //        System.err.println("gN: " + id + "\t" + factory.isVirtualConcept(key) + "\t" + result);

        if (result.size() == 0) {
            return Concept.TOP;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return new Conjunction(result);
        }
    }

    /**
     * Given a set of concepts, computes the subset such that no member of the subset is subsumed by another member.
     * 
     * result = {c | c in bs and not c' in b such that c' [ c}
     * 
     * @param concepts set of subsumptions to filter
     * @return
     */
    private IConceptSet getLeaves(final IConceptSet concepts) {
        final IConceptSet leafBs = IConceptSet.FACTORY.createConceptSet(concepts);
        final IConceptSet set = IConceptSet.FACTORY.createConceptSet(leafBs);

        for (final IntIterator bItr = set.iterator(); bItr.hasNext(); ) {
            final int b = bItr.next();

            final IConceptSet ancestors = IConceptSet.FACTORY.createConceptSet(getAncestors(no, b));
            ancestors.remove(b);
            leafBs.removeAll(ancestors);
        }
        return leafBs;
    }
    
    private static <T extends Comparable<T>> IConceptSet getAncestors(NormalisedOntology<T> no, int conceptInt) {
        return no.getContextIndex().get(conceptInt).getS();
    }

    final static class Builder<T extends Comparable<T>> {
        final private NormalisedOntology<T> no;
        final private IFactory<T> factory;
        final private ConcurrentMap<Integer, RoleSet> rc;

        final private List<IExistential<T>> items = new ArrayList<IExistential<T>>();

        private Builder(NormalisedOntology<T> no) {
            this.no = no;
            this.factory = no.factory;
            this.rc = no.getRoleClosureCache();
        }
        
        @SuppressWarnings("unchecked")
        static <T extends Comparable<T>> IConcept build(NormalisedOntology<T> no, IConcept... concepts) {
            final List<IConcept> list = new ArrayList<IConcept>();
            final Builder<T> b = new Builder<T>(no);
            
            for (final IConcept member: concepts) {
                if (member instanceof IExistential) {
                    final IExistential<T> existential = (IExistential<T>) member;
                    
                    b.build(existential.getRole(), build(no, existential.getConcept()));
                } else {
                    list.add(buildOne(no, member));
                }
            }
            
            list.addAll(b.get());
            
            if (list.size() == 1) {
                return list.get(0);
            } else {
                return new Conjunction(list);
            }
        }
        
        @SuppressWarnings("unchecked")
        private static <T extends Comparable<T>> IConcept buildOne(NormalisedOntology<T> no, IConcept concept) {
            if (concept instanceof IExistential) {
                final IExistential<T> existential = (IExistential<T>) concept;
                
                return new Existential<T>(existential.getRole(), buildOne(no, existential.getConcept()));
            } else if (concept instanceof IConjunction) {
                return build(no, ((IConjunction) concept).getConcepts());
            } else if (concept instanceof INamedConcept) {
                return concept;
            } else {
                throw new RuntimeException("Unexpected type: " + concept);
            }
        }
        
        /**
         * Two cases to handle:<ol>
         * <li> We are trying to add something that is redundant
         * <li> We are trying to add something that makes an already-added thing redundant
         * </ol>
         */
        @SuppressWarnings("unchecked")
        private void build(INamedRole<T> role, IConcept concept) {
            if (!(concept instanceof INamedConcept)) {
                log.debug("WARNING: pass through of complex value: " + concept);
                doAdd(role, concept);
                return;
            }
            if (log.isTraceEnabled()) log.trace("check for subsumption: " + role + "." + concept);

            final int cInt = factory.getConcept(((INamedConcept<T>) concept).getId());
            final IConceptSet cAncestorSet = getAncestors(no, cInt);
            final int rInt = factory.getRole(role.getId());
            final RoleSet rSet = rc.get(rInt);

            final List<IExistential<T>> remove = new ArrayList<IExistential<T>>();
            boolean subsumed = false;

            for (IExistential<T> candidate: items) {
                final int dInt = factory.getConcept(((INamedConcept<T>) candidate.getConcept()).getId());
                final IConceptSet dAncestorSet = getAncestors(no, dInt);
                final int sInt = factory.getRole(candidate.getRole().getId());
                final RoleSet sSet = rc.get(sInt);
                
                if (rInt == sInt && cInt == dInt) {
                    subsumed = true;
                } else {
                    if (rSet.contains(sInt)) {
                        if (cAncestorSet.contains(dInt)) {
                            remove.add(candidate);
                            if (log.isTraceEnabled()) log.trace("\tremove " + candidate);
                        }
                    }

                    if (sSet.contains(rInt)) {
                        if (dAncestorSet.contains(cInt)) {
                            subsumed = true;
                            if (log.isTraceEnabled()) log.trace("\tsubsumed");
                        }
                    }
                }
            }
            
            if (subsumed && !remove.isEmpty()) {
                throw new AssertionError("Should not have items to remove if item to be added is already subsumed.");
            }
            
            items.removeAll(remove);
            if (!subsumed) {
                doAdd(role, concept);
            }
        }
        
        private Collection<IExistential<T>> get() {
            return items;
        }

        private void doAdd(INamedRole<T> role, IConcept concept) {
            items.add(new Existential<T>(role, concept));
        }

    }
    
    /**
     * @deprecated Use {@link SnorocketReasoner#getClassifiedOntology()} 
     * instead.
     */
    public Taxonomy<T> getTaxonomy() {
        if(no == null)
            return null;
        
        Map<T, Node<T>> res = no.getTaxonomy();
        return new Taxonomy<T>(res);
    }

    public void save(OutputStream out) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(this); 
            oos.flush();
        } catch(Exception e) {
            log.error("Problem saving reasoner.", e);
            throw new RuntimeException(e);
        } finally {
            if(oos != null) {
                try { oos.close(); } catch(Exception e) {}
            }
        }
    }

    public boolean isClassified() {
        return isClassified;
    }

    public void loadAxioms(Set<IAxiom> axioms) {
        if(!isClassified) {
            no.loadAxioms(axioms);
        } else {
            no.loadIncremental(axioms);
        }
    }

    public void loadAxioms(Iterator<IAxiom> axioms) {
        Set<IAxiom> axiomSet = new HashSet<IAxiom>();
        while(axioms.hasNext()) {
            IAxiom axiom = axioms.next();
            if(axiom == null) continue;
            axiomSet.add(axiom);
            if(axiomSet.size() == BUFFER_SIZE) {
                loadAxioms(axiomSet);
                axiomSet.clear();
            }
        }
        
        if(!axiomSet.isEmpty()) {
            loadAxioms(axiomSet);
        }
    }

    public void loadAxioms(IOntology<T> ont) {
        loadAxioms(new HashSet<IAxiom>(ont.getStatedAxioms()));
    }

    public IReasoner<T> classify() {
        if(!isClassified) {
            no.classify();
            isClassified = true;
        } else {
            no.classifyIncremental();
        }
        return this;
    }
    
}
