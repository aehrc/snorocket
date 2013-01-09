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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import au.csiro.ontology.classification.IProgressMonitor;
import au.csiro.ontology.classification.NullProgressMonitor;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.util.Statistics;
import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.SparseConceptHashSet;
import au.csiro.snorocket.core.util.SparseConceptMap;

//import java.util.logging.Logger;

/**
 * Builds the taxonomy based on the result of the classification process.
 * 
 * @author Alejandro Metke
 * 
 */
public class PostProcessedData<T> {

    // private static final Logger LOGGER = Snorocket.getLogger();

    // Map of concepts to the node in the resulting taxonomy
    private IConceptMap<ClassNode> conceptNodeIndex;
    
    private final IFactory<T> factory;

    public PostProcessedData(IFactory<T> factory) {
        this.factory = factory;
    }

    /**
     * Indicates if cn is a child of cn2.
     * 
     * @param cn
     * @param cn2
     * @return
     */
    private boolean isChild(ClassNode cn, ClassNode cn2) {
        if (cn == cn2)
            return false;

        Queue<ClassNode> toProcess = new LinkedList<>();
        toProcess.addAll(cn.getParents());

        while (!toProcess.isEmpty()) {
            ClassNode tcn = toProcess.poll();
            if (tcn.equals(cn2))
                return true;
            Set<ClassNode> parents = tcn.getParents();
            if (parents != null && !parents.isEmpty())
                toProcess.addAll(parents);
        }

        return false;
    }

    /**
     * Computes an incremental DAG based on the subsumptions for concepts in the
     * new axioms.
     * 
     * @param newConceptSubs
     * @param affectedConceptSubs
     * @param monitor
     */
    public void computeDagIncremental(
            final IConceptMap<IConceptSet> newConceptSubs,
            final IConceptMap<IConceptSet> affectedConceptSubs,
            boolean includeVirtualConcepts,
            IProgressMonitor monitor) {

        // 1. Keep only the subsumptions that involve real atomic concepts
        IConceptMap<IConceptSet> allNew = new SparseConceptMap<IConceptSet>(
                newConceptSubs.size());

        IConceptMap<IConceptSet> allAffected = new SparseConceptMap<IConceptSet>(
                newConceptSubs.size());

        for (IntIterator itr = newConceptSubs.keyIterator(); itr.hasNext();) {
            final int x = itr.next();
            if (!factory.isVirtualConcept(x) || includeVirtualConcepts) {
                IConceptSet set = new SparseConceptHashSet();
                allNew.put(x, set);
                for (IntIterator it = newConceptSubs.get(x).iterator(); it
                        .hasNext();) {
                    int next = it.next();
                    if (!factory.isVirtualConcept(next) || 
                            includeVirtualConcepts) {
                        set.add(next);
                    }
                }
            }
        }

        for (IntIterator itr = affectedConceptSubs.keyIterator(); itr.hasNext();) {
            final int x = itr.next();
            if (!factory.isVirtualConcept(x) || includeVirtualConcepts) {
                IConceptSet set = new SparseConceptHashSet();
                allAffected.put(x, set);
                for (IntIterator it = affectedConceptSubs.get(x).iterator(); it
                        .hasNext();) {
                    int next = it.next();
                    if (!factory.isVirtualConcept(next) || 
                            includeVirtualConcepts) {
                        set.add(next);
                    }
                }
            }
        }

        // 2. Create nodes for new concepts and connect to node hierarchy
        // a. First create the nodes and add to index
        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = new ClassNode();
            cn.getEquivalentConcepts().add(key);
            conceptNodeIndex.put(key, cn);
        }

        // b. Now connect the nodes disregarding redundant connections
        ClassNode bottomNode = conceptNodeIndex.get(IFactory.BOTTOM_CONCEPT);
        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            IConceptSet parents = allNew.get(key);
            for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                // Create a connection to each parent
                int parentId = itr2.next();
                if (parentId == key)
                    continue;
                ClassNode parent = conceptNodeIndex.get(parentId);
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
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            IConceptSet parents = allAffected.get(key);
            for (IntIterator itr2 = parents.iterator(); itr2.hasNext();) {
                // Create a connection to each parent
                int parentId = itr2.next();
                if (parentId == key)
                    continue;
                ClassNode parent = conceptNodeIndex.get(parentId);
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

        ClassNode topNode = conceptNodeIndex.get(IFactory.TOP_CONCEPT);

        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            if (cn.getParents().isEmpty()) {
                cn.getParents().add(topNode);
                topNode.getChildren().add(cn);
            }
        }

