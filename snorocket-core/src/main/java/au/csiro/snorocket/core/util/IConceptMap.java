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

public interface IConceptMap<V> {
    public boolean containsKey(int key);

    public V get(int key);

    public IntIterator keyIterator();

    public void put(int key, V value);

    public void remove(int key);

    public void clear();

    public int size();

    public void grow(int newSize);

    interface IConceptMapFactory {
        IConceptMap<IConceptSet> createSparseConceptMap(int size);

        IConceptMap<IConceptSet> createDenseConceptMap(int size);
    }

    public static IConceptMapFactory FACTORY = new IConceptMapFactory() {

        public IConceptMap<IConceptSet> createSparseConceptMap(final int size) {
            return new SparseConceptMap<IConceptSet>(size);
        }

        public IConceptMap<IConceptSet> createDenseConceptMap(final int size) {
            return new DenseConceptMap<IConceptSet>(size);
        }
    };

}

final class ReadonlyConceptMap<V> implements IConceptMap<V> {

    final private IConceptMap<V> map;

    public ReadonlyConceptMap(final IConceptMap<V> map) {
        this.map = map;
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean containsKey(int key) {
        return map.containsKey(key);
    }

    public V get(int key) {
        return map.get(key);
    }

    public void grow(int newSize) {
        throw new UnsupportedOperationException();
    }

    public IntIterator keyIterator() {
        return map.keyIterator();
    }

    public void put(int key, V value) {
        throw new UnsupportedOperationException();
    }

    public void remove(int key) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        return map.size();
    }

}
