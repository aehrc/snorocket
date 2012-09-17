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


public final class FastConceptMap<V> implements IConceptMap<V> {

    private final static int EMPTY = -1;
    private final static Object TOMBSTOMB = null;

    private final static int REPROBE_LIMIT = 10;

    private int[] _keys;
    private Object[] _values;
    private int _size;

    public FastConceptMap(final int size, String label) {
        this();
    }

    public FastConceptMap() {
        clear();
    }

    public void clear() {
        reallocate(2);
    }

    private void reallocate(final int size) {
        _keys = new int[size];
        Arrays.fill(_keys, EMPTY);
        _values = new Object[size];
        Arrays.fill(_values, TOMBSTOMB);
        _size = 0;
    }

    public boolean containsKey(final int key) {
        return null != get(key);
    }

    @SuppressWarnings("unchecked")
	public V get(int key) {
        final int len = _keys.length;
        final int mask = len - 1;
        int reprobe_count = 0;
        int idx = key & mask;
        while (true) {
            final int K = _keys[idx];
            if (EMPTY == K) {
                return null;
            }
            if (key == K) {
                final Object val = _values[idx];
                return TOMBSTOMB == val ? null : (V) val;
            }
            if (++reprobe_count > (REPROBE_LIMIT+(len>>2))) {
                break;
            }
            idx = (idx+1) & mask;
        }
        resize();
        return get(key);
    }

    private void resize() {
        grow(_keys.length * 2);
    }

    public IntIterator keyIterator() {
        return new IntIterator() {

            int next = 0;

            public boolean hasNext() {
                while (next < _keys.length && (EMPTY == _keys[next] || TOMBSTOMB == _values[next])) {
                    next++;
                }
                return next < _keys.length;
            }

            public int next() {
                return hasNext() ? _keys[next++] : EMPTY;
            }

        };
    }

    public void put(int key, V value) {
        final int len = _keys.length;
        final int mask = len - 1;
        int reprobe_count = 0;
        int idx = key & mask;
        while (true) {
            final int K = _keys[idx];
            if (EMPTY == K) {
                _keys[idx] = key;
                _values[idx] = value;
                _size++;
                return;
            }
            if (key == K) {
                if (TOMBSTOMB == _values[idx]) {
                    _size++;
                }
                _values[idx] = value;
                return;
            }
            if (++reprobe_count > (REPROBE_LIMIT+(len>>2))) {
                break;
            }
            idx = (idx+1) & mask;
        }
        resize();
        put(key, value);
    }

    public void remove(int key) {
        final int mask = _keys.length - 1;
        int idx = key & mask;
        while (true) {
            final int K = _keys[idx];
            if (EMPTY == K) {
                return;
            }
            if (key == K) {
                if (TOMBSTOMB != _values[idx]) {
                    _size--;
                    _values[idx] = TOMBSTOMB;
                }
                return;
            }
            idx = (idx+1) & mask;
        }
    }

    public int size() {
        return _size;
    }

    @SuppressWarnings("unchecked")
	public void grow(final int newSize) {
        final int[] oldKeys = _keys;
        final Object[] oldValues = _values;

        reallocate(newSize);

//      System.err.println("resize from " + oldSize + " to " + newSize);
//      new Exception().printStackTrace();

        for (int i = 0; i < oldKeys.length; i++) {
            final int key = oldKeys[i];
            final Object val = oldValues[i];
            if (EMPTY != key && TOMBSTOMB != val) {
                put(key, (V) val);
            }
        }
    }
    
}
