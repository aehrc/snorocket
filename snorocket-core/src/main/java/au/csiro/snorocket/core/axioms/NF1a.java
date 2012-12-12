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

import au.csiro.snorocket.core.IFactory;

/**
 * Normal form 1: A<sub>1</sub>&nbsp;&#8849;&nbsp;B <br>
 * A<sub>1</sub> subsumes B
 * 
 * @author Michael Lawley
 * 
 */
public final class NF1a extends NormalFormGCI {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final private int lhsA;
    final private int rhsB;

    private IConjunctionQueueEntry entry;

    private NF1a(final int lhs, final int rhs) {
        rhsB = rhs;

        lhsA = lhs;

        entry = new IConjunctionQueueEntry() {
            /**
             * 
             */
            private static final long serialVersionUID = 1L;

            public int getB() {
                return rhsB;
            }

            public int getBi() {
                return IFactory.TOP_CONCEPT;
            }

            public String toString() {
                return "ConjunctionQueueEntry[ [ " + rhsB + ": " + lhsA + "]";
            }
        };
    }

    static public NF1a getInstance(final int lhs, final int rhs) {
        return new NF1a(lhs, rhs);
    }

    public IConjunctionQueueEntry getQueueEntry() {
        return entry;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(lhsA);
        // sb.append(" \u2291 ");
        sb.append(" [ ");
        sb.append(rhsB);

        return sb.toString();
    }

    public int lhsA() {
        return lhsA;
    }

    public int b() {
        return rhsB;
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { lhsA, rhsB };
    }

}
