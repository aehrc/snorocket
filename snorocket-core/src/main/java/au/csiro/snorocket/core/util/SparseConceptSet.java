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
 * Implementation of the IConceptSet API that does not support clear() or 
 * remove()/removeAll(). Set entries are stored in sorted order to allow for 
 * O(log n) lookup time. Inserts require copying all larger values to make 
 * remove for the inserted value. Worst-case insert performance happens when 
 * elements are added largest to smallest. 
 * 
 * @author law223
 *
 */
final public class SparseConceptSet implements IConceptSet {
    private int[] items;
    private int size;

    /**
     * 
     * @param capacity initial size of underlying array
     */
    public SparseConceptSet(final int capacity) {
        items = new int[capacity];
        size = 0;
        items[size] = -1;
    }

    public SparseConceptSet() {
        this(10);
    }

    public synchronized void add(final int concept) {
        int low = 0;
        int high = size;
        while ((high - low) > 16) {
            // binary search
            int probe = (low + high) >> 1; // don't need to worry about overflow
            if (items[probe] > concept) {
                low = probe + 1;
            } else if (items[probe] < concept) {
                high = probe - 1;
            } else {
                // concept found - nothing to do
                return;
            }
        }

        // fall through to linear search
        while (concept < items[low]) {
            low++;
        }

        if (concept > items[low]) {
            size++;
            if (items.length == size) {
                final int increment = 10;
                final int[] newItems = new int[size + increment];
                System.arraycopy(items, 0, newItems, 0, low);
                System.arraycopy(items, low, newItems, low + 1, size - low);
                items = newItems;
                //                System.err.println(size + ", " + i + ", " + items.length + ", " + (size-i));
            } else {
                System.arraycopy(items, low, items, low + 1, size - low);
            }
            items[low] = concept;
        } // else items[i] == concept, so do nothing
        else if (concept != items[low]) {
            throw new AssertionError(
                "Internal error detected, expecting items[i] == concept where i = "
                    + low + ", items[i] = " + items[low] + ", and concept = "
                    + concept);
        }
    }

    public void addAll(final IConceptSet set) {
        for (final IntIterator itr = set.iterator(); itr.hasNext();) {
            add(itr.next());
        }
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(final int concept) {
        int low = 0;
        int high = size;
        while ((high - low) > 16) {
            // binary search
            int probe = (low + high) >> 1; // don't need to worry about overflow
            if (items[probe] > concept) {
                low = probe + 1;
            } else if (items[probe] < concept) {
                high = probe - 1;
            } else {
                return true;
            }
        }

        // fall through to linear search
        while (low < items.length && concept < items[low]) {
            low++;
        }

        return low < items.length && concept == items[low];
    }

    public boolean containsAll(IConceptSet concepts) {
        if (concepts.isEmpty()) {
            return true;
        }
        final IConceptSet s = IConceptSet.FACTORY.createConceptSet(concepts);
        s.removeAll(this);
        return s.isEmpty();
        //        return false;
    }

    public boolean isEmpty() {
        return 0 == size();
    }

    public synchronized IntIterator iterator() {
    	return new SparseConceptSetIntIterator(items, size);
    }

    public void remove(int concept) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        for (int i = 0; i < size; i++) {
            sb.append(items[i]);
            if (i + 1 < size) {
                sb.append(", ");
            }
        }
        sb.append("}");
        return sb.toString();
        //        return size + ":" +Arrays.toString(items);
    }

    public int size() {
        return size;
    }

    public synchronized void grow(int newSize) {
    	assert newSize >= size;
    	
        final int[] newItems = new int[newSize];
        System.arraycopy(items, 0, newItems, 0, size);
        items = newItems;
    }

	@Override
	public int[] toArray() {
		int[] res = new int[size];
		for(int i = 0; i < size; i++) {
			res[i] = items[i];
		}
		
		return res;
	}

}
