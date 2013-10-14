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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import au.csiro.ontology.Node;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.Literal;
import au.csiro.ontology.model.NamedFeature;
import au.csiro.ontology.model.NamedRole;
import au.csiro.ontology.model.Role;
import au.csiro.ontology.model.RoleInclusion;
import au.csiro.ontology.util.Statistics;
import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.IConjunctionQueueEntry;
import au.csiro.snorocket.core.axioms.IRoleQueueEntry;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.axioms.NF1a;
import au.csiro.snorocket.core.axioms.NF1b;
import au.csiro.snorocket.core.axioms.NF2;
import au.csiro.snorocket.core.axioms.NF3;
import au.csiro.snorocket.core.axioms.NF4;
import au.csiro.snorocket.core.axioms.NF5;
import au.csiro.snorocket.core.axioms.NF6;
import au.csiro.snorocket.core.axioms.NF7;
import au.csiro.snorocket.core.axioms.NF8;
import au.csiro.snorocket.core.axioms.NormalFormGCI;
import au.csiro.snorocket.core.axioms.RI;
import au.csiro.snorocket.core.concurrent.CR;
import au.csiro.snorocket.core.concurrent.Context;
import au.csiro.snorocket.core.concurrent.TaxonomyWorker1;
import au.csiro.snorocket.core.concurrent.TaxonomyWorker2;
import au.csiro.snorocket.core.concurrent.Worker;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.AbstractLiteral;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.model.DateLiteral;
import au.csiro.snorocket.core.model.DecimalLiteral;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.model.StringLiteral;
import au.csiro.snorocket.core.util.AxiomSet;
import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.FastConceptHashSet;
import au.csiro.snorocket.core.util.FastConceptMap;
import au.csiro.snorocket.core.util.FeatureMap;
import au.csiro.snorocket.core.util.FeatureSet;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IMonotonicCollection;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.MonotonicCollection;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptHashSet;
import au.csiro.snorocket.core.util.SparseConceptMap;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * A normalised EL Ontology
 * 
 * @author law223
 * 
 */
public class NormalisedOntology implements Serializable {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Logger.
     */
    private final static Logger log = Logger.getLogger(NormalisedOntology.class);

    final protected IFactory factory;

    /**
     * The set of NF1 terms in the ontology
     * <ul>
     * <li>Concept map 76.5% full (SNOMED 20061230)</li>
     * </ul>
     * 
     * These terms are of the form A n Ai [ B and are indexed by A.
     */
    final protected IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> ontologyNF1;

    /**
     * The set of NF2 terms in the ontology
     * <ul>
     * <li>Concept map 34.7% full (SNOMED 20061230)</li>
     * </ul>
     * 
     * These terms are of the form A [ r.B and are indexed by A.
     */
    final protected IConceptMap<MonotonicCollection<NF2>> ontologyNF2;

    /**
     * The set of NF3 terms in the ontology
     * <ul>
     * <li>Concept map 9.3% full (SNOMED 20061230)</li>
     * <li>Unknown usage profile for Role maps</li>
     * </ul>
     * 
     * These terms are of the form r.A [ b and indexed by A.
     */
    final protected IConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>> ontologyNF3;

    /**
     * The set of NF4 terms in the ontology
     */
    final protected IMonotonicCollection<NF4> ontologyNF4;

    /**
     * The set of NF5 terms in the ontology
     */
    final protected IMonotonicCollection<NF5> ontologyNF5;

    /**
     * The set of reflexive roles in the ontology
     */
    final protected IConceptSet reflexiveRoles = new SparseConceptSet();

    /**
     * The set of NF7 terms in the ontology.
     * 
     * These terms are of the form A [ f.(o, v) and are indexed by A.
     */
    final protected IConceptMap<MonotonicCollection<NF7>> ontologyNF7;

    /**
     * The set of NF8 terms in the ontology.
     * 
     * These terms are of the form f.(o, v) [ A. These are indexed by f.
     */
    final protected FeatureMap<MonotonicCollection<NF8>> ontologyNF8;

    /**
     * The queue of contexts to process.
     */
    private final Queue<Context> todo = new ConcurrentLinkedQueue<Context>();

    /**
     * The map of contexts by concept id.
     */
    private final IConceptMap<Context> contextIndex;

    /**
     * The global role closure.
     */
    private final Map<Integer, RoleSet> roleClosureCache;

    /**
     * A set of new contexts added in an incremental classification.
     */
    private final Set<Context> newContexts = new HashSet<Context>();
    
    /**
     * The number of threads to use.
     */
    private int numThreads = Runtime.getRuntime().availableProcessors();
    
    private boolean hasBeenIncrementallyClassified = false;
    
    private transient Map<String, Node> conceptNodeIndex;
    
    /**
     * A map used to store NF7 terms and collapse them before saturation. First key is lhs and second key is feature.
     */
    private Map<Integer, Map<Integer, List<NF7>>> tempNf7Map = new HashMap<Integer, Map<Integer, List<NF7>>>();
    
    private static class ContextComparator implements Comparator<Context>, Serializable {
        /**
         * Serialisation version.
         */
        private static final long serialVersionUID = 1L;

        public int compare(Context o1, Context o2) {
            return ((Integer) o1.getConcept()).compareTo(o2.getConcept());
        }
    }
    
    /**
     * A set of contexts potentially affected by an incremental classification.
     */
    private final Set<Context> affectedContexts = 
            new ConcurrentSkipListSet<Context>(new ContextComparator());
    
    /**
     * Normalised axioms added incrementally.
     */
    private AxiomSet as = new AxiomSet(); 

    public IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> getOntologyNF1() {
        return ontologyNF1;
    }

    public IConceptMap<MonotonicCollection<NF2>> getOntologyNF2() {
        return ontologyNF2;
    }

    public IConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>> getOntologyNF3() {
        return ontologyNF3;
    }

    public IMonotonicCollection<NF4> getOntologyNF4() {
        return ontologyNF4;
    }

    public IMonotonicCollection<NF5> getOntologyNF5() {
        return ontologyNF5;
    }

    public IConceptSet getReflexiveRoles() {
        return reflexiveRoles;
    }

    public IConceptMap<MonotonicCollection<NF7>> getOntologyNF7() {
        return ontologyNF7;
    }

    public FeatureMap<MonotonicCollection<NF8>> getOntologyNF8() {
        return ontologyNF8;
    }

    public Queue<Context> getTodo() {
        return todo;
    }

    public IConceptMap<Context> getContextIndex() {
        return contextIndex;
    }

    public Map<Integer, RoleSet> getRoleClosureCache() {
        return roleClosureCache;
    }

    public Set<Context> getAffectedContexts() {
        return affectedContexts;
    }

    /**
     * 
     * @param factory
     * @param inclusions
     */
    public NormalisedOntology(final IFactory factory, final Set<? extends Axiom> inclusions) {
        this(factory);
        
        loadAxioms(inclusions);
        
        if(log.isTraceEnabled()) {
            printNormalisedAxioms();
        }
    }

    final static int CONCEPT_COUNT_ESTIMATE = 500000;

    /**
     * 
     * @param baseConceptCount
     * @param conceptCount
     *            if this value is too small, the algorithm performance will be
     *            impacted
     * @param roleCount
     */
    public NormalisedOntology(final IFactory factory) {
        // TODO: how do we estimate these numbers better?
        this(
            factory,
            new DenseConceptMap<MonotonicCollection<IConjunctionQueueEntry>>(CONCEPT_COUNT_ESTIMATE),
            new SparseConceptMap<MonotonicCollection<NF2>>(CONCEPT_COUNT_ESTIMATE, "ontologyNF2"),
            new SparseConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>>(CONCEPT_COUNT_ESTIMATE, "ontologyNF3"),
            new MonotonicCollection<NF4>(15), new MonotonicCollection<NF5>(1), 
            new SparseConceptMap<MonotonicCollection<NF7>>(10, "ontologyNF7"),
            new FeatureMap<MonotonicCollection<NF8>>(10)
        );
    }

    /**
     * 
     * @param factory
     * @param nf1q
     * @param nf2q
     * @param nf3q
     * @param nf4q
     * @param nf5q
     * @param nf7q
     * @param nf8q
     */
    protected NormalisedOntology(
            final IFactory factory,
            final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> nf1q,
            final IConceptMap<MonotonicCollection<NF2>> nf2q,
            final IConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>> nf3q,
            final IMonotonicCollection<NF4> nf4q,
            final IMonotonicCollection<NF5> nf5q,
            final IConceptMap<MonotonicCollection<NF7>> nf7q,
            final FeatureMap<MonotonicCollection<NF8>> nf8q) {
        this.factory = factory;
        contextIndex = new FastConceptMap<Context>(factory.getTotalConcepts(), "");
        roleClosureCache = new ConcurrentHashMap<Integer, RoleSet>(factory.getTotalRoles());

        this.ontologyNF1 = nf1q;
        this.ontologyNF2 = nf2q;
        this.ontologyNF3 = nf3q;
        this.ontologyNF4 = nf4q;
        this.ontologyNF5 = nf5q;
        this.ontologyNF7 = nf7q;
        this.ontologyNF8 = nf8q;
    }

