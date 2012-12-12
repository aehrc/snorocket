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

package au.csiro.snorocket.core.concurrent;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.ReadonlyConceptSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Concurrent version of R.
 * 
 * @author Alejandro Metke
 * 
 */
public final class CR implements Serializable {
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    private IConceptSet[] data;

    public CR(final int roles) {
        this.data = new IConceptSet[roles];
    }

    public boolean containsRole(int role) {
        return data[role] != null;
    }

    /**
     * 
     * @param r
     * @return
     */
    protected IConceptSet getConcept(int r) {
        if (r >= data.length) {
            resizeRoles(r + 1);
        }
        if (null == data[r]) {
            data[r] = new SparseConceptSet();
        }
        return data[r];
    }

    /**
     * Returns the set of concepts associated to the concept in a
     * {@link Context} by role r.
     * 
     * @param r
     *            The role
     * @return The set of concepts associated to the concept in the context.
     */
    public IConceptSet lookupConcept(int r) {
        if (r >= data.length) {
            return IConceptSet.EMPTY_SET;
        }

        if (null == data[r]) {
            return IConceptSet.EMPTY_SET;
        } else {
            return new ReadonlyConceptSet(data[r]);
        }
    }

    public void clear() {
        Arrays.fill(data, null);
    }

    private void resizeRoles(int maxRole) {
        final IConceptSet[] oldData = data;

        data = new IConceptSet[maxRole];
        System.arraycopy(oldData, 0, data, 0, oldData.length);
    }

    public String toString() {
        final StringBuilder sb = new StringBuilder();

        for (int index = 0; index < data.length; index++) {
            IConceptSet cs = data[index];
            if (null != cs) {
                sb.append(index + "." + cs.toString());
            }
        }

        return sb.toString();
    }

    /**
     * Record C [ r.B, where C is implicit (the concept in a context).
     * 
     * @param r
     * @param B
     */
    synchronized void store(int r, int B) {
        getConcept(r).add(B);
    }
    
    /**
     * Returns the roles stored in this data structure.
     * @return
     */
    public int[] getRoles() {
        List<Integer> roles = new ArrayList<>();
        for(int i = 0; i < data.length; i++) {
            if(data[i] != null) {
                roles.add(i);
            }
        }
        
        int[] res = new int[roles.size()];
        for(int i = 0; i < res.length; i++) {
            res[i] = roles.get(i);
        }
        return res;
    }

}
