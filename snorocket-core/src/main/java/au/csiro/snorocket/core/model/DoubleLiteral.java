/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.util.ArrayList;
import java.util.List;

import au.csiro.ontology.model.Operator;

/**
 * A double literal.
 * 
 * @author Alejandro Metke
 * 
 */
public class DoubleLiteral extends AbstractLiteral {

    private static final double EPSILON = 0.0000001d;
    
    private double lb = Double.MIN_VALUE;
    private double ub = Double.MAX_VALUE;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * 
     * @param type
     * @param value
     */
    public DoubleLiteral(Operator op, double value) {
        entries.add(new Entry(op, value));
    }

    /**
     * @return the lowerBound
     */
    public double getLowerBound() {
        return lb;
    }

    /**
     * @return the upperBound
     */
    public double getUpperBound() {
        return ub;
    }

    /**
     * @return the empty
     */
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(empty) {
            builder.append("[empty]");
        } else {
            builder.append("[");
            if(!equals(lb, Double.MIN_VALUE)) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(!equals(ub, Double.MAX_VALUE)) builder.append(ub); else builder.append('\u221E') ;
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (empty ? 1231 : 1237);
        long temp;
        temp = Double.doubleToLongBits(lb);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(ub);
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
        if (empty != other.empty)
            return false;
        if (Double.doubleToLongBits(lb) != Double.doubleToLongBits(other.lb))
            return false;
        if (Double.doubleToLongBits(ub) != Double.doubleToLongBits(other.ub))
            return false;
        return true;
    }

    @Override
    public boolean equals(AbstractLiteral other) {
        DoubleLiteral ol = (DoubleLiteral) other;
        if(empty && !ol.empty || !empty && ol.empty) return false;
        
        if(equals(lb, ol.lb) && equals(ub, ol.ub)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        if(empty) return false;
        
        DoubleLiteral ol = (DoubleLiteral) other;
        if(lb <= ol.lb && ub >= ol.ub) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean intersects(AbstractLiteral other) {
        // Not needed for now
        throw new UnsupportedOperationException();
    }
    
    public static class Entry {
        
        private Operator op;
        private double value;
        
        public Entry(Operator op, double value) {
            super();
            this.op = op;
            this.value = value;
        }
        
        public Operator getOp() {
            return op;
        }
        
        public void setOp(Operator op) {
            this.op = op;
        }
        
        public double getValue() {
            return value;
        }
        
        public void setValue(double value) {
            this.value = value;
        }

    }
    
    private boolean equals(final double first, final double second) {
        return (Math.abs(first - second) < EPSILON);
    }

    @Override
    public void evaluate() {
        lb = Double.MIN_VALUE;
        ub = Double.MAX_VALUE;
        empty = false;
        
        double exact = Double.MIN_VALUE;
        boolean hasOtherThanEq = false;
        
        for(Entry entry : entries) {
            double val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    if(equals(exact, Float.MIN_VALUE)) {
                        // Exact value not set
                        exact = val;
                    } else if(!equals(exact, val)) {
                        // If two exact different values are set then this literal is empty
                        empty = true;
                        return;
                    }
                    break;
                case GREATER_THAN:
                    hasOtherThanEq = true;
                    lb = Math.max(val + EPSILON, lb);
                    break;
                case GREATER_THAN_EQUALS:
                    hasOtherThanEq = true;
                    lb = Math.max(val, lb);
                    break;
                case LESS_THAN:
                    hasOtherThanEq = true;
                    ub = Math.min(val - EPSILON, ub);
                    break;
                case LESS_THAN_EQUALS:
                    hasOtherThanEq = true;
                    ub = Math.min(val, ub);
                    break;
                default:
                    break;
            }
        }
        if(lb > ub || (!equals(exact, Double.MIN_VALUE) && (hasOtherThanEq && (lb != exact || ub != exact)))) empty = true; 
    }

    @Override
    public void merge(AbstractLiteral other) {
        DoubleLiteral ol = (DoubleLiteral) other;
        entries.addAll(ol.entries);
    }

}
