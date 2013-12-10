/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.math.BigDecimal;

/**
 * A double literal.
 * 
 * @author Alejandro Metke
 * 
 */
public class DecimalLiteral extends AbstractLiteral {

    private final BigDecimal value;

    /**
     * Constructor.
     * 
     * @param type
     */
    public DecimalLiteral(BigDecimal value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public BigDecimal getValue() {
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
        DecimalLiteral other = (DecimalLiteral) obj;
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
        return value.compareTo(((DecimalLiteral) o).value);
    }
    
}
