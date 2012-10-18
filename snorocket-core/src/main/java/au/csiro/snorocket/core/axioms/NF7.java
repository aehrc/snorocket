/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.axioms;

import au.csiro.snorocket.core.model.Datatype;

/**
 * Normal form 7. A subsumes feature f with operator op and value v.
 * 
 * @author Alejandro Metke
 * 
 */
public final class NF7 extends NormalFormGCI implements IFeatureQueueEntry {

    final public int lhsA;
    final public Datatype rhsD;

    /**
	 * 
	 */
    public NF7(int lhsA, Datatype rhsD) {
        this.lhsA = lhsA;
        this.rhsD = rhsD;
    }

    public Datatype getD() {
        return rhsD;
    }

    static public NF7 getInstance(final int a, Datatype d) {
        return new NF7(a, d);
    }

    public String toString() {
        return lhsA + " [ " + rhsD.getFeature() + ".(" + rhsD.getOperator()
                + "," + rhsD.getLiteral() + ")";
    }

    @Override
    public int[] getConceptsInAxiom() {
        return new int[] { lhsA };
    }

}
