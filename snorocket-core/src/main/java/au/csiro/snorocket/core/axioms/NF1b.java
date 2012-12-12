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

/**
 * Normal form 1:
 * A<sub>1</sub>&nbsp;&#8851;&nbsp;A<sub>2</sub>&nbsp;&#8849;&nbsp;B <br>
 * A<sub>1</sub> and A<sub>2</sub> subsumes B
 * 
 * @author law223
 * 
 */
public final class NF1b extends NormalFormGCI {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final private int lhsA1;
    final private int lhsA2;
    final private int rhsB;

    private IConjunctionQueueEntry entryA1;
    private IConjunctionQueueEntry entryA2;

    private NF1b(final int lhs1, final int lhs2, final int rhs) {
        rhsB = rhs;

        lhsA1 = lhs1;
        lhsA2 = lhs2;

        entryA1 = new IConjunctionQueueEntry() {
            /**
             * Serialisation version.
             */
            private static final long serialVersionUID = 1L;

            public int getB() {
                return rhsB;
            }

            public int getBi() {
                return lhsA2;
            }

            public String toString() {
                return "ConjunctionQueueEntry[" + (lhsA2 < 0 ? "" : lhsA2)
                        + " [ " + rhsB + ": " + lhsA1 + "]";
            }
        };

        entryA2 = new IConjunctionQueueEntry() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public int getB() {
                return rhsB;
            }

            public int getBi() {
                return lhsA1;
            }

            public String toString() {
                return "ConjunctionQueueEntry[" + lhsA1 + " [ " + rhsB + ": "
                        + lhsA2 + "]";
            }
        };
    }

    static public NF1b getInstance(final int lhs1, final int lhs2, final int rhs) {
        return new NF1b(lhs1, lhs2, rhs);
    }

    public IConjunctionQueueEntry getQueueEntry1() {
        return entryA1;
    }

    public IConjunctionQueueEntry getQueueEntry2() {
        return entryA2;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(lhsA1);
        if (lhsA2 >= 0) {
            sb.append(" & ");
            sb.append(lhsA2);
        }
        // sb.append(" \u2291 ");
        sb.append(" [ ");
        sb.append(rhsB);

        return sb.toString();
    }

    public int lhsA1() {
        return lhsA1;
    }

    public int lhsA2() {
        return lhsA2;
    }

    public int b() {
        return rhsB;
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { lhsA1, lhsA2, rhsB };
    }

}
