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

public class Existential extends AbstractConcept {

    private int role;
    private AbstractConcept concept;

    public Existential(int role, AbstractConcept concept) {
        this.role = role;
        this.concept = concept;
    }

    @Override
    public String toString() {
        return role + " . " + concept;
        // return "\u2203 " + role + "." + concept;
    }

    public int getRole() {
        return role;
    }

    public AbstractConcept getConcept() {
        return concept;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + ((concept == null) ? 0 : concept.hashCode());
        result = PRIME * result + role;
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Existential other = (Existential) obj;
        if (concept == null) {
            if (other.concept != null)
                return false;
        } else if (!concept.equals(other.concept))
            return false;
        if (role != other.role)
            return false;
        return true;
    }

    @Override
    int compareToWhenHashCodesEqual(AbstractConcept other) {
        assert hashCode() == other.hashCode();
        assert other instanceof Existential;

        final Existential otherExistential = (Existential) other;

        final int roleCompare = role - otherExistential.role;

        return 0 == roleCompare ? (null == concept ? (null == otherExistential.concept ? 0
                : -1)
                : (concept.compareTo(otherExistential.concept)))
                : roleCompare;
    }

    // @Override
    // public String getKey() {
    // return role.getKey() + " . " + concept.getKey();
    // }

}
