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
    private boolean lbInc = true;
    private long ub = Long.MAX_VALUE;
    private boolean ubInc = true;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * Constructor.
     * 
     * @param type
     */
    public LongLiteral(Operator op, long value) {
        entries.add(new Entry(op, value));
        evaluate();
    }

    /**
     * @return the lowerBound
     */
    public long getLowerBound() {
        return lb;
    }

    /**
     * @return the upperBound
     */
    public long getUpperBound() {
        return ub;
    }

    /**
     * @return the empty
     */
    public boolean isEmpty() {
        return empty;
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
            if(lbInc) builder.append("["); else builder.append("(");
            if(lb != Long.MIN_VALUE) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(ub != Long.MAX_VALUE) builder.append(ub); else builder.append('\u221E') ;
            if(ubInc) builder.append("]"); else builder.append(")");
        }
        return builder.toString();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (empty ? 1231 : 1237);
        result = prime * result + ((entries == null) ? 0 : entries.hashCode());
        result = prime * result + (int) (lb ^ (lb >>> 32));
        result = prime * result + (lbInc ? 1231 : 1237);
        result = prime * result + (int) (ub ^ (ub >>> 32));
        result = prime * result + (ubInc ? 1231 : 1237);
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
        if (entries == null) {
            if (other.entries != null)
                return false;
        } else if (!entries.equals(other.entries))
            return false;
        if (lb != other.lb)
            return false;
        if (lbInc != other.lbInc)
            return false;
        if (ub != other.ub)
            return false;
        if (ubInc != other.ubInc)
            return false;
        return true;
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        if(empty) return false;
        
        LongLiteral ol = (LongLiteral) other;
        
        long thisLb = lbInc ? lb : lb + 1;
        long otherLb = ol.lbInc ? ol.lb : ol.lb + 1;
        long thisUb = ubInc ? ub : ub - 1;
        long otherUb = ol.ubInc ? ol.ub : ol.ub - 1;
        
        if(thisLb <= otherLb  && thisUb >= otherUb) {
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
    public void evaluate() {
        lb = Long.MIN_VALUE;
        ub = Long.MAX_VALUE;
        empty = false;
        
        List<Long> exacts = new ArrayList<Long>();
        boolean rangeConstraintsExist = false;
        
        for(Entry entry : entries) {
            long val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    exacts.add(val);
                    break;
                case GREATER_THAN:
                    rangeConstraintsExist = true;
                    
                    if(lb == val && lbInc) {
                        lbInc = false;
                    } else if(lb != val && val > lb) {
                        lbInc = false;
                        lb = val;
                    }
                    
                    break;
                case GREATER_THAN_EQUALS:
                    rangeConstraintsExist = true;
                    
                    if(lb != val && val > lb) {
                        lbInc = true;
                        lb = val;
                    }
                    
                    break;
                case LESS_THAN:
                    rangeConstraintsExist = true;
                    
                    if(ub == val && ubInc) {
                        ubInc = false;
                    } else if(ub != val && val < ub) {
                        ubInc = false;
                        ub = val;
                    }
                    
                    break;
                case LESS_THAN_EQUALS:
                    rangeConstraintsExist = true;
                    
                    if(ub != val && val < ub) {
                        ubInc = true;
                        ub = val;
                    }
                    
                    break;
                default:
                    break;
            }
        }
        if(lb != ub && lb > ub)  {
            // If the lower bound is greater than the upper bound then this literal is empty
            empty = true;
        } else {
            if(exacts.isEmpty()) {
                return;
            } else {
                // If there are exact values then all of these must be equal and must also equal the range if range 
                // constraints were specified
                if(rangeConstraintsExist) {
                    if(lb != ub) {
                        // Range constraints don't specify a single value and there are exact values - must be empty
                        empty = true;
                    } else {
                        // Compare all exacts with lb
                        for(Long exact : exacts) {
                            if(lb != exact.longValue()) {
                                empty = true;
                                break;
                            }
                        }
                    }
                } else {
                    // Compare just the exacts
                    lb = exacts.get(0).longValue();
                    ub = lb;
                    for(Long exact : exacts) {
                        if(lb != exact.longValue()) {
                            empty = true;
                            break;
                        }
                    }
                }
            }
        }
    }
    
    @Override
    public void merge(AbstractLiteral other) {
        LongLiteral ol = (LongLiteral) other;
        entries.addAll(ol.entries);
    }
    
    @Override
    public boolean equals(AbstractLiteral other) {
        return equals(other);
    }

}
