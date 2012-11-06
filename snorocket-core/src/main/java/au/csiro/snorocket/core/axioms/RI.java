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

package au.csiro.snorocket.core.axioms;

import java.util.Arrays;

import au.csiro.snorocket.core.IFactory;

/**
 * 
 * @param lhs
 *            lhs.length == 0 -> reflexive; == 1 -> role subtyping; >= 2 -> role
 *            composition
 * 
 * @param rhs
 */
public class RI<T> extends Inclusion<T> {

    private static final int PRIME = 31;

    final private int[] lhs;
    final private int rhs;
    final private int hashCode;

    public RI(final int[] lhs, final int rhs) {
        assert null != lhs;
        assert -1 < rhs;
        this.lhs = lhs;
        this.rhs = rhs;
        hashCode = PRIME * (PRIME + Arrays.hashCode(this.lhs)) + this.rhs;
    }

    public RI(int lhs, int rhs) {
        this(new int[] { lhs }, rhs);
    }

    public int[] getLhs() {
        return lhs;
    }

    public int getRhs() {
        return rhs;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Inclusion<T>[] normalise1(final IFactory<T> factory) {
        Inclusion[] result = { null, null };

        if (rule1(factory, result)) {

        } else {
            result = null;
        }

        return result;
    }

    @Override
    public Inclusion<T>[] normalise2(final IFactory<T> factory) {
        return null;
    }

    /**
     * r<sub>1</sub> &#8728; &#133; &#8728; r<sub>k</sub> &#8849; s &rarr;
     * {r<sub>1</sub> &#8728; &#133; &#8728; r<sub>k-1</sub> &#8849; u, u
     * &#8728; r<sub>k</sub> &#8849; s}
     * 
     * @param gcis
     * @return
     */
    boolean rule1(final IFactory<T> factory, final Inclusion<T>[] gcis) {
        boolean result = false;

        // TODO: make this "binarisation" more efficient by doing it for all
        // elements rather than relying on multiple calls to this rule and 
        // stripping off one Role for each call.
        if (lhs.length > 2) {
            result = true;

            final int k = lhs.length - 1;
            int[] newLhs1 = new int[k];
            System.arraycopy(lhs, 0, newLhs1, 0, newLhs1.length);
            int u = factory.getRole(newLhs1);

            int[] newLhs2 = { u, lhs[k] };

            gcis[0] = new RI<T>(newLhs1, u);
            gcis[1] = new RI<T>(newLhs2, rhs);
        }

        return result;
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RI other = (RI) obj;
        if (!Arrays.equals(lhs, other.lhs))
            return false;
        if (rhs != other.rhs)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (lhs.length > 0) {
            sb.append(lhs[0]);
        }
        for (int i = 1; i < lhs.length; i++) {
            sb.append(" ");
            sb.append(lhs[i]);
        }
        sb.append(" [ ");
        sb.append(rhs);

        return sb.toString();
    }

    @Override
    public NormalFormGCI getNormalForm() {
        switch (lhs.length) {
        case 1:
            return new NF4(lhs[0], rhs);
        case 2:
            return new NF5(lhs[0], lhs[1], rhs);
        case 0:
            return new NF6(rhs);
        default:
            throw new IllegalStateException("RI is not in Normal Form");
        }
    }

}
