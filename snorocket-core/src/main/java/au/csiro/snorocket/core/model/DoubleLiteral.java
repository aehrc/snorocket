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
    private boolean lbInc = true;
    private double ub = Double.MAX_VALUE;
    private boolean ubInc = true;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    /**
     * 
     * @param type
     */
    public DoubleLiteral(Operator op, double value) {
        entries.add(new Entry(op, value));
        evaluate();
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
            if(!equals(lb, Double.MIN_VALUE)) builder.append(lb); else builder.append("-\u221E") ;
            builder.append(", ");
            if(!equals(ub, Double.MAX_VALUE)) builder.append(ub); else builder.append('\u221E') ;
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
        long temp;
        temp = Double.doubleToLongBits(lb);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        result = prime * result + (lbInc ? 1231 : 1237);
        temp = Double.doubleToLongBits(ub);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        DoubleLiteral other = (DoubleLiteral) obj;
        if (empty != other.empty)
            return false;
        if (entries == null) {
            if (other.entries != null)
                return false;
        } else if (!entries.equals(other.entries))
            return false;
        if (Double.doubleToLongBits(lb) != Double.doubleToLongBits(other.lb))
            return false;
        if (lbInc != other.lbInc)
            return false;
        if (Double.doubleToLongBits(ub) != Double.doubleToLongBits(other.ub))
            return false;
        if (ubInc != other.ubInc)
            return false;
        return true;
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        if(empty) return false;
        
        DoubleLiteral ol = (DoubleLiteral) other;
        if(coversLowerBound(ol) && coversUpperBound(ol)) {
            return true;
        } else {
            return false;
        }
    }
    
    private boolean coversLowerBound(DoubleLiteral ol) {
        if(equals(lb, ol.lb)) {
            if(lbInc == ol.lbInc) {
                return true;
            } else if(lbInc) {
                return true;
            } else {
                return false;
            }
        } else {
            if(lb < ol.lb) {
                return true;
            } else {
                return false;
            }
        }
    }
    
    private boolean coversUpperBound(DoubleLiteral ol) {
        if(equals(ub, ol.ub)) {
            if(ubInc == ol.ubInc) {
                return true;
            } else if(ubInc) {
                return true;
            } else {
                return false;
            }
        } else {
            if(ub > ol.ub) {
                return true;
            } else {
                return false;
            }
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
        
        List<Double> exacts = new ArrayList<Double>();
        boolean rangeConstraintsExist = false;
        
        for(Entry entry : entries) {
            double val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    exacts.add(val);
                    break;
                case GREATER_THAN:
                    rangeConstraintsExist = true;
                    
                    if(equals(lb, val) && lbInc) {
                        lbInc = false;
                    } else if(!equals(lb, val) && val > lb) {
                        lbInc = false;
                        lb = val;
                    }
                    
                    break;
                case GREATER_THAN_EQUALS:
                    rangeConstraintsExist = true;
                    
                    if(!equals(lb, val) && val > lb) {
                        lbInc = true;
                        lb = val;
                    }
                    
                    break;
                case LESS_THAN:
                    rangeConstraintsExist = true;
                    
                    if(equals(ub, val) && ubInc) {
                        ubInc = false;
                    } else if(!equals(ub, val) && val < ub) {
                        ubInc = false;
                        ub = val;
                    }
                    
                    break;
                case LESS_THAN_EQUALS:
                    rangeConstraintsExist = true;
                    
                    if(!equals(ub, val) && val < ub) {
                        ubInc = true;
                        ub = val;
                    }
                    
                    break;
                default:
                    break;
            }
        }
        if(!equals(lb, ub) && lb > ub)  {
            // If the lower bound is greater than the upper bound then this literal is empty
            empty = true;
        } else {
            if(exacts.isEmpty()) {
                return;
            } else {
                // If there are exact values then all of these must be equal and must also equal the range if range 
                // constraints were specified
                if(rangeConstraintsExist) {
                    if(!equals(lb, ub)) {
                        // Range constraints don't specify a single value and there are exact values - must be empty
                        empty = true;
                    } else {
                        // Compare all exacts with lb
                        for(Double exact : exacts) {
                            if(!equals(lb, exact.doubleValue())) {
                                empty = true;
                                break;
                            }
                        }
                    }
                } else {
                    // Compare just the exacts
                    lb = exacts.get(0).doubleValue();
                    ub = lb;
                    for(Double exact : exacts) {
                        if(!equals(lb, exact.doubleValue())) {
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
        DoubleLiteral ol = (DoubleLiteral) other;
        entries.addAll(ol.entries);
    }

    @Override
    public boolean equals(AbstractLiteral other) {
        return equals(other);
    }
    
}
