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

import java.io.PrintWriter;
import java.util.logging.Level;

import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.core.Snorocket;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Existential;

/**
 * A General Concept Inclusion (C &#8849; D)
 * 
 * @author law223
 * 
 */
public class GCI_123 extends Inclusion_123 {

    private static final int PRIME = 31;

    final private AbstractConcept lhs;
    final private AbstractConcept rhs;
    final private int hashCode;

    public GCI_123(final int lhs, final AbstractConcept rhs) {
        this(new Concept(lhs), rhs);
    }

    public GCI_123(final AbstractConcept lhs, final int rhs) {
        this(lhs, new Concept(rhs));
    }

    public AbstractConcept lhs() {
        return lhs;
    }

    public AbstractConcept rhs() {
        return rhs;
    }

    /**
     * lhs &#8849; rhs
     * 
     * @param lhs
     * @param rhs
     */
    public GCI_123(final AbstractConcept lhs, final AbstractConcept rhs) {
        if (null == lhs) {
            throw new IllegalArgumentException("LHS cannot be null (RHS = "
                    + rhs + ")");
        }
        this.lhs = lhs;
        if (null == rhs) {
            this.rhs = new Concept(IFactory_123.TOP_CONCEPT);
        } else {
            this.rhs = rhs;
        }
        hashCode = PRIME * (PRIME + this.lhs.hashCode()) + this.rhs.hashCode();
    }

    public Inclusion_123[] normalise1(final IFactory_123 factory) {
        final Inclusion_123[] result = { null, null };

        if (rule2(factory, result) || rule3(factory, result) || rule4(result)) {
            // System.err.println("+");
            return result;
        }

        return null;
    }

    public Inclusion_123[] normalise2(final IFactory_123 factory) {
        final Inclusion_123[] result = { null, null, null, null, null, null,
                null, null };

        if (isRule7Applicable()) {
            return rule7(result);
        } else if (rule6(factory, result) || rule5(factory, result)) {
            // System.err.println("*");
            return result;
        }

        return null;
    }

    /**
     * C &#8851; D' &#8849; E &rarr; {D' &#8849; A, C &#8851; A &#8849; E}
     * 
     * @param gcis
     * @return
     */
    boolean rule2(final IFactory_123 factory, final Inclusion_123[] gcis) {
        boolean result = false;

        if (lhs instanceof Conjunction) {
            Conjunction conjunction = (Conjunction) lhs;
            final AbstractConcept[] concepts = conjunction.getConcepts();

            if (concepts.length == 1) {
                // unwrap redundant conjuncts
                gcis[0] = new GCI_123(concepts[0], rhs);
                result = true;
            } else if (concepts.length == 0) {
                Snorocket.getLogger().log(Level.WARNING,
                        "WARNING: Empty conjunct detected in: " + this);
                gcis[0] = new GCI_123(IFactory_123.TOP_CONCEPT, rhs);
                result = true;
            } else {
                // Swap out any non-Concept concepts (ie Existentials)
                for (int i = 0; !result && i < concepts.length; i++) {
                    if (!(concepts[i] instanceof Concept)) {
                        final Concept a = getA(factory, concepts[i]);
                        gcis[0] = new GCI_123(concepts[i], a);

                        final AbstractConcept[] newConcepts = new AbstractConcept[concepts.length];
                        System.arraycopy(concepts, 0, newConcepts, 0,
                                concepts.length);
                        newConcepts[i] = a;
                        gcis[1] = new GCI_123(new Conjunction(newConcepts), rhs);
                        result = true;
                    }
                }

                if (!result) {
                    if (concepts.length > 2) {
                        // Binarise a conjunction of Concepts (expected/assumed
                        // by NF1)
                        result = true;
                        final AbstractConcept[] newConcepts = new AbstractConcept[concepts.length - 1];
                        System.arraycopy(concepts, 1, newConcepts, 0,
                                concepts.length - 1);
                        final AbstractConcept d = new Conjunction(newConcepts);
                        final Concept a = getA(factory, d);
                        final AbstractConcept cAndA = new Conjunction(
                                new AbstractConcept[] { concepts[0], a });

                        gcis[0] = new GCI_123(cAndA, rhs);
                        gcis[1] = new GCI_123(d, a);
                    } else if (concepts.length < 2) {
                        throw new AssertionError(
                                "Conjunctions of fewer than two elements should not exist at this point: "
                                        + this);
                    }
                }
            }
        }

        return result;
    }

