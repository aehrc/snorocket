/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * @author Alejandro Metke
 * 
 */
public class LongLiteral extends AbstractLiteral {

    private final long value;

    public LongLiteral(long value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public long getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (value ^ (value >>> 32));
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
        LongLiteral other = (LongLiteral) obj;
        if (value != other.value)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    @Override
    public int compareTo(AbstractLiteral o) {
        LongLiteral ll = (LongLiteral) o;
        long otherValue = ll.value;
        return Long.compare(value, otherValue);
    }

}
