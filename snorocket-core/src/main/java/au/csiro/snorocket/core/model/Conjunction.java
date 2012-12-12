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

import java.util.Arrays;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;

public class Conjunction extends AbstractConcept {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    final private AbstractConcept[] concepts;
    final private int hashCode;

    public Conjunction(final AbstractConcept[] concepts) {
        final SortedSet<AbstractConcept> sorted = new TreeSet<AbstractConcept>();
        for (AbstractConcept concept : concepts) {
            sorted.add(concept);
        }
        this.concepts = sorted.toArray(new AbstractConcept[sorted.size()]);
        hashCode = sorted.hashCode();
    }

    public Conjunction(final Collection<? extends AbstractConcept> concepts) {
        // store the concepts in hashCode order so that equals() is order
        // independent
        // i.e. Conjunctions are reflexive (should also be transitive, but Agile
        // says STTCPW)

        final SortedSet<AbstractConcept> sorted = new TreeSet<AbstractConcept>(
                concepts);
        this.concepts = sorted.toArray(new AbstractConcept[sorted.size()]);
        hashCode = sorted.hashCode();
    }

    public AbstractConcept[] getConcepts() {
        return concepts;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(concepts[0]);
        for (int i = 1; i < concepts.length; i++) {
            sb.append(" + ");
            sb.append(concepts[i]);
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Conjunction other = (Conjunction) obj;
        return hashCode == other.hashCode
                && Arrays.equals(concepts, other.concepts);
    }

    @Override
    int compareToWhenHashCodesEqual(AbstractConcept other) {
        assert hashCode() == other.hashCode();
        assert other instanceof Conjunction;

        final Conjunction otherConjunction = (Conjunction) other;

        // First check that the conjunctions have the same length
        int result = concepts.length - otherConjunction.concepts.length;

        // Then, compare members in order
        for (int i = 0; 0 == result && i < concepts.length; i++) {
            result = concepts[i].compareTo(otherConjunction.concepts[i]);
        }

        return result;
    }

}
