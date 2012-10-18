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

import java.util.Arrays;
import java.util.BitSet;

import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.ReadonlyConceptSet;
import au.csiro.snorocket.core.util.SparseConceptHashSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

public class R extends R1 {
    R(int concepts, int roles) {
        super(concepts, roles);
    }

    R(int concepts, int roles, R rr) {
        super(concepts, roles, rr);
    }

    /**
     * Record A [ r.B
     * 
     * @param A
     * @param r
     * @param B
     */
    void store(int A, int r, int B) {
        getB(A, r).add(B);
        getA(B, r).add(A);
    }

}

abstract class AR {
    protected int CONCEPTS;
    protected int ROLES;

    AR(final int concepts, final int roles) {
        CONCEPTS = concepts;
        ROLES = roles + 1;
    }
}

/**
 * Implements R, which is of the form RoleMap&lt;ConceptMap&lt;ConceptSet>>
 * where the two maps are complete/total (ie there is an entry for every key),
 * thus it's faster and more space-efficient to just use an array (flattened
 * from 2D to 1D).
 * 
 * @author law223
 * 
 */
abstract class R1 extends AR {

    final private BitSet currentRoles;
    final private IConceptSet[] base;

    private IConceptSet[] data;

    R1(final int concepts, final int roles) {
        this(concepts, roles, null);
    }

    /**
     * The initialState is assumed to be static and the local state is lazily
     * initialised from it as lookup* and get* are called.
     * 
     * @param concepts
     * @param roles
     * @param initialState
     *            used as a base state
     */
    R1(int concepts, int roles, R1 initialState) {
        super(concepts, roles);

        this.data = new IConceptSet[(CONCEPTS * ROLES) << 1];
        this.currentRoles = new BitSet(ROLES);

        // This is relatively cheap to initialise up front (unlike this.data)
        if (null != initialState) {
            this.base = initialState.data;
            currentRoles.or(initialState.currentRoles);
        } else {
            this.base = null;
        }
    }

    /**
     * This should only ever be called when the relationships wrap an initial
     * state and no other methods have been called.
     * 
     * @param relationships
     */
    public void subtract(R1 relationships) {
        if (null == base) {
            throw new AssertionError("");
        }
        for (int i = 0; i < base.length; i++) {
            if (null == base[i]) {
                continue;
            }
            final IConceptSet set = data[i] = new SparseConceptHashSet();

            set.addAll(base[i]);
            if (null != relationships.data[i]) {
                set.removeAll(relationships.data[i]);
            }
        }
    }

    public boolean containsRole(int role) {
        return currentRoles.get(role);
    }

    /**
     * Returns {B | A [ r.B} or {B | (A,B) in R(r)}
     * 
     * @param A
     * @param r
     * @return
     */
    protected IConceptSet getB(int A, int r) {
        if (A >= CONCEPTS) {
            resizeConcepts(A);
        }
        if (r >= ROLES) {
            resizeRoles(r);
        }
        final int index = indexOf(A, r);
        if (null == data[index]) {
            data[index] = new SparseConceptSet();
            addRole(r);

            if (null != base && index < base.length && null != base[index]) {
                data[index].addAll(base[index]);
            }
        }
        return data[index];
    }

    public void addRole(int r) {
        currentRoles.set(r);
    }

    /**
     * 
     * @param B
     * @param r
     * @return Set of concepts A such that A [ r.B
     */
    public IConceptSet lookupA(int B, int r) {
        if (B >= CONCEPTS || r >= ROLES) {
            return IConceptSet.EMPTY_SET;
        }
        final int index = indexOf(B, r) + 1;
        if (null == data[index]) {
            if (null != base && index < base.length && null != base[index]) {
                data[index] = new SparseConceptSet();
                data[index].addAll(base[index]);
                return new ReadonlyConceptSet(data[index]);
            } else {
                return IConceptSet.EMPTY_SET;
            }
        } else {
            return new ReadonlyConceptSet(data[index]);
        }
    }

    /**
     * 
     * @param A
     * @param r
     * @return Set of concepts B such that A [ r.B
     */
    public IConceptSet lookupB(int A, int r) {
        if (A >= CONCEPTS || r >= ROLES) {
            return IConceptSet.EMPTY_SET;
        }
        final int index = indexOf(A, r);
        if (null == data[index]) {
            if (null != base && index < base.length && null != base[index]) {
                data[index] = new SparseConceptSet();
                data[index].addAll(base[index]);
                return new ReadonlyConceptSet(data[index]);
            } else {
                return IConceptSet.EMPTY_SET;
            }
        } else {
            return new ReadonlyConceptSet(data[index]);
        }
    }

    /**
     * Returns {A | A [ r.B} or {A | (A,B) in R(r)}
     * 
     * Lazily initialise data from base as a side-effect of this call.
     * 
     * @param B
     * @param r
     * @return
     */
    protected IConceptSet getA(int B, int r) {
        if (B >= CONCEPTS) {
            resizeConcepts(B);
        }
        if (r >= ROLES) {
            resizeRoles(r);
        }
        // Note the "+1" in the following line:
        final int index = indexOf(B, r) + 1;
        if (null == data[index]) {
            data[index] = new SparseConceptSet();
            addRole(r);

            if (null != base && index < base.length && null != base[index]) {
                data[index].addAll(base[index]);
            }
        }
        return data[index];
    }

    private int indexOf(int concept, int role) {
        if (role >= ROLES) {
            throw new IllegalArgumentException("role " + role
                    + " must be smaller than " + ROLES);
        }
        return ((concept * ROLES) + role) << 1;
    }

    public void clear() {
        Arrays.fill(data, null);
        currentRoles.clear();
    }

    private void resizeConcepts(int maxConcept) {
        final IConceptSet[] oldData = data;

        CONCEPTS = maxConcept + 1;
        data = new IConceptSet[(CONCEPTS * ROLES) << 1];
        System.arraycopy(oldData, 0, data, 0, oldData.length);
    }

    private void resizeRoles(int maxRole) {
        final int OLD_ROLES = ROLES;
        final IConceptSet[] oldData = data;

        ROLES = maxRole + 1;
        data = new IConceptSet[(CONCEPTS * ROLES) << 1];
        for (int c = 0; c < CONCEPTS; c++) {
            for (int r = 0; r < OLD_ROLES; r++) {
                final int newI = ((c * ROLES) + r) << 1;
                final int oldI = ((c * OLD_ROLES) + r) << 1;
                data[newI] = oldData[oldI];
                data[newI + 1] = oldData[oldI + 1];
            }
        }
    }

    public void grow(int newTotalCocepts) {
        resizeConcepts(newTotalCocepts);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();
        boolean separator = false;

        for (int index = 0; index < data.length; index += 2) {
            if (null != data[index]) {
                if (separator) {
                    sb.append(", ");
                }
                final int hash = index >> 1;
                int r = (int) Math.IEEEremainder(hash, ROLES);
                int A = (hash - r) / ROLES;

                sb.append(A).append(" [ ").append(r).append(".")
                        .append(data[index]);

                separator = true;
            }
        }

        return sb.toString();
    }

}