    /**
     * Normalises and loads a set of axioms.
     * 
     * @param inclusions
     */
    public void loadAxioms(final Set<? extends Axiom> inclusions) {
        long start = System.currentTimeMillis();
        if(log.isInfoEnabled())
            log.info("Loading " + inclusions.size() + " axioms");
        Set<Inclusion> normInclusions = normalise(inclusions);
        if(log.isInfoEnabled()) log.info("Processing " + normInclusions.size() + " normalised axioms");
        Statistics.INSTANCE.setTime("normalisation", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        for (Inclusion i : normInclusions) {
            addTerm(i.getNormalForm());
        }
        
        Statistics.INSTANCE.setTime("indexing", System.currentTimeMillis() - start);
    }
    
    /**
     * Transforms a {@link Set} of {@link AbstractAxiom}s into a {@link Set} of {@link Inclusion}s.
     * 
     * @param axioms The axioms in the ontology model format.
     * @return The axioms in the internal model format.
     */
    private Set<Inclusion> transformAxiom(final Set<? extends Axiom> axioms) {
        Set<Inclusion> res = new HashSet<Inclusion>();
        
        for(Axiom aa : axioms) {
            if(aa instanceof ConceptInclusion) {
                ConceptInclusion ci = (ConceptInclusion)aa;
                Concept lhs = ci.getLhs();
                Concept rhs = ci.getRhs();
                res.add(new GCI(transformConcept(lhs), transformConcept(rhs)));
            } else if(aa instanceof RoleInclusion) {
                RoleInclusion ri = (RoleInclusion)aa;
                Role[] lh = ri.getLhs();
                NamedRole[] lhs = new NamedRole[lh.length];
                for(int i = 0; i < lh.length; i++) {
                    lhs[i] = (NamedRole) lh[i];
                }
                NamedRole rhs = (NamedRole) ri.getRhs();
                int[] lhsInt = new int[lhs.length];
                for(int i = 0; i < lhsInt.length; i++) {
                    lhsInt[i] = factory.getRole(lhs[i].getId());
                }
                res.add(new RI(lhsInt, factory.getRole(rhs.getId())));
            }
        }
        
        return res;
    }
    
    /**
     * Transforms an {@link AbstractConcept} into an {@link au.csiro.snorocket.core.model.AbstractConcept}.
     * 
     * @param c The concept in the ontology model format.
     * @return The concept in the internal model format.
     */
    private au.csiro.snorocket.core.model.AbstractConcept transformConcept(Concept c) {
        if(c.equals(au.csiro.ontology.model.NamedConcept.TOP_CONCEPT)) {
            return new au.csiro.snorocket.core.model.Concept(IFactory.TOP_CONCEPT);
        } else if(c.equals(au.csiro.ontology.model.NamedConcept.BOTTOM_CONCEPT)) {
            return new au.csiro.snorocket.core.model.Concept(IFactory.BOTTOM_CONCEPT);
        } else if(c instanceof au.csiro.ontology.model.NamedConcept) {
            return new au.csiro.snorocket.core.model.Concept(
                    factory.getConcept(((au.csiro.ontology.model.NamedConcept) c).getId()));
        } else if(c instanceof au.csiro.ontology.model.Conjunction) {
            Concept[] modelCons = ((au.csiro.ontology.model.Conjunction)c).getConcepts();
            au.csiro.snorocket.core.model.AbstractConcept[] cons = 
                    new au.csiro.snorocket.core.model.AbstractConcept[modelCons.length];
            for(int i = 0; i < modelCons.length; i++) {
                cons[i] = transformConcept(modelCons[i]);
            }
            return new Conjunction(cons);
        } else if(c instanceof au.csiro.ontology.model.Datatype) {
            au.csiro.ontology.model.Datatype dt = (au.csiro.ontology.model.Datatype) c;
            return new Datatype(factory.getFeature(((NamedFeature) dt.getFeature()).getId()), dt.getOperator(),
                    transformLiteral(dt.getLiteral()));
        } else if(c instanceof au.csiro.ontology.model.Existential) {
            au.csiro.ontology.model.Existential e = (au.csiro.ontology.model.Existential) c;
            return new Existential(factory.getRole(((NamedRole) e.getRole()).getId()), 
                    transformConcept(e.getConcept())); 
        } else {
            throw new RuntimeException("Unexpected AbstractConcept "+c.getClass().getName());
        }
    }
    
    /**
     * Transforms an {@link ILiteral} into an {@link au.csiro.snorocket.core.model.AbstractLiteral}.
     * 
     * @param l The literal in the ontology model format.
     * @return The literal in the internal model format.
     */
    private au.csiro.snorocket.core.model.AbstractLiteral transformLiteral(Literal l) {
        if(l instanceof au.csiro.ontology.model.DateLiteral) {
            return new DateLiteral(((au.csiro.ontology.model.DateLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.DecimalLiteral) {
            return new DecimalLiteral(((au.csiro.ontology.model.DecimalLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.IntegerLiteral) {
            return new IntegerLiteral(((au.csiro.ontology.model.IntegerLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.StringLiteral) {
            return new StringLiteral(((au.csiro.ontology.model.StringLiteral) l).getValue());
        } else {
            throw new RuntimeException("Unexpected AbstractLiteral "+l.getClass().getName());
        }
    }

    /**
     * Returns a set of Inclusions in normal form suitable for classifying.
     */
    public Set<Inclusion> normalise(final Set<? extends Axiom> inclusions) {
        
        // Exhaustively apply NF1 to NF4
        Set<Inclusion> newIs = transformAxiom(inclusions);
        Set<Inclusion> oldIs = new HashSet<Inclusion>(newIs.size());
        final Set<Inclusion> done = new HashSet<Inclusion>(newIs.size());

        do {
            final Set<Inclusion> tmp = oldIs;
            oldIs = newIs;
            newIs = tmp;
            newIs.clear();

            for (Inclusion i : oldIs) {
                Inclusion[] s = i.normalise1(factory);
                if (null != s) {
                    for (int j = 0; j < s.length; j++) {
                        if (null != s[j]) {
                            newIs.add(s[j]);
                        }
                    }
                } else {
                    done.add(i);
                }
            }
        } while (!newIs.isEmpty());

        newIs.addAll(done);
        done.clear();

        // Then exhaustively apply NF5 to NF7
        do {
            final Set<Inclusion> tmp = oldIs;
            oldIs = newIs;
            newIs = tmp;
            newIs.clear();

            for (Inclusion i : oldIs) {
                Inclusion[] s = i.normalise2(factory);
                if (null != s) {
                    for (int j = 0; j < s.length; j++) {
                        if (null != s[j]) {
                            newIs.add(s[j]);
                        }
                    }
                } else {
                    done.add(i);
                }
            }
        } while (!newIs.isEmpty());
        
        if(log.isTraceEnabled()) {
            log.trace("Normalised axioms:");
            for(Inclusion inc : done) {
                StringBuilder sb = new StringBuilder();
                if(inc instanceof GCI) {
                    GCI gci = (GCI)inc;
                    sb.append(printInternalObject(gci.lhs()));
                    sb.append(" [ ");
                    sb.append(printInternalObject(gci.rhs()));
                } else if(inc instanceof RI) {
                    RI ri = (RI)inc;
                    int[] lhs = ri.getLhs();
                    sb.append(factory.lookupRoleId(lhs[0]));
                    for(int i = 1; i < lhs.length; i++) {
                        sb.append(" * ");
                        sb.append(factory.lookupRoleId(lhs[i]));
                    }
                    sb.append(" [ ");
                    sb.append(factory.lookupRoleId(ri.getRhs()));
                }
                log.trace(sb.toString());
            }
        }
        
        return done;
    }
    
    /**
     * Prints an object of the internal model using the string representation
     * of the corresponding object in the external model.
     * 
     * @param o
     * @return
     */
    private String printInternalObject(Object o) {
        if(o instanceof Conjunction) {
            Conjunction con = (Conjunction)o;
            StringBuilder sb = new StringBuilder();
            AbstractConcept[] cons = con.getConcepts();
            sb.append(printInternalObject(cons[0]));
            for(int i = 1; i < cons.length; i++) {
                sb.append(" + ");
                sb.append(printInternalObject(cons[i]));
            }
            return sb.toString();
        } else if(o instanceof Existential) {
            Existential e = (Existential)o;
            AbstractConcept c = e.getConcept();
            int role = e.getRole();
            return factory.lookupRoleId(role)+"."+printInternalObject(c);
        } else if(o instanceof Datatype) {
            StringBuilder sb = new StringBuilder();
            Datatype d = (Datatype)o;
            String feature = factory.lookupFeatureId(d.getFeature());
            sb.append(feature.toString());
            sb.append(".(");
            AbstractLiteral literal = d.getLiteral();
            sb.append(literal);
            sb.append(")");
            return sb.toString();
        } else if(o instanceof Concept) {
            Object obj = factory.lookupConceptId(((Concept)o).hashCode());
            if(obj == au.csiro.ontology.model.NamedConcept.TOP) {
                return "TOP";
            } else if(obj == au.csiro.ontology.model.NamedConcept.BOTTOM) {
                return "BOTTOM";
            } else if(obj instanceof AbstractConcept) {
                return "<"+printInternalObject(obj)+">";
            } else {
                return obj.toString();
            }
        } else if(o instanceof Comparable<?>) {
            return o.toString();
        } else {
            throw new RuntimeException("Unexpected object with class "+
                    o.getClass().getName());
        }
    }

    /**
     * Adds a normalised term to the ontology.
     * 
     * @param term
     *            The normalised term.
     */
    protected void addTerm(NormalFormGCI term) {
        if (term instanceof NF1a) {
            final NF1a nf1 = (NF1a) term;
            final int a = nf1.lhsA();
            addTerms(ontologyNF1, a, nf1.getQueueEntry());
        } else if (term instanceof NF1b) {
            final NF1b nf1 = (NF1b) term;
            final int a1 = nf1.lhsA1();
            final int a2 = nf1.lhsA2();
            addTerms(ontologyNF1, a1, nf1.getQueueEntry1());
            addTerms(ontologyNF1, a2, nf1.getQueueEntry2());
        } else if (term instanceof NF2) {
            final NF2 nf2 = (NF2) term;
            addTerms(ontologyNF2, nf2);
        } else if (term instanceof NF3) {
            final NF3 nf3 = (NF3) term;
            addTerms(ontologyNF3, nf3);
        } else if (term instanceof NF4) {
            ontologyNF4.add((NF4) term);
        } else if (term instanceof NF5) {
            ontologyNF5.add((NF5) term);
        } else if (term instanceof NF6) {
            reflexiveRoles.add(((NF6) term).getR());
        } else if (term instanceof NF7) {
            final NF7 nf7 = (NF7) term;
            addTerms(ontologyNF7, nf7);
        } else if (term instanceof NF8) {
            final NF8 nf8 = (NF8) term;
            addTerms(ontologyNF8, nf8);
        } else {
            throw new IllegalArgumentException("Type of " + term
                    + " must be one of NF1 through NF8");
        }
    }

    /**
     * 
     * @param entries
     * @param a
     * @param queueEntry
     */
    protected void addTerms(final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> entries, final int a, 
            final IConjunctionQueueEntry queueEntry) {
        MonotonicCollection<IConjunctionQueueEntry> queueA = entries.get(a);
        if (null == queueA) {
            queueA = new MonotonicCollection<IConjunctionQueueEntry>(2);
            entries.put(a, queueA);
        }
        queueA.add(queueEntry);
    }

    /**
     * 
     * @param entries
     * @param nf2
     */
    protected void addTerms(
            final IConceptMap<MonotonicCollection<NF2>> entries, final NF2 nf2) {
        MonotonicCollection<NF2> set = entries.get(nf2.lhsA);
        if (null == set) {
            set = new MonotonicCollection<NF2>(2);
            entries.put(nf2.lhsA, set);
        }
        set.add(nf2);
    }

    /**
     * 
     * @param queue
     * @param nf3
     */
    protected void addTerms(final IConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>> queue,
            final NF3 nf3) {
        ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>> map = queue.get(nf3.lhsA);
        Collection<IConjunctionQueueEntry> entry;
        if (null == map) {
            map = new ConcurrentHashMap<Integer, Collection<IConjunctionQueueEntry>>(
                    factory.getTotalRoles());
            queue.put(nf3.lhsA, map);
            entry = null;
        } else {
            entry = map.get(nf3.lhsR);
        }
        if (null == entry) {
            entry = new HashSet<IConjunctionQueueEntry>();
            entry.add(nf3.getQueueEntry());
            map.put(nf3.lhsR, entry);
        } else {
            entry.add(nf3.getQueueEntry());
        }
    }
    
    protected void addTerms(final IConceptMap<MonotonicCollection<NF7>> entries, final NF7 nf7) {
        MonotonicCollection<NF7> set = entries.get(nf7.lhsA);
        if (null == set) {
            set = new MonotonicCollection<NF7>(2);
            entries.put(nf7.lhsA, set);
        }
        set.add(nf7);
    }

    /**
     * 
     * @param entries
     * @param nf8
     */
    protected void addTerms(final FeatureMap<MonotonicCollection<NF8>> entries, final NF8 nf8) {
        MonotonicCollection<NF8> set = entries.get(nf8.lhsD.getFeature());
        if (null == set) {
            set = new MonotonicCollection<NF8>(2);
            entries.put(nf8.lhsD.getFeature(), set);
        }
        set.add(nf8);
    }
    
    /**
     * 
     * @param incAxioms
     */
    public void loadIncremental(Set<Axiom> incAxioms) {
        
        // Normalise
        Set<Inclusion> norm = normalise(incAxioms);
        
        // Prepare temp structure for NF7 collapse - reload existing axioms affected by new ones based on added axioms
        for (Inclusion i : norm) {
            NormalFormGCI nf = i.getNormalForm();
            if(nf instanceof NF7) {
                NF7 nf7 = (NF7) nf;
                int a = nf7.lhsA;
                int f = nf7.rhsD.getFeature();
                
                Map<Integer, List<NF7>> m = tempNf7Map.get(a);
                if(m == null) {
                    m = new HashMap<Integer, List<NF7>>();
                    tempNf7Map.put(a,  m);
                }
                
                List<NF7> nf7s = m.get(f);
                if(nf7s == null) {
                    nf7s = new ArrayList<NF7>();
                    m.put(f, nf7s);
                }
                nf7s.add(nf7);
            }
        }
        
        for(IntIterator it = ontologyNF7.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            if(!tempNf7Map.containsKey(a)) continue;
            Map<Integer, List<NF7>> fs = tempNf7Map.get(a);
            MonotonicCollection<NF7> mc = ontologyNF7.get(a);
            for(Iterator<NF7> it2 = mc.iterator(); it2.hasNext(); ) {
                NF7 nf7 = it2.next();
                int f = nf7.rhsD.getFeature();
                if(fs.containsKey(f)) {
                    List<NF7> l = fs.get(f);
                    l.add(nf7);
                }
            }
        }
        
        for(Inclusion inc : norm) {
            NormalFormGCI nf = inc.getNormalForm();
            // Exclude NF7s - already collapsed
            if(!(nf instanceof NF7)) {
                as.addAxiom(nf);
                
                // Will work because there are no NF7s
                // TODO: fix how this works to make it more understandable
                addTerm(nf);
            }
        }
    }

    /**
     * Runs an incremental classification.
     * 
     * @return
     */
    public void classifyIncremental() {
        if(as.isEmpty()) return;
        
        // Clear any state from previous incremental classifications
        newContexts.clear();
        affectedContexts.clear();

        int numNewConcepts = 0;

        // Determine which contexts are affected
        for (NF1a i : as.getNf1aAxioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF1b i : as.getNf1bAxioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF2 i : as.getNf2Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF3 i : as.getNf3Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF4 i : as.getNf4Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF5 i : as.getNf5Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF6 i : as.getNf6Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF7 i : as.getNf7Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        for (NF8 i : as.getNf8Axioms()) {
            numNewConcepts = processInclusion(numNewConcepts, i);
        }
        
        if(log.isInfoEnabled()) log.info("Added " + numNewConcepts + " new concepts to the ontology");

        // TODO: this is potentially slow
        IConceptMap<IConceptSet> subsumptions = getSubsumptions();

        rePrimeNF1(as, subsumptions);
        rePrimeNF2(as, subsumptions);
        rePrimeNF3(as, subsumptions);
        rePrimeNF4(as, subsumptions);
        rePrimeNF5(as, subsumptions);
        rePrimeNF6(as, subsumptions);
        rePrimeNF7(as, subsumptions);
        rePrimeNF8(as, subsumptions);

        // Classify
        if(log.isInfoEnabled())
            log.info("Classifying incrementally with " + numThreads + " threads");
        
        if(log.isInfoEnabled())
            log.info("Running saturation");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int j = 0; j < numThreads; j++) {
            Runnable worker = new Worker(todo);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assert (todo.isEmpty());

        // Stop tracking changes in reactivated contexts
        for (Context ctx : affectedContexts) {
            ctx.endTracking();
        }

        affectedContexts.removeAll(newContexts);
        
        hasBeenIncrementallyClassified = true;
        as.clear();
        
        if(log.isTraceEnabled())
            log.trace("Processed " + contextIndex.size() + " contexts");
    }

    protected int processInclusion(int numNewConcepts, NormalFormGCI nf) {
        // Add a context to the context index for every new concept in the axioms being added incrementally
        int[] cids = nf.getConceptsInAxiom();

        for (int j = 0; j < cids.length; j++) {
            int cid = cids[j];
            if (!contextIndex.containsKey(cid)) {
                Context c = new Context(cid);
                contextIndex.put(cid, c);
                if (c.activate()) {
                    todo.add(c);
                }
                if (log.isTraceEnabled()) {
                    log.trace("Added context " + cid);
                }

                // Keep track of the newly added contexts
                newContexts.add(c);
                numNewConcepts++;
            }
        }
        return numNewConcepts;
    }

    /**
     * Processes the axioms in normal form 1 from a set of axioms added
     * incrementally and does the following:
     * <ol>
     * <li>Adds the axioms to the local map.</li>
     * <li>Calculates the new query entries derived from the addition of these
     * axioms.</li>
     * <li>Adds query entries to corresponding contexts and activates them.</li>
     * </ol>
     * 
     * @param as
     *            The set of axioms added incrementally.
     */
    private void rePrimeNF1(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        // NF1. A1 + ... + X + ... + An [ B
        // Q(A) += {A1 + ... + An -> B}, for all X in S(A)

        // Want the set <x, a> such that <x, a> in S and exists c such that
        // <a, c> in deltaOntologyNF1QueueEntries that is, we want to join S and
        // deltaOntologyNF1QueueEntries on S.col2 and
        // deltaOntologyNF1QueueEntries.key
        int size = as.getNf1aAxioms().size() + as.getNf1bAxioms().size();
        if (size == 0)
            return;
        IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> deltaNF1 = 
                new SparseConceptMap<MonotonicCollection<IConjunctionQueueEntry>>(
                size);
        for (NF1a nf1a : as.getNf1aAxioms()) {
            IConjunctionQueueEntry qe = nf1a.getQueueEntry();
            addTerms(deltaNF1, nf1a.lhsA(), qe);
        }

        for (NF1b nf1b : as.getNf1bAxioms()) {
            final int a1 = nf1b.lhsA1();
            final int a2 = nf1b.lhsA2();
            addTerms(deltaNF1, a1, nf1b.getQueueEntry1());
            addTerms(deltaNF1, a2, nf1b.getQueueEntry2());
        }

        // Get all the subsumptions a [ x
        for (final IntIterator aItr = subsumptions.keyIterator(); aItr
                .hasNext();) {
            final int a = aItr.next();

            final IConceptSet Sa = subsumptions.get(a);

            for (final IntIterator xItr = Sa.iterator(); xItr.hasNext();) {
                final int x = xItr.next();

                // If any of the new axioms is of the form x [ y then add
                // an entry
                if (deltaNF1.containsKey(x)) {
                    final IMonotonicCollection<IConjunctionQueueEntry> set = deltaNF1
                            .get(x);

                    for (final IConjunctionQueueEntry entry : set) {
                        // Add to corresponding context and activate
                        Context ctx = contextIndex.get(a);
                        ctx.addConceptQueueEntry(entry);
                        affectedContexts.add(ctx);
                        ctx.startTracking();
                        if (ctx.activate()) {
                            todo.add(ctx);
                        }
                    }
                }
            }
        }
    }

    private void rePrimeNF2(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        // NF2. A [ r.B
        // Q(A) += {-> r.B}, for all X in S(A)

        int size = as.getNf2Axioms().size();
        if (size == 0)
            return;
        IConceptMap<MonotonicCollection<NF2>> deltaNF2 = 
                new SparseConceptMap<MonotonicCollection<NF2>>(size);
        for (NF2 nf2 : as.getNf2Axioms()) {
            addTerms(deltaNF2, nf2);
        }

        for (final IntIterator aItr = subsumptions.keyIterator(); aItr.hasNext();) {
            final int a = aItr.next();
            Context ctx = contextIndex.get(a);

            final IConceptSet Sa = subsumptions.get(a);

            for (final IntIterator xItr = Sa.iterator(); xItr.hasNext();) {
                final int x = xItr.next();

                if (deltaNF2.containsKey(x)) {
                    final IMonotonicCollection<NF2> set = deltaNF2.get(x);
                    for (NF2 entry : set) {
                        ctx.addRoleQueueEntry(entry);
                        affectedContexts.add(ctx);
                        ctx.startTracking();
                        if (ctx.activate()) {
                            todo.add(ctx);
                        }
                    }
                }
            }
        }
    }

    private void rePrimeNF3(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        // NF3. r.X [ Y
        // Q(A) += {-> Y}, for all (A,B) in R(r) and X in S(B)

        int size = as.getNf3Axioms().size();
        if (size == 0)
            return;
        IConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>> deltaNF3 = 
                new SparseConceptMap<ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>>>(size);
        for (NF3 nf3 : as.getNf3Axioms()) {
            addTerms(deltaNF3, nf3);
        }

        for (final IntIterator xItr = deltaNF3.keyIterator(); xItr.hasNext();) {
            final int x = xItr.next();
            final ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>> entries = deltaNF3
                    .get(x);

            final Set<Integer> keySet = entries.keySet();
            for (int r : keySet) {
                for (final IConjunctionQueueEntry entry : entries.get(r)) {
                    for (final IntIterator aItr = subsumptions.keyIterator(); aItr
                            .hasNext();) {
                        final int a = aItr.next();
                        boolean addIt = false;

                        // Get all of a's successors with role r
                        Context aCtx = contextIndex.get(a);
                        IConceptSet cs = aCtx.getSucc().lookupConcept(r);
                        for (final IntIterator bItr = cs.iterator(); bItr
                                .hasNext();) {
                            final int b = bItr.next();

                            if (subsumptions.get(b).contains(x)) {
                                addIt = true;
                                break;
                            }
                        }

                        if (addIt) {
                            aCtx.addConceptQueueEntry(entry);
                            affectedContexts.add(aCtx);
                            aCtx.startTracking();
                            if (aCtx.activate()) {
                                todo.add(aCtx);
                            }
                        }
                    }
                }
            }
        }
    }

    private void rePrimeNF4(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        // NF4. r [ s
        // Q(A) += {-> s.B}, for all (A,B) in R(r)

        int size = as.getNf4Axioms().size();
        if (size == 0)
            return;
        IMonotonicCollection<NF4> deltaNF4 = new MonotonicCollection<NF4>(size);
        for (NF4 nf4 : as.getNf4Axioms()) {
            deltaNF4.add(nf4);
        }

        for (final NF4 nf4 : deltaNF4) {
            for (final IntIterator aItr = subsumptions.keyIterator(); aItr
                    .hasNext();) {
                final int a = aItr.next();

                Context aCtx = contextIndex.get(a);
                IConceptSet cs = aCtx.getSucc().lookupConcept(nf4.getR());

                for (final IntIterator bItr = cs.iterator(); bItr.hasNext();) {
                    final int b = bItr.next();

                    IRoleQueueEntry entry = new IRoleQueueEntry() {
                        /**
                         * Serialisation version.
                         */
                        private static final long serialVersionUID = 1L;

                        public int getR() {
                            return nf4.getS();
                        }

                        public int getB() {
                            return b;
                        }

                    };
                    aCtx.addRoleQueueEntry(entry);
                    affectedContexts.add(aCtx);
                    aCtx.startTracking();
                    if (aCtx.activate()) {
                        todo.add(aCtx);
                    }
                }
            }
        }
    }

    private void rePrimeNF5(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        // NF5. r o s [ t
        // Q(A) += {-> t.C}, for all (A,B) in R(r), (B,C) in R(s), (A,C) not in
        // R(t)
        int size = as.getNf5Axioms().size();
        if (size == 0)
            return;
        IMonotonicCollection<NF5> deltaNF5 = new MonotonicCollection<NF5>(size);
        for (NF5 nf5 : as.getNf5Axioms()) {
            deltaNF5.add(nf5);
        }

        for (final NF5 nf5 : deltaNF5) {
            final int t = nf5.getT();

            for (final IntIterator aItr = subsumptions.keyIterator(); aItr.hasNext();) {
                final int a = aItr.next();

                Context aCtx = contextIndex.get(a);

                for (final IntIterator bItr = aCtx.getSucc().lookupConcept(nf5.getR()).iterator(); bItr.hasNext();) {
                    final int b = bItr.next();

                    Context bCtx = contextIndex.get(b);

                    for (final IntIterator cItr = bCtx.getSucc().lookupConcept(nf5.getS()).iterator(); cItr.hasNext();) {
                        final int c = cItr.next();

                        if (!aCtx.getSucc().lookupConcept(t).contains(c)) {
                            final IRoleQueueEntry entry = new IRoleQueueEntry() {

                                /**
                                 * Serialisation version.
                                 */
                                private static final long serialVersionUID = 1L;

                                public int getR() {
                                    return t;
                                }

                                public int getB() {
                                    return c;
                                }
                            };
                            aCtx.addRoleQueueEntry(entry);
                            affectedContexts.add(aCtx);
                            aCtx.startTracking();
                            if (aCtx.activate()) {
                                todo.add(aCtx);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * These are reflexive role axioms. If an axiom of this kind is added, then
     * an "external edge" must be added to all the contexts that contain that
     * role and don't contain a successor to themselves.
     * 
     * @param as
     * @param subsumptions
     */
    private void rePrimeNF6(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        int size = as.getNf6Axioms().size();
        if (size == 0)
            return;
        IConceptSet deltaNF6 = new SparseConceptSet(size);
        for (NF6 nf6 : as.getNf6Axioms()) {
            deltaNF6.add(nf6.getR());
        }

        for (IntIterator it = deltaNF6.iterator(); it.hasNext();) {
            int role = it.next();
            for (IntIterator it2 = contextIndex.keyIterator(); it2.hasNext();) {
                int concept = it2.next();
                Context ctx = contextIndex.get(concept);
                if (ctx.getSucc().containsRole(role) && !ctx.getSucc().lookupConcept(role).contains(concept)) {
                    ctx.processExternalEdge(role, concept);
                    affectedContexts.add(ctx);
                    ctx.startTracking();
                    if (ctx.activate()) {
                        todo.add(ctx);
                    }
                }
            }
        }
    }

    /**
     * These axioms are of the form A [ f.(o, v) and are indexed by A. A feature queue element must be added to the 
     * contexts that have A in their subsumptions.
     * 
     * @param as
     * @param subsumptions
     */
    private void rePrimeNF7(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        int size = as.getNf7Axioms().size();
        if (size == 0)
            return;
        IConceptMap<MonotonicCollection<NF7>> deltaNF7 = new SparseConceptMap<MonotonicCollection<NF7>>(size);
        for (NF7 nf7 : as.getNf7Axioms()) {
            int a = nf7.lhsA;
            MonotonicCollection<NF7> list = deltaNF7.get(a);
            if(list == null) {
                list = new MonotonicCollection<NF7>(2);
                deltaNF7.put(a, list);
            }
            list.add(nf7);
        }

        // Get all the subsumptions a [ x
        for (final IntIterator aItr = subsumptions.keyIterator(); aItr.hasNext();) {
            final int a = aItr.next();

            final IConceptSet Sa = subsumptions.get(a);

            for (final IntIterator xItr = Sa.iterator(); xItr.hasNext();) {
                final int x = xItr.next();

                // If any of the new axioms is of the form x [ y then add
                // an entry
                if (deltaNF7.containsKey(x)) {
                    final IMonotonicCollection<NF7> set = deltaNF7.get(x);

                    for (final NF7 entry : set) {
                        // Add to corresponding context and activate
                        Context ctx = contextIndex.get(a);
                        ctx.addFeatureQueueEntry(entry);
                        affectedContexts.add(ctx);
                        ctx.startTracking();
                        if (ctx.activate()) {
                            todo.add(ctx);
                        }
                    }
                }
            }
        }
    }

    /**
     * TODO: check this!
     * 
     * @param as
     * @param subsumptions
     */
    private void rePrimeNF8(AxiomSet as, IConceptMap<IConceptSet> subsumptions) {
        int size = as.getNf8Axioms().size();
        if (size == 0) return;
        
        FeatureMap<MonotonicCollection<NF8>> deltaNF8 = new FeatureMap<MonotonicCollection<NF8>>(size);
        for (NF8 nf8 : as.getNf8Axioms()) {
            addTerms(deltaNF8, nf8);
        }

        FeatureSet fs = deltaNF8.keySet();
        for (int fid = fs.nextSetBit(0); fid >= 0; fid = fs.nextSetBit(fid+1)) {
            for (IntIterator it = ontologyNF7.keyIterator(); it.hasNext();) {
                int a = it.next();
                Context aCtx = contextIndex.get(a);

                for (Iterator<NF7> i = ontologyNF7.get(a).iterator(); i.hasNext();) {
                    NF7 nf7 = i.next();
                    if (nf7.rhsD.getFeature() == fid) {
                        aCtx.addFeatureQueueEntry(nf7);
                        affectedContexts.add(aCtx);
                        aCtx.startTracking();
                        if (aCtx.activate()) {
                            todo.add(aCtx);
                        }
                    }
                }
            } 
        }
    }

    /**
     * Starts the concurrent classification process.
     */
    public void classify() {
        long start = System.currentTimeMillis();
        if(log.isInfoEnabled())
            log.info("Classifying with " + numThreads + " threads");

        Context.init(NormalisedOntology.this);

        // Create contexts for init concepts in the ontology
        int numConcepts = factory.getTotalConcepts();
        for (int i = 0; i < numConcepts; i++) {
            Context c = new Context(i);
            contextIndex.put(i, c);
            if (c.activate()) {
                todo.add(c);
            }
            if(log.isTraceEnabled()) {
                log.trace("Added context " + i);
            }
        }
        
        if(log.isInfoEnabled())
            log.info("Running saturation");
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int j = 0; j < numThreads; j++) {
            Runnable worker = new Worker(todo);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assert (todo.isEmpty());

        if (log.isTraceEnabled()) {
            log.trace("Processed " + contextIndex.size() + " contexts");
        }
        
        hasBeenIncrementallyClassified = false;
        Statistics.INSTANCE.setTime("classification",
                System.currentTimeMillis() - start);
    }

    public IConceptMap<IConceptSet> getSubsumptions() {
        IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(
                factory.getTotalConcepts());
        // Collect subsumptions from context index
        for (IntIterator it = contextIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            Context ctx = contextIndex.get(key);
            res.put(key, ctx.getS());
        }
        return res;
    }

    /**
     * Returns the subsumptions for the new concepts added in an incremental
     * classification.
     * 
     * @return
     */
    public IConceptMap<IConceptSet> getNewSubsumptions() {
        IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(
                newContexts.size());
        // Collect subsumptions from new contexts
        for (Context ctx : newContexts) {
            res.put(ctx.getConcept(), ctx.getS());
        }
        return res;
    }

    /**
     * Returns the subsumptions for the existing concepts that have additional
     * subsumptions due to the axioms added in an incremental classification.
     * 
     * @return
     */
    public IConceptMap<IConceptSet> getAffectedSubsumptions() {
        int size = 0;
        for (Context ctx : affectedContexts) {
            if (ctx.hasNewSubsumptions()) {
                size++;
            }
        }

        IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(size);
        // Collect subsumptions from affected contexts
        for (IntIterator it = contextIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            Context ctx = contextIndex.get(key);
            if (ctx.hasNewSubsumptions()) {
                res.put(key, ctx.getS());
            }
        }
        return res;
    }
    
    /**
     * Collects all the information contained in the concurrent R structures in
     * every context and returns a single R structure with all their content.
     * 
     * @return R
     */
    public R getRelationships() {
        R r = new R(factory.getTotalConcepts(), factory.getTotalRoles());
        
        // Collect subsumptions from context index
        for (IntIterator it = contextIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            Context ctx = contextIndex.get(key);
            int concept = ctx.getConcept();
            CR pred = ctx.getPred();
            CR succ = ctx.getSucc();
            
            int[] predRoles = pred.getRoles();
            for(int i = 0; i < predRoles.length; i++) {
                IConceptSet cs = pred.lookupConcept(predRoles[i]);
                for(IntIterator it2 = cs.iterator(); it2.hasNext(); ) {
                    int predC = it2.next();
                    r.store(predC, predRoles[i], concept);
                }
            }
            
            int[] succRoles = succ.getRoles();
            for(int i = 0; i < succRoles.length; i++) {
                IConceptSet cs = succ.lookupConcept(succRoles[i]);
                for(IntIterator it2 = cs.iterator(); it2.hasNext(); ) {
                    int succC = it2.next();
                    r.store(concept, succRoles[i], succC);
                }
            }
        }
        
        return r;
    }

    /**
     * 
     */
    public void printStats() {
        System.err.println("stats");
        int count1 = countKeys(ontologyNF1);
        System.err.println("ontologyNF1QueueEntries: #keys=" + count1
                + ", #Concepts=" + factory.getTotalConcepts() + " ratio="
                + ((double) count1 / factory.getTotalConcepts()));
        int count2 = countKeys(ontologyNF2);
        System.err.println("ontologyNF2: #keys=" + count2 + ", #Concepts="
                + factory.getTotalConcepts() + " ratio="
                + ((double) count2 / factory.getTotalConcepts()));
        int count3 = countKeys(ontologyNF3);
        System.err.println("ontologyNF3QueueEntries: #keys=" + count3
                + ", #Concepts=" + factory.getTotalConcepts() + " ratio="
                + ((double) count3 / factory.getTotalConcepts()));
    }

    /**
     * 
     * @param map
     * @return
     */
    private int countKeys(IConceptMap<?> map) {
        int count = 0;
        for (IntIterator itr = map.keyIterator(); itr.hasNext();) {
            itr.next();
            count++;
        }
        return count;
    }

    public IFactory getFactory() {
        return factory;
    }
    
    /**
     * Returns the stated axioms in the ontology.
     * 
     * @return
     */
    public Set<Axiom> getStatedAxioms() {
        Set<Axiom> res = new HashSet<Axiom>();
        // These terms are of the form A n Bi [ B and are indexed by A.
        for(IntIterator it = ontologyNF1.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            MonotonicCollection<IConjunctionQueueEntry> mc = ontologyNF1.get(a);
            for(Iterator<IConjunctionQueueEntry> it2 = mc.iterator(); 
                    it2.hasNext(); ) {
                IConjunctionQueueEntry nf1 = it2.next();
                int bi = nf1.getBi();
                int b = nf1.getB();
                // Build the axiom
                Object oa = factory.lookupConceptId(a);
                Object ob = factory.lookupConceptId(b);
                if(bi == IFactory.TOP_CONCEPT) {
                    res.add(new ConceptInclusion(transform(oa), transform(ob)));
                } else {
                    Object obi = factory.lookupConceptId(bi);
                    res.add(new ConceptInclusion(
                        new au.csiro.ontology.model.Conjunction(
                            new Concept[] {transform(oa), transform(obi)}), transform(ob)
                    ));
                }
            }
        }
        
        // These terms are of the form A [ r.B and are indexed by A.
        for(IntIterator it = ontologyNF2.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            MonotonicCollection<NF2> mc = ontologyNF2.get(a);
            for(Iterator<NF2> it2 = mc.iterator(); it2.hasNext(); ) {
                NF2 nf2 = it2.next();
                Object oa = factory.lookupConceptId(nf2.lhsA);
                String r = factory.lookupRoleId(nf2.rhsR).toString();
                Object ob = factory.lookupConceptId(nf2.rhsB);
                res.add(new ConceptInclusion(
                    transform(oa),
                    new au.csiro.ontology.model.Existential(new NamedRole(r), transform(ob))
                ));
            }
        }
        
        // These terms are of the form r.A [ b and indexed by A.
        for(IntIterator it = ontologyNF3.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>> mc = 
                    ontologyNF3.get(a);
            Set<Integer> keys = mc.keySet();
            for (int i : keys) {
                Collection<IConjunctionQueueEntry> cc = mc.get(i);
                for(Iterator<IConjunctionQueueEntry> it2 = cc.iterator(); 
                        it2.hasNext(); ) {
                    IConjunctionQueueEntry nf3 = it2.next();
                    Object oa = factory.lookupConceptId(a);
                    String r = factory.lookupRoleId(i).toString();
                    Object ob = factory.lookupConceptId(nf3.getB());
                    res.add(new ConceptInclusion(
                        new au.csiro.ontology.model.Existential(new NamedRole(r), transform(ob)),
                        transform(oa)  
                    ));
                }
            }
        }
        
        for(Iterator<NF4> it = ontologyNF4.iterator(); it.hasNext(); ) {
            NF4 nf4 = it.next();
            int r = nf4.getR();
            int s = nf4.getS();
            
            res.add(
                new RoleInclusion(
                    new NamedRole(factory.lookupRoleId(r).toString()),
                    new NamedRole(factory.lookupRoleId(s).toString())
                )
            );
        }
        
        for(Iterator<NF5> it = ontologyNF5.iterator(); it.hasNext(); ) {
            NF5 nf5 = it.next();
            int r = nf5.getR();
            int s = nf5.getS();
            int t = nf5.getT();
            
            res.add(
                new RoleInclusion(
                    new Role[] {
                            new NamedRole(factory.lookupRoleId(r).toString()),
                            new NamedRole(factory.lookupRoleId(s).toString())
                    },
                    new NamedRole(factory.lookupRoleId(t).toString())
                )
            );
        }
        
        for(IntIterator it = reflexiveRoles.iterator(); it.hasNext(); ) {
            int r = it.next();
            res.add(
                new RoleInclusion(
                    new Role[] {},
                    new NamedRole(factory.lookupRoleId(r).toString())
                )
            );
        }
        
        // These terms are of the form A [ f.(o, v) and are indexed by A.
        for(IntIterator it = ontologyNF7.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            MonotonicCollection<NF7> mc = ontologyNF7.get(a);
            for(Iterator<NF7> it2 = mc.iterator(); it2.hasNext(); ) {
                NF7 nf7 = it2.next();
                res.add(new ConceptInclusion(
                    transform(factory.lookupConceptId(a)),
                    transform(nf7.rhsD)
                ));
            }
        }
        
        // These terms are of the form f.(o, v) [ A. These are indexed by f.
        FeatureSet keys = ontologyNF8.keySet();
        for (int i = keys.nextSetBit(0); i >= 0; i = keys.nextSetBit(i+1)) {
            MonotonicCollection<NF8> mc = ontologyNF8.get(i);
            for(Iterator<NF8> it2 = mc.iterator(); it2.hasNext(); ) {
                NF8 nf8 = it2.next();
                res.add(new ConceptInclusion(
                    transform(nf8.lhsD),
                    transform(factory.lookupConceptId(nf8.rhsB))
                ));
            }
        }
        
        return res;
    }
    
    /**
     * @param o
     * @return
     */
    public Concept transform(Object o) {
        if(o instanceof Conjunction) {
            Conjunction con = (Conjunction)o;
            List<Concept> concepts = new ArrayList<Concept>();
            for(AbstractConcept ac : con.getConcepts()) {
                concepts.add(transform(ac));
            }
            return new au.csiro.ontology.model.Conjunction(concepts);
        } else if(o instanceof Existential) {
            Existential e = (Existential)o;
            AbstractConcept c = e.getConcept();
            Concept iconcept = transform(c);
            int role = e.getRole();
            NamedRole irole = new NamedRole(factory.lookupRoleId(role).toString());
            return new au.csiro.ontology.model.Existential(irole, iconcept);
        } else if(o instanceof Datatype) {
            Datatype d = (Datatype) o;
            String feature = factory.lookupFeatureId(d.getFeature());
            AbstractLiteral literal = d.getLiteral();
            
            if(literal instanceof DateLiteral) {
                return new au.csiro.ontology.model.Datatype(new NamedFeature(feature), d.getOperator(), 
                        new au.csiro.ontology.model.DateLiteral(((DateLiteral) literal).getValue()));
            } else if(literal instanceof DecimalLiteral) {
                return new au.csiro.ontology.model.Datatype(new NamedFeature(feature), d.getOperator(), 
                        new au.csiro.ontology.model.DecimalLiteral(((DecimalLiteral) literal).getValue()));
            } else if(literal instanceof IntegerLiteral) {
                return new au.csiro.ontology.model.Datatype(new NamedFeature(feature), d.getOperator(), 
                        new au.csiro.ontology.model.IntegerLiteral(((IntegerLiteral) literal).getValue()));
            } else if(literal instanceof StringLiteral) {
                return new au.csiro.ontology.model.Datatype(new NamedFeature(feature), d.getOperator(), 
                        new au.csiro.ontology.model.StringLiteral(((StringLiteral) literal).getValue()));
            } else {
                throw new RuntimeException("Unexpected literal " + literal.getClass().getName());
            }
            
        } else if(o instanceof Concept) {
            Object obj = factory.lookupConceptId(((Concept)o).hashCode());
            return transform(obj);
        } else if(o instanceof String) {
            return new au.csiro.ontology.model.NamedConcept((String) o);
        } else {
            throw new RuntimeException("Unexpected object with class "+o.getClass().getName());
        }
    }

    /**
     * @param numThreads the numThreads to set
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
    
    protected void buildTaxonomyConcurrent() {
        long start = System.currentTimeMillis();
        
        // Part 1 - creates equivalent and direct sets
        Queue<Integer> todo = new ConcurrentLinkedQueue<Integer>();
        for (IntIterator itr = contextIndex.keyIterator(); itr.hasNext();) {
            todo.add(itr.next());
        }
        
        final ConcurrentMap<Integer, IConceptSet> equiv = 
                new ConcurrentHashMap<Integer, IConceptSet>();
        final ConcurrentMap<Integer, IConceptSet> direc = 
                new ConcurrentHashMap<Integer, IConceptSet>();
        
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        for (int j = 0; j < numThreads; j++) {
            Runnable worker = new TaxonomyWorker1(contextIndex, 
                    equiv, direc, factory, todo);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        
        assert (todo.isEmpty());
        
        int bottomConcept = CoreFactory.BOTTOM_CONCEPT;
        if (!equiv.containsKey(bottomConcept)) {
            TaxonomyWorker1.addToSet(equiv, bottomConcept, bottomConcept);
        }

        int topConcept = CoreFactory.TOP_CONCEPT;
        if (!equiv.containsKey(topConcept)) {
            TaxonomyWorker1.addToSet(equiv, topConcept, topConcept);
        }
        
        Statistics.INSTANCE.setTime("taxonomy 1",
                System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        
        // Part 2 - Creates a node per equivalent concepts
        conceptNodeIndex = new ConcurrentHashMap<String, Node>();
        
        Node top = null;
        Node bottom = null;
        
        IConceptSet processed = new FastConceptHashSet();
        Set<Node> nodeSet = new HashSet<Node>();
        
        for(int key : equiv.keySet()) {
            if(processed.contains(key)) continue;
            IConceptSet equivs = equiv.get(key);
            processed.addAll(equivs);
            
            Node n = new Node();
            for(IntIterator it = equivs.iterator(); it.hasNext(); ) {
                int val = it.next();
                String tval = factory.lookupConceptId(val).toString();
                n.getEquivalentConcepts().add(tval);
                conceptNodeIndex.put(tval, n);

                if (val == CoreFactory.TOP_CONCEPT)
                    top = n;
                if (val == CoreFactory.BOTTOM_CONCEPT)
                    bottom = n;
            }
            nodeSet.add(n);
        }
            
        if(top == null) {
            top = new Node();
            top.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.TOP);
        }
        
        if(bottom == null) {
            bottom = new Node();
            bottom.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.BOTTOM);
        }
        
        Statistics.INSTANCE.setTime("taxonomy 2",
                System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        
        // Step 3 - Connects nodes
        Queue<Node> todo2 = new ConcurrentLinkedQueue<Node>(nodeSet);
        executor = Executors.newFixedThreadPool(numThreads);
        for (int j = 0; j < numThreads; j++) {
            Runnable worker = new TaxonomyWorker2(factory, 
                    conceptNodeIndex, direc, todo2, nodeSet);
            executor.execute(worker);
        }

        executor.shutdown();
        while (!executor.isTerminated()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        assert (todo2.isEmpty());
        
        Statistics.INSTANCE.setTime("taxonomy 3",
                System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        
        // Connect bottom
        nodeSet.remove(bottom);
        bottom.getParents().addAll(nodeSet);
        for(Node n : nodeSet) {
            n.getChildren().add(bottom);
        }
        
        Statistics.INSTANCE.setTime("taxonomy connect bottom",
                System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        
        // Connect top
        for (String key : conceptNodeIndex.keySet()) {
            if (key.equals(au.csiro.ontology.model.NamedConcept.TOP) || 
                    key.equals(au.csiro.ontology.model.NamedConcept.BOTTOM))
                continue;
            Node node = conceptNodeIndex.get(key);
            if (node.getParents().isEmpty()) {
                node.getParents().add(top);
                top.getChildren().add(node);
            }
        }

        // TODO: deal with special case where only top and bottom are present.
        Statistics.INSTANCE.setTime("taxonomy connect top", System.currentTimeMillis() - start);
    }
    
    private void addToSet(IConceptMap<IConceptSet> map, int key, int val) {
        IConceptSet set = map.get(key);
        if (set == null) {
            set = new SparseConceptHashSet();
            map.put(key, set);
        }
        set.add(val);
    }
    
    protected void buildTaxonomySequential() {
        long start = System.currentTimeMillis();
        
        final IConceptMap<IConceptSet> subsumptions = getSubsumptions();

        // Keep only the subsumptions that involve real atomic concepts
        IConceptMap<IConceptSet> cis = new SparseConceptMap<IConceptSet>(
                factory.getTotalConcepts());

        for (IntIterator itr = subsumptions.keyIterator(); itr.hasNext();) {
            final int X = itr.next();
            if (!factory.isVirtualConcept(X)) {
                IConceptSet set = new SparseConceptHashSet();
                cis.put(X, set);
                for (IntIterator it = subsumptions.get(X).iterator(); it
                        .hasNext();) {
                    int next = it.next();
                    if (!factory.isVirtualConcept(next)) {
                        set.add(next);
                    }
                }
            }
        }

        IConceptMap<IConceptSet> equiv = new SparseConceptMap<IConceptSet>(
                factory.getTotalConcepts());
        IConceptMap<IConceptSet> direc = new SparseConceptMap<IConceptSet>(
                factory.getTotalConcepts());

        // Build equivalent and direct concept sets
        for (IntIterator itr = cis.keyIterator(); itr.hasNext();) {
            final int a = itr.next();

            for (IntIterator itr2 = cis.get(a).iterator(); itr2.hasNext();) {
                int c = itr2.next();
                IConceptSet cs = cis.get(c);

                if (c == IFactory.BOTTOM_CONCEPT) {
                    addToSet(equiv, a, c);
                } else if (cs != null && cs.contains(a)) {
                    addToSet(equiv, a, c);
                } else {
                    boolean isDirect = true;
                    IConceptSet d = direc.get(a);
                    if (d != null) {
                        IConceptSet toRemove = new SparseConceptHashSet();
                        for (IntIterator itr3 = d.iterator(); itr3.hasNext();) {
                            int b = itr3.next();
                            IConceptSet bs = cis.get(b);
                            if (bs != null && bs.contains(c)) {
                                isDirect = false;
                                break;
                            }
                            if (cs != null && cs.contains(b)) {
                                toRemove.add(b);
                            }
                        }
                        d.removeAll(toRemove);
                    }
                    if (isDirect) {
                        addToSet(direc, a, c);
                    }
                };
            }
        }

        int bottomConcept = CoreFactory.BOTTOM_CONCEPT;
        if (!equiv.containsKey(bottomConcept)) {
            addToSet(equiv, bottomConcept, bottomConcept);
        }

        int topConcept = CoreFactory.TOP_CONCEPT;
        if (!equiv.containsKey(topConcept)) {
            addToSet(equiv, topConcept, topConcept);
        }

        // Introduce one taxonomy node for each distinct class of equivalent
        // concepts
        conceptNodeIndex = new HashMap<String, Node>();
        
        Node top = new Node();
        top.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.TOP);
        
        Node bottom = new Node();
        bottom.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.BOTTOM);

        for (IntIterator it = equiv.keyIterator(); it.hasNext();) {
            int key = it.next();
            IConceptSet equivs = equiv.get(key);
            // Check if any of the equivalent classes is already part of an
            // equivalent node
            Node n = null;
            for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                String e = factory.lookupConceptId(it2.next()).toString();
                if (conceptNodeIndex.containsKey(e)) {
                    n = conceptNodeIndex.get(e);
                    break;
                }
            }

            if (n == null) {
                n = new Node();
            }
            n.getEquivalentConcepts().add(factory.lookupConceptId(key).toString());
            for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                n.getEquivalentConcepts().add(factory.lookupConceptId(it2.next()).toString());
            }
            
            for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                int e = it2.next();
                if (e == CoreFactory.TOP_CONCEPT)
                    top = n;
                if (e == CoreFactory.BOTTOM_CONCEPT)
                    bottom = n;
                conceptNodeIndex.put(factory.lookupConceptId(e).toString(), n);
            }
        }

        // Connect the nodes according to the direct super-concept relationships
        Set<Node> processed = new HashSet<Node>();
        for (String key : conceptNodeIndex.keySet()) {
            Node node = conceptNodeIndex.get(key);
            if (processed.contains(node) || node == top || node == bottom)
                continue;
            processed.add(node);
            for (String c : node.getEquivalentConcepts()) {
                // Get direct super-concepts
                IConceptSet dc = direc.get(factory.getConcept(c));
                if (dc != null) {
                    for (IntIterator it3 = dc.iterator(); it3.hasNext();) {
                        int d = it3.next();
                        Node parent = conceptNodeIndex.get(factory.lookupConceptId(d));
                        if (parent != null) {
                            node.getParents().add(parent);
                            parent.getChildren().add(node);
                        }
                    }
                }
            }
        }
        processed = null;

        // Add bottom
        if (bottom == null) {
            bottom = new Node();
            bottom.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.BOTTOM);
            conceptNodeIndex.put(au.csiro.ontology.model.NamedConcept.BOTTOM, bottom);
        }

        for (String key : conceptNodeIndex.keySet()) {
            if (key == au.csiro.ontology.model.NamedConcept.TOP || key == au.csiro.ontology.model.NamedConcept.BOTTOM)
                continue;
            Node node = conceptNodeIndex.get(key);
            if (node.getEquivalentConcepts().contains(au.csiro.ontology.model.NamedConcept.BOTTOM))
                continue;
            if (node.getChildren().isEmpty()) {
                bottom.getParents().add(node);
                node.getChildren().add(bottom);
            }
        }

        // Add top
        if (top == null) {
            top = new Node();
            top.getEquivalentConcepts().add(au.csiro.ontology.model.NamedConcept.TOP);
            conceptNodeIndex.put(au.csiro.ontology.model.NamedConcept.TOP, top);
        }

        for (String key : conceptNodeIndex.keySet()) {
            if (key == au.csiro.ontology.model.NamedConcept.TOP || 
                    key == au.csiro.ontology.model.NamedConcept.BOTTOM)
                continue;
            Node node = conceptNodeIndex.get(key);
            if (node.getParents().isEmpty()) {
                node.getParents().add(top);
                top.getChildren().add(node);
            }
        }

        equiv = null;
        direc = null;

        // TODO: deal with special case where only top and bottom are present.
        Statistics.INSTANCE.setTime("taxonomy construction", System.currentTimeMillis() - start);
    }
    
    /**
     * Calculates the taxonomy after classification.
     * 
     * @return
     */
    public void buildTaxonomy() {
        
        // Determine if a full or incremental calculation is required
        if(!hasBeenIncrementallyClassified) {
            buildTaxonomyConcurrent();
        } else {
            final IConceptMap<IConceptSet> newConceptSubs = getNewSubsumptions();
            final IConceptMap<IConceptSet> affectedConceptSubs = getAffectedSubsumptions();
            
            // 1. Keep only the subsumptions that involve real atomic concepts
            IConceptMap<IConceptSet> allNew = new SparseConceptMap<IConceptSet>(newConceptSubs.size());

            IConceptMap<IConceptSet> allAffected = new SparseConceptMap<IConceptSet>(newConceptSubs.size());

            for (IntIterator itr = newConceptSubs.keyIterator(); itr.hasNext();) {
                final int x = itr.next();
                if (!factory.isVirtualConcept(x)) {
                    IConceptSet set = new SparseConceptHashSet();
                    allNew.put(x, set);
                    for (IntIterator it = newConceptSubs.get(x).iterator(); it.hasNext();) {
                        int next = it.next();
                        if (!factory.isVirtualConcept(next)) {
                            set.add(next);
                        }
                    }
                }
            }

            for (IntIterator itr = affectedConceptSubs.keyIterator(); itr.hasNext();) {
                final int x = itr.next();
                if (!factory.isVirtualConcept(x)) {
                    IConceptSet set = new SparseConceptHashSet();
                    allAffected.put(x, set);
                    for (IntIterator it = affectedConceptSubs.get(x).iterator(); it.hasNext();) {
                        int next = it.next();
                        if (!factory.isVirtualConcept(next)) {
                            set.add(next);
                        }
                    }
                }
            }
            
            // 2. Create nodes for new concepts and connect to node hierarchy
            // a. First create the nodes and add to index
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final String key = factory.lookupConceptId(itr.next()).toString();
                Node cn = new Node();
                cn.getEquivalentConcepts().add(key);
                conceptNodeIndex.put(key, cn);
            }

            // b. Now connect the nodes disregarding redundant connections
            Node bottomNode = conceptNodeIndex.get(au.csiro.ontology.model.NamedConcept.BOTTOM);
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                int id = itr.next();
                final String key = factory.lookupConceptId(id).toString();
                Node cn = conceptNodeIndex.get(key);
                IConceptSet parents = allNew.get(id);
                for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                    // Create a connection to each parent
                    int parentId = itr2.next();
                    if (parentId == id)
                        continue;
                    Node parent = conceptNodeIndex.get(factory.lookupConceptId(parentId));
                    cn.getParents().add(parent);
                    parent.getChildren().add(cn);
                    // All nodes that get new children and are connected to BOTTOM
                    // must be disconnected
                    if (parent.getChildren().contains(bottomNode)) {
                        parent.getChildren().remove(bottomNode);
                        bottomNode.getParents().remove(parent);
                    }
                }
            }
            
            Set<Integer> toRemoveFromAffected = new HashSet<Integer>();
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int id = itr.next();
                final String key = factory.lookupConceptId(id).toString();
                Node cn = conceptNodeIndex.get(key);
                IConceptSet parents = allAffected.get(id);
                
                if(parents.contains(IFactory.BOTTOM_CONCEPT)) {
                    // Special case - bottom is parent
                    
                    // a. add equivalents to bottom node
                    bottomNode.getEquivalentConcepts().addAll(cn.getEquivalentConcepts());
                    
                    Set<Node> tempParents = cn.getParents();
                    Set<Node> tempChildren = cn.getChildren();
                    
                    // b. reconnect parents to children
                    for(Node parent : tempParents) {
                        parent.getChildren().remove(cn);
                        parent.getChildren().addAll(tempChildren);
                    }
                    
                    for(Node child : tempChildren) {
                        child.getParents().remove(cn);
                        child.getParents().addAll(tempParents);
                    }
                    
                    for(String k : cn.getEquivalentConcepts()) {
                        conceptNodeIndex.remove(k);
                        conceptNodeIndex.put(key, bottomNode);
                    }
                    toRemoveFromAffected.add(id);
                } else {
                    for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                        // Create a connection to each parent
                        int parentId = itr2.next();
                        if (parentId == id)
                            continue;
                        Node parent = conceptNodeIndex.get(factory.lookupConceptId(parentId));
                        cn.getParents().add(parent);
                        parent.getChildren().add(cn);
                        // All nodes that get new children and are connected to BOTTOM must be disconnected
                        if (parent.getChildren().contains(bottomNode)) {
                            parent.getChildren().remove(bottomNode);
                            bottomNode.getParents().remove(parent);
                        }
                    }
                }
            }
            
            for(Integer i : toRemoveFromAffected) {
                allAffected.remove(i.intValue());
                allNew.remove(i.intValue());
            }

            // 3. Connect new nodes without parents to TOP
            Node topNode = conceptNodeIndex.get(au.csiro.ontology.model.NamedConcept.TOP);

            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final String key = factory.lookupConceptId(itr.next()).toString();
                Node cn = conceptNodeIndex.get(key);
                if (cn.getParents().isEmpty()) {
                    cn.getParents().add(topNode);
                    topNode.getChildren().add(cn);
                }
            }

            // 4. Fix connections for new and affected concepts
            // a. Check for equivalents
            Set<Pair> pairsToMerge = new HashSet<Pair>();
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final String key = factory.lookupConceptId(itr.next()).toString();
                Node cn = conceptNodeIndex.get(key);
                for (Node parent : cn.getParents()) {
                    if (parent.getParents().contains(cn)) {
                        pairsToMerge.add(new Pair(cn, parent));
                    }
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final String key = factory.lookupConceptId(itr.next()).toString();
                Node cn = conceptNodeIndex.get(key);
                for (Node parent : cn.getParents()) {
                    if (parent.getParents().contains(cn)) {
                        pairsToMerge.add(new Pair(cn, parent));
                    }
                }
            }

            Set<Node> affectedByMerge = new HashSet<Node>();

            // Merge equivalents
            for (Pair p : pairsToMerge) {
                Node cn1 = p.getA();
                Node cn2 = p.getB();

                affectedByMerge.addAll(cn1.getChildren());
                affectedByMerge.addAll(cn2.getChildren());

                // Merge into cn1 - remove cn2 from index and replace with cn1
                for (String n : cn2.getEquivalentConcepts()) {
                    conceptNodeIndex.put(n, cn1);
                }

                cn1.getEquivalentConcepts().addAll(cn2.getEquivalentConcepts());

                // Remove relationships between merged concepts
                cn1.getParents().remove(cn2);
                cn2.getChildren().remove(cn1);
                cn2.getParents().remove(cn1);
                cn1.getChildren().remove(cn2);

                // Taxonomy is bidirectional
                cn1.getParents().addAll(cn2.getParents());
                for (Node parent : cn2.getParents()) {
                    parent.getChildren().remove(cn2);
                    parent.getChildren().add(cn1);
                }
                cn1.getChildren().addAll(cn2.getChildren());
                for (Node child : cn2.getChildren()) {
                    child.getParents().remove(cn2);
                    child.getParents().add(cn1);
                }

                cn2 = null; // nothing should reference cn2 now
            }

            // b. Fix all new and affected nodes
            Set<Node> all = new HashSet<Node>();
            for (IntIterator it = allNew.keyIterator(); it.hasNext();) {
                all.add(conceptNodeIndex.get(factory.lookupConceptId(it.next())));
            }

            for (IntIterator it = allAffected.keyIterator(); it.hasNext();) {
                all.add(conceptNodeIndex.get(factory.lookupConceptId(it.next())));
            }

            for (Node cn : affectedByMerge) {
                all.add(cn);
            }

            // Add also the children of the affected nodes
            Set<Node> childrenToAdd = new HashSet<Node>();
            for (Node cn : all) {
                for (Node ccn : cn.getChildren()) {
                    if (ccn.equals(bottomNode))
                        continue;
                    childrenToAdd.add(ccn);
                }
            }
            all.addAll(childrenToAdd);

            // Find redundant relationships
            for (Node cn : all) {
                Set<Node> ps = cn.getParents();

                Object[] parents = ps.toArray(new Object[ps.size()]);
                Set<Node> toRemove = new HashSet<Node>();
                for (int i = 0; i < parents.length; i++) {
                    for (int j = i + 1; j < parents.length; j++) {
                        if (isChild((Node)parents[j], (Node)parents[i])) {
                            toRemove.add((Node)parents[i]);
                            continue;
                        }
                        if (isChild((Node)parents[i], (Node)parents[j])) {
                            toRemove.add((Node)parents[j]);
                            continue;
                        }
                    }
                }
                for (Node tr : toRemove) {
                    cn.getParents().remove(tr);
                    tr.getChildren().remove(cn);
                }
            }

            // 5. Connect bottom to new and affected concepts with no children
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getChildren().isEmpty()) {
                    cn.getChildren().add(bottomNode);
                    bottomNode.getParents().add(cn);
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getChildren().isEmpty()) {
                    cn.getChildren().add(bottomNode);
                    bottomNode.getParents().add(cn);
                }
            }

            // 6. Connect the top node to new and affected concepts with no parents
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getParents().isEmpty()) {
                    cn.getParents().add(topNode);
                    topNode.getChildren().add(cn);
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getParents().isEmpty()) {
                    cn.getParents().add(topNode);
                    topNode.getChildren().add(cn);
                }
            }
        }  
    }
    
    /**
     * Returns the full taxonomy.
     * 
     * @return
     */
    public Map<String, Node> getTaxonomy() {
        return conceptNodeIndex;
    }
    
    /**
     * Returns a {@link Set} of {@link Node}s potentially affected by an
     * incremental classification.
     * 
     * @return
     */
    public Set<Node> getAffectedNodes() {
        Set<Node> res = new HashSet<Node>();
        for(IntIterator it = getNewSubsumptions().keyIterator(); 
                        it.hasNext(); ) {
            String key = factory.lookupConceptId(it.next()).toString();
            res.add(conceptNodeIndex.get(key));
        }
        for(IntIterator it = getAffectedSubsumptions().keyIterator(); 
                        it.hasNext(); ) {
            String key = factory.lookupConceptId(it.next()).toString();
            res.add(conceptNodeIndex.get(key));
        }
        return res;
    }
    
    /**
     * Indicates if cn is a child of cn2.
     * 
     * @param cn
     * @param cn2
     * @return
     */
    private boolean isChild(Node cn, Node cn2) {
        if (cn == cn2)
            return false;

        Queue<Node> toProcess = new LinkedList<Node>();
        toProcess.addAll(cn.getParents());

        while (!toProcess.isEmpty()) {
            Node tcn = toProcess.poll();
            if (tcn.equals(cn2))
                return true;
            Set<Node> parents = tcn.getParents();
            if (parents != null && !parents.isEmpty())
                toProcess.addAll(parents);
        }

        return false;
    }
    
    class Pair {

        private final Node a;
        private final Node b;

        /**
         * Creates a new pair.
         * 
         * @param a
         * @param b
         */
        public Pair(Node a, Node b) {
            String[] aa = new String[a.getEquivalentConcepts().size()];
            String[] bb = new String[b.getEquivalentConcepts().size()];

            if (aa.length < bb.length) {
                this.a = a;
                this.b = b;
            } else if (aa.length > bb.length) {
                this.a = b;
                this.b = a;
            } else {
                int i = 0;
                for (String c : a.getEquivalentConcepts()) {
                    aa[i++] = c;
                }
                i = 0;
                for (String c : b.getEquivalentConcepts()) {
                    bb[i++] = c;
                }

                int res = 0; // 0 equal, 1 a <, 2 a >

                for (i = 0; i < aa.length; i++) {
                    if (aa[i].compareTo(bb[i]) < 0) {
                        res = 1;
                        break;
                    } else if (aa[i].compareTo(bb[i]) > 0) {
                        res = 2;
                        break;
                    }
                }

                if (res == 1) {
                    this.a = a;
                    this.b = b;
                } else if (res == 2) {
                    this.a = b;
                    this.b = a;
                } else {
                    this.a = a;
                    this.b = b;
                }
            }
        }

        /**
         * @return the a
         */
        public Node getA() {
            return a;
        }

        /**
         * @return the b
         */
        public Node getB() {
            return b;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((a == null) ? 0 : a.hashCode());
            result = prime * result + ((b == null) ? 0 : b.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Pair other = (Pair) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (a == null) {
                if (other.a != null)
                    return false;
            } else if (!a.equals(other.a))
                return false;
            if (b == null) {
                if (other.b != null)
                    return false;
            } else if (!b.equals(other.b))
                return false;
            return true;
        }

        private NormalisedOntology getOuterType() {
            return NormalisedOntology.this;
        }
    }
    
    public boolean isTaxonomyComputed() {
        return conceptNodeIndex != null;
    }
    
    public Node getBottomNode() {
        return conceptNodeIndex.get(au.csiro.ontology.model.NamedConcept.BOTTOM);
    }
    
    public Node getTopNode() {
        return conceptNodeIndex.get(au.csiro.ontology.model.NamedConcept.TOP);
    }
    
    public Node getEquivalents(String cid) {
        return conceptNodeIndex.get(cid);
    }
    
    public class TopBottomNodes {
        private Node top;
        private Node bottom;

        public Node getTop() {
            return top;
        }

        public void setTop(Node top) {
            this.top = top;
        }

        public Node getBottom() {
            return bottom;
        }

        public void setBottom(Node bottom) {
            this.bottom = bottom;
        }
    }
    
    private void printNormalisedAxioms() {
        for(IntIterator it = ontologyNF1.keyIterator(); it.hasNext(); ) {
            int key = it.next();
            MonotonicCollection<IConjunctionQueueEntry> entries = ontologyNF1.get(key);
            Iterator<IConjunctionQueueEntry> it2 = entries.iterator();
            while(it2.hasNext()) {
                IConjunctionQueueEntry entry = it2.next();
                
                Object a = factory.lookupConceptId(key);
                String as = (a instanceof String) ? (String) a : "[" + a.toString() + "]";
                Object bi = factory.lookupConceptId(entry.getBi());
                String bis = (bi instanceof String) ? (String) bi :  "[" + bi.toString() + "]";
                Object b = factory.lookupConceptId(entry.getB());
                String bs = (b instanceof String) ? (String) b :  "[" + b.toString() + "]";
                System.out.println("NF1: " + as + " + " + bis + " [ " + bs);
            }
        }

        for(IntIterator it = ontologyNF2.keyIterator(); it.hasNext(); ) {
            int key = it.next();
            MonotonicCollection<NF2> entries = ontologyNF2.get(key);
            Iterator<NF2> it2 = entries.iterator();
            while(it2.hasNext()) {
                NF2 entry = it2.next();
                
                // These terms are of the form A [ r.B and are indexed by A.
                
                Object a = factory.lookupConceptId(entry.lhsA);
                String as = (a instanceof String) ? (String) a : "[" + a.toString() + "]";
                Object r = factory.lookupRoleId(entry.rhsR);
                String rs = (r instanceof String) ? (String) r :  "[" + r.toString() + "]";
                Object b = factory.lookupConceptId(entry.rhsB);
                String bs = (b instanceof String) ? (String) b :  "[" + b.toString() + "]";
                System.out.println("NF2: " + as + " [ " + rs + "." + bs);
            }
        }
        
        for(IntIterator it = ontologyNF3.keyIterator(); it.hasNext(); ) {
            int aId = it.next();
            ConcurrentMap<Integer, Collection<IConjunctionQueueEntry>> entries = ontologyNF3.get(aId);
            for(Integer rId : entries.keySet()) {
                
                // These terms are of the form r.A [ b and indexed by A (and then by r)
                
                for(IConjunctionQueueEntry entry : entries.get(rId)) {
                    int bId = entry.getB();
                    Object r = factory.lookupRoleId(rId.intValue());
                    String rs = (r instanceof String) ? (String) r :  "[" + r.toString() + "]";
                    Object a = factory.lookupConceptId(aId);
                    String as = (a instanceof String) ? (String) a : "[" + a.toString() + "]";
                    Object b = factory.lookupConceptId(bId);
                    String bs = (b instanceof String) ? (String) b :  "[" + b.toString() + "]";
                    System.out.println("NF3: " + rs + "." + as + " [ " + bs);
                }
            }
        }
        
        for(Iterator<NF4> it = ontologyNF4.iterator(); it.hasNext(); ) {
            NF4 nf4 = it.next();
            
            Object r = factory.lookupRoleId(nf4.getR());
            String rs = (r instanceof String) ? (String) r :  "[" + r.toString() + "]";
            Object s = factory.lookupRoleId(nf4.getS());
            String ss = (s instanceof String) ? (String) s :  "[" + s.toString() + "]";
            System.out.println("NF4: " + rs + " [ " + ss);
        }
        
        for(Iterator<NF5> it = ontologyNF5.iterator(); it.hasNext(); ) {
            NF5 nf5 = it.next();
            
            Object r = factory.lookupRoleId(nf5.getR());
            String rs = (r instanceof String) ? (String) r :  "[" + r.toString() + "]";
            Object s = factory.lookupRoleId(nf5.getS());
            String ss = (s instanceof String) ? (String) s :  "[" + s.toString() + "]";
            Object t = factory.lookupRoleId(nf5.getT());
            String ts = (t instanceof String) ? (String) t :  "[" + t.toString() + "]";
            System.out.println("NF5: " + rs + " o " + ss + " [ " + ts);
        }
        
        for(IntIterator it = ontologyNF7.keyIterator(); it.hasNext(); ) {
            int key = it.next();
            MonotonicCollection<NF7> entries = ontologyNF7.get(key);
            
            for(Iterator<NF7> it2 = entries.iterator(); it2.hasNext(); ) {
                NF7 nf7 = it2.next();
                int aId = nf7.lhsA;
                Datatype dt = nf7.rhsD;
                int fId = dt.getFeature();
                
                Object a = factory.lookupConceptId(aId);
                String as = (a instanceof String) ? (String) a : "[" + a.toString() + "]";
                String f = factory.lookupFeatureId(fId);
                
                System.out.println("NF7: " + as + " [ " + f + ".(" + dt.getLiteral() +")");
            }
        }
        
        FeatureSet keys = ontologyNF8.keySet();
        for (int i = keys.nextSetBit(0); i >= 0; i = keys.nextSetBit(i+1)) {
            MonotonicCollection<NF8> mc = ontologyNF8.get(i);
            for(Iterator<NF8> it2 = mc.iterator(); it2.hasNext(); ) {
                NF8 nf8 = it2.next();
                Datatype dt = nf8.lhsD;
                int bId = nf8.rhsB;
                int fId = dt.getFeature();
                
                Object b = factory.lookupConceptId(bId);
                String bs = (b instanceof String) ? (String) b : "[" + b.toString() + "]";
                String f = factory.lookupFeatureId(fId);
                
                System.out.println("NF8: " + f + ".(" + dt.getLiteral() +")" + " [ " + bs);
            }
        }
        
    }
    
}
