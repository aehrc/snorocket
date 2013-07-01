/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * @author Alejandro Metke
 * 
 */
public class BooleanLiteral extends AbstractLiteral {

    private final boolean value;

    /**
     * 
     * @param type
     * @param value
     */
    public BooleanLiteral(boolean value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public boolean getValue() {
        return value;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (value ? 1231 : 1237);
        return result;
    }

    /**
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
        BooleanLiteral other = (BooleanLiteral) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public String toString() {
        if (value)
            return "true";
        return "false";
    }

    @Override
    public boolean equals(AbstractLiteral other) {
        BooleanLiteral ol = (BooleanLiteral) other;
        return value == ol.value;
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        return equals(other);
    }

    @Override
    public boolean intersects(AbstractLiteral other) {
        return equals(other);
    }

    @Override
    public void evaluate() {
        // Nothing to do!
    }

    @Override
    public void merge(AbstractLiteral other) {
        // Nothing to do - BooleanLiterals cannot be merged!
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

}
