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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import au.csiro.ontology.Node;
import au.csiro.ontology.axioms.ConceptInclusion;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.axioms.IConceptInclusion;
import au.csiro.ontology.axioms.IRoleInclusion;
import au.csiro.ontology.axioms.RoleInclusion;
import au.csiro.ontology.model.Feature;
import au.csiro.ontology.model.IConcept;
import au.csiro.ontology.model.ILiteral;
import au.csiro.ontology.model.INamedRole;
import au.csiro.ontology.model.IRole;
import au.csiro.ontology.model.Operator;
import au.csiro.ontology.model.Role;
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
import au.csiro.snorocket.core.concurrent.Worker;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.AbstractLiteral;
import au.csiro.snorocket.core.model.BooleanLiteral;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.model.DateLiteral;
import au.csiro.snorocket.core.model.DoubleLiteral;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.model.FloatLiteral;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.model.LongLiteral;
import au.csiro.snorocket.core.model.StringLiteral;
import au.csiro.snorocket.core.util.AxiomSet;
import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.FastConceptMap;
import au.csiro.snorocket.core.util.FeatureMap;
import au.csiro.snorocket.core.util.FeatureSet;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IMonotonicCollection;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.MonotonicCollection;
import au.csiro.snorocket.core.util.RoleMap;
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
public class NormalisedOntology<T extends Comparable<T>> implements Serializable {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    // Logger
    private final static Logger log = Logger.getLogger(
            NormalisedOntology.class);

    final protected IFactory<T> factory;

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
    final protected IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> ontologyNF3;

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
    private final Queue<Context> todo = new ConcurrentLinkedQueue<>();

    /**
     * The map of contexts by concept id.
     */
    private final IConceptMap<Context> contextIndex;

    /**
     * The global role closure.
     */
    private final RoleMap<RoleSet> roleClosureCache;

    /**
     * A set of new contexts added in an incremental classification.
     */
    private final Set<Context> newContexts = new HashSet<>();
    
    /**
     * The number of threads to use.
     */
    private int numThreads = Runtime.getRuntime().availableProcessors();
    
    private boolean hasBeenIncrementallyClassified = false;
    
    private transient Map<T, Node<T>> conceptNodeIndex;

    
    private static class ContextComparator implements Comparator<Context>, Serializable {
        /**
         * Serialisation version.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public int compare(Context o1, Context o2) {
            return Integer.compare(o1.getConcept(), o2.getConcept());
        }
    }
    
    /**
     * A set of contexts potentially affected by an incremental classification.
     */
    private final Set<Context> affectedContexts = new ConcurrentSkipListSet<>(
            new ContextComparator());

    public IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> getOntologyNF1() {
        return ontologyNF1;
    }

    public IConceptMap<MonotonicCollection<NF2>> getOntologyNF2() {
        return ontologyNF2;
    }

