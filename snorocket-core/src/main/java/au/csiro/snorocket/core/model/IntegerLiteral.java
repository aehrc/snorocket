/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.util.ArrayList;
import java.util.List;

import au.csiro.ontology.model.Operator;


/**
 * An integer literal.
 * 
 * @author Alejandro Metke
 * 
 */
public class IntegerLiteral extends AbstractLiteral {

    private int lb = Integer.MIN_VALUE;
    private int ub = Integer.MAX_VALUE;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * Constructor.
     * 
     * @param type
     */
    public IntegerLiteral(Operator op, int value) {
        entries.add(new Entry(op, value));
    }

    /**
     * @return the lowerBound
     */
    public int getLowerBound() {
        return lb;
    }

    /**
     * @return the upperBound
     */
    public int getUpperBound() {
        return ub;
    }

    /**
     * @return the empty
     */
    public boolean isEmpty() {
        return empty;
    }

    @Override
    public boolean equals(AbstractLiteral other) {
        IntegerLiteral ol = (IntegerLiteral) other;
        if(empty && !ol.empty || !empty && ol.empty) return false;
        
        if(lb == ol.lb && ub == ol.ub) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        if(empty) return false;
        
        IntegerLiteral ol = (IntegerLiteral) other;
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
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(empty) {
            builder.append("[empty]");
        } else {
            builder.append("[");
            if(lb != Integer.MIN_VALUE) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(ub != Integer.MAX_VALUE) builder.append(ub); else builder.append('\u221E') ;
            builder.append("]");
        }
        return builder.toString();
    }
    
    @Override
    public void evaluate() {
        empty = false;
        lb = Integer.MIN_VALUE;
        ub = Integer.MAX_VALUE;
        int exact = Integer.MIN_VALUE;
        
        for(Entry entry : entries) {
            int val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    if(exact == Integer.MIN_VALUE) {
                        // Exact value not set
                        exact = val;
                    } else if(exact != val) {
                        // If two exact different values are set then this literal is empty
                        empty = true;
                        return;
                    }
                    break;
                case GREATER_THAN:
                    // Calculate the intersection
                    lb = Math.max(val + 1, lb);
                    break;
                case GREATER_THAN_EQUALS:
                    // Calculate the intersection
                    lb = Math.max(val, lb);
                    break;
                case LESS_THAN:
                    ub = Math.min(val - 1, ub);
                    break;
                case LESS_THAN_EQUALS:
                    ub = Math.min(val, ub);
                    break;
                default:
                    break;
            }
        }
        if(lb > ub || (exact != Integer.MIN_VALUE && (lb != exact || ub != exact))) empty = true;
    }
    
    @Override
    public void merge(AbstractLiteral other) {
        IntegerLiteral ol = (IntegerLiteral) other;
        entries.addAll(ol.entries);
    }

    public static class Entry {
        
        private Operator op;
        private int value;
        
        public Entry(Operator op, int value) {
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
        
        public int getValue() {
            return value;
        }
        
        public void setValue(int value) {
            this.value = value;
        }

    }

}
