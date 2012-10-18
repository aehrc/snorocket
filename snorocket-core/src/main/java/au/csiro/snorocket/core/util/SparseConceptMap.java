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

public final class SparseConceptMap<V> extends AbstractConceptMap<V> {
    final private IConceptSet keySet;
    protected SparseArray<V> members;
    protected String lbl;

    // public ConceptMap() {
    // this(Concept.getTotalConcepts());
    // }

    // public void finalize() {
    // int unused = 0;
    // for (int i = 0; i < members.length; i++) {
    // if (null == members[i]) {
    // unused++;
    // }
    // }
    // System.err.println(lbl + ": " + members.length + ", " + unused + ", " +
    // 100.0*unused/members.length);
    // }

    public int size() {
        return keySet.size();
    }

    public SparseConceptMap(final int size) {
        this(size, null);
    }

    public SparseConceptMap(final int size, String lbl) {
        this.lbl = lbl;
        keySet = IConceptSet.FACTORY.createConceptSet(size);
        members = new SparseArray<V>(size);
        // System.err.println("ConceptMap: " + size);
    }

    public boolean containsKey(int key) {
        return null != members.get(key);
        // return key < members.length && null != members[key];
    }

    public V get(int key) {
        return members.get(key);
    }

    // public ConceptSet keySet() {
    // return keySet;
    // }

    public void put(int key, V value) {
        keySet.add(key);
        members.set(key, value);
    }

    public void remove(int key) {
        keySet.remove(key);
        members.set(key, null);
    }

    public IntIterator keyIterator() {
        return keySet.iterator();
    }

    public void clear() {
        keySet.clear();
        members.clear();
    }

    public void grow(int newSize) {
        keySet.grow(newSize);
        members.grow(newSize);
    }

}
