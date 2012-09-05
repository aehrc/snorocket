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

package au.csiro.snorocket.core;

final class SparseConceptSet2 implements IConceptSet {
    final static int M = 48;

    final long[][] groups;

    private static long getBit(int concept) {
        return 1 << (concept & 63);
    }

    private static int getGroup(int concept) {
        return (concept >> 6) / M;
    }

    private static int getIndex(int concept) {
        return (concept >> 6) % M;
    }

    public SparseConceptSet2(int size) {
        groups = new long[(((size - 1) >> 6) / M) + 1][];
        //        System.err.println(groups.length);
    }

    public void add(int concept) {
        //        System.err.println("C " + concept + ", " + groups.length);
        //        System.err.println(getGroup(concept));
        //        System.err.println(getIndex(concept));
        //        System.err.println(getBit(concept));
        long[] group = groups[getGroup(concept)];
        if (null == group) {
            group = groups[getGroup(concept)] = new long[M];
            group[getIndex(concept)] = getBit(concept);
        } else {
            group[getIndex(concept)] |= getBit(concept);
        }
    }

    public void addAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(int concept) {
        long[] group = groups[getGroup(concept)];
        return null != group
            && 0 != (group[getIndex(concept)] & getBit(concept));
    }

    public boolean containsAll(IConceptSet concepts) {
        throw new UnsupportedOperationException();
        //        return false;
    }

    public boolean isEmpty() {
        throw new UnsupportedOperationException();
        //        return false;
    }

    public IntIterator iterator() {
        throw new UnsupportedOperationException();
    }

    public int first() {
        for (int i = 0; i < groups.length; i++) {
            if (null != groups[i]) {
                for (int j = 0; j < M; j++) {
                    long bits = groups[i][j];
                    if (0 != bits) {
                        for (int k = 0; k < 63; k++) {
                            if (0 != (bits & 1)) {
                                return (i * M) + (j * 64) + k;
                            } else {
                                bits >>= 1;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public int next(int fromIndex) {
        for (int i = getGroup(fromIndex); i < groups.length; i++) {
            if (null != groups[i]) {
                for (int j = 0; j < M; j++) {
                    long bits = groups[i][j];
                    if (0 != bits) {
                        for (int k = 0; k < 63; k++) {
                            final int next = (i * M) + (j * 64) + k;
                            if (0 != (bits & 1) & next >= fromIndex) {
                                return next;
                            } else {
                                bits >>= 1;
                            }
                        }
                    }
                }
            }
        }
        return -1;
    }

    public void remove(int concept) {
        throw new UnsupportedOperationException();
    }

    public void removeAll(IConceptSet set) {
        throw new UnsupportedOperationException();
    }

    public int size() {
        throw new UnsupportedOperationException();
    }

    public void grow(int newSize) {
        throw new UnsupportedOperationException();
    }

}
