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

/**
 * 
 * @author law223
 */
final class S {

    final private IConceptMap<IConceptSet> base;
    private IConceptMap<IConceptSet> set;
    private int size = 0;
    private boolean stripped = false;
    
    S(final int capacity) {
        this(capacity, null);
    }
    
    // make copy of pre-computed subsumption state
    // TODO check the time-cost of this -- ~3 seconds (too long)
    // 
    S(final int capacity, final IConceptMap<IConceptSet> subsumptions) {
        base = subsumptions;
        set = new DenseConceptMap<IConceptSet>(capacity);

//        final long start = System.currentTimeMillis();
        
        for (int i = 0; i < capacity; i++) {
            // pre-initialise our state such that each concept subsumes itself and TOP
            if (null == base || !base.containsKey(i)) {
                final SparseConceptSet subsumes = new SparseConceptSet();
                subsumes.add(i);
                subsumes.add(IFactory.TOP_CONCEPT);
                set.put(i, subsumes);
            }
        }
//        System.err.println("Init of S[" + capacity + "] took (ms) " + (System.currentTimeMillis()-start));        // TODO delete
        size = capacity;
    }
    
    IConceptMap<IConceptSet> getSet() {
        if (!stripped && null != base) {
            for (final IntIterator itr = set.keyIterator(); itr.hasNext(); ) {
                final int key = itr.next();
                final IConceptSet old = base.get(key);
                if (null != old) {
                    final IConceptSet delta = new FastConceptHashSet();
                    delta.addAll(set.get(key));
                    delta.removeAll(old);
                    set.put(key, delta);
                }
            }
            stripped = true;
        }
        return set;
    }
    
    /* (non-Javadoc)
     * @see au.csiro.snorocket.Subsumptions#get(int)
     */
    IConceptSet get(int concept) {
        IConceptSet subsumes = set.get(concept);
        
        if (null == subsumes) {
            if (null != base && base.containsKey(concept)) {
                subsumes = base.get(concept);
            } else {
                // A Concept always subsumes itself and TOP
                //
                subsumes = new SparseConceptSet();
                subsumes.add(concept);
                subsumes.add(IFactory.TOP_CONCEPT);
                set.put(concept, subsumes);
                size++;
            }
        }
        
        return subsumes;
    }
    
    /* (non-Javadoc)
     * @see au.csiro.snorocket.Subsumptions#containsKey(int)
     */
    boolean containsKey(int concept) {
        return set.containsKey(concept) || (null != base && base.containsKey(concept));
    }
    
    /**
     * Returns an iterator over the keys of this map.
     * The keys are returned in no particular order.
     * 
     * @return an iterator over the keys of this map.
     */
    IntIterator keyIterator() {
        if (null == base) {
            return set.keyIterator();
        } else {
            return new IntIterator() {

                final IntIterator baseItr = base.keyIterator();
                final IntIterator setitr = set.keyIterator();

                public boolean hasNext() {
                    return baseItr.hasNext() || setitr.hasNext();
                }

                public int next() {
                    return baseItr.hasNext() ? baseItr.next() : setitr.next();
                }

            };
        }
    }
    
    int keyCount() {
        return size;
    }

    void put(int child, int parent) {
//        System.err.println(child + " [ " + parent);
        IConceptSet subsumes = set.get(child);
        
        if (null == subsumes) {
            subsumes = new SparseConceptSet();
            set.put(child, subsumes);
            size++;
            if (null != base && base.containsKey(child)) {
                subsumes.addAll(base.get(child));
            } else {
                // A Concept always subsumes itself and TOP
                //
                subsumes.add(child);
                subsumes.add(IFactory.TOP_CONCEPT);
            }
        }

        subsumes.add(parent);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (IntIterator itr = keyIterator(); itr.hasNext(); ) {
            int key = itr.next();
            sb.append(key);
            sb.append(": ");
            sb.append(get(key));
            if (itr.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    /* (non-Javadoc)
     * @see au.csiro.snorocket.Subsumptions#grow(int)
     */
    void grow(int newSize) {
        set.grow(newSize);
    }
    
}
