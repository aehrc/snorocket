/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * Represents a literal in a data property expression.
 * 
 * @author Alejandro Metke
 * 
 */
public abstract class AbstractLiteral {
    
    /**
     * Indicates if this literal is equal to another. For example, the integer literal (=,1) is equal to the literal 
     * (>,0) & (<,2).
     * 
     * @param other
     * @return
     */
    public abstract boolean equals(AbstractLiteral other);
    
    /**
     * Indicates if this literal completely covers the values of the other literal. For example, the integer  
     * (>,0) & (<,4) covers the literal (=,1).
     * 
     * @param other
     * @return
     */
    public abstract boolean covers(AbstractLiteral other);
    
    /**
     * Indicates if this literal intersects with another literal.
     * 
     * @param other
     * @return
     */
    public abstract boolean intersects(AbstractLiteral other);

    /**
     * Evaluates the conjunction of all the entries in a literal.
     */
    public abstract void evaluate();
    
    /**
     * Merges the entries of another literal into this literal.
     * 
     * @param other
     */
    public abstract void merge(AbstractLiteral other);
    
    /**
     * Indicates if this literal is an empty range.
     * 
     * @return
     */
    public abstract boolean isEmpty();
}
