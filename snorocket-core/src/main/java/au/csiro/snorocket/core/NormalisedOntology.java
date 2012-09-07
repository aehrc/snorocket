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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import au.csiro.snorocket.core.axioms.IConjunctionQueueEntry;
import au.csiro.snorocket.core.axioms.IFeatureQueueEntry;
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
import au.csiro.snorocket.core.model.AbstractLiteral;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.FeatureMap;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IMonotonicCollection;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.LineReader;
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

    /**
     * If true, then process all entries in Q(a) rather than one entry per loop.
     */
    private static boolean BATCH_PROCESS = true;

    final private static boolean TRACE_PRIME = Snorocket.DEBUGGING;
    final private static boolean TRACE_LOOP = Snorocket.DEBUGGING;

    final private static int TOP = IFactory.TOP_CONCEPT;

    final protected IFactory factory;

    /**
     * The set of NF1 terms in the ontology
     * <ul><li>Concept map 76.5% full (SNOMED 20061230)</li></ul>
     * 
     * These terms are of the form A n Ai [ B and are indexed by A.
     */
    final protected IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> ontologyNF1;

    /**
     * The set of NF2 terms in the ontology
     * <ul><li>Concept map 34.7% full (SNOMED 20061230)</li></ul>
     * 
     * These terms are of the form A [ r.B and are indexed by A.
     */
    final protected IConceptMap<MonotonicCollection<NF2>> ontologyNF2;

    /**
     * The set of NF3 terms in the ontology
     * <ul><li>Concept map 9.3% full (SNOMED 20061230)</li><li>Unknown usage profile for Role maps</li></ul>
     * 
     * These terms are of the form r.A [ b and indexed by A.
     */
    final protected IConceptMap<RoleMap<IConjunctionQueueEntry>> ontologyNF3;

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

    public static void setBatchMode(boolean batchProcess) {
        BATCH_PROCESS = batchProcess;
    }

    public static boolean isBatchMode() {
        return BATCH_PROCESS;
    }
    
    private NormalisedOntology(final LineReader reader) throws IOException, ParseException {
        this(Factory.loadAll(reader));

        if (Snorocket.DEBUGGING) System.err.println("Cs: " + factory.getTotalConcepts() + "\tRs: " + factory.getTotalRoles());

        loadNormalisedOntology(reader);
    }

    public NormalisedOntology(final IFactory factory, 
    		final Set<? extends Inclusion> inclusions) {
        this(factory);
        
        for (Inclusion i: normalise(inclusions)) {
            addTerm(i.getNormalForm());
        }
    }
    
    /**
     * 
     * @param baseConceptCount
     * @param conceptCount if this value is too small, the algorithm performance will be impacted
     * @param roleCount
     */
    public NormalisedOntology(final IFactory factory) {
        this(factory,
            new DenseConceptMap<MonotonicCollection<IConjunctionQueueEntry>>(factory.getTotalConcepts()),
            new SparseConceptMap<MonotonicCollection<NF2>>(factory.getTotalConcepts(), "ontologyNF2"),
            new SparseConceptMap<RoleMap<IConjunctionQueueEntry>>(factory.getTotalConcepts(), "ontologyNF3"),
            new MonotonicCollection<NF4>(15),
            new MonotonicCollection<NF5>(1),
            new SparseConceptMap<MonotonicCollection<NF7>>(factory.getTotalConcepts(), "ontologyNF7"),
            new FeatureMap<MonotonicCollection<NF8>>(factory.getTotalConcepts())
        );
    }

    protected NormalisedOntology(
            final IFactory factory,
            final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> nf1q,
            final IConceptMap<MonotonicCollection<NF2>> nf2q,
            final IConceptMap<RoleMap<IConjunctionQueueEntry>> nf3q,
            final IMonotonicCollection<NF4> nf4q,
            final IMonotonicCollection<NF5> nf5q,
            final IConceptMap<MonotonicCollection<NF7>> nf7q,
            final FeatureMap<MonotonicCollection<NF8>> nf8q
            ) {
        this.factory = factory;

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
    	for (Inclusion i: normalise(inclusions)) {
            addTerm(i.getNormalForm());
        }
    }

    public static Classification loadClassification(final BufferedReader reader) throws IOException, ParseException {
        final LineReader lineReader = new LineReader(reader);
        return new NormalisedOntology(lineReader).new Classification(lineReader);
    }
    
    /**
     * Returns a set of Inclusions in normal form suitable for classifying.
     */
    public Set<Inclusion> normalise(
    		final Set<? extends Inclusion> inclusions) {
        
        // Exhaustively apply NF1 to NF4
        final Set<Inclusion> done = new HashSet<Inclusion>();
        Set<Inclusion> oldIs = new HashSet<Inclusion>();
        Set<Inclusion> newIs = new HashSet<Inclusion>(inclusions);

        do {
            final Set<Inclusion> tmp = oldIs;
            oldIs = newIs;
            newIs = tmp;
            newIs.clear();
            
            for (Inclusion i: oldIs) {
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
            
            for (Inclusion i: oldIs) {
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
     * @param term The normalised term.
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
        } else if(term instanceof NF7) {
        	final NF7 nf7 = (NF7) term;
        	addTerms(ontologyNF7, nf7);
        } else if(term instanceof NF8) {
        	final NF8 nf8 = (NF8) term;
        	addTerms(ontologyNF8, nf8);
        } else {
            throw new IllegalArgumentException("type of " + term + " must be one of NF1 through NF6");
        }
    }
    
    protected void addTerms(final IConceptMap<MonotonicCollection<IConjunctionQueueEntry>> entries, final int a, final IConjunctionQueueEntry queueEntry) {
        MonotonicCollection<IConjunctionQueueEntry> queueA = entries.get(a);
        if (null == queueA) {
            queueA = new MonotonicCollection<IConjunctionQueueEntry>(2); // FIXME can we estimate size better?
            entries.put(a, queueA);
        }

        queueA.add(queueEntry);
    }
    
    protected void addTerms(final IConceptMap<MonotonicCollection<NF2>> entries, final NF2 nf2) {
        MonotonicCollection<NF2> set = entries.get(nf2.lhsA);
        if (null == set) {
            set = new MonotonicCollection<NF2>(2);  // FIXME can we estimate size better?
            entries.put(nf2.lhsA, set);
        }
        set.add(nf2);
    }
    
    protected void addTerms(final IConceptMap<RoleMap<IConjunctionQueueEntry>> queue, final NF3 nf3) {
        RoleMap<IConjunctionQueueEntry> map = queue.get(nf3.lhsA);
        IConjunctionQueueEntry entry;
        if (null == map) {
            map = new RoleMap<IConjunctionQueueEntry>(factory.getTotalRoles());
            queue.put(nf3.lhsA, map);
            entry = null;
        } else {
            entry = map.get(nf3.lhsR);
        }
        if (null == entry) {
            map.put(nf3.lhsR, nf3.getQueueEntry());
        } else if (nf3.rhsB != nf3.getQueueEntry().getB()) {
            throw new IllegalArgumentException("This implementation only supports a single GCI per LHS role,concept pair: " + factory.lookupRoleId(nf3.lhsR) + "." + factory.lookupConceptId(nf3.lhsA));
        }
    }
    
    protected void addTerms(final IConceptMap<MonotonicCollection<NF7>> entries, final NF7 nf7) {
        MonotonicCollection<NF7> set = entries.get(nf7.lhsA);
        if (null == set) {
            set = new MonotonicCollection<NF7>(2);  // FIXME can we estimate size better?
            entries.put(nf7.lhsA, set);
        }
        set.add(nf7);
    }
    
    protected void addTerms(
    		final FeatureMap<MonotonicCollection<NF8>> entries, 
    		final NF8 nf8) {
        MonotonicCollection<NF8> set = 
        		entries.get(nf8.lhsD.getFeature());
        if(null == set) {
        	set = new MonotonicCollection<NF8>(2);
        	entries.put(nf8.lhsD.getFeature(), set);
        }
        set.add(nf8);
    }

    /**
     * Entry to the CEL Classification algorithm.
     * 
     */
    public Classification getClassification() {
        return new Classification();
    }

    public void printStats() {
        System.err.println("stats");
        int count1 = countKeys(ontologyNF1);
        System.err.println("ontologyNF1QueueEntries: #keys=" + count1 + ", #Concepts=" + factory.getTotalConcepts() + " ratio=" + ((double)count1/factory.getTotalConcepts()));
        int count2 = countKeys(ontologyNF2);
        System.err.println("ontologyNF2: #keys=" + count2 + ", #Concepts=" + factory.getTotalConcepts() + " ratio=" + ((double)count2/factory.getTotalConcepts()));
        int count3 = countKeys(ontologyNF3);
        System.err.println("ontologyNF3QueueEntries: #keys=" + count3 + ", #Concepts=" + factory.getTotalConcepts() + " ratio=" + ((double)count3/factory.getTotalConcepts()));
    }

    private int countKeys(IConceptMap<?> map) {
        int count = 0;
        for (IntIterator itr = map.keyIterator(); itr.hasNext(); ) {
            itr.next();
            count++;
        }
        return count;
    }

    protected void printClassification(final PrintWriter writer) {
        // must print factory state first
        factory.printAll(writer);

        writer.println(FILE_VERSION);
        
        // print ontology rules
        writer.println("Rules--------");
        for (final IntIterator itr = ontologyNF1.keyIterator(); itr.hasNext(); ) {
            final int a = itr.next();
            MonotonicCollection<IConjunctionQueueEntry> entries = ontologyNF1.get(a);
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
        
        for (final IntIterator itr = ontologyNF2.keyIterator(); itr.hasNext(); ) {
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
        
        for (final IntIterator itr = ontologyNF3.keyIterator(); itr.hasNext(); ) {
            final int a = itr.next();
            RoleMap<IConjunctionQueueEntry> map = ontologyNF3.get(a);
            for (int r = 0; r < factory.getTotalRoles(); r++) {
                if (map.containsKey(r)) {
                    final IConjunctionQueueEntry entry = map.get(r);
                    writer.print(a + "\t" + r + "\t=>");
                    writer.print("\t" + entry.getB());
                    if (entry.getBi() > TOP) {
                        writer.print("\t" + entry.getBi());
                    }
                    writer.println();
                }
            }
        }
        writer.println("--------");
        
        for (NF4 nf: ontologyNF4) {
            writer.println(nf.getR() + "\t" + nf.getS());
        }
        writer.println("--------");
        
        for (NF5 nf: ontologyNF5) {
            writer.println(nf.getR() + "\t" + nf.getS() + "\t" + nf.getT());
        }
        writer.println("--------");
        
        for (IntIterator itr = reflexiveRoles.iterator(); itr.hasNext(); ) {
            final int role = itr.next();
            writer.println(role);
    }
        writer.println("--------");

    }

    private void loadNormalisedOntology(final LineReader reader) throws IOException, ParseException {
        String line = reader.readLine();	// Read file version
        // TODO Implement failed version compatibility code
        if (!FILE_VERSION.equals(line)) {
            throw new ParseException("Unsupported file format version, found " + line + ", expected " + FILE_VERSION + " or compatible.", reader);
        }

        // Load up ontology rules
        long start = System.currentTimeMillis();
        line = reader.readLine();   // Read rules header
        loadNF1Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF1\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();
        loadNF2Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF2\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();
        loadNF3Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF3\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();
        loadNF4Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF4\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();
        loadNF5Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF5\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();
        loadNF6Rules(reader);
        if (Snorocket.DEBUGGING) System.err.println("NF6\t" + (System.currentTimeMillis() - start)/1000.0); start = System.currentTimeMillis();

    }

    private void loadNF1Rules(final LineReader reader) throws IOException {
        String line;
        // NF1 SYNTAX:
        // num '\t' '=>' '\t' num ('\t' num)?
        while (!"--------".equals(line = reader.readLine())) {
//          final String[] fields = line.split("\t");
//          final int a = Integer.parseInt(fields[0]);
            int idx = line.indexOf('\t');
            final int a = Integer.parseInt(line.substring(0, idx));
            int oldIdx = idx + 4;
            MonotonicCollection<IConjunctionQueueEntry> entries = ontologyNF1.get(a);
            if (null == entries) {
                entries = new MonotonicCollection<IConjunctionQueueEntry>(2);
                ontologyNF1.put(a, entries);
            }
            final int b;
            final int bi;
            final IConjunctionQueueEntry entry;
            idx = line.indexOf('\t', oldIdx);
            if (idx > 0) {
                b = Integer.parseInt(line.substring(oldIdx, idx));
                oldIdx = idx + 1;
                bi = Integer.parseInt(line.substring(oldIdx));
            } else {
                b = Integer.parseInt(line.substring(oldIdx));
                bi = TOP;
            }

            entry = new IConjunctionQueueEntry() {
                public int getB() {
                    return b;
                }
                public int getBi() {
                    return bi;
                }
            };

//          final int b = Integer.parseInt(fields[2]);
//          final int k = fields.length-3;
//          final int[] bis = k > 0 ? new int[k] : NormalFormGCI.EMPTY_ARRAY;
//          for (int j = 0; j < k; j++) {
//          bis[j] = Integer.parseInt(fields[j+2]);
//          }
            entries.add(entry);
//          System.err.println(a + "\t" + entry);
        }
    }

    private void loadNF2Rules(LineReader reader) throws IOException {
        String line;
        // NF2 SYNTAX:
        // num '\t' '=>' '\t' num '\t' num
        while (!"--------".equals(line = reader.readLine())) {
            final String[] fields = line.split("\t");
            final int a = Integer.parseInt(fields[0]);
            MonotonicCollection<NF2> entries = ontologyNF2.get(a);
            if (null == entries) {
                entries = new MonotonicCollection<NF2>(2);
                ontologyNF2.put(a, entries);
            }
            final int r = Integer.parseInt(fields[2]);
            final int b = Integer.parseInt(fields[3]);
            NF2 entry = NF2.getInstance(a, r, b);
            entries.add(entry);
//          System.err.println(a + "\t" + entry);
        }
    }

    private void loadNF3Rules(LineReader reader) throws IOException {
        String line;
        // NF3 SYNTAX:
        // num '\t' num '\t' '=>' '\t' num ('\t' num)?
        while (!"--------".equals(line = reader.readLine())) {
            final String[] fields = line.split("\t");
            final int a = Integer.parseInt(fields[0]);
            final int r = Integer.parseInt(fields[1]);
            RoleMap<IConjunctionQueueEntry> entries = ontologyNF3.get(a);
            if (null == entries) {
                entries = new RoleMap<IConjunctionQueueEntry>(factory.getTotalRoles());
                ontologyNF3.put(a, entries);
            }
            final int b = Integer.parseInt(fields[3]);
            final int bi;
            if (fields.length > 4) {
                bi = Integer.parseInt(fields[4]);
            } else {
                bi = TOP;
            }

            final IConjunctionQueueEntry entry = new IConjunctionQueueEntry() {
                public int getB() {
                    return b;
                }
                public int getBi() {
                    return bi;
                }
            };

            entries.put(r, entry);
//          System.err.println(a + "\t" + r + "\t" + entry);
        }
    }

    private void loadNF4Rules(LineReader reader) throws IOException {
        String line;
        // NF4 SYNTAX:
        // num '\t' num
        while (!"--------".equals(line = reader.readLine())) {
            final String[] fields = line.split("\t");
            final int r = Integer.parseInt(fields[0]);
            final int s = Integer.parseInt(fields[1]);
            NF4 nf = new NF4(r, s);
            ontologyNF4.add(nf);
//          System.err.println("NF4: " + r + " [ " + s);
        }
    }

    private void loadNF5Rules(LineReader reader) throws IOException {
        String line;
        // NF5 SYNTAX:
        // num '\t' num '\t' num
        while (!"--------".equals(line = reader.readLine())) {
            final String[] fields = line.split("\t");
            final int r = Integer.parseInt(fields[0]);
            final int s = Integer.parseInt(fields[1]);
            final int t = Integer.parseInt(fields[1]);
            NF5 nf = new NF5(r, s, t);
            ontologyNF5.add(nf);
//          System.err.println("NF5: " + r + " o " + s + " [ " + t);
        }
    }

    private void loadNF6Rules(LineReader reader) throws IOException {
        String line;
        // NF6 SYNTAX:
        // num
        while (!"--------".equals(line = reader.readLine())) {
            final int r = Integer.parseInt(line);
            reflexiveRoles.add(r);
        }
    }

    public IFactory getFactory() {
        return factory;
    }

    public class Classification {
        
        /**
         * Maps a concept to a set of subsumed concepts.
         * <ul><li>map is dense</li><li>sets are sparse</li></ul>
         */
        protected final S subsumptions;

        /**
         * Maps a role to a set of Pairs.
         * Conceptually, a RoleMap&lt;ConceptMap&lt;ConceptSet>>, but implemented as
         * two &lt;ConceptMap&lt;ConceptSet>>[]s to support the common lookup usage patterns.
         * <ul><li>Role map is dense</li><li>Concept maps are dense</li><li>Concept sets are sparse</li></ul>
         */
        protected final R Rr;

        /**
         * Maps a concept to a queue (List) of ConjunctionQueueEntries indicating work to be done
         * <ul><li>map is dense</li><li>queues grow and shrink</li></ul>
         */
        protected final IConceptMap<IQueue<IConjunctionQueueEntry>> conceptQueues;
        
        /**
         * Maps a concept to a queue (List) of RoleQueueEntries indicating work to be done
         * <ul><li>map is dense</li><li>queues grow and shrink</li></ul>
         */
        protected final IConceptMap<IQueue<IRoleQueueEntry>> roleQueues;
        
        /**
         * Queue entries of the form A [ f.(o, v).
         */
        protected final IConceptMap<IQueue<IFeatureQueueEntry>> featureQueues;

        /**
         * Stores the (incrementally computed) transitive closure of NF4
         * <ul><li>map is dense</li><li>set is sparse (wrt SNOMED 20061230)</li></ul>
         */
        final private RoleMap<RoleSet> roleClosureCache;

        Classification() {
            this(
                new S(getFactory().getTotalConcepts()),
                new R(getFactory().getTotalConcepts(), getFactory().getTotalRoles()),
                true
            );
            
            classify();
        }
        
        Classification(final LineReader reader) throws IOException, ParseException {
            this(
                new S(getFactory().getTotalConcepts()),
                new R(getFactory().getTotalConcepts(), getFactory().getTotalRoles()),
                false
                );
            
            loadClassification(reader);
        }
        
        protected Classification(final S subsumptions, final R relationships, final boolean allocateQueues) {
            this.subsumptions = subsumptions;
            this.Rr = relationships;
            
            final int totalConcepts = getFactory().getTotalConcepts();
            final int totalRoles = getFactory().getTotalRoles();

            if (allocateQueues) {
                // Dense (complete) Maps
                roleQueues = new SparseConceptMap<IQueue<IRoleQueueEntry>>(totalConcepts);
                conceptQueues = new SparseConceptMap<IQueue<IConjunctionQueueEntry>>(totalConcepts);
                featureQueues = new SparseConceptMap<IQueue<IFeatureQueueEntry>>(totalConcepts);

                roleClosureCache = new RoleMap<RoleSet>(totalRoles);
            } else {
                roleQueues = null;
                conceptQueues = null;
                featureQueues = null;
                
                roleClosureCache = null;
            }
        }

        protected void classify() {
            if (Snorocket.DEBUGGING) {
                Snorocket.getLogger().info("Classifying " + getFactory().getTotalConcepts() + " concepts, " + getFactory().getTotalRoles() + " roles.");
            }
        
            addBottomRules();
            
            primeQueue();
        
            processOntology();
        
            if (Snorocket.DEBUGGING) {
                checkQueuesEmpty();
            }
        }
        
        // Semantics for bottom...
        private void addBottomRules() {
            final int totalRoles = factory.getTotalRoles();
            for (int role = 0; role < totalRoles ; role++) {
                if (!factory.isBaseRole(role)) {
                    final NF3 bottomRule = NF3.getInstance(role, IFactory.BOTTOM_CONCEPT, IFactory.BOTTOM_CONCEPT);
                    addTerm(bottomRule);
                }
            }
        }

        public IFactory getExtensionFactory() {
            return new DuoFactory(getFactory());
        }
        
        public IConceptMap<IConceptSet> getSubsumptions() {
            return subsumptions.getSet();
        }

        public R getRelationships() {
            return Rr;
        }
        
//        public PostProcessedData getPostProcessedData() {
//            return new PostProcessedData(factory, getSubsumptions());
//        }

        public void printClassification(final PrintWriter writer) {
            NormalisedOntology.this.printClassification(writer);
            
            printS(writer);
            
            printR(writer);
            
        }

        private void printS(final PrintWriter writer) {
            writer.println("S--------");
            int keyCount = subsumptions.keyCount();
            writer.println(keyCount);
            for (final IntIterator itr1 = subsumptions.keyIterator(); itr1.hasNext(); ) {
                final int a = itr1.next();
                keyCount--;
                if (IFactory.TOP_CONCEPT == a) {
                    continue;
                }
                writer.print(a + "\t[");
            
                final IConceptSet sA = subsumptions.get(a);
                for (final IntIterator itr2 = sA.iterator(); itr2.hasNext(); ) {
                    final int b = itr2.next();
                    if (IFactory.TOP_CONCEPT == b && sA.size() > 1) {
                        continue;
                    }
                    writer.print("\t" + b);
                }
            
                writer.println();
            }
            if (keyCount != 0) {
                throw new AssertionError("inconsistency in number of keys");
            }
        }

        private void printR(final PrintWriter writer) {
            writer.println("R--------");
            int keyCount = subsumptions.keyCount();
            writer.println(keyCount);
            for (final IntIterator itr1 = subsumptions.keyIterator(); itr1.hasNext(); ) {
                final int a = itr1.next();
                keyCount--;
                if (IFactory.TOP_CONCEPT == a) {
                    continue;
                }
                StringBuilder sb = new StringBuilder();
            
                for (int r = 0; r < factory.getTotalRoles(); r++) {
                    final IConceptSet bs = Rr.lookupB(a, r);
                    for (final IntIterator itr = bs.iterator(); itr.hasNext(); ) {
                        final int b = itr.next();
                        sb.append("\t").append(r).append(".").append(b);
                    }
                }
            
                writer.println(a + "\t[" + sb);
            }
            if (keyCount != 0) {
                throw new AssertionError("inconsistency in number of keys");
            }
        }


        private void loadClassification(final LineReader reader) throws IOException, ParseException {
            long start = System.currentTimeMillis();

//            FIXME - do we need this any more?
//            // Initialise for roles
//            for (int role = 0; role < factory.getTotalRoles(); role++) {
//                Rr.addRole(role);
//            }


            String line = reader.readLine().trim();   // Read S header
            if (!"S--------".equals(line)) {
                throw new IllegalStateException("Expected S-------- but got " + line);
            }

            double n = loadS(reader);
            long ms = System.currentTimeMillis() - start;
            if (Snorocket.DEBUGGING) System.err.println("S\t" + (ms)/1000.0 + "\t" + ms/n); start = System.currentTimeMillis();

            line = reader.readLine().trim();   // Read R header
            if (!"R--------".equals(line)) {
                throw new IllegalStateException("Expected R-------- but got " + line);
            }

            n = loadR(reader);
            ms = System.currentTimeMillis() - start;
            if (Snorocket.DEBUGGING) System.err.println("R\t" + (ms)/1000.0 + "\t" + ms/n); start = System.currentTimeMillis();
        }

        private int loadR(final LineReader reader) throws IOException, ParseException {
            String line = reader.readLine();
            final int limit = Integer.parseInt(line);
            int n = 1;
            while (n < limit && (line = reader.readLine()) != null) {
                n++;
                // SYNTAX:
                // num '\t' '[' ('\t' num '.' num)*

                int idx = line.indexOf('\t');
                if (idx < 0) {
                    throw new ParseException("No tab character found: " + line, reader);
                }
                final int aKey = Integer.parseInt(line.substring(0, idx));

                int oldIdx = idx + 1;
                idx = line.indexOf('\t', oldIdx);
                while (idx > 0) {
                    oldIdx = idx + 1;       // start index of first num
                    idx = line.indexOf('.', oldIdx);
                    final int rKey = Integer.parseInt(line.substring(oldIdx, idx));
                    oldIdx = idx + 1;       // start index of second num
                    idx = line.indexOf('\t', oldIdx);
                    final int bKey;
                    if (idx > 0) {
                        bKey = Integer.parseInt(line.substring(oldIdx, idx));
                    } else {
                        bKey = Integer.parseInt(line.substring(oldIdx));
                    }
                    
                    Rr.store(aKey, rKey, bKey);
                }
            }

            return n;
        }

        private int loadS(final LineReader reader) throws IOException {
            String line = reader.readLine();
            final int limit = Integer.parseInt(line);
            int n = 1;
            while (n < limit && (line = reader.readLine()) != null && !"R--------".equals(line.trim())) {
                n++;
                // SYNTAX:
                // num '\t' '[' ('\t' num)+

                int idx = line.indexOf('\t');
                final int aKey = Integer.parseInt(line.substring(0, idx));
                final IConceptSet subsumes = subsumptions.get(aKey);

                int oldIdx = idx + 3;
                while ((idx = line.indexOf('\t', oldIdx)) > 0) {
                    final int bKey = Integer.parseInt(line.substring(oldIdx, idx));
                    subsumes.add(bKey);
                    oldIdx = idx + 1;
                }
                final int bKey = Integer.parseInt(line.substring(oldIdx));
                subsumes.add(bKey);
            }
            return n;
        }

        
        
        protected void checkQueuesEmpty() {
            // Check that the queues are indeed empty
            for (IntIterator itr = conceptQueues.keyIterator(); itr.hasNext(); ) {
                final int a = itr.next();
                final IQueue<IConjunctionQueueEntry> queueA = conceptQueues.get(a);
        
                if (!queueA.isEmpty()) {
                    System.err.println("Concept queue for " + factory.lookupConceptId(a) + " is not empty");
                }
            }
            for (IntIterator itr = roleQueues.keyIterator(); itr.hasNext(); ) {
                final int a = itr.next();
                final IQueue<IRoleQueueEntry> queueA = roleQueues.get(a);
        
                if (!queueA.isEmpty()) {
                    System.err.println("Role queue for " + factory.lookupConceptId(a) + " is not empty");
                }
            }
        }
        
        @SuppressWarnings("unused")
		private String formatEntry(IFeatureQueueEntry entry) {
        	Datatype d = entry.getD();
            return factory.lookupFeatureId(d.getFeature()) + ".(" + getOperator(d.getOperator()) + ", " + d.getLiteral() + ")";
        }
        
        private String getOperator(int o) {
        	switch(o) {
	        	case Datatype.OPERATOR_EQUALS:
	        		return "=";
	        	case Datatype.OPERATOR_GREATER_THAN:
	        		return ">";
	        	case Datatype.OPERATOR_GREATER_THAN_EQUALS:
	        		return ">=";
	        	case Datatype.OPERATOR_LESS_THAN:
	        		return "<";
	        	case Datatype.OPERATOR_LESS_THAN_EQUALS:
	        		return "<=";
        		default:
        			throw new RuntimeException("Unknown operator constant "+o);
        	}
        }

        private String formatEntry(IConjunctionQueueEntry entry) {
            return factory.lookupConceptId(entry.getBi()) + " -> " + factory.lookupConceptId(entry.getB());
        }
        
        private String formatEntry(IRoleQueueEntry entry) {
            return factory.lookupRoleId(entry.getR()) + "." + factory.lookupConceptId(entry.getB());
        }
        
        protected void processOntology() {
            boolean done;

            do {
                done = true;
                
                // Process concept queue
                for (IntIterator itr = conceptQueues.keyIterator(); itr.hasNext(); ) {
                    final int a = itr.next();
                    final IQueue<IConjunctionQueueEntry> queueA = conceptQueues.get(a);

                    if (!queueA.isEmpty()) {
                        do {
                            done = false;
                            final IConjunctionQueueEntry entry = queueA.remove();
                            final int b = entry.getB();

                            if (TRACE_LOOP) { System.err.println("  A = " + factory.lookupConceptId(a) + ", X = " + formatEntry(entry)); }   // TRACE

                            final IConceptSet sa = subsumptions.get(a);
                            if (!sa.contains(b)) {
                                final int bi = entry.getBi();
                                if (sa.contains(bi)) {
                                    if (TRACE_LOOP) { System.err.println("    Add " + factory.lookupConceptId(a) + " [ " + factory.lookupConceptId(b)); } // TRACE
                                    subsumptions.put(a, b);
                                    processNewSubsumption(a, b);
                                }
                            }
                        } while (isBatchMode() && !queueA.isEmpty());
                    }
                }
                
                // Process feature queue
                for (IntIterator itr = featureQueues.keyIterator(); 
                		itr.hasNext(); ) {
                    final int a = itr.next();
                    final IQueue<IFeatureQueueEntry> queueA = 
                    		featureQueues.get(a);

                    if (!queueA.isEmpty()) {
                        do {
                            done = false;
                            final IFeatureQueueEntry entry = queueA.remove();
                            Datatype d = entry.getD();
                            
                            // Get right hand sides from NF8 expressions that
                            // match d on their left hand side
                            MonotonicCollection<NF8> entries = 
                            		ontologyNF8.get(d.getFeature());
                            
                            if(entries == null) continue;
                            
                            // Evaluate to determine the ones that match
                            MonotonicCollection<IConjunctionQueueEntry> res = 
                            		new MonotonicCollection<IConjunctionQueueEntry>(2);
                            for(final NF8 e : entries) {
                            	Datatype d2 = e.lhsD;
                            	
                            	// If they match add a conjunction queue entry
                            	// to queueA
                            	if(datatypeMatches(d, d2)) {
                            		res.add(new IConjunctionQueueEntry() {
										@Override
										public int getBi() {
											return Factory.TOP_CONCEPT;
										}
										
										@Override
										public int getB() {
											return e.rhsB;
										}
									});
                            	}
                            }
                            
                            final IQueue<IConjunctionQueueEntry> cqa = conceptQueues.get(a);
                            cqa.addAll(res);
                            
                            //if (TRACE_LOOP) { System.err.println("  (FQ) A = " + factory.lookupConceptId(a) + ", X = " + formatEntry(entry)); }   // TRACE
                            
                            // TODO: finish this
                        } while (isBatchMode() && !queueA.isEmpty());
                    }
                }
                
                // Process role queue
                for (IntIterator itr = roleQueues.keyIterator(); itr.hasNext(); ) {
                    final int a = itr.next();
                    final IQueue<IRoleQueueEntry> queue = roleQueues.get(a);

                    if (!queue.isEmpty()) {
                        done = false;
                        final IRoleQueueEntry entry = queue.remove();

                        if (TRACE_LOOP) { System.err.println("  A = " + factory.lookupConceptId(a) + ", X = " + formatEntry(entry)); }   // TRACE

                        if (!Rr.lookupB(a, entry.getR()).contains(entry.getB())) {
                            process_new_edge(a, entry.getR(), entry.getB());
                        }
                    }
                }

            } while (!done);
        }
        
        /**
         * Evaluates the equivalence of two {@link Datatype}s. This method
         * assumes that the literals both have the same feature and therefore
         * also have matching literal types.
         * 
         * @param d1 Data type from an NF7 entry.
         * @param d2 Data type from an NF8 entry.
         * @return boolean
         */
        private boolean datatypeMatches(Datatype d1, Datatype d2) {
        	assert(d1.getFeature() == d2.getFeature());
        	int lhsOp = d1.getOperator();
        	int rhsOp = d2.getOperator();
        	AbstractLiteral lhsLit = d1.getLiteral();
        	AbstractLiteral rhsLit = d2.getLiteral();
        	if(rhsOp == Datatype.OPERATOR_EQUALS) {
        		// If the rhs operator is =, then the expression will only match
        	    // if the lhs operator is also = and the literal values are the
        		// same.
        		return d1.getLiteral().equals(d2.getLiteral());
        	} else if(rhsOp == Datatype.OPERATOR_GREATER_THAN) {
        		if(lhsOp == Datatype.OPERATOR_LESS_THAN || 
        				lhsOp == Datatype.OPERATOR_LESS_THAN_EQUALS) {
        			return false;
        		} else if(lhsOp == Datatype.OPERATOR_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) > 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_GREATER_THAN) {
        			if(compareLiterals(lhsLit, rhsLit) >= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_GREATER_THAN_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) > 0) {
        				return true;
        			} else {
        				return false;
        			}
        		}
        	} else if(rhsOp == Datatype.OPERATOR_GREATER_THAN_EQUALS) {
        		if(lhsOp == Datatype.OPERATOR_LESS_THAN || 
        				lhsOp == Datatype.OPERATOR_LESS_THAN_EQUALS) {
        			return false;
        		} else if(lhsOp == Datatype.OPERATOR_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) >= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_GREATER_THAN) {
        			if(compareLiterals(lhsLit, rhsLit) >= -1) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_GREATER_THAN_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) >= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		}
        	} else if(rhsOp == Datatype.OPERATOR_LESS_THAN) {
        		if(lhsOp == Datatype.OPERATOR_GREATER_THAN || 
        				lhsOp == Datatype.OPERATOR_GREATER_THAN_EQUALS) {
        			return false;
        		} else if(lhsOp == Datatype.OPERATOR_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) < 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_LESS_THAN) {
        			if(compareLiterals(lhsLit, rhsLit) <= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_LESS_THAN_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) < 0) {
        				return true;
        			} else {
        				return false;
        			}
        		}
        	} else if(rhsOp == Datatype.OPERATOR_LESS_THAN_EQUALS) {
        		if(lhsOp == Datatype.OPERATOR_GREATER_THAN || 
        				lhsOp == Datatype.OPERATOR_GREATER_THAN_EQUALS) {
        			return false;
        		} else if(lhsOp == Datatype.OPERATOR_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) <= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_LESS_THAN) {
        			if(compareLiterals(lhsLit, rhsLit) <= 1) {
        				return true;
        			} else {
        				return false;
        			}
        		} else if(lhsOp == Datatype.OPERATOR_LESS_THAN_EQUALS) {
        			if(compareLiterals(lhsLit, rhsLit) <= 0) {
        				return true;
        			} else {
        				return false;
        			}
        		}
        	} 
        	
        	return d1.getLiteral().equals(d2.getLiteral());
        }
        
        /**
         * Return 0 if both literals are equals. Returns an int > 0 if l1 is
         * greater than l2 and an int < 0 if l1 is less than l2.
         * @param l1
         * @param l2
         * @return
         */
        private int compareLiterals(AbstractLiteral l1, AbstractLiteral l2) {
        	return l1.compareTo(l2);
        }

        private void processNewSubsumption(final int a, final int b) {
        	// Get the set of parent concepts of (b n x) in the ontology
            final MonotonicCollection<IConjunctionQueueEntry> bConceptEntries = ontologyNF1.get(b);
            if (null != bConceptEntries && bConceptEntries.size() > 0) {
                if (TRACE_LOOP) {
                    System.err.println("    Queue(" + factory.lookupConceptId(a) + ") += " + bConceptEntries.size());        // TRACE
                    final Iterator<IConjunctionQueueEntry> itr = bConceptEntries.iterator();
                    while (itr.hasNext()) {
                        System.err.println("\t" + formatEntry(itr.next()));
                    }
                }
                // Add these to the queue of a
                final IQueue<IConjunctionQueueEntry> cQ = getConceptQueue(a);
                cQ.addAll(bConceptEntries);
            }
            final MonotonicCollection<NF2> bRoleEntries = ontologyNF2.get(b);
            if (null != bRoleEntries) {
                if (TRACE_LOOP) System.err.println("    RoleQueue(" + factory.lookupConceptId(a) + ") += " + bRoleEntries.size());        // TRACE
                getRoleQueueEntry(a).addAll(bRoleEntries);
            }
        
            // inlined ontHat(conceptQueues.get(pairA(p)), r, b) in following
            // to move test and fetch outside innermost loop
            //
            final RoleMap<IConjunctionQueueEntry> map = ontologyNF3.get(b);
            if (null != map) {
                final RoleSet keySet = map.keySet();
                for (int r = keySet.first(); r >= 0; r = keySet.next(r + 1)) {
                    final IConjunctionQueueEntry entry = map.get(r);
        
                    if (null != entry) {
                        final IConceptSet aPrimes = Rr.lookupA(a, r);
                        for (final IntIterator itr = aPrimes.iterator(); itr.hasNext(); ) {
                            final int aa = itr.next();
                            getConceptQueue(aa).add(entry);
                        }
                    }
                }
            }
        }

        protected IQueue<IConjunctionQueueEntry> getConceptQueue(final int a) {
            IQueue<IConjunctionQueueEntry> queue = conceptQueues.get(a);
            if (null == queue) {
                queue = newConceptQueue();
                conceptQueues.put(a, queue);
            }
            return queue;
        }

        protected IQueue<IRoleQueueEntry> getRoleQueueEntry(final int a) {
            final IQueue<IRoleQueueEntry> queue;
            if (!roleQueues.containsKey(a)) {
                queue = newRoleQueue();
                roleQueues.put(a, queue);
            } else {
                queue = roleQueues.get(a);
            }
            return queue;
        }
        
        protected IQueue<IFeatureQueueEntry> getFeatureQueueEntry(final int a) {
        	final IQueue<IFeatureQueueEntry> queue;
        	if(!featureQueues.containsKey(a)) {
        		queue = newFeatureQueue();
        		featureQueues.put(a, queue);
        	} else {
        		queue = featureQueues.get(a);
        	}
        	return queue;
        }

        /**
         * Computes the minimal set of QueueEntries from:
         * <li> r.a [ B is in O </li>
         * @param queue 
         * @param r
         * @param b
         * @return
         */
        private void addOntHat(IQueue<IConjunctionQueueEntry> queue, final int r, final IConceptSet sb) {
            for (IntIterator itr = sb.iterator(); itr.hasNext(); ) {
                final int b = itr.next();

                final RoleMap<IConjunctionQueueEntry> map = ontologyNF3.get(b);

                if (TRACE_LOOP) System.err.println("    _+_+ " + factory.lookupConceptId(b) + " " + map);
                
                if (null != map) {
                    final IConjunctionQueueEntry entry = map.get(r);
                    if (null != entry) {
                        queue.add(entry);
                    }
                }
            }
        }

        /**
         * Process new subsumption: a [ role.b
         * 
         * @param a
         * @param role
         * @param b
         */
        private void process_new_edge(int a, int role, int b) {
            final RoleSet roleClosure = getRoleClosure(role);
            for (int s = roleClosure.first(); s >= 0; s = roleClosure.next(s + 1)) {
        
                // R(s) := R(s) u {(A,B)}
                if (TRACE_LOOP) System.err.println("    Add " + factory.lookupConceptId(a) + " [ " + factory.lookupRoleId(s) + "." + factory.lookupConceptId(b));
                Rr.store(a, s, b);
        
                // queue(A) := queue(A) u U{B'|B' in S(B)}.O^(s.B')
                final IConceptSet sb = subsumptions.get(b);
                addOntHat(getConceptQueue(a), s, sb);
        
                // Handle reflexive roles
                if (isReflexive(s)) {
                    // check for (a,a) in R(s)
                    if (!Rr.lookupA(a, s).contains(a)) {
                        process_new_edge(a, s, a);
                    }
                    // check for (b,b) in R(s)
                    if (!Rr.lookupA(b, s).contains(b)) {
                        process_new_edge(b, s, b);
                    }
                }

                final List<int[]> work = new ArrayList<int[]>();
                for (final NF5 nf5: ontologyNF5) {
                    if (s == nf5.getS()) {
                        final int t = nf5.getR();
                        final int u = nf5.getT();
                        final IConceptSet aTPrimes = Rr.lookupA(a, t);
                        final IConceptSet bUPrimes = Rr.lookupA(b, u);
        
                        for (final IntIterator itr = aTPrimes.iterator(); itr.hasNext(); ) {
                            final int aa = itr.next();
        
                            if (!bUPrimes.contains(aa)) {
                                work.add(new int[] {aa, u});
                            }
        
                        }
                    }
                }
                for (final int[] pair: work) {
                    process_new_edge(pair[0], pair[1], b); 
                }
        
                work.clear();
                for (final NF5 nf5: ontologyNF5) {
                    if (s == nf5.getR()) {
                        final int t = nf5.getS();
                        final int u = nf5.getT();
                        final IConceptSet bTPrimes = Rr.getB(b, t);
                        final IConceptSet aUPrimes = Rr.getB(a, u);
        
                        for (final IntIterator itr = bTPrimes.iterator(); itr.hasNext(); ) {
                            final int bb = itr.next();
        
                            if (!aUPrimes.contains(bb)) {
                                work.add(new int[] {u, bb});
                            }
        
                        }
                    }
                }
                for (final int[] pair: work) {
                    process_new_edge(a, pair[0], pair[1]); 
                }
            }
        }

        private boolean isReflexive(final int r) {
            return reflexiveRoles.contains(r);
        }

        public RoleSet getRoleClosure(final int r) {
            RoleSet result = roleClosureCache.get(r);
            if (null == result) {
                result = new RoleSet();
                result.add(r);
                for (final NF4 nf4: ontologyNF4) {
                    if (r == nf4.getR()) {
                        result.addAll(getRoleClosure(nf4.getS()));
                    }
                }
                // We do this after the above recursive call to trigger a stack overflow in
                // case there's a role-inclusion cycle
                roleClosureCache.put(r, result);
            }
            return result;
        }

        protected void initQueues() {
            final int totalConcepts = factory.getTotalConcepts();
            
            for (int concept = 0; concept < totalConcepts; concept++) {
                conceptQueues.put(concept, newConceptQueue());
            }
        }

        protected void primeQueue() {
            if (TRACE_PRIME) System.err.println("PRIMING");  // TRACE

            initQueues();
            
            // inlined ontHatConcept to narrow loop scope to just ontologyNF1 members
            for (IntIterator itr = ontologyNF1.keyIterator(); itr.hasNext(); ) {
                final int a = itr.next();
                final MonotonicCollection<IConjunctionQueueEntry> entries = ontologyNF1.get(a);
        
                if (null != entries) {
                    if (TRACE_PRIME) System.err.println("    " + a + "\t" + entries);    // TRACE
                    getConceptQueue(a).addAll(entries);
                }
            }
            // inlined ontHatRole to narrow loop scope to just ontologyNF2 members
            for (IntIterator itr = ontologyNF2.keyIterator(); itr.hasNext(); ) {
                final int a = itr.next();
                final MonotonicCollection<NF2> entries = ontologyNF2.get(a);
                if (TRACE_PRIME) System.err.println("   *" + a + "\t" + entries);    // TRACE
                getRoleQueueEntry(a).addAll(entries);
            }
            
            for (IntIterator itr = ontologyNF7.keyIterator(); itr.hasNext(); ) {
                final int a = itr.next();
                final MonotonicCollection<NF7> entries = ontologyNF7.get(a);
                if (TRACE_PRIME) System.err.println("   *" + a + "\t" + entries);    // TRACE
                getFeatureQueueEntry(a).addAll(entries);
            }
        }

        protected QueueImpl<IConjunctionQueueEntry> newConceptQueue() {
            return new QueueImpl<IConjunctionQueueEntry>(IConjunctionQueueEntry.class);
        }

        protected QueueImpl<IRoleQueueEntry> newRoleQueue() {
            return new QueueImpl<IRoleQueueEntry>(IRoleQueueEntry.class);
        }
        
        protected QueueImpl<IFeatureQueueEntry> newFeatureQueue() {
        	return new QueueImpl<IFeatureQueueEntry>(IFeatureQueueEntry.class);
        }

    }
    
}
