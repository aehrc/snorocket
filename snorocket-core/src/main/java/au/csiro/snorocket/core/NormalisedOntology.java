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

import java.io.PrintWriter;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

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
import au.csiro.snorocket.core.concurrent.Context;
import au.csiro.snorocket.core.concurrent.Worker;
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
import au.csiro.snorocket.core.util.SparseConceptMap;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * A normalised EL Ontology
 * 
 * @author law223
 * 
 */
public class NormalisedOntology {

    static final Logger LOGGER = Snorocket.getLogger();

    // Increment 3rd place for upwards/backwards compatible change
    // Increment 2nd place for upwards compatible change
    // Increment 1st place for incompatible change
    private static final String FILE_VERSION = "3.0.0";

    final private static int TOP = IFactory.TOP_CONCEPT;

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
     * A set of contexts potentially affected by an incremental classification.
     */
    private final Set<Context> affectedContexts = new ConcurrentSkipListSet<>(
            new Comparator<Context>() {

                @Override
                public int compare(Context arg0, Context arg1) {
                    return Integer.compare(arg0.getConcept(), arg1.getConcept());
                }
            });

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
    public NormalisedOntology(final IFactory factory,
            final Set<? extends Inclusion> inclusions) {
        this(factory);

        for (Inclusion i : normalise(inclusions)) {
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
    public NormalisedOntology(final IFactory factory) {
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
            final IFactory factory,
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
    public void loadAxioms(final Set<? extends Inclusion> inclusions) {
        LOGGER.info("Loading " + inclusions.size() + " axioms");
        Set<Inclusion> normInclusions = normalise(inclusions);
        LOGGER.info("Processing " + normInclusions.size()
                + " normalised axioms");
        for (Inclusion i : normInclusions) {
            addTerm(i.getNormalForm());
        }
    }

    /**
     * Returns a set of Inclusions in normal form suitable for classifying.
     */
    public Set<Inclusion> normalise(final Set<? extends Inclusion> inclusions) {

        // Exhaustively apply NF1 to NF4
        final Set<Inclusion> done = new HashSet<Inclusion>();
        Set<Inclusion> oldIs = new HashSet<Inclusion>();
        Set<Inclusion> newIs = new HashSet<Inclusion>(inclusions);

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

        return done;
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
    public void classifyIncremental(Set<Inclusion> incAxioms) {
        // Clear any state from previous incremental classifications
        newContexts.clear();
        affectedContexts.clear();

        // Normalise axioms
        Set<Inclusion> inclusions = normalise(incAxioms);

        // Add new axioms to corresponding normal form
        AxiomSet as = new AxiomSet();
        int numNewConcepts = 0;

        for (Inclusion i : inclusions) {
            NormalFormGCI nf = i.getNormalForm();
            as.addAxiom(nf);
            addTerm(nf);
        }

        // Determine which contexts are affected
        for (Inclusion i : inclusions) {
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
                    if (LOGGER.isLoggable(Level.FINE)) {
                        LOGGER.fine("Added context " + c);
                    }

                    // Keep track of the newly added contexts
                    newContexts.add(c);
                    numNewConcepts++;
                }
            }
        }

        LOGGER.info("Added " + numNewConcepts
                + " new concepts to the ontology.");

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
        int numThreads = Runtime.getRuntime().availableProcessors();
        LOGGER.log(Level.INFO, "Classifying incrementally with " + numThreads
                + " threads");

        LOGGER.log(Level.INFO, "Running saturation");
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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Processed " + contextIndex.size() + " contexts");
        }
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
        IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> deltaNF1 = new SparseConceptMap<MonotonicCollection<IConjunctionQueueEntry>>(
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
        IConceptMap<MonotonicCollection<NF2>> deltaNF2 = new SparseConceptMap<MonotonicCollection<NF2>>(
                size);
        for (NF2 nf2 : as.getNf2Axioms()) {
            addTerms(deltaNF2, nf2);
        }

        for (final IntIterator aItr = subsumptions.keyIterator(); aItr
                .hasNext();) {
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
        IConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>> deltaNF3 = new SparseConceptMap<RoleMap<Collection<IConjunctionQueueEntry>>>(
                size);
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

                    for (final IntIterator cItr = bCtx.getSucc()
                            .lookupConcept(nf5.getS()).iterator(); cItr
                            .hasNext();) {
                        final int c = cItr.next();

                        if (!aCtx.getSucc().lookupConcept(t).contains(c)) {
                            final IRoleQueueEntry entry = new IRoleQueueEntry() {

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
                if (ctx.getSucc().containsRole(role)
                        && !ctx.getSucc().lookupConcept(role).contains(concept)) {
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
        IConceptMap<MonotonicCollection<NF7>> deltaNF7 = new SparseConceptMap<MonotonicCollection<NF7>>(
                size);
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

                for (Iterator<NF7> i = ontologyNF7.get(a).iterator(); i
                        .hasNext();) {
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
        int numThreads = Runtime.getRuntime().availableProcessors();
        LOGGER.log(Level.INFO, "Classifying with " + numThreads + " threads");

        Context.init(NormalisedOntology.this);

        // Create contexts for init concepts in the ontology
        int numConcepts = factory.getTotalConcepts();
        for (int i = 0; i < numConcepts; i++) {
            Context c = new Context(i);
            contextIndex.put(i, c);
            if (c.activate()) {
                todo.add(c);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Added context " + c);
            }
        }

        LOGGER.log(Level.INFO, "Running saturation");
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

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Processed " + contextIndex.size() + " contexts");
        }
    }

    public IConceptMap<IConceptSet> getSubsumptions() {
        IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(
                factory.getTotalConcepts());
        // Collect subsumptions from context index
        for (IntIterator it = contextIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            Context ctx = contextIndex.get(key);
            res.put(key, ctx.getS().getSet());
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
            res.put(ctx.getConcept(), ctx.getS().getSet());
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
                res.put(key, ctx.getS().getSet());
            }
        }
        return res;
    }

    public R getRelationships() {
        // Collect relationships from context index
        // TODO: implement or check if needed.
        return null;
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

    /**
     * 
     * @param writer
     */
    protected void printClassification(final PrintWriter writer) {
        // must print factory state first
        factory.printAll(writer);

        writer.println(FILE_VERSION);

        // print ontology rules
        writer.println("Rules--------");
        for (final IntIterator itr = ontologyNF1.keyIterator(); itr.hasNext();) {
            final int a = itr.next();
            MonotonicCollection<IConjunctionQueueEntry> entries = ontologyNF1
                    .get(a);
            for (final IConjunctionQueueEntry entry : entries) {
                writer.print(a + "\t=>");
                writer.print("\t" + entry.getB());
                if (entry.getBi() > TOP) {
                    writer.print("\t" + entry.getBi());
                }
                writer.println();
            }
        }
        writer.println("--------");

        for (final IntIterator itr = ontologyNF2.keyIterator(); itr.hasNext();) {
            final int a = itr.next();
            MonotonicCollection<NF2> entries = ontologyNF2.get(a);
            for (final NF2 entry : entries) {
                writer.print(a + "\t=>");
                writer.print("\t" + entry.getR());
                writer.print("\t" + entry.getB());
                writer.println();
            }
        }
        writer.println("--------");

        for (final IntIterator itr = ontologyNF3.keyIterator(); itr.hasNext();) {
            final int a = itr.next();
            RoleMap<Collection<IConjunctionQueueEntry>> map = ontologyNF3
                    .get(a);
            for (int r = 0; r < factory.getTotalRoles(); r++) {
                if (map.containsKey(r)) {
                    final Collection<IConjunctionQueueEntry> entries = map
                            .get(r);
                    for (IConjunctionQueueEntry entry : entries) {
                        writer.print(a + "\t" + r + "\t=>");
                        writer.print("\t" + entry.getB());
                        if (entry.getBi() > TOP) {
                            writer.print("\t" + entry.getBi());
                        }
                        writer.println();
                    }
                }
            }
        }
        writer.println("--------");

        for (NF4 nf : ontologyNF4) {
            writer.println(nf.getR() + "\t" + nf.getS());
        }
        writer.println("--------");

        for (NF5 nf : ontologyNF5) {
            writer.println(nf.getR() + "\t" + nf.getS() + "\t" + nf.getT());
        }
        writer.println("--------");

        for (IntIterator itr = reflexiveRoles.iterator(); itr.hasNext();) {
            final int role = itr.next();
            writer.println(role);
        }
        writer.println("--------");

    }

    public IFactory getFactory() {
        return factory;
    }

}
