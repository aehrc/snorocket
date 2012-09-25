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
 * Normal form 5:       r &#8728; s &#8849; t
 * <br>
 * role r composed with role s subsumes role t
 * 
 * @author law223
 *
 */
public final class NF5 extends NormalFormGCI {

    private int lhsR;
    private int lhsS;
    private int rhsT;

    public NF5(int r, int s, int t) {
        lhsR = r;
        lhsS = s;
        rhsT = t;
    }

    public int getR() {
        return lhsR;
    }

    public int getS() {
        return lhsS;
    }

    public int getT() {
        return rhsT;
    }

	@Override
	public int[] getConceptsInAxiom() {
		return new int[]{};
	}

}
