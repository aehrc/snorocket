/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.util.ArrayList;
import java.util.List;

import au.csiro.ontology.model.Operator;

/**
 * A float literal.
 * 
 * @author Alejandro Metke
 * 
 */
public class FloatLiteral extends AbstractLiteral {
    
    private static final float EPSILON = 0.0000001f;
    
    private float lb = Float.MIN_VALUE;
    private float ub = Float.MAX_VALUE;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * 
     * @param type
     */
    public FloatLiteral(Operator op, float value) {
        entries.add(new Entry(op, value));
    }

    /**
     * @return the lowerBound
     */
    public float getLowerBound() {
        return lb;
    }

    /**
     * @return the upperBound
     */
    public float getUpperBound() {
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
            if(!equals(lb, Float.MIN_VALUE)) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(!equals(ub, Float.MAX_VALUE)) builder.append(ub); else builder.append('\u221E') ;
            builder.append("]");
        }
        return builder.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (empty ? 1231 : 1237);
        result = prime * result + Float.floatToIntBits(lb);
        result = prime * result + Float.floatToIntBits(ub);
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
        if (empty != other.empty)
            return false;
        if (!equals(lb, other.lb))
            return false;
        if (Float.floatToIntBits(ub) != Float.floatToIntBits(other.ub))
            return false;
        return true;
    }

    @Override
    public boolean equals(AbstractLiteral other) {
        FloatLiteral ol = (FloatLiteral) other;
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
        
        FloatLiteral ol = (FloatLiteral) other;
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
        private float value;
        
        public Entry(Operator op, float value) {
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
        
        public float getValue() {
            return value;
        }
        
        public void setValue(float value) {
            this.value = value;
        }

    }
    
    private boolean equals(final float first, final float second) {
        // Float.floatToIntBits(lb) != Float.floatToIntBits(other.lb)
        return (Math.abs(first - second) < EPSILON);
    }

    @Override
    public void evaluate() {
        lb = Float.MIN_VALUE;
        ub = Float.MAX_VALUE;
        empty = false;
        
        float exact = Float.MIN_VALUE;
        
        for(Entry entry : entries) {
            float val = entry.value;
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
                    // Calculate the intersection
                    lb = Math.max(val + EPSILON, lb);
                    break;
                case GREATER_THAN_EQUALS:
                    // Calculate the intersection
                    lb = Math.max(val, lb);
                    break;
                case LESS_THAN:
                    ub = Math.min(val - EPSILON, ub);
                    break;
                case LESS_THAN_EQUALS:
                    ub = Math.min(val, ub);
                    break;
                default:
                    break;
            }
        }
        if(lb > ub || (exact != Integer.MIN_VALUE && (!equals(lb, exact) || !equals(ub, exact)))) empty = true;
    }

    @Override
    public void merge(AbstractLiteral other) {
        FloatLiteral ol = (FloatLiteral) other;
        entries.addAll(ol.entries);
    }

}
