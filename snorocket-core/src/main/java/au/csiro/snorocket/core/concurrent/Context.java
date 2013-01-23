package au.csiro.snorocket.core.concurrent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import au.csiro.ontology.model.Operator;
import au.csiro.snorocket.core.CoreFactory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.IQueue;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.QueueImpl;
import au.csiro.snorocket.core.axioms.IConjunctionQueueEntry;
import au.csiro.snorocket.core.axioms.IFeatureQueueEntry;
import au.csiro.snorocket.core.axioms.IRoleQueueEntry;
import au.csiro.snorocket.core.axioms.NF2;
import au.csiro.snorocket.core.axioms.NF4;
import au.csiro.snorocket.core.axioms.NF5;
import au.csiro.snorocket.core.axioms.NF7;
import au.csiro.snorocket.core.axioms.NF8;
import au.csiro.snorocket.core.model.AbstractLiteral;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.util.FeatureMap;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IMonotonicCollection;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.MonotonicCollection;
import au.csiro.snorocket.core.util.RoleMap;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Represents a context where derivations associated to one concept are
 * executed.
 * 
 * @author Alejandro Metke
 * 
 */
public class Context implements Serializable {
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The internal concept id.
     */
    private final int concept;

    /**
     * Flag to indicate if this context is active or not.
     */
    private final AtomicBoolean active = new AtomicBoolean(false);

    /**
     * Queue (List) of ConjunctionQueueEntries indicating work to be done for
     * this concept.
     */
    private final Queue<IConjunctionQueueEntry> conceptQueue = 
            new ConcurrentLinkedQueue<>();

    /**
     * Queue (List) of RoleQueueEntries indicating work to be done for this
     * concept.
     */
    private final IQueue<IRoleQueueEntry> roleQueue = 
            new QueueImpl<IRoleQueueEntry>(IRoleQueueEntry.class);

    /**
     * Queue (List) of FeatureQueueEntries indicating work to be done for this
     * concept. Queue entries of the form A [ f.(o, v).
     */
    private final IQueue<IFeatureQueueEntry> featureQueue = 
            new QueueImpl<IFeatureQueueEntry>(IFeatureQueueEntry.class);

    /**
     * Queue used to process entries from other contexts that trigger calls to
     * processNewEdge.
     */
    private final Queue<IRoleQueueEntry> externalQueue = 
            new ConcurrentLinkedQueue<>();

    /**
     * Keeps track of the parents of this concept.
     */
    private final IConceptSet s;

    /**
     * Keeps track of the concepts that are linked this concept through some
     * role.
     */
    private final CR pred;

    /**
     * Keeps track of the concepts that this concept is liked to through some
     * role.
     */
    private final CR succ;

    /**
     * Flag to indicate if changes to the subsumptions of this context should be
     * tracked.
     */
    private AtomicBoolean track = new AtomicBoolean(false);

    /**
     * Flag used to indicate if this context has generated new subsumptions
     * while being tracked.
     */
    private boolean changed = false;

    /**
     * Reference to the parent context queue. Used to add this context back to
     * the queue when reactivated.
     */
    private static Queue<Context> parentTodo;

    /**
     * Reference to the parent context index. Used to add queue entries to other
     * contexts.
     */
    private static IConceptMap<Context> contextIndex;

    /**
     * Reference to the global role closure cache.
     */
    private static RoleMap<RoleSet> roleClosureCache;

    /**
     * Reference to the global factory.
     */
    @SuppressWarnings("rawtypes")
    private static IFactory factory;

    /**
     * The set of NF1 terms in the ontology.
     * 
     * These terms are of the form A n Ai [ B and are indexed by A.
     */
    private static IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> 
        ontologyNF1;

    /**
     * The set of NF2 terms in the ontology.
     * 
     * These terms are of the form A [ r.B and are indexed by A.
     */
    private static IConceptMap<MonotonicCollection<NF2>> ontologyNF2;

    /**
     * The set of NF3 terms in the ontology.
     * 
     * These terms are of the form r.A [ b and indexed by A.
     */
    private static IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>>
        ontologyNF3;

    /**
     * The set of NF4 terms in the ontology
     */
    private static IMonotonicCollection<NF4> ontologyNF4;

    /**
     * The set of NF5 terms in the ontology
     */
    private static IMonotonicCollection<NF5> ontologyNF5;

