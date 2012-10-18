/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.axioms;

import au.csiro.snorocket.core.model.Datatype;

/**
 * Normal form 8. Feature f with operator op and value v subsumes B.
 * 
 * @author Alejandro Metke
 * 
 */
public final class NF8 extends NormalFormGCI {

    final public Datatype lhsD;
    final public int rhsB;

    /**
	 * 
	 */
    public NF8(Datatype lhsD, int rhsB) {
        this.lhsD = lhsD;
        this.rhsB = rhsB;
    }

    public static NF8 getInstance(Datatype d, int b) {
        return new NF8(d, b);
    }

    public String toString() {
        return lhsD.getFeature() + ".(" + lhsD.getOperator() + ","
                + lhsD.getLiteral() + ")" + " [ " + rhsB;
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { rhsB };
    }

}
