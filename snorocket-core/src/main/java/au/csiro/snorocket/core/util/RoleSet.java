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
 * Stores a set of Role indexes in the range 0..127 using the bits of two longs.
 * 
 * Originally this was a hand-rolled implementation in an attempt to deal with
 * excess memory usage problems, but it turns out that this class was not a
 * significant contributor to the problem.
 * 
 * @author law223
 */
final public class RoleSet extends java.util.BitSet {
    /**
	 * 
	 */
    private static final long serialVersionUID = 3307286235156743593L;

    public RoleSet() {
    }

    public RoleSet(final RoleSet initial) {
        or(initial);
    }

    public void add(int role) {
        set(role);
    }

    public void addAll(RoleSet set) {
        or(set);
    }

    public boolean contains(int role) {
        return get(role);
    }

    public int first() {
        return nextSetBit(0);
    }

    public int next(int role) {
        return nextSetBit(role);
    }
}