    /**
     * The set of reflexive roles in the ontology
     */
    private static IConceptSet reflexiveRoles = new SparseConceptSet();

    /**
     * The set of NF7 terms in the ontology.
     * 
     * These terms are of the form A [ f.(o, v) and are indexed by A.
     */
    private static IConceptMap<MonotonicCollection<NF7>> ontologyNF7;

    /**
     * The set of NF8 terms in the ontology.
     * 
     * These terms are of the form f.(o, v) [ A. These are indexed by f.
     */
    private static FeatureMap<MonotonicCollection<NF8>> ontologyNF8;

    /**
     * The map of global affected contexts used in incremental classification.
     */
    private static Set<Context> affectedContexts;

    /**
     * Initialises the static variables.
     * 
     * @param ont
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void init(NormalisedOntology ont) {
        parentTodo = ont.getTodo();
        contextIndex = ont.getContextIndex();
        ontologyNF1 = ont.getOntologyNF1();
        ontologyNF2 = ont.getOntologyNF2();
        ontologyNF3 = ont.getOntologyNF3();
        ontologyNF4 = ont.getOntologyNF4();
        ontologyNF5 = ont.getOntologyNF5();
        reflexiveRoles = ont.getReflexiveRoles();
        ontologyNF7 = ont.getOntologyNF7();
        ontologyNF8 = ont.getOntologyNF8();
        roleClosureCache = ont.getRoleClosureCache();
        factory = ont.getFactory();
        affectedContexts = ont.getAffectedContexts();
    }

    /**
     * Constructor.
     * 
     * @param concept
     */
    public Context(int concept) {
        this.concept = concept;
        s = new SparseConceptSet();
        s.add(concept);
        s.add(IFactory.TOP_CONCEPT);

        pred = new CR(factory.getTotalRoles());
        succ = new CR(factory.getTotalRoles());

        // Prime queues for this context
        primeQueue();
    }

    public int getConcept() {
        return concept;
    }

    private void addToConceptQueue(
            MonotonicCollection<IConjunctionQueueEntry> entries) {
        Iterator<IConjunctionQueueEntry> it = entries.iterator();
        while (it.hasNext()) {
            conceptQueue.add(it.next());
        }
    }

    private void primeQueue() {
        final MonotonicCollection<IConjunctionQueueEntry> nf1e = ontologyNF1
                .get(concept);
        if (nf1e != null)
            addToConceptQueue(nf1e);

        final MonotonicCollection<NF2> nf2e = ontologyNF2.get(concept);
        if (nf2e != null)
            roleQueue.addAll(nf2e);

        final MonotonicCollection<NF7> nf7e = ontologyNF7.get(concept);
        if (nf7e != null)
            featureQueue.addAll(nf7e);
    }

    /**
     * Adds queue entries for this concept based on the new axioms added in an
     * incremental classification.
     * 
     * @param conceptEntries
     * @param roleEntries
     * @param featureEntries
     */
    public void primeQueuesIncremental(
            MonotonicCollection<IConjunctionQueueEntry> conceptEntries,
            MonotonicCollection<IRoleQueueEntry> roleEntries,
            MonotonicCollection<IFeatureQueueEntry> featureEntries) {
        if (conceptEntries != null)
            addToConceptQueue(conceptEntries);
        if (roleEntries != null)
            roleQueue.addAll(roleEntries);
        if (featureEntries != null)
            featureQueue.addAll(featureEntries);
    }

    /**
     * Returns this concept's subsumptions.
     * 
     * @return
     */
    public IConceptSet getS() {
        return s;
    }

    /**
     * Returns the data structure used to hold the concepts and roles that point
     * to this concept.
     * 
     * @return
     */
    public CR getPred() {
        return pred;
    }

    /**
     * Returns the data structure used to hold the concepts and roles that this
     * concept points at.
     * 
     * @return
     */
    public CR getSucc() {
        return succ;
    }

    /**
     * Activates the context. Returns true if the context was inactive and was
     * activated by this method call or false otherwise.
     * 
     * @return boolean
     */
    public boolean activate() {
        return active.compareAndSet(false, true);
    }

    /**
     * Deactivates the context. Returns true if the context was active and was
     * deactivated by this method call or false otherwise.
     */
    public void deactivate() {
        active.set(false);
        if (!(conceptQueue.isEmpty() && roleQueue.isEmpty() && featureQueue
                .isEmpty())) {
            if (activate()) {
                parentTodo.add(this);
            }
        }
    }

