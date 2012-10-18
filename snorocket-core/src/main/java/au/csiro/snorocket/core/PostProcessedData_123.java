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

import java.util.logging.Logger;

import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

/**
 * Class to compute DAG, equivalents, and other post-classification information
 * 
 * @author law223
 * 
 */
public class PostProcessedData_123 {

    private static final Logger LOGGER = Snorocket.getLogger();

    // Input state
    final private IConceptMap<IConceptSet> subsumptions;
    final private IFactory_123 factory;

    // Computed state
    final private IConceptSet classified;
    final private IConceptMap<IConceptSet> parents;
    final private IConceptMap<IConceptSet> equivalents;
    final private IConceptSet redundant;
    final private IConceptSet problems;

    private ConceptSetFactory FACTORY = new ConceptSetFactory() {

        public IConceptSet createConceptSet() {
            return createConceptSet(1);
        }

        public IConceptSet createConceptSet(int size) {
            return IConceptSet.FACTORY.createConceptSet(size);
        }

        public IConceptSet createConceptSet(IConceptSet initial) {
            return IConceptSet.FACTORY.createConceptSet(initial);
        }

    };

    public PostProcessedData_123(final IFactory_123 factory2,
            final IConceptMap<IConceptSet> subsumptions) {
        this.factory = factory2;
        this.subsumptions = subsumptions;

        classified = IConceptSet.FACTORY.createConceptSet(factory2
                .getTotalConcepts());
        parents = IConceptMap.FACTORY.createDenseConceptMap(factory2
                .getTotalConcepts());
        // children = new
        // DenseConceptMap<ConceptSet>(factory.getTotalConcepts());
        equivalents = IConceptMap.FACTORY.createSparseConceptMap(factory2
                .getTotalConcepts());
        redundant = IConceptSet.FACTORY.createConceptSet();
        problems = IConceptSet.FACTORY.createConceptSet(20000); // FIXME size
                                                                // estimate?!?

        computeDag();
    }

    public PostProcessedData_123(final IFactory_123 factory,
            final IConceptMap<IConceptSet> baseSubsumptions,
            final IConceptMap<IConceptSet> deltaSubsumptions) {
        this.factory = factory;
        this.subsumptions = deltaSubsumptions;

        classified = IConceptSet.FACTORY.createConceptSet(factory
                .getTotalConcepts());
        parents = IConceptMap.FACTORY.createDenseConceptMap(factory
                .getTotalConcepts());
        // children = new
        // DenseConceptMap<ConceptSet>(factory.getTotalConcepts());
        equivalents = IConceptMap.FACTORY.createSparseConceptMap(factory
                .getTotalConcepts());
        redundant = IConceptSet.FACTORY.createConceptSet();
        problems = IConceptSet.FACTORY.createConceptSet(20000); // FIXME size
                                                                // estimate?!?

        computeDeltaDag(baseSubsumptions);
    }

    public IConceptMap<IConceptSet> getParents() {
        return parents;
    }

    public IConceptMap<IConceptSet> getEquivalents() {
        return equivalents;
    }

    public IConceptSet getProblems() {
        return problems;
    }

    /**
     * 
     * @param baseSubsumptions
     */
    private void computeDeltaDag(final IConceptMap<IConceptSet> baseSubsumptions) {
        // FIXME - Currently assumes no equivalents (cycles)

        for (final IntIterator dItr = subsumptions.keyIterator(); dItr
                .hasNext();) {
            final int key = dItr.next();

            // D'[k] = D[k] \ k
            final IConceptSet currentParents = FACTORY
                    .createConceptSet(subsumptions.get(key));
            currentParents.remove(key);

            // foreach v: S[K], v != k
            // D'[k].removeAll(D[v]\v)
            final IConceptSet baseAncestors = baseSubsumptions.get(key);
            if (null != baseAncestors) {
                for (final IntIterator bItr = baseAncestors.iterator(); bItr
                        .hasNext();) {
                    final int val = bItr.next();
                    if (val != key) {
                        boolean flag = currentParents.contains(val);
                        if (subsumptions.containsKey(val)) {
                            currentParents.removeAll(subsumptions.get(val));
                        }
                        if (flag) {
                            currentParents.add(val);
                        }
                    }
                }
            }

            // foreach v: D[K], v != k
            // D'[k].removeAll(S[v]\v)
            // D'[k].removeAll(D[v]\v)
            for (final IntIterator bItr = subsumptions.get(key).iterator(); bItr
                    .hasNext();) {
                final int val = bItr.next();
                if (val != key) {
                    boolean flag = currentParents.contains(val);
                    if (baseSubsumptions.containsKey(val)) {
                        currentParents.removeAll(baseSubsumptions.get(val));
                    }
                    if (subsumptions.containsKey(val)) {
                        currentParents.removeAll(subsumptions.get(val));
                    }
                    if (flag) {
                        currentParents.add(val);
                    }
                }
            }

            parents.put(key, currentParents);
        }

    }

