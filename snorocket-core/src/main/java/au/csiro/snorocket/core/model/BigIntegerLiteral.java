/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.model;

import java.math.BigInteger;

/**
 * @author Alejandro Metke
 *
 */
public class BigIntegerLiteral extends AbstractLiteral {
    private final BigInteger value;

    /**
     * Constructor.
     * 
     * @param type
     */
    public BigIntegerLiteral(BigInteger value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public BigInteger getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((value == null) ? 0 : value.hashCode());
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
        BigIntegerLiteral other = (BigIntegerLiteral) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }

    public int compareTo(AbstractLiteral o) {
        return value.compareTo(((BigIntegerLiteral) o).value);
    }
}
