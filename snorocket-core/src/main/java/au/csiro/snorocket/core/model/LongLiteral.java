/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.util.ArrayList;
import java.util.List;

import au.csiro.ontology.model.Operator;


/**
 * A long literal.
 * 
 * @author Alejandro Metke
 * 
 */
public class LongLiteral extends AbstractLiteral {

    private long lb = Long.MIN_VALUE;
    private long ub = Long.MAX_VALUE;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    public LongLiteral(Operator op, long value) {
        entries.add(new Entry(op, value));
    }
    
    public long getLowerBound() {
        return lb;
    }
    
    public long getUpperBound() {
        return ub;
    }

    /**
     * @return the entries
     */
    public List<Entry> getEntries() {
        return entries;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(empty) {
            builder.append("[empty]");
        } else {
            builder.append("[");
            if(lb != Long.MIN_VALUE) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(ub != Long.MAX_VALUE) builder.append(ub); else builder.append('\u221E') ;
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (empty ? 1231 : 1237);
        result = prime * result + (int) (lb ^ (lb >>> 32));
        result = prime * result + (int) (ub ^ (ub >>> 32));
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
        if (empty != other.empty)
            return false;
        if (lb != other.lb)
            return false;
        if (ub != other.ub)
            return false;
        return true;
    }

    public static class Entry {
        private Operator op;
        private long value;
        
        public Entry(Operator op, long value) {
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
        
        public long getValue() {
            return value;
        }
        
        public void setValue(long value) {
            this.value = value;
        }

    }

    @Override
    public boolean equals(AbstractLiteral other) {
        LongLiteral ol = (LongLiteral) other;
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
        
        LongLiteral ol = (LongLiteral) other;
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
    public void evaluate() {
        empty = false;
        lb = Long.MIN_VALUE;
        ub = Long.MAX_VALUE;
        long exact = Long.MIN_VALUE;
        boolean hasOtherThanEq = false;
        
        for(Entry entry : entries) {
            long val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    if(exact == Long.MIN_VALUE) {
                        // Exact value not set
                        exact = val;
                    } else if(exact != val) {
                        // If two exact different values are set then this literal is empty
                        empty = true;
                        return;
                    }
                    break;
                case GREATER_THAN:
                    hasOtherThanEq = true;
                    lb = Math.max(val + 1, lb);
                    break;
                case GREATER_THAN_EQUALS:
                    hasOtherThanEq = true;
                    lb = Math.max(val, lb);
                    break;
                case LESS_THAN:
                    hasOtherThanEq = true;
                    ub = Math.min(val - 1, ub);
                    break;
                case LESS_THAN_EQUALS:
                    hasOtherThanEq = true;
                    ub = Math.min(val, ub);
                    break;
                default:
                    break;
            }
        }
        if(lb > ub || (exact != Long.MIN_VALUE && (hasOtherThanEq && (lb != exact || ub != exact)))) empty = true; 
    }

    @Override
    public void merge(AbstractLiteral other) {
        LongLiteral ol = (LongLiteral) other;
        entries.addAll(ol.entries);
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

}
