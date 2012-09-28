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

import java.util.ArrayList;


/**
 * Concepts are integers >= 0
 * 
 * @author law223
 */
public final class SparseConceptHashSet implements IConceptSet {

    private static final int TOMBSTOMB = -2;
    private static final int EMPTY = -1;

    private int[] _items;
    private int _size;

    public SparseConceptHashSet() {
        clear();
    }

    public SparseConceptHashSet(final int initialCapacity) {
        reallocate(initialCapacity);
    }

    private void reallocate(int initialCapacity) {
        if (initialCapacity < 1) {
            initialCapacity = 1;
        }
        // we should maybe check that size is a "good" number (e.g., prime-ish)
        _items = new int[initialCapacity];
        java.util.Arrays.fill(_items, EMPTY);
        _size = 0;
    }

    private int hash1(final int concept) {
        return concept % _items.length;
    }

    private int hash2(final int concept, final int base) {
//      System.err.println(base + " " + items.length);
        return (base + 1) % _items.length;
    }

    public void add(final int concept) {
        // first ensure there's room for an extra concept
        if (_size >= (_items.length)) {
            resize(_items.length * 2);
        }

        int h = hash1(concept);

        // Since there must be room in _items for a new concept
        // (we did a resize above to ensure this), then we must
        // find an empty (or tombstoned) spot or the concept is
        // already there.
        
        while (_items[h] >= 0 && _items[h] != concept) {
            h = hash2(concept, h);
        }

        if (_items[h] < 0) {
            // Note, we can get here if the slot is a TOMBSTONE and we will
            // re-use the slot (unlike Cliff Click's lock-free hashtable),
            // but this may result in the same value being stored more than
            // once.  We compensate for this in remove(), ensuring that all
            // values are deleted.  Another consequence is that size() returns
            // the number of stored values, not the number of unique stored values.
            _items[h] = concept;
            _size++;
        }
        
        assert _items[h] == concept;
    }

    public void addAll(final IConceptSet set) {
        for (final IntIterator itr = set.iterator(); itr.hasNext(); ) {
            add(itr.next());
        }
    }

    public void clear() {
        reallocate(1);
    }

    public boolean contains(final int concept) {
        int h = hash1(concept);

        // Use counter to handle the case when there are no EMPTY slots and the
        // item is not present.
        int counter = _items.length;
        while (_items[h] != EMPTY && _items[h] != concept && counter-- > 0) {
            h = hash2(concept, h);
        }

        return _items[h] == concept;
    }

    public boolean containsAll(IConceptSet concepts) {
        throw new UnsupportedOperationException();
    }

    public void remove(final int concept) {
        int h = hash1(concept);

        // Use counter to handle the case when there are no EMPTY slots and the
        // item is not present.
        int counter = _items.length;
        while (_items[h] != EMPTY && counter-- > 0) {
            if (concept == _items[h]) {
                _items[h] = TOMBSTOMB;
                _size--;
            }
            h = hash2(concept, h);
        }
    }

    public void removeAll(IConceptSet set) {
        if (size() < set.size()) {
            // Can't use iterator since we're going to modify the set
            for (int i = 0; i < _items.length; i++) {
                final int concept = _items[i];
                if (concept >= 0 && set.contains(concept)) {
                    _items[i] = TOMBSTOMB;
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

    public boolean isEmpty() {
        return 0 == _size;
    }

    public IntIterator iterator() {
        // shrink table to reduce iterator cost when suitable
        //
        if (_items.length > 1000 && _size < (_items.length >> 3)) {
//            final double emptySpace = _items.length-_size;
//            System.err.println("OH: " + emptySpace + "\t" + (emptySpace/_items.length));
            resize(_items.length >> 2);
        }
        return new IntIterator() {

            int next = 0;

            public boolean hasNext() {
                while (next < _items.length && _items[next] < 0) {
                    next++;
                }
                return next < _items.length;
            }

            public int next() {
                return hasNext() ? _items[next++] : EMPTY;
            }

        };
    }

    /**
     * The number of values stored in the set's backing store.
     * Note that this may be larger than the number of unique values in the set.
     */
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
        resize(newSize);
    }
    

    private void resize(final int newSize) {
//      System.err.println(hashCode() + " resize from " + items.length);
        final int[] oldItems = _items;
        reallocate(newSize);

        for (int i = 0; i < oldItems.length; i++) {
            final int concept = oldItems[i];
            if (concept >= 0) {
                add(concept);
            }
        }
    }

	@Override
	public int[] toArray() {
		ArrayList<Integer> res = new ArrayList<>();
		for(IntIterator i = iterator(); i.hasNext(); ) {
			res.add(i.next());
		}
		int[] arr = new int[res.size()];
		for(int i = 0; i < res.size(); i++) {
			arr[i] = res.get(i);
		}
		return arr;
 	}

}