    /**
     * &#8707;r.C' &#8849; D &rarr; {C' &#8849; A, &#8707;r.A &#8849; D}
     * 
     * @param gcis
     * @return
     */
    boolean rule3(final IFactory_123 factory, final Inclusion_123[] gcis) {
        boolean result = false;

        if (lhs instanceof Existential) {
            Existential existential = (Existential) lhs;
            final AbstractConcept cHat = existential.getConcept();
            if (!(cHat instanceof Concept)) {
                result = true;
                Concept a = getA(factory, cHat);
                gcis[0] = new GCI_123(cHat, a);
                gcis[1] = new GCI_123(
                        new Existential(existential.getRole(), a), rhs);
            }
        }

        return result;
    }

    /**
     * &#8869; &#8849; D &rarr; &empty;
     * 
     * This rule matches &#8869; (bottom), but there is no &#x22A5; in our
     * Ontologies (AFAIK) so it's redundant.
     * 
     * @param gcis
     * @return
     */
    boolean rule4(Inclusion_123[] gcis) {
        boolean result = false;
        return result;
    }

    /**
     * C' &#8849; D' &rarr; {C' &#8849; A, A &#8849; D'}
     * 
     * @param gcis
     * @return
     */
    boolean rule5(final IFactory_123 factory, final Inclusion_123[] gcis) {
        boolean result = false;

        if (!(lhs instanceof Concept) && !(rhs instanceof Concept)) {
            result = true;
            // System.err.println(lhs);
            // System.err.println(rhs);
            Concept a = getA(factory, lhs);
            gcis[0] = new GCI_123(lhs, a);
            gcis[1] = new GCI_123(a, rhs);
        }

        return result;
    }

    /**
     * B &#8849; &#8707;r.C' &rarr; {B &#8849; &#8707;r.A, A &#8849; C'}
     * 
     * @param gcis
     * @return
     */
    boolean rule6(final IFactory_123 factory, final Inclusion_123[] gcis) {
        boolean result = false;

        if (rhs instanceof Existential) {
            Existential existential = (Existential) rhs;
            final AbstractConcept cHat = existential.getConcept();
            if (!(cHat instanceof Concept)) {
                result = true;
                Concept a = getA(factory, cHat);
                gcis[0] = new GCI_123(lhs, new Existential(
                        existential.getRole(), a));
                gcis[1] = new GCI_123(a, cHat);
            }
        }

        return result;
    }

    private Concept getA(final IFactory_123 factory, final AbstractConcept cHat) {
        final String key = "<" + structure(factory, cHat) + ">";
        final boolean alreadyExists = factory.conceptExists(key);
        // :!!!:@@@:???: CONCEPT GENERATION <>
        final int a = factory.getConceptIdx(key);
        if (!alreadyExists) {
            factory.setVirtualConceptCIdx(a, true);
        }
        return new Concept(a);
    }

    boolean isRule7Applicable() {
        return rhs instanceof Conjunction;
    }

    /**
     * B &#8849; C &#8851; D &rarr; {B &#8849; C, B &#8849; D}
     * 
     * @param gcis
     * @return
     */
    Inclusion_123[] rule7(Inclusion_123[] gcis) {
        assert isRule7Applicable();

        final Conjunction conjunction = (Conjunction) rhs;
        final AbstractConcept[] concepts = conjunction.getConcepts();

        if (concepts.length > gcis.length) {
            gcis = new Inclusion_123[concepts.length];
        }

        for (int i = 0; i < concepts.length; i++) {
            gcis[i] = new GCI_123(lhs, concepts[i]);
        }

        return gcis;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hashCode;
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
        final GCI_123 other = (GCI_123) obj;
        if (!lhs.equals(other.lhs))
            return false;
        if (!rhs.equals(other.rhs))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return // lhs.hashCode() +
        structure(lhs) + " [ " + structure(rhs);
        // return lhs + " \u2291 " + rhs;
    }

    @Override
    public NormalFormGCI getNormalForm() {
        final NormalFormGCI result;
        // try {
        if (lhs instanceof Concept) {
            if (rhs instanceof Concept) {
                final int newLhs = lhs.hashCode();
                result = NF1a.getInstance(newLhs, rhs.hashCode());
            } else if (rhs instanceof Existential) {
                // System.err.println(this);
                final Existential existential = (Existential) rhs;
                result = NF2.getInstance(lhs.hashCode(), existential.getRole(),
                        existential.getConcept().hashCode());
            } else {
                throw new IllegalStateException(
                        "GCI is not in Normal Form: lhs is Concept but rhs is neither Concept nor Existential; it is "
                                + structure(rhs));
            }
        } else if (lhs instanceof Conjunction) {
            final Conjunction conjunction = (Conjunction) lhs;
            final AbstractConcept[] concepts = conjunction.getConcepts();
            if (concepts.length == 1) {
                result = NF1a.getInstance(concepts[0].hashCode(),
                        rhs.hashCode());
                // System.err.println("***** " + rhs.getClass());
            } else if (concepts.length == 2) {
                result = NF1b.getInstance(concepts[0].hashCode(),
                        concepts[1].hashCode(), rhs.hashCode());
            } else {
                throw new IllegalStateException(
                        "Conjunction should have exactly one or two Concepts, not "
                                + concepts.length + ": "
                                + structure(conjunction));
            }
        } else if (lhs instanceof Existential) {
            Existential existential = (Existential) lhs;
            result = NF3.getInstance(existential.getRole(), existential
                    .getConcept().hashCode(), rhs.hashCode());
        } else {
            throw new IllegalStateException("GCI is not in Normal Form: "
                    + structure(lhs) + ", " + structure(rhs));
        }

        return result;
    }

