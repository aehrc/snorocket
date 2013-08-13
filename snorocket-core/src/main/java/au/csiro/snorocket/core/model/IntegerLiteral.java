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
    private boolean lbInc = true;
    private int ub = Integer.MAX_VALUE;
    private boolean ubInc = true;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * Constructor.
     * 
     * @param type
     */
    public IntegerLiteral(Operator op, int value) {
        entries.add(new Entry(op, value));
        evaluate();
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
            if(lb != Integer.MIN_VALUE) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(ub != Integer.MAX_VALUE) builder.append(ub); else builder.append('\u221E') ;
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
        result = prime * result + lb;
        result = prime * result + (lbInc ? 1231 : 1237);
        result = prime * result + ub;
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
        IntegerLiteral other = (IntegerLiteral) obj;
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
        
        IntegerLiteral ol = (IntegerLiteral) other;
        
        int thisLb = lbInc ? lb : lb + 1;
        int otherLb = ol.lbInc ? ol.lb : ol.lb + 1;
        int thisUb = ubInc ? ub : ub - 1;
        int otherUb = ol.ubInc ? ol.ub : ol.ub - 1;
        
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
    
    @Override
    public void evaluate() {
        lb = Integer.MIN_VALUE;
        ub = Integer.MAX_VALUE;
        empty = false;
        
        List<Integer> exacts = new ArrayList<Integer>();
        boolean rangeConstraintsExist = false;
        
        for(Entry entry : entries) {
            int val = entry.value;
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
                        for(Integer exact : exacts) {
                            if(lb != exact.intValue()) {
                                empty = true;
                                break;
                            }
                        }
                    }
                } else {
                    // Compare just the exacts
                    lb = exacts.get(0).intValue();
                    ub = lb;
                    for(Integer exact : exacts) {
                        if(lb != exact.intValue()) {
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
        IntegerLiteral ol = (IntegerLiteral) other;
        entries.addAll(ol.entries);
    }
    
    @Override
    public boolean equals(AbstractLiteral other) {
        return equals(other);
    }

}