    public IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> getOntologyNF3() {
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

    public RoleMap<RoleSet> getRoleClosureCache() {
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
    public NormalisedOntology(final IFactory<T> factory,
            final Set<? extends IAxiom> inclusions) {
        this(factory);
        
        for (Inclusion<T> i : normalise(inclusions)) {
            addTerm(i.getNormalForm());
        }
    }

    /**
     * 
     * @param baseConceptCount
     * @param conceptCount
     *            if this value is too small, the algorithm performance will be
     *            impacted
     * @param roleCount
     */
    public NormalisedOntology(final IFactory<T> factory) {
        this(
                factory,
                new DenseConceptMap<MonotonicCollection<IConjunctionQueueEntry>>(
                        factory.getTotalConcepts()),
                new SparseConceptMap<MonotonicCollection<NF2>>(
                        factory.getTotalConcepts(), "ontologyNF2"),
                new SparseConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>>(
                        factory.getTotalConcepts(), "ontologyNF3"),
                new MonotonicCollection<NF4>(15), new MonotonicCollection<NF5>(
                        1), new SparseConceptMap<MonotonicCollection<NF7>>(
                        factory.getTotalConcepts(), "ontologyNF7"),
                new FeatureMap<MonotonicCollection<NF8>>(
                        factory.getTotalConcepts()));
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
            final IFactory<T> factory,
            final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> nf1q,
            final IConceptMap<MonotonicCollection<NF2>> nf2q,
            final IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> nf3q,
            final IMonotonicCollection<NF4> nf4q,
            final IMonotonicCollection<NF5> nf5q,
            final IConceptMap<MonotonicCollection<NF7>> nf7q,
            final FeatureMap<MonotonicCollection<NF8>> nf8q) {
        this.factory = factory;
        contextIndex = new FastConceptMap<>(factory.getTotalConcepts(), "");
        roleClosureCache = new RoleMap<RoleSet>(factory.getTotalRoles());

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
    public void loadAxioms(final Set<? extends IAxiom> inclusions) {
        long start = System.currentTimeMillis();
        if(log.isInfoEnabled())
            log.info("Loading " + inclusions.size() + " axioms");
        Set<Inclusion<T>> normInclusions = normalise(inclusions);
        if(log.isInfoEnabled())
            log.info("Processing " + normInclusions.size()
                + " normalised axioms");
        Statistics.INSTANCE.setTime("normalisation",
                System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        for (Inclusion<T> i : normInclusions) {
            addTerm(i.getNormalForm());
        }
        Statistics.INSTANCE.setTime("indexing", 
                System.currentTimeMillis() - start);
    }
    
    /**
     * Transforms a {@link Set} of {@link AbstractAxiom}s into a {@link Set} of
     * {@link Inclusion}s.
     * 
     * @param axioms The axioms in the ontology model format.
     * @return The axioms in the internal model format.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Set<Inclusion<T>> transformAxiom(final Set<? extends IAxiom> axioms) {
        Set<Inclusion<T>> res = new HashSet<>();
        
        for(IAxiom aa : axioms) {
            if(aa instanceof IConceptInclusion) {
                IConceptInclusion ci = (IConceptInclusion)aa;
                IConcept lhs = ci.lhs();
                IConcept rhs = ci.rhs();
                res.add(new GCI<T>(transformConcept(lhs), transformConcept(rhs)));
            } else if(aa instanceof IRoleInclusion) {
                IRoleInclusion ri = (IRoleInclusion)aa;
                IRole[] lh = ri.lhs();
                INamedRole[] lhs = new INamedRole[lh.length];
                for(int i = 0; i < lh.length; i++) {
                    lhs[i] = (INamedRole)lh[i];
                }
                INamedRole rhs = (INamedRole)ri.rhs();
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
     * Transforms an {@link AbstractConcept} into an 
     * {@link au.csiro.snorocket.core.model.AbstractConcept}.
     * 
     * @param c The concept in the ontology model format.
     * @return The concept in the internal model format.
     */
    @SuppressWarnings("unchecked")
    private au.csiro.snorocket.core.model.AbstractConcept transformConcept(IConcept c) {
        if(c == au.csiro.ontology.model.Concept.TOP) {
            return new Concept(IFactory.TOP_CONCEPT);
        } else if(c == au.csiro.ontology.model.Concept.BOTTOM) {
            return new Concept(IFactory.BOTTOM_CONCEPT);
        } else if(c instanceof au.csiro.ontology.model.Concept) {
            return new Concept(factory.getConcept(((au.csiro.ontology.model.Concept<T>)c).getId()));
        } else if(c instanceof au.csiro.ontology.model.Conjunction) {
            IConcept[] modelCons = ((au.csiro.ontology.model.Conjunction)c).getConcepts();
            au.csiro.snorocket.core.model.AbstractConcept[] cons = 
                    new au.csiro.snorocket.core.model.AbstractConcept[modelCons.length];
            for(int i = 0; i < modelCons.length; i++) {
                cons[i] = transformConcept(modelCons[i]);
            }
            return new Conjunction(cons);
        } else if(c instanceof au.csiro.ontology.model.Datatype) {
            au.csiro.ontology.model.Datatype<T> dt = (au.csiro.ontology.model.Datatype<T>) c;
            return new Datatype(factory.getFeature(dt.getFeature().getId()), 
                    dt.getOperator(), transformLiteral(dt.getLiteral()));
        } else if(c instanceof au.csiro.ontology.model.Existential) {
            au.csiro.ontology.model.Existential<T> e = (au.csiro.ontology.model.Existential<T>) c;
            return new Existential(factory.getRole(e.getRole().getId()), 
                    transformConcept(e.getConcept())); 
        } else {
            throw new RuntimeException("Unexpected AbstractConcept "+c.getClass().getName());
        }
    }
    
    /**
     * Transforms an {@link ILiteral} into an 
     * {@link au.csiro.snorocket.core.model.AbstractLiteral}.
     * 
     * @param l The literal in the ontology model format.
     * @return The literal in the internal model format.
     */
    private au.csiro.snorocket.core.model.AbstractLiteral transformLiteral(ILiteral l) {
        if(l instanceof au.csiro.ontology.model.BooleanLiteral) {
            return new BooleanLiteral(((au.csiro.ontology.model.BooleanLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.DateLiteral) {
            return new DateLiteral(((au.csiro.ontology.model.DateLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.DoubleLiteral) {
            return new DoubleLiteral(((au.csiro.ontology.model.DoubleLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.FloatLiteral) {
            return new FloatLiteral(((au.csiro.ontology.model.FloatLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.IntegerLiteral) {
            return new IntegerLiteral(((au.csiro.ontology.model.IntegerLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.LongLiteral) {
            return new LongLiteral(((au.csiro.ontology.model.LongLiteral) l).getValue());
        } else if(l instanceof au.csiro.ontology.model.StringLiteral) {
            return new StringLiteral(((au.csiro.ontology.model.StringLiteral) l).getValue());
        } else {
            throw new RuntimeException("Unexpected AbstractLiteral "+l.getClass().getName());
        }
    }

    /**
     * Returns a set of Inclusions in normal form suitable for classifying.
     */
    @SuppressWarnings("rawtypes")
    public Set<Inclusion<T>> normalise(final Set<? extends IAxiom> inclusions) {
        
        // Exhaustively apply NF1 to NF4
        final Set<Inclusion<T>> done = new HashSet<>();
        Set<Inclusion<T>> oldIs = new HashSet<>();
        Set<Inclusion<T>> newIs = transformAxiom(inclusions);

        do {
            final Set<Inclusion<T>> tmp = oldIs;
            oldIs = newIs;
            newIs = tmp;
            newIs.clear();

            for (Inclusion<T> i : oldIs) {
                Inclusion<T>[] s = i.normalise1(factory);
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
            final Set<Inclusion<T>> tmp = oldIs;
            oldIs = newIs;
            newIs = tmp;
            newIs.clear();

            for (Inclusion<T> i : oldIs) {
                Inclusion<T>[] s = i.normalise2(factory);
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
            for(Inclusion<T> inc : done) {
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
            T feature = factory.lookupFeatureId(d.getFeature());
            sb.append(feature.toString());
            sb.append(".(");
            Operator op = d.getOperator();
            sb.append(op.toString());
            sb.append(",");
            AbstractLiteral literal = d.getLiteral();
            sb.append(literal);
            sb.append(")");
            return sb.toString();
        } else if(o instanceof Concept) {
            Object obj = factory.lookupConceptId(((Concept)o).hashCode());
            if(obj == au.csiro.ontology.model.Concept.TOP) {
                return "TOP";
            } else if(obj == au.csiro.ontology.model.Concept.BOTTOM) {
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
    protected void addTerms(
            final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> entries,
            final int a, final IConjunctionQueueEntry queueEntry) {
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
    protected void addTerms(
            final IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> queue,
            final NF3 nf3) {
        RoleMap<Collection<IConjunctionQueueEntry>> map = queue.get(nf3.lhsA);
        Collection<IConjunctionQueueEntry> entry;
        if (null == map) {
            map = new RoleMap<Collection<IConjunctionQueueEntry>>(
                    factory.getTotalRoles());
            queue.put(nf3.lhsA, map);
            entry = null;
        } else {
            entry = map.get(nf3.lhsR);
        }
        if (null == entry) {
            entry = new HashSet<>();
            entry.add(nf3.getQueueEntry());
            map.put(nf3.lhsR, entry);
        } else {
            entry.add(nf3.getQueueEntry());
        }
    }

    /**
     * 
     * @param entries
     * @param nf7
     */
    protected void addTerms(
            final IConceptMap<MonotonicCollection<NF7>> entries, final NF7 nf7) {
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
    protected void addTerms(final FeatureMap<MonotonicCollection<NF8>> entries,
            final NF8 nf8) {
        MonotonicCollection<NF8> set = entries.get(nf8.lhsD.getFeature());
        if (null == set) {
            set = new MonotonicCollection<NF8>(2);
            entries.put(nf8.lhsD.getFeature(), set);
        }
        set.add(nf8);
    }

    /**
     * Runs an incremental classification.
     * 
     * @return
     */
    public void classifyIncremental(Set<IAxiom> incAxioms) {
        // Clear any state from previous incremental classifications
        newContexts.clear();
        affectedContexts.clear();

        // Normalise axioms
        Set<Inclusion<T>> inclusions = normalise(incAxioms);

        // Add new axioms to corresponding normal form
        AxiomSet as = new AxiomSet();
        int numNewConcepts = 0;

        for (Inclusion<T> i : inclusions) {
            NormalFormGCI nf = i.getNormalForm();
            as.addAxiom(nf);
            addTerm(nf);
        }

        // Determine which contexts are affected
        for (Inclusion<T> i : inclusions) {
            NormalFormGCI nf = i.getNormalForm();

            // Add a context to the context index for every new concept in the
            // axioms being added incrementally
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
        }
        
        if(log.isInfoEnabled()) 
            log.info("Added " + numNewConcepts + 
                    " new concepts to the ontology");

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
            log.info("Classifying incrementally with " + numThreads + 
                    " threads");
        
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
        
        if(log.isTraceEnabled())
            log.trace("Processed " + contextIndex.size() + " contexts");
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
        IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> deltaNF3 = 
                new SparseConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>>(size);
        for (NF3 nf3 : as.getNf3Axioms()) {
            addTerms(deltaNF3, nf3);
        }

        for (final IntIterator xItr = deltaNF3.keyIterator(); xItr.hasNext();) {
            final int x = xItr.next();
            final RoleMap<Collection<IConjunctionQueueEntry>> entries = deltaNF3
                    .get(x);

            final RoleSet keySet = entries.keySet();
            for (int r = keySet.first(); r >= 0; r = keySet.next(r + 1)) {
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

                        @Override
                        public int getR() {
                            return nf4.getS();
                        }

                        @Override
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

            for (final IntIterator aItr = subsumptions.keyIterator(); aItr
                    .hasNext();) {
                final int a = aItr.next();

                Context aCtx = contextIndex.get(a);

                for (final IntIterator bItr = aCtx.getSucc()
                        .lookupConcept(nf5.getR()).iterator(); bItr.hasNext();) {
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

                                @Override
                                public int getR() {
                                    return t;
                                }

                                @Override
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
     * These axioms are of the form A [ f.(o, v) and are indexed by A. A feature
     * queue element must be added to the contexts that have A in their
     * subsumptions.
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
            addTerms(deltaNF7, nf7);
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
        if (size == 0)
            return;
        FeatureMap<MonotonicCollection<NF8>> deltaNF8 = new FeatureMap<MonotonicCollection<NF8>>(
                size);
        for (NF8 nf8 : as.getNf8Axioms()) {
            addTerms(deltaNF8, nf8);
        }

        FeatureSet fs = deltaNF8.keySet();
        int fid = fs.first();
        while (fid != -1) {
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

    public IFactory<T> getFactory() {
        return factory;
    }
    
    /**
     * Returns the stated axioms in the ontology.
     * 
     * @return
     */
    public Set<IAxiom> getStatedAxioms() {
        Set<IAxiom> res = new HashSet<>();
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
                            new IConcept[] {transform(oa), transform(obi)}),
                        transform(ob)
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
                T r = factory.lookupRoleId(nf2.rhsR);
                Object ob = factory.lookupConceptId(nf2.rhsB);
                res.add(new ConceptInclusion(
                    transform(oa),
                    new au.csiro.ontology.model.Existential<T>(
                            new Role<T>(r), transform(ob))
                ));
            }
        }
        
        // These terms are of the form r.A [ b and indexed by A.
        for(IntIterator it = ontologyNF3.keyIterator(); it.hasNext(); ) {
            int a = it.next();
            RoleMap<Collection<IConjunctionQueueEntry>> mc = ontologyNF3.get(a);
            RoleSet keys = mc.keySet();
            for (int i = keys.nextSetBit(0); i >= 0; i = keys.nextSetBit(i+1)) {
                Collection<IConjunctionQueueEntry> cc = mc.get(i);
                for(Iterator<IConjunctionQueueEntry> it2 = cc.iterator(); 
                        it2.hasNext(); ) {
                    IConjunctionQueueEntry nf3 = it2.next();
                    Object oa = factory.lookupConceptId(a);
                    T r = factory.lookupRoleId(i);
                    Object ob = factory.lookupConceptId(nf3.getB());
                    res.add(new ConceptInclusion(
                        new au.csiro.ontology.model.Existential<T>(
                            new Role<T>(r), transform(ob)),
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
                    new Role<T>(factory.lookupRoleId(r)),
                    new Role<T>(factory.lookupRoleId(s))
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
                    new IRole[] {
                            new Role<T>(factory.lookupRoleId(r)),
                            new Role<T>(factory.lookupRoleId(s))
                    },
                    new Role<T>(factory.lookupRoleId(t))
                )
            );
        }
        
        for(IntIterator it = reflexiveRoles.iterator(); it.hasNext(); ) {
            int r = it.next();
            res.add(
                new RoleInclusion(
                    new IRole[] {},
                    new Role<T>(factory.lookupRoleId(r))
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
                    transform(factory.lookupConceptId(i))
                ));
            }
        }
        
        return res;
    }
    
    @SuppressWarnings("unchecked")
    public IConcept transform(Object o) {
        if(o instanceof Conjunction) {
            Conjunction con = (Conjunction)o;
            List<IConcept> concepts = new ArrayList<>();
            for(AbstractConcept ac : con.getConcepts()) {
                concepts.add(transform(ac));
            }
            return new au.csiro.ontology.model.Conjunction(concepts);
        } else if(o instanceof Existential) {
            Existential e = (Existential)o;
            AbstractConcept c = e.getConcept();
            IConcept iconcept = transform(c);
            int role = e.getRole();
            INamedRole<T> irole = new Role<>(factory.lookupRoleId(role));
            return new au.csiro.ontology.model.Existential<T>(irole, iconcept);
        } else if(o instanceof Datatype) {
            Datatype d = (Datatype)o;
            T feature = factory.lookupFeatureId(d.getFeature());
            Operator op = d.getOperator();
            AbstractLiteral literal = d.getLiteral();
            
            ILiteral iliteral = null;
            if(literal instanceof BooleanLiteral) {
                iliteral = new au.csiro.ontology.model.BooleanLiteral(
                        ((BooleanLiteral)literal).getValue());
            } else if(literal instanceof DateLiteral) {
                iliteral = new au.csiro.ontology.model.DateLiteral(
                        ((DateLiteral)literal).getValue());
            } else if(literal instanceof DoubleLiteral) {
                iliteral = new au.csiro.ontology.model.DoubleLiteral(
                        ((DoubleLiteral)literal).getValue());
            } else if(literal instanceof FloatLiteral) {
                iliteral = new au.csiro.ontology.model.FloatLiteral(
                        ((FloatLiteral)literal).getValue());
            } else if(literal instanceof IntegerLiteral) {
                iliteral = new au.csiro.ontology.model.IntegerLiteral(
                        ((IntegerLiteral)literal).getValue());
            } else if(literal instanceof LongLiteral) {
                iliteral = new au.csiro.ontology.model.LongLiteral(
                        ((LongLiteral)literal).getValue());
            } else if(literal instanceof StringLiteral) {
                iliteral = new au.csiro.ontology.model.StringLiteral(
                        ((StringLiteral)literal).getValue());
            } else {
                throw new RuntimeException("Unexpected literal "+
                        literal.getClass().getName());
            }
            return new au.csiro.ontology.model.Datatype<T>(
                    new Feature<T>(feature), op, iliteral);
        } else if(o instanceof Concept) {
            Object obj = factory.lookupConceptId(((Concept)o).hashCode());
            return transform(obj);
        } else if(o instanceof Comparable<?>) {
            return new au.csiro.ontology.model.Concept<T>((T)o);
        } else {
            throw new RuntimeException("Unexpected object with class "+
                    o.getClass().getName());
        }
    }

    /**
     * @param numThreads the numThreads to set
     */
    public void setNumThreads(int numThreads) {
        this.numThreads = numThreads;
    }
    
    /**
     * Calculates the taxonomy after classification.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public void buildTaxonomy() {
        
        final IConceptMap<IConceptSet> subsumptions = getSubsumptions();
        
        // Determine if a full or incremental calculation is required
        if(!hasBeenIncrementallyClassified) {
            long start = System.currentTimeMillis();

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
            conceptNodeIndex = new HashMap<>();
            
            Node<T> top = new Node<>();
            top.getEquivalentConcepts().add((T)au.csiro.ontology.model.Concept.TOP);
            
            Node<T> bottom = new Node<>();
            bottom.getEquivalentConcepts().add((T)au.csiro.ontology.model.Concept.BOTTOM);

            for (IntIterator it = equiv.keyIterator(); it.hasNext();) {
                int key = it.next();
                IConceptSet equivs = equiv.get(key);
                // Check if any of the equivalent classes is already part of an
                // equivalent node
                Node<T> n = null;
                for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                    T e = factory.lookupConceptId(it2.next());
                    if (conceptNodeIndex.containsKey(e)) {
                        n = conceptNodeIndex.get(e);
                        break;
                    }
                }

                if (n == null) {
                    n = new Node<T>();
                }
                n.getEquivalentConcepts().add(factory.lookupConceptId(key));
                for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                    n.getEquivalentConcepts().add(factory.lookupConceptId(it2.next()));
                }
                
                for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                    int e = it2.next();
                    if (e == CoreFactory.TOP_CONCEPT)
                        top = n;
                    if (e == CoreFactory.BOTTOM_CONCEPT)
                        bottom = n;
                    conceptNodeIndex.put(factory.lookupConceptId(e), n);
                }
            }

            // Connect the nodes according to the direct super-concept relationships
            Set<Node<T>> processed = new HashSet<>();
            for (T key : conceptNodeIndex.keySet()) {
                Node<T> node = conceptNodeIndex.get(key);
                if (processed.contains(node) || node == top || node == bottom)
                    continue;
                processed.add(node);
                for (T c : node.getEquivalentConcepts()) {
                    // Get direct super-concepts
                    IConceptSet dc = direc.get(factory.getConcept(c));
                    if (dc != null) {
                        for (IntIterator it3 = dc.iterator(); it3.hasNext();) {
                            int d = it3.next();
                            Node<T> parent = conceptNodeIndex.get(factory.lookupConceptId(d));
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
                bottom = new Node<T>();
                bottom.getEquivalentConcepts().add((T)au.csiro.ontology.model.Concept.BOTTOM);
                conceptNodeIndex.put((T)au.csiro.ontology.model.Concept.BOTTOM, bottom);
            }

            for (T key : conceptNodeIndex.keySet()) {
                if (key == au.csiro.ontology.model.Concept.TOP || key == au.csiro.ontology.model.Concept.BOTTOM)
                    continue;
                Node<T> node = conceptNodeIndex.get(key);
                if (node.getEquivalentConcepts().contains(au.csiro.ontology.model.Concept.BOTTOM))
                    continue;
                if (node.getChildren().isEmpty()) {
                    bottom.getParents().add(node);
                    node.getChildren().add(bottom);
                }
            }

            // Add top
            if (top == null) {
                top = new Node<T>();
                top.getEquivalentConcepts().add((T)au.csiro.ontology.model.Concept.TOP);
                conceptNodeIndex.put((T)au.csiro.ontology.model.Concept.TOP, top);
            }

            for (T key : conceptNodeIndex.keySet()) {
                if (key == au.csiro.ontology.model.Concept.TOP || key == au.csiro.ontology.model.Concept.BOTTOM)
                    continue;
                Node<T> node = conceptNodeIndex.get(key);
                if (node.getParents().isEmpty()) {
                    node.getParents().add(top);
                    top.getChildren().add(node);
                }
            }

            equiv = null;
            direc = null;

            // TODO: deal with special case where only top and bottom are present.
            Statistics.INSTANCE.setTime("taxonomy construction",
                    System.currentTimeMillis() - start);
        } else {
            final IConceptMap<IConceptSet> newConceptSubs = getNewSubsumptions();
            final IConceptMap<IConceptSet> affectedConceptSubs = getAffectedSubsumptions();
            
            // 1. Keep only the subsumptions that involve real atomic concepts
            IConceptMap<IConceptSet> allNew = new SparseConceptMap<IConceptSet>(
                    newConceptSubs.size());

            IConceptMap<IConceptSet> allAffected = new SparseConceptMap<IConceptSet>(
                    newConceptSubs.size());

            for (IntIterator itr = newConceptSubs.keyIterator(); itr.hasNext();) {
                final int x = itr.next();
                if (!factory.isVirtualConcept(x)) {
                    IConceptSet set = new SparseConceptHashSet();
                    allNew.put(x, set);
                    for (IntIterator it = newConceptSubs.get(x).iterator(); it
                            .hasNext();) {
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
                    for (IntIterator it = affectedConceptSubs.get(x).iterator(); it
                            .hasNext();) {
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
                final T key = factory.lookupConceptId(itr.next());
                Node<T> cn = new Node<>();
                cn.getEquivalentConcepts().add(key);
                conceptNodeIndex.put(key, cn);
            }

            // b. Now connect the nodes disregarding redundant connections
            Node<T> bottomNode = conceptNodeIndex.get(au.csiro.ontology.model.Concept.BOTTOM);
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                int id = itr.next();
                final T key = factory.lookupConceptId(id);
                Node<T> cn = conceptNodeIndex.get(key);
                IConceptSet parents = allNew.get(id);
                for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                    // Create a connection to each parent
                    int parentId = itr2.next();
                    if (parentId == id)
                        continue;
                    Node<T> parent = conceptNodeIndex.get(factory.lookupConceptId(parentId));
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

            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int id = itr.next();
                final T key = factory.lookupConceptId(id);
                Node<T> cn = conceptNodeIndex.get(key);
                IConceptSet parents = allAffected.get(id);
                for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                    // Create a connection to each parent
                    int parentId = itr2.next();
                    if (parentId == id)
                        continue;
                    Node<T> parent = conceptNodeIndex.get(factory.lookupConceptId(parentId));
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

            // 3. Connect new nodes without parents to TOP
            Node<T> topNode = conceptNodeIndex.get(au.csiro.ontology.model.Concept.TOP);

            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final T key = factory.lookupConceptId(itr.next());
                Node<T> cn = conceptNodeIndex.get(key);
                if (cn.getParents().isEmpty()) {
                    cn.getParents().add(topNode);
                    topNode.getChildren().add(cn);
                }
            }

            // 4. Fix connections for new and affected concepts
            // a. Check for equivalents
            Set<Pair> pairsToMerge = new HashSet<>();
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final T key = factory.lookupConceptId(itr.next());
                Node<T> cn = conceptNodeIndex.get(key);
                for (Node<T> parent : cn.getParents()) {
                    if (parent.getParents().contains(cn)) {
                        pairsToMerge.add(new Pair(cn, parent));
                    }
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final T key = factory.lookupConceptId(itr.next());
                Node<T> cn = conceptNodeIndex.get(key);
                for (Node<T> parent : cn.getParents()) {
                    if (parent.getParents().contains(cn)) {
                        pairsToMerge.add(new Pair(cn, parent));
                    }
                }
            }

            Set<Node<T>> affectedByMerge = new HashSet<>();

            // Merge equivalents
            for (Pair p : pairsToMerge) {
                Node<T> cn1 = p.getA();
                Node<T> cn2 = p.getB();

                affectedByMerge.addAll(cn1.getChildren());
                affectedByMerge.addAll(cn2.getChildren());

                // Merge into cn1 - remove cn2 from index and replace with cn1
                for (T n : cn2.getEquivalentConcepts()) {
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
                for (Node<T> parent : cn2.getParents()) {
                    parent.getChildren().remove(cn2);
                    parent.getChildren().add(cn1);
                }
                cn1.getChildren().addAll(cn2.getChildren());
                for (Node<T> child : cn2.getChildren()) {
                    child.getParents().remove(cn2);
                    child.getParents().add(cn1);
                }

                cn2 = null; // nothing should reference cn2 now
            }

            // b. Fix all new and affected nodes
            Set<Node<T>> all = new HashSet<>();
            for (IntIterator it = allNew.keyIterator(); it.hasNext();) {
                all.add(conceptNodeIndex.get(factory.lookupConceptId(it.next())));
            }

            for (IntIterator it = allAffected.keyIterator(); it.hasNext();) {
                all.add(conceptNodeIndex.get(factory.lookupConceptId(it.next())));
            }

            for (Node<T> cn : affectedByMerge) {
                all.add(cn);
            }

            // Add also the children of the affected nodes
            Set<Node<T>> childrenToAdd = new HashSet<>();
            for (Node<T> cn : all) {
                for (Node<T> ccn : cn.getChildren()) {
                    if (ccn.equals(bottomNode))
                        continue;
                    childrenToAdd.add(ccn);
                }
            }
            all.addAll(childrenToAdd);

            // Find redundant relationships
            for (Node<T> cn : all) {
                Set<Node<T>> ps = cn.getParents();

                Object[] parents = ps.toArray(new Object[ps.size()]);
                Set<Node<T>> toRemove = new HashSet<>();
                for (int i = 0; i < parents.length; i++) {
                    for (int j = i + 1; j < parents.length; j++) {
                        if (isChild((Node<T>)parents[j], (Node<T>)parents[i])) {
                            toRemove.add((Node<T>)parents[i]);
                            continue;
                        }
                        if (isChild((Node<T>)parents[i], (Node<T>)parents[j])) {
                            toRemove.add((Node<T>)parents[j]);
                            continue;
                        }
                    }
                }
                for (Node<T> tr : toRemove) {
                    cn.getParents().remove(tr);
                    tr.getChildren().remove(cn);
                }
            }

            // 5. Connect bottom to new and affected concepts with no children
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node<T> cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getChildren().isEmpty()) {
                    cn.getChildren().add(bottomNode);
                    bottomNode.getParents().add(cn);
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node<T> cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getChildren().isEmpty()) {
                    cn.getChildren().add(bottomNode);
                    bottomNode.getParents().add(cn);
                }
            }

            // 6. Connect the top node to new and affected concepts with no parents
            for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node<T> cn = conceptNodeIndex.get(factory.lookupConceptId(key));
                if (cn.getParents().isEmpty()) {
                    cn.getParents().add(topNode);
                    topNode.getChildren().add(cn);
                }
            }
            for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
                final int key = itr.next();
                Node<T> cn = conceptNodeIndex.get(factory.lookupConceptId(key));
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
    public Map<T, Node<T>> getTaxonomy() {
        return conceptNodeIndex;
    }
    
    /**
     * Returns a {@link Set} of {@link Node}s potentially affected by an
     * incremental classification.
     * 
     * @return
     */
    public Set<Node<T>> getAffectedNodes() {
        Set<Node<T>> res = new HashSet<>();
        for(IntIterator it = getNewSubsumptions().keyIterator(); 
        		it.hasNext(); ) {
            T key = factory.lookupConceptId(it.next());
            res.add(conceptNodeIndex.get(key));
        }
        for(IntIterator it = getAffectedSubsumptions().keyIterator(); 
        		it.hasNext(); ) {
            T key = factory.lookupConceptId(it.next());
            res.add(conceptNodeIndex.get(key));
        }
        return res;
    }
    
    private void addToSet(IConceptMap<IConceptSet> map, int key, int val) {
        IConceptSet set = map.get(key);
        if (set == null) {
            set = new SparseConceptHashSet();
            map.put(key, set);
        }
        set.add(val);
    }
    
    /**
     * Indicates if cn is a child of cn2.
     * 
     * @param cn
     * @param cn2
     * @return
     */
    private boolean isChild(Node<T> cn, Node<T> cn2) {
        if (cn == cn2)
            return false;

        Queue<Node<T>> toProcess = new LinkedList<>();
        toProcess.addAll(cn.getParents());

        while (!toProcess.isEmpty()) {
            Node<T> tcn = toProcess.poll();
            if (tcn.equals(cn2))
                return true;
            Set<Node<T>> parents = tcn.getParents();
            if (parents != null && !parents.isEmpty())
                toProcess.addAll(parents);
        }

        return false;
    }
    
    class Pair {

        private final Node<T> a;
        private final Node<T> b;

        /**
         * Creates a new pair.
         * 
         * @param a
         * @param b
         */
        @SuppressWarnings("unchecked")
        public Pair(Node<T> a, Node<T> b) {
            T[] aa = (T[]) new Object[a.getEquivalentConcepts().size()];
            T[] bb = (T[]) new Object[b.getEquivalentConcepts().size()];

            if (aa.length < bb.length) {
                this.a = a;
                this.b = b;
            } else if (aa.length > bb.length) {
                this.a = b;
                this.b = a;
            } else {
                int i = 0;
                for (T c : a.getEquivalentConcepts()) {
                    aa[i++] = c;
                }
                i = 0;
                for (T c : b.getEquivalentConcepts()) {
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
        public Node<T> getA() {
            return a;
        }

        /**
         * @return the b
         */
        public Node<T> getB() {
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

        @SuppressWarnings("unchecked")
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

        @SuppressWarnings("rawtypes")
        private NormalisedOntology getOuterType() {
            return NormalisedOntology.this;
        }
    }
    
    public boolean isTaxonomyComputed() {
        return conceptNodeIndex != null;
    }
    
    public Node<T> getBottomNode() {
        return conceptNodeIndex.get(au.csiro.ontology.model.Concept.BOTTOM);
    }
    
    public Node<T> getTopNode() {
        return conceptNodeIndex.get(au.csiro.ontology.model.Concept.TOP);
    }
    
    public Node<T> getEquivalents(T cid) {
        return conceptNodeIndex.get(cid);
    }
    
}
