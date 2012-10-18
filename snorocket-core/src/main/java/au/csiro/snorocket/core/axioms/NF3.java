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
 * Normal form 3: &#8707;r.A &#8851; B <br>
 * role r with value A subsumes B
 * 
 * @author law223
 * 
 */
public final class NF3 extends NormalFormGCI {

    final public int lhsR;
    final public int lhsA;
    final public int rhsB;

    /**
     * r.A [ B
     * 
     * @param ontology
     * @param a
     * @param r
     * @param b
     */
    private NF3(int r, int a, int b) {
        lhsR = r;
        lhsA = a;
        rhsB = b;
    }

    public IConjunctionQueueEntry getQueueEntry() {
        return new IConjunctionQueueEntry() {

            public int getB() {
                return rhsB;
            }

            public int getBi() {
                return IFactory.TOP_CONCEPT;
            }

            public String toString() {
                return "ConjunctionQueueEntry[" + rhsB + ", []] r=" + lhsR
                        + ", a=" + lhsA;
            }

        };

    }

    public String toString() {
        return lhsR + "." + lhsA + " [ " + rhsB;
    }

    public static NF3 getInstance(int r, int A, int B) {
        return new NF3(r, A, B);
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { lhsA, rhsB };
    }

}
