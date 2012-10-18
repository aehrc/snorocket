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

package au.csiro.snorocket.core.util;

import au.csiro.snorocket.core.ConceptSetFactory;

public interface IConceptSet {
    final static IConceptSet EMPTY_SET = new EmptyConceptSet();

    public static ConceptSetFactory FACTORY = new ConceptSetFactory() {

        public IConceptSet createConceptSet() {
            return new SparseConceptHashSet();
        }

        public IConceptSet createConceptSet(final int size) {
            return new SparseConceptHashSet(size);
        }

        public IConceptSet createConceptSet(final IConceptSet initial) {
            final IConceptSet result;
            if (null == initial) {
                result = createConceptSet();
            } else {
                result = createConceptSet(initial.size());
                result.addAll(initial);
            }
            return result;
        }
    };

    public void add(int concept);

    public void addAll(IConceptSet set);

    public boolean contains(int concept);

    public boolean containsAll(IConceptSet concepts);

    public void remove(int concept);

    public void removeAll(IConceptSet set);

    public boolean isEmpty();

    public int hashCode();

    public boolean equals(Object o);

    public String toString();

    public IntIterator iterator();

    public void clear();

    public int size();

    public void grow(int newSize);

    public int[] toArray();
}

final class EmptyConceptSet implements IConceptSet {

    final static IntIterator EMPTY_ITERATOR = new IntIterator() {

        public boolean hasNext() {
            return false;
        }

        public int next() {
            return -1;
        }

    };

    public void add(int concept) {
        throw new UnsupportedOperationException();
    }

    public void addAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(int concept) {
        return false;
    }

    public boolean containsAll(IConceptSet concepts) {
        return concepts.isEmpty();
    }

    public IntIterator iterator() {
        return EMPTY_ITERATOR;
    }

    public void remove(int concept) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(IConceptSet set) {
        // Empty - nothing to do here
    }

    public boolean isEmpty() {
        return true;
    }

    public int size() {
        return 0;
    }

    public void grow(int increment) {
        throw new UnsupportedOperationException(
                "Cannot grow the EmptyConceptSet!");
    }

    public String toString() {
        return "{}";
    }

    @Override
    public int[] toArray() {
        return new int[0];
    }
}
