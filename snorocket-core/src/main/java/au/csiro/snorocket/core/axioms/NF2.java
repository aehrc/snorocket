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
 * Normal form 2: A &#8849; &#8707;r.B <br>
 * A subsumes role r with value B
 * 
 * @author law223
 * 
 */
public final class NF2 extends NormalFormGCI implements IRoleQueueEntry {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final public int lhsA;
    final public int rhsB;
    final public int rhsR;

    private NF2(int a, int r, int b) {
        lhsA = a;
        rhsB = b;
        rhsR = r;
    }

    /**
     * A [ r.B
     * 
     * @param ontology
     * @param a
     * @param r
     * @param b
     * @return
     */
    static public NF2 getInstance(final int a, final int r, final int b) {
        return new NF2(a, r, b);
    }

    public String toString() {
        return lhsA + " [ " + rhsR + "." + rhsB;
    }

    // ----------------------
    // RoleQueueEntry methods

    public int getB() {
        return rhsB;
    }

    public int getR() {
        return rhsR;
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { lhsA, rhsB };
    }

}
