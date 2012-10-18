/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

/**
 * A datatype expression that represents a set of individuals that have a
 * property with a certain value. The expression consists of a feature, an
 * operator (=, <, <=, >, >=), and a literal value.
 * 
 * @author Alejandro Metke
 * 
 */
public class Datatype extends AbstractConcept {

    // Operator constants
    public static final int OPERATOR_EQUALS = 0;
    public static final int OPERATOR_LESS_THAN = 1;
    public static final int OPERATOR_LESS_THAN_EQUALS = 2;
    public static final int OPERATOR_GREATER_THAN = 3;
    public static final int OPERATOR_GREATER_THAN_EQUALS = 4;

    private int feature;
    private int operator;
    private AbstractLiteral literal;

    /**
	 * 
	 */
    public Datatype(int feature, int operator, AbstractLiteral literal) {
        this.feature = feature;
        this.operator = operator;
        this.literal = literal;
    }

    public int getFeature() {
        return feature;
    }

    public int getOperator() {
        return operator;
    }

    public AbstractLiteral getLiteral() {
        return literal;
    }

    @Override
    public String toString() {
        return feature + ".";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + feature;
        result = prime * result + ((literal == null) ? 0 : literal.hashCode());
        result = prime * result + operator;
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
        if (operator != other.operator)
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
            final int operatorCompare = operator - otherDatatype.operator;
            if (operatorCompare == 0) {
                return literal.toString().compareTo(otherDatatype.toString());
            } else {
                return operatorCompare;
            }
        } else {
            return featureCompare;
        }
    }

}
