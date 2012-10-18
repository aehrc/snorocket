/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * @author Alejandro Metke
 * 
 */
public class FloatLiteral extends AbstractLiteral {

    private final float value;

    /**
     * 
     * @param type
     */
    public FloatLiteral(float value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public float getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Float.floatToIntBits(value);
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
        FloatLiteral other = (FloatLiteral) obj;
        if (Float.floatToIntBits(value) != Float.floatToIntBits(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(AbstractLiteral o) {
        FloatLiteral fl = (FloatLiteral) o;
        float otherValue = fl.value;
        return Float.compare(value, otherValue);
    }

}
