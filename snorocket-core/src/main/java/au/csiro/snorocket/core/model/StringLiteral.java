/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * Represents a literal that is best stored as a Java string.
 * 
 * @author Alejandro Metke
 * 
 */
public class StringLiteral extends AbstractLiteral {

    private final String value;

    public StringLiteral(String value) {
        this.value = value;
    }

    /**
     * @return the value
     */
    public String getValue() {
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
        StringLiteral other = (StringLiteral) obj;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public int compareTo(AbstractLiteral o) {
        StringLiteral sl = (StringLiteral) o;
        String otherValue = sl.value;
        return value.compareTo(otherValue);
    }

}
