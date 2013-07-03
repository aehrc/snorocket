/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;


/**
 * A datatype expression that represents a set of individuals that have a property with a certain value. The expression 
 * consists of a feature and a literal value. The literal value is a range of values.
 * 
 * @author Alejandro Metke
 * 
 */
public class Datatype extends AbstractConcept {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;
    
    private int feature;
    private AbstractLiteral literal;

    /**
	 * 
	 */
    public Datatype(int feature, AbstractLiteral literal) {
        this.feature = feature;
        this.literal = literal;
    }

    public int getFeature() {
        return feature;
    }

    public AbstractLiteral getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return feature + ".(" + literal + ")";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + feature;
        result = prime * result + ((literal == null) ? 0 : literal.hashCode());
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
        Datatype other = (Datatype) obj;
        if (feature != other.feature)
            return false;
        if (literal == null) {
            if (other.literal != null)
                return false;
        } else if (!literal.equals(other.literal))
            return false;
        return true;
    }

    @Override
    int compareToWhenHashCodesEqual(AbstractConcept other) {
        assert hashCode() == other.hashCode();
        assert other instanceof Datatype;

        final Datatype otherDatatype = (Datatype) other;

        final int featureCompare = feature - otherDatatype.feature;

        if (featureCompare == 0) {
            return literal.toString().compareTo(otherDatatype.literal.toString());
        } else {
            return featureCompare;
        }
    }

}