    private String structure(AbstractConcept concept) {
        return structure(LookupFactory.INSTANCE, concept);
    }

    /* :!!!:!!!: String STRUCTURE must be resolved. */
    private String structure(IFactory_123 factory, AbstractConcept concept) {
        if (null == concept) {
            return "NULL";
        } else if (concept instanceof Conjunction) {
            // "(C1 + C2 + ... + Cn)"
            final Conjunction conj = (Conjunction) concept;
            final AbstractConcept[] concepts = conj.getConcepts();
            final StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append(structure(factory, concepts[0]));
            for (int i = 1; i < concepts.length; i++) {
                sb.append(" + ");
                sb.append(structure(factory, concepts[i]));
            }
            sb.append(")");

            return sb.toString();

        } else if (concept instanceof Existential) {
            // R.structure(C)
            final Existential existential = (Existential) concept;
            return factory.lookupRoleId(existential.getRole()) + "."
                    + structure(factory, existential.getConcept());
        } else { // It's a Concept
            // :!!!:@@@:???:ADD: String.valueOf(nid(cidx))
            return factory.lookupConceptStrId(((Concept) concept).hashCode());
        }
    }

    private static final class LookupFactory implements IFactory_123 {

        final static LookupFactory INSTANCE = new LookupFactory(); // SINGLETON

        private LookupFactory() {
        }

        //
        public int lookupConceptId(int id) {
            return id;
        }

        public int lookupRoleId(int id) {
            return id;
        }

        public String lookupConceptStrId(int id) {
            // return String.valueOf(id); // :!!!:@@@:???:
            throw new UnsupportedOperationException();
        }

        public String lookupRoleStrId(int id) {
            // return String.valueOf(id); // :!!!:@@@:???:
            throw new UnsupportedOperationException();
        }

        public boolean conceptExists(int key) {
            throw new UnsupportedOperationException();
        }

        public int getConceptIdx(int key) {
            throw new UnsupportedOperationException();
        }

        public int getRoleIdx(int key) {
            throw new UnsupportedOperationException();
        }

        public int getTotalConcepts() {
            throw new UnsupportedOperationException();
        }

        public int getTotalRoles() {
            throw new UnsupportedOperationException();
        }

        public boolean isBaseConcept(int id) {
            throw new UnsupportedOperationException();
        }

        public boolean isBaseRole(int id) {
            throw new UnsupportedOperationException();
        }

        public boolean isVirtualConcept(int id) {
            throw new UnsupportedOperationException();
        }

        public boolean isVirtualRole(int id) {
            throw new UnsupportedOperationException();
        }

        public void printAll(PrintWriter writer) {
            throw new UnsupportedOperationException();
        }

        public boolean roleExists(int key) {
            throw new UnsupportedOperationException();
        }

        public void setVirtualConceptCIdx(int id, boolean isVirtual) {
            throw new UnsupportedOperationException();
        }

        public void setVirtualRole(int id, boolean isVirtual) {
            throw new UnsupportedOperationException();
        }

        public int findConceptIdx(int key) {
            throw new UnsupportedOperationException();
        }

        public int findRoleIdx(int key) {
            throw new UnsupportedOperationException();
        }

        public int getConceptIdx(String key) {
            throw new UnsupportedOperationException();
        }

        public int getRoleIdx(String key) {
            throw new UnsupportedOperationException();
        }

        public boolean conceptExists(String key) {
            throw new UnsupportedOperationException();
        }

        public boolean roleExists(String key) {
            throw new UnsupportedOperationException();
        }

        public String toStringStats() {
            throw new UnsupportedOperationException();
        }

        public int[] getConceptArray() {
            throw new UnsupportedOperationException();
        }

        public int[] getRoleArray() {
            throw new UnsupportedOperationException();
        }

    }

}
