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
 * This class represents an abstract axiom that is not (necessarily) normalised.
 * 
 * @author Alejandro Metke
 * 
 */
abstract public class Inclusion<T> {
    /**
     * Runs the first phase in the normalisation process (see Pushing the EL
     * Envelope).
     * 
     * @param factory
     * @return
     */
    abstract public Inclusion<T>[] normalise1(IFactory<T> factory);

    /**
     * Runs the second phase in the normalisation process (see Pushing the EL
     * Envelope).
     * 
     * @param factory
     * @return
     */
    abstract public Inclusion<T>[] normalise2(IFactory<T> factory);

    @Override
    abstract public int hashCode();

    @Override
    abstract public boolean equals(Object o);

    @Override
    abstract public String toString();

    abstract public NormalFormGCI getNormalForm();

}
