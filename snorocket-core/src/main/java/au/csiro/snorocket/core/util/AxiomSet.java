/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;

import au.csiro.snorocket.core.axioms.NF1a;
import au.csiro.snorocket.core.axioms.NF1b;
import au.csiro.snorocket.core.axioms.NF2;
import au.csiro.snorocket.core.axioms.NF3;
import au.csiro.snorocket.core.axioms.NF4;
import au.csiro.snorocket.core.axioms.NF5;
import au.csiro.snorocket.core.axioms.NF6;
import au.csiro.snorocket.core.axioms.NF7;
import au.csiro.snorocket.core.axioms.NF8;
import au.csiro.snorocket.core.axioms.NormalFormGCI;

/**
 * Represents a set of axioms in normal form.
 * 
 * @author Alejandro Metke
 * 
 */
public class AxiomSet implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    
    private final Collection<NF1a> nf1aAxioms = new HashSet<NF1a>();
    private final Collection<NF1b> nf1bAxioms = new HashSet<NF1b>();
    private final Collection<NF2> nf2Axioms = new HashSet<NF2>();
    private final Collection<NF3> nf3Axioms = new HashSet<NF3>();
    private final Collection<NF4> nf4Axioms = new HashSet<NF4>();
    private final Collection<NF5> nf5Axioms = new HashSet<NF5>();
    private final Collection<NF6> nf6Axioms = new HashSet<NF6>();
    private final Collection<NF7> nf7Axioms = new HashSet<NF7>();
    private final Collection<NF8> nf8Axioms = new HashSet<NF8>();

    public void addAxiom(NormalFormGCI a) {
        if (a instanceof NF1a) {
            nf1aAxioms.add((NF1a) a);
        } else if (a instanceof NF1b) {
            nf1bAxioms.add((NF1b) a);
        } else if (a instanceof NF2) {
            nf2Axioms.add((NF2) a);
        } else if (a instanceof NF3) {
            nf3Axioms.add((NF3) a);
        } else if (a instanceof NF4) {
            nf4Axioms.add((NF4) a);
        } else if (a instanceof NF5) {
            nf5Axioms.add((NF5) a);
        } else if (a instanceof NF6) {
            nf6Axioms.add((NF6) a);
        } else if (a instanceof NF7) {
            nf7Axioms.add((NF7) a);
        } else if (a instanceof NF8) {
            nf8Axioms.add((NF8) a);
        }
    }

    public Collection<NF1a> getNf1aAxioms() {
        return nf1aAxioms;
    }

    public Collection<NF1b> getNf1bAxioms() {
        return nf1bAxioms;
    }

    public Collection<NF2> getNf2Axioms() {
        return nf2Axioms;
    }

    public Collection<NF3> getNf3Axioms() {
        return nf3Axioms;
    }

    public Collection<NF4> getNf4Axioms() {
        return nf4Axioms;
    }

    public Collection<NF5> getNf5Axioms() {
        return nf5Axioms;
    }

    public Collection<NF6> getNf6Axioms() {
        return nf6Axioms;
    }

    public Collection<NF7> getNf7Axioms() {
        return nf7Axioms;
    }

    public Collection<NF8> getNf8Axioms() {
        return nf8Axioms;
    }
    
    public void clear() {
        nf1aAxioms.clear();
        nf1bAxioms.clear();
        nf2Axioms.clear();
        nf3Axioms.clear();
        nf4Axioms.clear();
        nf5Axioms.clear();
        nf6Axioms.clear();
        nf7Axioms.clear();
        nf8Axioms.clear();
    }
    
    public boolean isEmpty() {
        return nf1aAxioms.isEmpty() && nf1bAxioms.isEmpty() && nf2Axioms.isEmpty() && nf3Axioms.isEmpty() && 
                nf4Axioms.isEmpty() && nf5Axioms.isEmpty() && nf6Axioms.isEmpty() && nf7Axioms.isEmpty() && 
                nf8Axioms.isEmpty();
    }

}
