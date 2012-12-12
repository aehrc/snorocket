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

import java.util.Arrays;

/**
 * Use when all or most keys are going to be mapped to something.
 * 
 * @author law223
 * 
 * @param <V>
 */
public final class DenseConceptMap<V> extends AbstractConceptMap<V> {
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    private V[] members;

    /**
     * 
     * @param capacity
     *            estimated maximum key value + 1
     */
    @SuppressWarnings("unchecked")
    public DenseConceptMap(final int capacity) {
        members = (V[]) new Object[capacity];
    }

    public boolean containsKey(int key) {
        return key < members.length && null != members[key];
    }

    public V get(int key) {
        return key < members.length ? members[key] : null;
    }

    @SuppressWarnings("unchecked")
    public void put(int key, V value) {
        final int len = members.length;
        if (key >= len) {
            V[] newMembers = (V[]) new Object[key + 1];
            System.arraycopy(members, 0, newMembers, 0, len);
            members = newMembers;
            // System.err.println(getClass().getSimpleName() + " resize: " +
            // members.length /* + ", " + keySet.size() */);
        }
        members[key] = value;
    }

    public void remove(int key) {
        if (key < members.length) {
            members[key] = null;
        }
    }

    public IntIterator keyIterator() {
        return new IntIterator() {

            int next = 0;

            public boolean hasNext() {
                while (next < members.length && null == members[next]) {
                    next++;
                }
                return next < members.length;
            }

            public int next() {
                return hasNext() ? next++ : -1;
            }

        };
    }

    public void clear() {
        Arrays.fill(members, null);
    }

    public int size() {
        // throw new UnsupportedOperationException();
        return members.length;
    }

    @SuppressWarnings("unchecked")
    public void grow(int newSize) {
        V[] newMembers = (V[]) new Object[newSize];
        System.arraycopy(members, 0, newMembers, 0, members.length);
        members = newMembers;
    }

    @Override
    public String toString() {
        boolean separator = false;
        final StringBuilder sb = new StringBuilder();
        for (final IntIterator itr = keyIterator(); itr.hasNext();) {
            if (separator) {
                sb.append(", ");
            }
            int key = itr.next();
            sb.append(key);
            sb.append(" [ ");
            sb.append(get(key));

            separator = true;
        }
        return sb.toString();
    }
}
