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

import java.io.Serializable;
import java.util.Arrays;

public class FeatureMap<V> implements Serializable {
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final private FeatureSet keySet;
    private V[] members;

    @SuppressWarnings("unchecked")
    public FeatureMap(final int size) {
        keySet = new FeatureSet();
        members = (V[]) new Object[size];
    }

    public boolean containsKey(int key) {
        return keySet.contains(key);
    }

    public V get(int key) {
        if (key >= members.length)
            return null;
        return members[key];
    }

    public FeatureSet keySet() {
        return keySet;
    }

    @SuppressWarnings("unchecked")
    public void put(int key, V value) {
        keySet.add(key);
        final int len = members.length;
        if (key >= len) {
            V[] newMembers = (V[]) new Object[key + 1];
            System.arraycopy(members, 0, newMembers, 0, len);
            members = newMembers;
        }
        members[key] = value;
    }

    public void clear() {
        keySet.clear();
        Arrays.fill(members, null);
    }

}
