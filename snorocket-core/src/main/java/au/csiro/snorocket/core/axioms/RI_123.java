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

import au.csiro.snorocket.core.IFactory_123;

/**
 * 
 * @param lhs
 *            lhs.length == 0 -> reflexive; == 1 -> role subtyping; >= 2 -> role
 *            composition
 * @param rhs
 */
public class RI_123 extends Inclusion_123 {

    private static final int PRIME = 31;

    final private int[] lhs;
    final private int rhs;
    final private int hashCode;

    public RI_123(final int[] lhs, final int rhs) {
        assert null != lhs;
        assert -1 < rhs;
        this.lhs = lhs;
        this.rhs = rhs;
        hashCode = PRIME * (PRIME + Arrays.hashCode(this.lhs)) + this.rhs;
    }

    // public RI(final Collection<Integer> lhs, final int rhs) {
    // this(lhs.toArray(new i[lhs.size()]), rhs);
    // }

    public int[] getLhs() {
        return lhs;
    }

    public int getRhs() {
        return rhs;
    }

    @Override
    public Inclusion_123[] normalise1(final IFactory_123 factory) {
        Inclusion_123[] result = { null, null };

        if (rule1(factory, result)) {
            // System.err.println(".");
        } else {
            result = null;
        }

        return result;
    }

    @Override
    public Inclusion_123[] normalise2(final IFactory_123 factory) {
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
    boolean rule1(final IFactory_123 factory, final Inclusion_123[] gcis) {
        boolean result = false;

        // TODO make this "binarisation" more efficient by doing it for all
        // elements rather than relying on multiple calls to this rule
        // and stripping off one Role for each call.
        if (lhs.length > 2) {
            result = true;

            final int k = lhs.length - 1;
            int[] newLhs1 = new int[k];
            System.arraycopy(lhs, 0, newLhs1, 0, newLhs1.length);
            int u = factory.getRoleIdx("* " + getKey(newLhs1));
            // :!!!:@@@:???: role generation
            factory.setVirtualRole(u, true);

            int[] newLhs2 = { u, lhs[k] };

            gcis[0] = new RI_123(newLhs1, u);
            gcis[1] = new RI_123(newLhs2, rhs);
        }

        return result;
    }

    private String getKey(final int[] roles) {
        final StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < roles.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(roles[i]);
        }

        sb.append("]");

        return sb.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final RI_123 other = (RI_123) obj;
        if (!Arrays.equals(lhs, other.lhs))
            return false;
        if (rhs != other.rhs)
            return false;
        return true;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        // sb.append("R ");
        if (lhs.length > 0) {
            sb.append(lhs[0]);
        }
        for (int i = 1; i < lhs.length; i++) {
            // sb.append(" \u2293 ");
            sb.append(" ");
            sb.append(lhs[i]);
        }
        // sb.append(" \u2291 ");
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
