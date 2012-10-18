/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * @author Alejandro Metke
 * 
 */
public class DoubleLiteral extends AbstractLiteral {

    private final double value;

    /**
     * 
     * @param type
     * @param value
     */
    public DoubleLiteral(double value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public double getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(value);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        DoubleLiteral other = (DoubleLiteral) obj;
        if (Double.doubleToLongBits(value) != Double
                .doubleToLongBits(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(AbstractLiteral o) {
        DoubleLiteral dl = (DoubleLiteral) o;
        double otherValue = dl.value;
        return Double.compare(value, otherValue);
    }

}
