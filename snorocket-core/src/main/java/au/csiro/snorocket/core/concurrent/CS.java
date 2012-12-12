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

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Concurrent version of S.
 * 
 * @author Alejandro Metke
 */
public final class CS implements Serializable {
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    private IConceptSet set;

    CS(final int cid) {
        set = new SparseConceptSet();
        set.add(cid);
        set.add(IFactory.TOP_CONCEPT);
    }

    public IConceptSet getSet() {
        return set;
    }

    void put(int parent) {
        set.add(parent);
    }

    public String toString() {
        return set.toString();
    }

}