        // 4. Fix connections for new and affected concepts
        // a. Check for equivalents
        Set<Pair> pairsToMerge = new HashSet<>();
        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            for (ClassNode parent : cn.getParents()) {
                if (parent.getParents().contains(cn)) {
                    pairsToMerge.add(new Pair(cn, parent));
                }
            }
        }
        for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            for (ClassNode parent : cn.getParents()) {
                if (parent.getParents().contains(cn)) {
                    pairsToMerge.add(new Pair(cn, parent));
                }
            }
        }

        Set<ClassNode> affectedByMerge = new HashSet<>();

        // Merge equivalents
        for (Pair p : pairsToMerge) {
            ClassNode cn1 = p.getA();
            ClassNode cn2 = p.getB();

            affectedByMerge.addAll(cn1.getChildren());
            affectedByMerge.addAll(cn2.getChildren());

            // Merge into cn1 - remove cn2 from index and replace with cn1
            for (IntIterator it = cn2.getEquivalentConcepts().iterator(); it
                    .hasNext();) {
                conceptNodeIndex.put(it.next(), cn1);
            }

            cn1.getEquivalentConcepts().addAll(cn2.getEquivalentConcepts());

            // Remove relationships between merged concepts
            cn1.getParents().remove(cn2);
            cn2.getChildren().remove(cn1);
            cn2.getParents().remove(cn1);
            cn1.getChildren().remove(cn2);

            // Taxonomy is bidirectional
            cn1.getParents().addAll(cn2.getParents());
            for (ClassNode parent : cn2.getParents()) {
                parent.getChildren().remove(cn2);
                parent.getChildren().add(cn1);
            }
            cn1.getChildren().addAll(cn2.getChildren());
            for (ClassNode child : cn2.getChildren()) {
                child.getParents().remove(cn2);
                child.getParents().add(cn1);
            }

            cn2 = null; // nothing should reference cn2 now
        }

        // b. Fix all new and affected nodes
        Set<ClassNode> all = new HashSet<>();
        for (IntIterator it = allNew.keyIterator(); it.hasNext();) {
            all.add(conceptNodeIndex.get(it.next()));
        }

        for (IntIterator it = allAffected.keyIterator(); it.hasNext();) {
            all.add(conceptNodeIndex.get(it.next()));
        }

        for (ClassNode cn : affectedByMerge) {
            all.add(cn);
        }

        // Add also the children of the affected nodes
        Set<ClassNode> childrenToAdd = new HashSet<>();
        for (ClassNode cn : all) {
            for (ClassNode ccn : cn.getChildren()) {
                if (ccn.equals(bottomNode))
                    continue;
                childrenToAdd.add(ccn);
            }
        }
        all.addAll(childrenToAdd);

        // Find redundant relationships
        for (ClassNode cn : all) {
            Set<ClassNode> ps = cn.getParents();

            ClassNode[] parents = ps.toArray(new ClassNode[ps.size()]);
            Set<ClassNode> toRemove = new HashSet<>();
            for (int i = 0; i < parents.length; i++) {
                for (int j = i + 1; j < parents.length; j++) {
                    if (isChild(parents[j], parents[i])) {
                        toRemove.add(parents[i]);
                        continue;
                    }
                    if (isChild(parents[i], parents[j])) {
                        toRemove.add(parents[j]);
                        continue;
                    }
                }
            }
            for (ClassNode tr : toRemove) {
                cn.getParents().remove(tr);
                tr.getChildren().remove(cn);
            }
        }

        // 5. Connect bottom to new and affected concepts with no children
        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            if (cn.getChildren().isEmpty()) {
                cn.getChildren().add(bottomNode);
                bottomNode.getParents().add(cn);
            }
        }
        for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            if (cn.getChildren().isEmpty()) {
                cn.getChildren().add(bottomNode);
                bottomNode.getParents().add(cn);
            }
        }

        // 6. Connect the top node to new and affected concepts with no parents
        for (IntIterator itr = allNew.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            if (cn.getParents().isEmpty()) {
                cn.getParents().add(topNode);
                topNode.getChildren().add(cn);
            }
        }
        for (IntIterator itr = allAffected.keyIterator(); itr.hasNext();) {
            final int key = itr.next();
            ClassNode cn = conceptNodeIndex.get(key);
            if (cn.getParents().isEmpty()) {
                cn.getParents().add(topNode);
                topNode.getChildren().add(cn);
            }
        }
    }

    public void computeDag(
            final IConceptMap<IConceptSet> subsumptions, 
            boolean includeVirtualConcepts,
            IProgressMonitor monitor) {
        long start = System.currentTimeMillis();
        if (monitor == null)
            monitor = new NullProgressMonitor();
        conceptNodeIndex = new DenseConceptMap<>(factory.getTotalConcepts());

        // Keep only the subsumptions that involve real atomic concepts
        IConceptMap<IConceptSet> cis = new SparseConceptMap<IConceptSet>(
                factory.getTotalConcepts());

        for (IntIterator itr = subsumptions.keyIterator(); itr.hasNext();) {
            final int X = itr.next();
            if (!factory.isVirtualConcept(X) || includeVirtualConcepts) {
                IConceptSet set = new SparseConceptHashSet();
                cis.put(X, set);
                for (IntIterator it = subsumptions.get(X).iterator(); it
                        .hasNext();) {
                    int next = it.next();
                    if (!factory.isVirtualConcept(next) || 
                            includeVirtualConcepts) {
                        set.add(next);
                    }
                }
            }
        }

        int totalWork = cis.size();
        int workDone = 0;

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
                }

                workDone++;
                monitor.step(workDone, totalWork);
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

        monitor.taskEnded();
        monitor.taskStarted("Building taxonomy");

        totalWork = (conceptNodeIndex.size() * 3) + equiv.size();
        workDone = 0;

        // Introduce one taxonomy node for each distinct class of equivalent
        // concepts
        ClassNode top = null;
        ClassNode bottom = null;

        for (IntIterator it = equiv.keyIterator(); it.hasNext();) {
            int key = it.next();
            IConceptSet equivs = equiv.get(key);
            // Check if any of the equivalent classes is already part of an
            // equivalent node
            ClassNode n = null;
            for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                int e = it2.next();
                if (conceptNodeIndex.containsKey(e)) {
                    n = conceptNodeIndex.get(e);
                    break;
                }
            }

            if (n == null) {
                n = new ClassNode();
            }
            n.getEquivalentConcepts().add(key);
            n.getEquivalentConcepts().addAll(equivs);
            for (IntIterator it2 = equivs.iterator(); it2.hasNext();) {
                int e = it2.next();
                if (e == CoreFactory.TOP_CONCEPT)
                    top = n;
                if (e == CoreFactory.BOTTOM_CONCEPT)
                    bottom = n;
                conceptNodeIndex.put(e, n);
            }

            totalWork++;
            monitor.step(workDone, totalWork);
        }

        // Connect the nodes according to the direct super-concept relationships
        Set<ClassNode> processed = new HashSet<>();
        for (IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            ClassNode node = (ClassNode) conceptNodeIndex.get(key);
            if (processed.contains(node) || node == top || node == bottom)
                continue;
            processed.add(node);
            for (IntIterator it2 = node.getEquivalentConcepts().iterator(); it2
                    .hasNext();) {
                int c = it2.next();
                // Get direct superconcepts
                IConceptSet dc = direc.get(c);
                if (dc != null) {
                    for (IntIterator it3 = dc.iterator(); it3.hasNext();) {
                        int d = it3.next();
                        ClassNode parent = (ClassNode) conceptNodeIndex.get(d);
                        if (parent != null) {
                            node.getParents().add(parent);
                            parent.getChildren().add(node);
                        }
                    }
                }
            }
            totalWork++;
            monitor.step(workDone, totalWork);
        }
        processed = null;

        // Add bottom
        if (bottom == null) {
            bottom = new ClassNode();
            bottom.getEquivalentConcepts().add(bottomConcept);
            conceptNodeIndex.put(bottomConcept, bottom);
        }

        for (IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            if (key == CoreFactory.BOTTOM_CONCEPT || key == CoreFactory.TOP_CONCEPT)
                continue;
            ClassNode node = (ClassNode) conceptNodeIndex.get(key);
            if (node.getEquivalentConcepts().contains(CoreFactory.BOTTOM_CONCEPT))
                continue;
            if (node.getChildren().isEmpty()) {
                bottom.getParents().add(node);
                node.getChildren().add(bottom);
            }
            totalWork++;
            monitor.step(workDone, totalWork);
        }

        // Add top
        if (top == null) {
            top = new ClassNode();
            top.getEquivalentConcepts().add(topConcept);
            conceptNodeIndex.put(topConcept, top);
        }

        for (IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext();) {
            int key = it.next();
            if (key == CoreFactory.BOTTOM_CONCEPT || key == CoreFactory.TOP_CONCEPT)
                continue;
            ClassNode node = (ClassNode) conceptNodeIndex.get(key);
            if (node.getParents().isEmpty()) {
                node.getParents().add(top);
                top.getChildren().add(node);
            }
            totalWork++;
            monitor.step(workDone, totalWork);
        }

        equiv = null;
        direc = null;

        // TODO: deal with special case where only top and bottom are present.

        monitor.taskEnded();
        Statistics.INSTANCE.setTime("taxonomy construction",
                System.currentTimeMillis() - start);
    }

    public IConceptMap<IConceptSet> getParents(final IFactory<T> factory) {
        IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(
                factory.getTotalConcepts());
        for (IntIterator it = getConceptIterator(); it.hasNext();) {
            int c = it.next();
            ClassNode eq = getEquivalents(c);
            for (ClassNode pn : eq.getParents()) {
                for (IntIterator it2 = pn.getEquivalentConcepts().iterator(); it2
                        .hasNext();) {
                    addToSet(res, c, it2.next());
                }
            }
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

    public ClassNode getEquivalents(int concept) {
        return conceptNodeIndex.get(concept);
    }
    
    public ClassNode getEquivalents(Object concept) {
        // Special cases
        if(concept == Concept.TOP) {
            return conceptNodeIndex.get(0);
        } else if(concept == Concept.BOTTOM) {
            return conceptNodeIndex.get(1);
        } else {
            return conceptNodeIndex.get(factory.getConcept(concept));
        }
    }

    public IntIterator getConceptIterator() {
        return conceptNodeIndex.keyIterator();
    }

    public boolean hasData() {
        return conceptNodeIndex != null;
    }

    class Pair {

        private final ClassNode a;
        private final ClassNode b;

        /**
         * Creates a new pair.
         * 
         * @param a
         * @param b
         */
        public Pair(ClassNode a, ClassNode b) {
            int[] aa = new int[a.getEquivalentConcepts().size()];
            int[] bb = new int[b.getEquivalentConcepts().size()];

            if (aa.length < bb.length) {
                this.a = a;
                this.b = b;
            } else if (aa.length > bb.length) {
                this.a = b;
                this.b = a;
            } else {
                int i = 0;
                for (IntIterator it = a.getEquivalentConcepts().iterator(); it
                        .hasNext();) {
                    aa[i++] = it.next();
                }
                i = 0;
                for (IntIterator it = b.getEquivalentConcepts().iterator(); it
                        .hasNext();) {
                    bb[i++] = it.next();
                }

                int res = 0; // 0 equal, 1 a <, 2 a >

                for (i = 0; i < aa.length; i++) {
                    if (aa[i] < bb[i]) {
                        res = 1;
                        break;
                    } else if (aa[i] > bb[i]) {
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
        public ClassNode getA() {
            return a;
        }

        /**
         * @return the b
         */
        public ClassNode getB() {
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

        private PostProcessedData<T> getOuterType() {
            return PostProcessedData.this;
        }
    }

}
