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

public class SparseArray<T> implements Serializable {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    private static final int M = 16;

    private int _capacity = 0;
    int counter = 0;
    T[][] groups;

    public void Xfinalize() {
        int unused = 0;
        int total = 0;
        for (int i = 0; i < groups.length; i++) {
            total++;
            if (null == groups[i]) {
                unused++;
            } else {
                for (int j = 0; j < groups[i].length; j++) {
                    total++;
                    if (null == groups[i][j]) {
                        unused++;
                    }
                }
            }
        }
        System.err.println("SparseArray: " + total + ", " + unused + ", "
                + 100.0 * unused / total);
    }

    @SuppressWarnings("unchecked")
    SparseArray(final int size) {
        _capacity = size;
        groups = (T[][]) new Object[((_capacity - 1) / M) + 1][];
    }

    T get(int index) {
        if (index == -1) {
            return null;
        } else if ((index / M) < groups.length) {
            final T[] group = groups[index / M];
            return null == group ? null : group[index % M];
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    void set(int index, T value) {
        if ((index / M) >= groups.length) {
            resize(index + 1);
        }
        T[] group = groups[index / M];
        if (null == group) {
            group = groups[index / M] = (T[]) new Object[M];
        }
        if (null == group[index % M])
            counter++;
        group[index % M] = value;
    }

    public void clear() {
        Arrays.fill(groups, null);
    }

    @SuppressWarnings("unchecked")
    private void resize(final int newCapacity) {
        _capacity = newCapacity;
        final T[][] oldGroup = groups;
        final int newSize = ((_capacity - 1) / M) + 1;
        groups = (T[][]) new Object[newSize][];
        System.arraycopy(oldGroup, 0, groups, 0, oldGroup.length);
    }

    public void grow(int newSize) {
        resize(newSize);
    }

}
