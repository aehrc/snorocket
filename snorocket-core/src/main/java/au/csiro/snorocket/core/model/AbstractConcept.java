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

package au.csiro.snorocket.core.model;

import java.io.Serializable;

/**
 * Represents a simple concept.
 * 
 * @author law223
 * 
 */
public abstract class AbstractConcept implements Comparable<AbstractConcept>, Serializable {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    @Override
    abstract public String toString();

    @Override
    abstract public int hashCode();

    @Override
    abstract public boolean equals(Object o);

    /**
     * Called to compare this object with the specified object when the
     * {@link #hashCode}s are equal.
     * 
     * @param other
     *            the Object to be compared.
     * 
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     * @see {@link #compareTo(AbstractConcept)},
     *      {@link Comparable#compareTo(Object)}
     */
    abstract int compareToWhenHashCodesEqual(AbstractConcept other);

    /**
     * Default {@link Comparable#compareTo(Object)} implementation that calls
     * {@link #compareToWhenHashCodesEqual(AbstractConcept)} if and only if the
     * {@link #hashCode()} values of {@code this} and {@code other} are equal.
     * 
     * @return a negative integer, zero, or a positive integer as this object is
     *         less than, equal to, or greater than the specified object.
     */
    public int compareTo(AbstractConcept other) {
        // Need to sort by concrete subclass first, and then by object hashCode
        final int meta = getClass().hashCode() - other.getClass().hashCode();
        final int quickCompare = hashCode() - other.hashCode();

        int result = 0 == meta ? (0 == quickCompare ? compareToWhenHashCodesEqual(other)
                : quickCompare)
                : meta;

        // If compare result is 0, then the objects must be equal()
        assert 0 != result || this.equals(other);

        return result;
    }

}