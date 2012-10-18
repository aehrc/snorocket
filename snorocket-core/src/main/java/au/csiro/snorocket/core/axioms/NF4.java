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
 * Normal form 5: r &#8849; s <br>
 * role r subsumes role s
 * 
 * @author law223
 * 
 */
public final class NF4 extends NormalFormGCI {

    final private int lhsR;
    final private int rhsS;

    public NF4(int r, int s) {
        lhsR = r;
        rhsS = s;
    }

    public int getR() {
        return lhsR;
    }

    public int getS() {
        return rhsS;
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] {};
    }

}