    /**
     * Adds an entry to this context's concept queue.
     * 
     * @param entry
     */
    public void addConceptQueueEntry(IConjunctionQueueEntry entry) {
        conceptQueue.add(entry);
    }

    public void addConceptQueueEntries(
            Collection<IConjunctionQueueEntry> entries) {
        conceptQueue.addAll(entries);
    }

    /**
     * Adds an entry to this context's role queue.
     * 
     * @param entry
     */
    public void addRoleQueueEntry(IRoleQueueEntry entry) {
        roleQueue.add(entry);
    }

    /**
     * Adds and entry to this context's feature queue.
     * 
     * @param entry
     */
    public void addFeatureQueueEntry(IFeatureQueueEntry entry) {
        featureQueue.add(entry);
    }

    /**
     * Triggers the processing of an edge based on events that happened in
     * another {@link Context}.
     * 
     * @param role
     * @param src
     */
    public void processExternalEdge(final int role, final int src) {
        externalQueue.add(new IRoleQueueEntry() {
            /**
             * Serialisation version.
             */
            private static final long serialVersionUID = 1L;

            @Override
            public int getR() {
                return role;
            }

            @Override
            public int getB() {
                return src;
            }
        });
    }

    /**
     * Starts the classification process.
     */
    public void processOntology() {
        // This code is duplicated for performance reasons. When not running in
        // incremental mode the evaluation of the track flag is only done once
        // for each time the context is activated.
        if (track.get()) {
            processOntologyTracking();
        } else {
            processOntologyInternal();
        }

        deactivate();
    }

    private void processOntologyInternal() {
        boolean done;

        do {
            done = true;

            // Process concept queue
            if (!conceptQueue.isEmpty()) {
                do {
                    done = false;
                    final IConjunctionQueueEntry entry = conceptQueue.remove();
                    final int b = entry.getB();

                    if (!s.contains(b)) {
                        final int bi = entry.getBi();
                        if (s.contains(bi)) {
                            s.add(b);
                            processNewSubsumption(b);
                        }
                    }
                } while (!conceptQueue.isEmpty());
            }

            // Process feature queue
            if (!featureQueue.isEmpty()) {
                do {
                    done = false;
                    final IFeatureQueueEntry entry = featureQueue.remove();

                    Datatype d = entry.getD();

                    // Get right hand sides from NF8 expressions that
                    // match d on their left hand side
                    MonotonicCollection<NF8> entries = ontologyNF8.get(d
                            .getFeature());

                    if (entries == null)
                        continue;

                    // Evaluate to determine the ones that match
                    MonotonicCollection<IConjunctionQueueEntry> res = 
                            new MonotonicCollection<IConjunctionQueueEntry>(2);
                    for (final NF8 e : entries) {
                        Datatype d2 = e.lhsD;

                        // If they match add a conjunction queue entry
                        // to queueA
                        if (datatypeMatches(d, d2)) {
                            res.add(new IConjunctionQueueEntry() {
                                /**
                                 * Serialisation version.
                                 */
                                private static final long serialVersionUID = 1L;

                                @Override
                                public int getBi() {
                                    return CoreFactory.TOP_CONCEPT;
                                }

                                @Override
                                public int getB() {
                                    return e.rhsB;
                                }
                            });
                        }
                    }

                    addToConceptQueue(res);
                } while (!featureQueue.isEmpty());
            }

            // Process role queue
            if (!roleQueue.isEmpty()) {
                done = false;
                final IRoleQueueEntry entry = roleQueue.remove();

                if (!succ.lookupConcept(entry.getR()).contains(entry.getB())) {
                    processNewEdge(entry.getR(), entry.getB());
                }
            }

            if (!externalQueue.isEmpty()) {
                done = false;
                final IRoleQueueEntry entry = externalQueue.remove();
                processNewEdge(entry.getR(), entry.getB());
            }

        } while (!done);
    }

