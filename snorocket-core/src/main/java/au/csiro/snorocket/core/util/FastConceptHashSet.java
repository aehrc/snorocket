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


/**
 * Concepts are integers >= 0
 * 
 * @author law223
 */
public final class FastConceptHashSet implements IConceptSet {

    private static final int TOMBSTOMB = -2;
    private static final int EMPTY = -1;

    private final static int REPROBE_LIMIT = 10;

    private int[] _keys;
    private int _size;

    public FastConceptHashSet() {
        clear();
    }

    /**
     * @param size must be a power of 2
     */
    private void reallocate(final int size) {
        _keys = new int[size];
        java.util.Arrays.fill(_keys, EMPTY);
        _size = 0;
    }

    public void add(final int concept) {
        final int len = _keys.length;
        final int mask = len - 1;
        int reprobe_count = 0;
        int idx = concept & mask;
        while (true) {
            final int K = _keys[idx];
            if (K < 0) {	// K == EMPTY || K == TOMBSTONE
                _keys[idx] = concept;
                _size++;
                return;
            }
            if (concept == K) {
                return;
            }
            if (++reprobe_count > reprobeLimit(len)) {
                break;
            }
            idx = (idx+1) & mask;
        }
        resize();
        add(concept);
    }

    private void resize() {
        grow(_keys.length * 2);
//      System.err.println(hashCode() + " resize from " + items.length);
    }

    public void addAll(IConceptSet set) {
        for (final IntIterator itr = set.iterator(); itr.hasNext(); ) {
            add(itr.next());
        }
    }

    public void clear() {
        reallocate(1);
    }

    public boolean contains(int concept) {
        final int len = _keys.length;
        final int mask = len - 1;
        int reprobe_count = 0;
        int idx = concept & mask;
        while (true) {
            final int K = _keys[idx];
            if (K == EMPTY) {
                return false;
            }
            if (concept == K) {
                return true;
            }
            if (++reprobe_count > reprobeLimit(len)) {
                break;
            }
            idx = (idx+1) & mask;
        }
        return false;
    }

    private int reprobeLimit(final int len) {
        return REPROBE_LIMIT + (len>>2);
    }

    public boolean containsAll(IConceptSet concepts) {
        throw new UnsupportedOperationException();
    }

    public boolean isEmpty() {
        return 0 == _size;
    }

    public IntIterator iterator() {
        if (_size < (_keys.length >> 1)) {
//            System.err.println("Shrinking from " + _keys.length);
            final int[] oldItems = _keys;
            reallocate(_keys.length >> 1);

            for (int i = 0; i < oldItems.length; i++) {
                final int concept = oldItems[i];
                if (concept >= 0) {	// concept != EMPTY && concept != TOMBSTONE
                    add(concept);
                }
            }
        }
        return new IntIterator() {

            int next = 0;

            public boolean hasNext() {
                while (next < _keys.length && _keys[next] < 0) {
                    next++;
                }
                return next < _keys.length;
            }

            public int next() {
                return hasNext() ? _keys[next++] : EMPTY;
            }

        };
    }

    public void remove(final int concept) {
        final int len = _keys.length;
        final int mask = _keys.length - 1;
        int reprobe_count = 0;
        int idx = concept & mask;
        while (true) {
            final int K = _keys[idx];
            if (EMPTY == K) {
                return;
            }
            if (concept == K) {
                _size--;
                _keys[idx] = TOMBSTOMB;
            }
            if (++reprobe_count > reprobeLimit(len)) {
                break;
            }
            idx = (idx+1) & mask;
        }
    }

    public void removeAll(IConceptSet set) {
        if (size() < set.size()) {
            // Can't use iterator since we're going to modify the set
            for (int i = 0; i < _keys.length; i++) {
                final int concept = _keys[i];
                if (concept >= 0 && set.contains(concept)) {
                    _keys[i] = TOMBSTOMB;
                    _size--;
                }
            }
        } else {
            for (final IntIterator itr = set.iterator(); itr.hasNext(); ) {
                final int concept = itr.next();
                remove(concept);
            }
        }
    }

    public int size() {
        return _size;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (final IntIterator itr = iterator(); itr.hasNext(); ) {
            final int c = itr.next();
            sb.append(c);
            if (itr.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
    }

    public void grow(int newSize) {
        final int[] oldItems = _keys;
        reallocate(newSize);

        for (int i = 0; i < oldItems.length; i++) {
            final int concept = oldItems[i];
            if (concept >= 0) { // concept != EMPTY && concept != TOMBSTONE
                add(concept);
            }
        }
    }

	@Override
	public int[] toArray() {
		throw new UnsupportedOperationException();
	}
}