    private void computeDag() {
        long start = System.currentTimeMillis();

        for (IntIterator itr = subsumptions.keyIterator(); itr.hasNext();) {
            final int X = itr.next();
            // if conceptsOnly, then skip all Concepts named "* ..." since they
            // are an artifact of the normalisation process.
            //
            if (!factory.isVirtualConcept(X)) {
                parents.put(X, FACTORY.createConceptSet());
                // children.put(X, FACTORY.createConceptSet());
                equivalents.put(X, FACTORY.createConceptSet());
            }
        }

        start = System.currentTimeMillis();
        for (IntIterator itr = subsumptions.keyIterator(); itr.hasNext();) {
            final int A = itr.next();
            if (!factory.isVirtualConcept(A) && !classified.contains(A)) {
                dagClassify(A);
            }
        }
        if (Snorocket.DEBUGGING)
            LOGGER.info("Compute DAG time (s):\t"
                    + (System.currentTimeMillis() - start) / 1000.0);

        stripEquivalents();

    }

    private void stripEquivalents() {
        for (final IntIterator keyItr = parents.keyIterator(); keyItr.hasNext();) {
            final int key = keyItr.next();
            if (redundant.contains(key)) {
                parents.get(key).clear();
            } else {
                parents.get(key).removeAll(redundant);
            }
        }
    }

    private void dagClassify(int A) {
        // This is really doing a self-join on S1.parent and S2.child
        // (A = S1.child, B = S1.parent = S2.child, check for A == S2.parent &&
        // A != B)
        //
        final IConceptSet sA = subsumptions.get(A);

        final IConceptSet candidates = FACTORY.createConceptSet(sA.size()); // factory.getTotalConcepts());
        for (IntIterator itr = sA.iterator(); itr.hasNext();) {
            final int B = itr.next();
            if (A != B && !factory.isVirtualConcept(B)) {
                if (subsumptions.get(B).contains(A)) {
                    classified.add(B);
                    equivalents.get(A).add(B);
                    redundant.add(B);
                } else {
                    if (!classified.contains(B)) {
                        dagClassify(B);
                    }
                    candidates.add(B);
                }
            }
        }
        dagInsert(A, candidates);

        classified.add(A);
    }

    private void dagInsert(final int A, final IConceptSet candidates) {
        final IConceptSet marked = FACTORY
                .createConceptSet(candidates.size() * 3); // factory.getTotalConcepts());
        for (IntIterator itr = candidates.iterator(); itr.hasNext();) {
            final int B = itr.next();
            marked.addAll(parents.get(B));
        }

        IConceptSet work = FACTORY.createConceptSet();
        IConceptSet newWork = FACTORY.createConceptSet();
        work.addAll(marked);
        do {
            // accumulate the set of parents of concepts in the work set
            for (IntIterator itr = work.iterator(); itr.hasNext();) {
                final int X = itr.next();
                newWork.addAll(parents.get(X));
            }
            // exclude from the new work set those we've processed previously
            newWork.removeAll(marked);
            // record the new work set as being processed
            marked.addAll(newWork);
            // swap work and newWork
            final IConceptSet tmp = work;
            work = newWork;
            newWork = tmp;
            newWork.clear();
        } while (!work.isEmpty());

        candidates.removeAll(marked);
        parents.put(A, candidates);
        if (candidates.contains(IFactory_123.BOTTOM_CONCEPT)) {
            problems.add(A);
        }
        // for (IntIterator itr = candidates.iterator(); itr.hasNext(); ) {
        // final int B = itr.next();
        // children.get(B).add(A);
        // }
    }

}