    private void processNewSubsumption(final int b) {
        // Get the set of parent concepts of (b n x) in the ontology
        final MonotonicCollection<IConjunctionQueueEntry> bConceptEntries = 
                ontologyNF1.get(b);
        if (null != bConceptEntries && bConceptEntries.size() > 0) {
            // Add these to the queue of a
            addToConceptQueue(bConceptEntries);
        }
        final MonotonicCollection<NF2> bRoleEntries = ontologyNF2.get(b);
        if (null != bRoleEntries) {
            roleQueue.addAll(bRoleEntries);
        }

        // inlined ontHat(conceptQueues.get(pairA(p)), r, b) in following
        // to move test and fetch outside innermost loop
        //
        final RoleMap<Collection<IConjunctionQueueEntry>> map = 
                ontologyNF3.get(b);
        if (null != map) {
            final RoleSet keySet = map.keySet();
            for (int r = keySet.first(); r >= 0; r = keySet.next(r + 1)) {
                final Collection<IConjunctionQueueEntry> entries = map.get(r);

                if (null != entries) {
                    final IConceptSet aPrimes = pred.lookupConcept(r);
                    for (final IntIterator itr = aPrimes.iterator(); itr
                            .hasNext();) {
                        final int aa = itr.next();
                        // Add to queue aa
                        if (concept == aa) {
                            conceptQueue.addAll(entries);
                        } else {
                            // Add to external context concept queue and
                            // activate
                            Context oc = contextIndex.get(aa);
                            oc.addConceptQueueEntries(entries);
                            if (oc.activate())
                                parentTodo.add(oc);
                        }
                    }
                }
            }
        }
    }

