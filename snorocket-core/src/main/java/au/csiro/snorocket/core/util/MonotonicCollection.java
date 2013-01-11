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

import java.util.Iterator;

import org.apache.log4j.Logger;

/**
 * Hm, not strictly monotonic due to inclusion of clear() method...
 * 
 * @author law223
 * 
 * @param <T>
 */
public final class MonotonicCollection<T> implements IMonotonicCollection<T> {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    // Logger
    private final static Logger log = Logger.getLogger(
            MonotonicCollection.class);
    
    public T[] data;
    int count = 0;

    @SuppressWarnings("unchecked")
    public MonotonicCollection(final int size) {
        data = (T[]) new Object[size];
    }

    public void add(T element) {
        checkSize();
        data[count++] = element;
    }

    @SuppressWarnings("unchecked")
    private void checkSize() {
        if (count == data.length) {
            final int newSize = count < 134217728 ? count << 1
                    : count + 10000000;
            if (log.isTraceEnabled() && count > 1024)
                log.trace(hashCode() + "\t"
                        + getClass().getSimpleName() + " resize to: "
                        + (newSize));
            // For SNOMED 20061230, only a couple of these grow to 2048 entries
            T[] newData = (T[]) new Object[newSize];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;
        }
    }

    public Iterator<T> iterator() {
        return new Iterator<T>() {

            int next = 0;

            public boolean hasNext() {
                return next < count;
            }

            public T next() {
                return hasNext() ? data[next++] : null;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }

        };
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (final Iterator<T> itr = iterator(); itr.hasNext();) {
            final T o = itr.next();
            sb.append(o);
            if (itr.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append("]");
        return sb.toString();
    }

    public void addAll(MonotonicCollection<T> collection) {
        for (T element : collection) {
            add(element);
        }
    }

    public int size() {
        return count;
    }

    public void clear() {
        count = 0;
    }

}
