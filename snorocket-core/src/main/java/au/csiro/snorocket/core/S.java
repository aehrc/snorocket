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

import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * 
 * @author law223
 */
final class S {

    private IConceptMap<IConceptSet> set;
    private int size = 0;
    
    S(final int capacity) {
    	set = new DenseConceptMap<IConceptSet>(capacity);
      for (int i = 0; i < capacity; i++) {
    	  final SparseConceptSet subsumes = new SparseConceptSet();
          subsumes.add(i);
          subsumes.add(IFactory.TOP_CONCEPT);
          set.put(i, subsumes);
      }
      size = capacity;
    }
    
    IConceptMap<IConceptSet> getSet() {
        return set;
    }
    
    /* (non-Javadoc)
     * @see au.csiro.snorocket.Subsumptions#get(int)
     */
    IConceptSet get(int concept) {
        IConceptSet subsumes = set.get(concept);
        
        if (null == subsumes) {
        	// A Concept always subsumes itself and TOP
            subsumes = new SparseConceptSet();
            subsumes.add(concept);
            subsumes.add(IFactory.TOP_CONCEPT);
            set.put(concept, subsumes);
            size++;
        }
        
        return subsumes;
    }
    
    /* (non-Javadoc)
     * @see au.csiro.snorocket.Subsumptions#containsKey(int)
     */
    boolean containsKey(int concept) {
        return set.containsKey(concept);
    }
    
    /**
     * Returns an iterator over the keys of this map.
     * The keys are returned in no particular order.
     * 
     * @return an iterator over the keys of this map.
     */
    IntIterator keyIterator() {
    	return new IntIterator() {

            final IntIterator setitr = set.keyIterator();

            public boolean hasNext() {
                return setitr.hasNext();
            }

            public int next() {
                return setitr.next();
            }

        };
    }
    
    int keyCount() {
        return size;
    }

    void put(int child, int parent) {
        IConceptSet subsumes = set.get(child);
        
        if (null == subsumes) {
            subsumes = new SparseConceptSet();
            set.put(child, subsumes);
            size++;
            // A Concept always subsumes itself and TOP
            subsumes.add(child);
            subsumes.add(IFactory.TOP_CONCEPT);
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