    /**
     * Evaluates the equivalence of two {@link Datatype}s. This method assumes
     * that the literals both have the same feature and therefore also have
     * matching literal types.
     * 
     * @param d1
     *            Data type from an NF7 entry.
     * @param d2
     *            Data type from an NF8 entry.
     * @return boolean
     */
    private boolean datatypeMatches(Datatype d1, Datatype d2) {
        assert (d1.getFeature() == d2.getFeature());
        Operator lhsOp = d1.getOperator();
        Operator rhsOp = d2.getOperator();
        AbstractLiteral lhsLit = d1.getLiteral();
        AbstractLiteral rhsLit = d2.getLiteral();
        if (rhsOp == Operator.EQUALS) {
            // If the rhs operator is =, then the expression will only match
            // if the lhs operator is also = and the literal values are the
            // same.
            return d1.getLiteral().equals(d2.getLiteral());
        } else if (rhsOp == Operator.GREATER_THAN) {
            if (lhsOp == Operator.LESS_THAN
                    || lhsOp == Operator.LESS_THAN_EQUALS) {
                return false;
            } else if (lhsOp == Operator.EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) > 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.GREATER_THAN) {
                if (compareLiterals(lhsLit, rhsLit) >= 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.GREATER_THAN_EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) > 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } else if (rhsOp == Operator.GREATER_THAN_EQUALS) {
            if (lhsOp == Operator.LESS_THAN
                    || lhsOp == Operator.LESS_THAN_EQUALS) {
                return false;
            } else if (lhsOp == Operator.EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) >= 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.GREATER_THAN) {
                if (compareLiterals(lhsLit, rhsLit) >= -1) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.GREATER_THAN_EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) >= 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } else if (rhsOp == Operator.LESS_THAN) {
            if (lhsOp == Operator.GREATER_THAN
                    || lhsOp == Operator.GREATER_THAN_EQUALS) {
                return false;
            } else if (lhsOp == Operator.EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) < 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.LESS_THAN) {
                if (compareLiterals(lhsLit, rhsLit) <= 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.LESS_THAN_EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) < 0) {
                    return true;
                } else {
                    return false;
                }
            }
        } else if (rhsOp == Operator.LESS_THAN_EQUALS) {
            if (lhsOp == Operator.GREATER_THAN
                    || lhsOp == Operator.GREATER_THAN_EQUALS) {
                return false;
            } else if (lhsOp == Operator.EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) <= 0) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.LESS_THAN) {
                if (compareLiterals(lhsLit, rhsLit) <= 1) {
                    return true;
                } else {
                    return false;
                }
            } else if (lhsOp == Operator.LESS_THAN_EQUALS) {
                if (compareLiterals(lhsLit, rhsLit) <= 0) {
                    return true;
                } else {
                    return false;
                }
            }
        }

        return d1.getLiteral().equals(d2.getLiteral());
    }

    /**
     * Return 0 if both literals are equals. Returns an int > 0 if l1 is greater
     * than l2 and an int < 0 if l1 is less than l2.
     * 
     * @param l1
     * @param l2
     * @return
     */
    private int compareLiterals(AbstractLiteral l1, AbstractLiteral l2) {
        return l1.compareTo(l2);
    }

    /**
     * Process new subsumption: a [ role.b
     * 
     * @param a
     * @param role
     * @param b
     */
    private void processNewEdge(int role, int b) {
        final RoleSet roleClosure = getRoleClosure(role);
        processRole(role, b);
        for (int s = roleClosure.first(); s >= 0; s = roleClosure.next(s + 1)) {
            if (s == role)
                continue;
            processRole(s, b);
        }
    }

    private void processRole(int s, int b) {
        // R(s) := R(s) u {(A,B)}
        succ.store(s, b);

        // Add the predecessor to the the corresponding context
        // Is this necessary?
        Context bContext = contextIndex.get(b);

        bContext.getPred().store(s, concept);

        // queue(A) := queue(A) u U{B'|B' in S(B)}.O^(s.B')
        final IConceptSet sb = contextIndex.get(b).getS();

        // Computes the minimal set of QueueEntries from s.a [ bb is in O
        for (IntIterator itr = sb.iterator(); itr.hasNext();) {
            final int bb = itr.next();
            final RoleMap<Collection<IConjunctionQueueEntry>> map = 
                    ontologyNF3.get(bb);

            if (null != map) {
                final Collection<IConjunctionQueueEntry> entries = map.get(s);
                if (null != entries) {
                    conceptQueue.addAll(entries);
                }
            }
        }

        // Handle reflexive roles
        if (reflexiveRoles.contains(s)) {
            // check for (a,a) in R(s)
            if (!pred.lookupConcept(s).contains(concept)) {
                processNewEdge(s, concept);
            }

            // check for (b,b) in R(s)
            Context tc = contextIndex.get(b);
            if (!tc.getPred().lookupConcept(s).contains(b)) {
                tc.processExternalEdge(s, b);
                if (tc.activate()) {
                    parentTodo.add(tc);
                }
            }
        }

        final List<int[]> work = new ArrayList<int[]>();
        for (final NF5 nf5 : ontologyNF5) {
            if (s == nf5.getS()) {
                final int t = nf5.getR();
                final int u = nf5.getT();
                final IConceptSet aTPrimes = pred.lookupConcept(t);

                // Again in this case there is a dependency with the
                // predecessors of an external context.
                final IConceptSet bUPrimes = contextIndex.get(b).getPred()
                        .lookupConcept(u);

                for (final IntIterator itr = aTPrimes.iterator(); 
                        itr.hasNext();) {
                    final int aa = itr.next();

                    if (!bUPrimes.contains(aa)) {
                        work.add(new int[] { aa, u });
                    }

                }
            }
        }

        for (final int[] pair : work) {
            if (pair[0] == concept) {
                processNewEdge(pair[1], b);
            } else {
                Context tc = contextIndex.get(pair[0]);
                tc.processExternalEdge(pair[1], b);
                if (tc.activate()) {
                    parentTodo.add(tc);
                }
            }
        }

        work.clear();
        for (final NF5 nf5 : ontologyNF5) {
            if (s == nf5.getR()) {
                final int t = nf5.getS();
                final int u = nf5.getT();
                // In this case there is a dependency with the
                // successors of an external context.
                final IConceptSet bTPrimes = contextIndex.get(b).getSucc()
                        .lookupConcept(t);
                final IConceptSet aUPrimes = succ.lookupConcept(u);

                for (final IntIterator itr = bTPrimes.iterator(); 
                        itr.hasNext();) {
                    final int bb = itr.next();

                    if (!aUPrimes.contains(bb)) {
                        work.add(new int[] { u, bb });
                    }

                }
            }
        }
        for (final int[] pair : work) {
            processNewEdge(pair[0], pair[1]);
        }
    }

    private RoleSet getRoleClosure(final int r) {
        RoleSet result = roleClosureCache.get(r);
        if (null == result) {
            result = new RoleSet();
            result.add(r);
            for (final NF4 nf4 : ontologyNF4) {
                if (r == nf4.getR()) {
                    result.addAll(getRoleClosure(nf4.getS()));
                }
            }
            // We do this after the above recursive call to trigger a stack
            // overflow in case there's a role-inclusion cycle
            roleClosureCache.put(r, result);
        }
        return result;
    }

    /**
     * Starts tracking changes in the context's subsumptions. It is used in
     * incremental classification to detect which contexts have been affected by
     * the new axioms.
     */
    public void startTracking() {
        if (track.compareAndSet(false, true)) {
            changed = false;
        }
    }

    /**
     * Stops tracking changes to the context's subsumptions.
     */
    public void endTracking() {
        track.set(false);
    }

    /**
     * Indicates if the context has generated new subsumptions while being
     * tracked.
     */
    public boolean hasNewSubsumptions() {
        return changed;
    }

    ////////////////////////////////////////////////////////////////////////////
    // Tracking versions of the methods
    ////////////////////////////////////////////////////////////////////////////

    private void processOntologyTracking() {
        boolean done;

        do {
            done = true;

            // Process concept queue
            if (!conceptQueue.isEmpty()) {
                do {
                    done = false;
                    final IConjunctionQueueEntry entry = conceptQueue.remove();
                    final int b = entry.getB();

                    if (!s.contains(b)) {
                        final int bi = entry.getBi();
                        if (s.contains(bi)) {
                            s.add(b);
                            changed = true;
                            processNewSubsumptionTracking(b);
                        }
                    }
                } while (!conceptQueue.isEmpty());
            }

            // Process feature queue
            if (!featureQueue.isEmpty()) {
                do {
                    done = false;
                    final IFeatureQueueEntry entry = featureQueue.remove();

                    Datatype d = entry.getD();

                    // Get right hand sides from NF8 expressions that
                    // match d on their left hand side
                    MonotonicCollection<NF8> entries = 
                            ontologyNF8.get(d.getFeature());

                    if (entries == null)
                        continue;

                    // Evaluate to determine the ones that match
                    MonotonicCollection<IConjunctionQueueEntry> res = 
                            new MonotonicCollection<IConjunctionQueueEntry>(2);
                    for (final NF8 e : entries) {
                        Datatype d2 = e.lhsD;

                        // If they match add a conjunction queue entry
                        // to queueA
                        if (datatypeMatches(d, d2)) {
                            res.add(new IConjunctionQueueEntry() {
                                /**
                                 * Serialisation version.
                                 */
                                private static final long serialVersionUID = 1L;

                                @Override
                                public int getBi() {
                                    return CoreFactory.TOP_CONCEPT;
                                }

                                @Override
                                public int getB() {
                                    return e.rhsB;
                                }
                            });
                        }
                    }

                    addToConceptQueue(res);
                } while (!featureQueue.isEmpty());
            }

            // Process role queue
            if (!roleQueue.isEmpty()) {
                done = false;
                final IRoleQueueEntry entry = roleQueue.remove();

                if (!succ.lookupConcept(entry.getR()).contains(entry.getB())) {
                    processNewEdgeTracking(entry.getR(), entry.getB());
                }
            }

            if (!externalQueue.isEmpty()) {
                done = false;
                final IRoleQueueEntry entry = externalQueue.remove();
                processNewEdgeTracking(entry.getR(), entry.getB());
            }

        } while (!done);
    }

    private void processNewSubsumptionTracking(final int b) {
        // Get the set of parent concepts of (b n x) in the ontology
        final MonotonicCollection<IConjunctionQueueEntry> bConceptEntries = 
                ontologyNF1.get(b);
        if (null != bConceptEntries && bConceptEntries.size() > 0) {
            // Add these to the queue of a
            addToConceptQueue(bConceptEntries);
        }
        final MonotonicCollection<NF2> bRoleEntries = ontologyNF2.get(b);
        if (null != bRoleEntries) {
            roleQueue.addAll(bRoleEntries);
        }

        // inlined ontHat(conceptQueues.get(pairA(p)), r, b) in following
        // to move test and fetch outside innermost loop
        //
        final RoleMap<Collection<IConjunctionQueueEntry>> map = ontologyNF3
                .get(b);
        if (null != map) {
            final RoleSet keySet = map.keySet();
            for (int r = keySet.first(); r >= 0; r = keySet.next(r + 1)) {
                final Collection<IConjunctionQueueEntry> entries = map.get(r);

                if (null != entries) {
                    final IConceptSet aPrimes = pred.lookupConcept(r);
                    for (final IntIterator itr = aPrimes.iterator(); itr
                            .hasNext();) {
                        final int aa = itr.next();
                        // Add to queue aa
                        if (concept == aa) {
                            conceptQueue.addAll(entries);
                        } else {
                            // Add to external context concept queue and
                            // activate
                            Context oc = contextIndex.get(aa);
                            oc.addConceptQueueEntries(entries);
                            affectedContexts.add(oc);
                            oc.startTracking();
                            if (oc.activate())
                                parentTodo.add(oc);
                        }
                    }
                }
            }
        }
    }

    private void processNewEdgeTracking(int role, int b) {
        final RoleSet roleClosure = getRoleClosure(role);
        processRoleTracking(role, b);
        for (int s = roleClosure.first(); s >= 0; s = roleClosure.next(s + 1)) {
            if (s == role)
                continue;
            processRoleTracking(s, b);
        }
    }

    private void processRoleTracking(int s, int b) {
        // R(s) := R(s) u {(A,B)}
        succ.store(s, b);

        // Add the predecessor to the the corresponding context
        // Is this necessary?
        Context bContext = contextIndex.get(b);

        bContext.getPred().store(s, concept);

        // queue(A) := queue(A) u U{B'|B' in S(B)}.O^(s.B')
        final IConceptSet sb = contextIndex.get(b).getS();

        // Computes the minimal set of QueueEntries from s.a [ bb is in O
        for (IntIterator itr = sb.iterator(); itr.hasNext();) {
            final int bb = itr.next();
            final RoleMap<Collection<IConjunctionQueueEntry>> map = 
                    ontologyNF3.get(bb);

            if (null != map) {
                final Collection<IConjunctionQueueEntry> entries = map.get(s);
                if (null != entries) {
                    conceptQueue.addAll(entries);
                }
            }
        }

        // Handle reflexive roles
        if (reflexiveRoles.contains(s)) {
            // check for (a,a) in R(s)
            if (!pred.lookupConcept(s).contains(concept)) {
                processNewEdgeTracking(s, concept);
            }

            // check for (b,b) in R(s)
            Context tc = contextIndex.get(b);
            if (!tc.getPred().lookupConcept(s).contains(b)) {
                tc.processExternalEdge(s, b);
                affectedContexts.add(tc);
                tc.startTracking();
                if (tc.activate()) {
                    parentTodo.add(tc);
                }
            }
        }

        final List<int[]> work = new ArrayList<int[]>();
        for (final NF5 nf5 : ontologyNF5) {
            if (s == nf5.getS()) {
                final int t = nf5.getR();
                final int u = nf5.getT();
                final IConceptSet aTPrimes = pred.lookupConcept(t);

                // Again in this case there is a dependency with the
                // predecessors of an external context.
                final IConceptSet bUPrimes = 
                        contextIndex.get(b).getPred().lookupConcept(u);

                for (final IntIterator itr = aTPrimes.iterator(); 
                        itr.hasNext();) {
                    final int aa = itr.next();

                    if (!bUPrimes.contains(aa)) {
                        work.add(new int[] { aa, u });
                    }

                }
            }
        }

        for (final int[] pair : work) {
            if (pair[0] == concept) {
                processNewEdgeTracking(pair[1], b);
            } else {
                Context tc = contextIndex.get(pair[0]);
                tc.processExternalEdge(pair[1], b);
                affectedContexts.add(tc);
                tc.startTracking();
                if (tc.activate()) {
                    parentTodo.add(tc);
                }
            }
        }

        work.clear();
        for (final NF5 nf5 : ontologyNF5) {
            if (s == nf5.getR()) {
                final int t = nf5.getS();
                final int u = nf5.getT();
                // In this case there is a dependency with the
                // successors of an external context.
                final IConceptSet bTPrimes = contextIndex.get(b).getSucc()
                        .lookupConcept(t);
                final IConceptSet aUPrimes = succ.lookupConcept(u);

                for (final IntIterator itr = bTPrimes.iterator(); 
                        itr.hasNext();) {
                    final int bb = itr.next();

                    if (!aUPrimes.contains(bb)) {
                        work.add(new int[] { u, bb });
                    }

                }
            }
        }
        for (final int[] pair : work) {
            processNewEdgeTracking(pair[0], pair[1]);
        }
    }

}
