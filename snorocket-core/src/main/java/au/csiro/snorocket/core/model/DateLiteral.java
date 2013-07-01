/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import au.csiro.ontology.model.Operator;

/**
 * @author Alejandro Metke
 * 
 */
public class DateLiteral extends AbstractLiteral {

    private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    
    private Calendar lb;
    private Calendar ub;
    private boolean empty;
    
    private final List<Entry> entries = new ArrayList<Entry>();

    public DateLiteral(Operator op, Calendar value) {
        entries.add(new Entry(op, value));
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        if(empty) {
            builder.append("[empty]");
        } else {
            builder.append("[");
            if(lb != null) builder.append(sdf.format(lb)); else builder.append("-\u221E") ;
            builder.append(", ");
            if(ub != null) builder.append(sdf.format(ub)); else builder.append('\u221E') ;
            builder.append("]");
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (empty ? 1231 : 1237);
        result = prime * result + ((lb == null) ? 0 : lb.hashCode());
        result = prime * result + ((ub == null) ? 0 : ub.hashCode());
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
        DateLiteral other = (DateLiteral) obj;
        if (empty != other.empty)
            return false;
        if (lb == null) {
            if (other.lb != null)
                return false;
        } else if (!lb.equals(other.lb))
            return false;
        if (ub == null) {
            if (other.ub != null)
                return false;
        } else if (!ub.equals(other.ub))
            return false;
        return true;
    }

    public static class Entry {
        
        private Operator op;
        private Calendar value;
        
        public Entry(Operator op, Calendar value) {
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
        
        public Calendar getValue() {
            return value;
        }
        
        public void setValue(Calendar value) {
            this.value = value;
        }

    }

    @Override
    public boolean equals(AbstractLiteral other) {
        DateLiteral ol = (DateLiteral) other;
        
        if(empty && !ol.empty || !empty && ol.empty) return false;
        
        if(lb.equals(ol.lb) && ub.equals(ol.ub)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean covers(AbstractLiteral other) {
        if(empty) return false;
        
        DateLiteral ol = (DateLiteral) other;
        if((lb.equals(ol.lb) || lb.before(ol.lb)) && (ub.equals(ol.ub) || ub.after(ol.ub))) {
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
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date(Long.MIN_VALUE));
        lb = cal;
        
        cal = Calendar.getInstance();
        cal.setTime(new Date(Long.MAX_VALUE));
        ub = cal;
        
        Calendar exact = null;
        
        for(Entry entry : entries) {
            Calendar val = entry.value;
            switch(entry.op) {
                case EQUALS:
                    if(exact == null) {
                        // Exact value not set
                        exact = val;
                    } else if(!exact.equals(val)) {
                        // If two exact different values are set then this literal is empty
                        empty = true;
                        return;
                    }
                    break;
                case GREATER_THAN:
                    Calendar newVal = (Calendar) val.clone();
                    newVal.add(Calendar.MILLISECOND, 1);
                    lb = (newVal.after(lb)) ? newVal : lb;
                    //lb = Math.max(val + 1, lb);
                    break;
                case GREATER_THAN_EQUALS:
                    lb = (val.after(lb)) ? val : lb;
                    //lb = Math.max(val, lb);
                    break;
                case LESS_THAN:
                    newVal = (Calendar) val.clone();
                    newVal.add(Calendar.MILLISECOND, -1);
                    ub = (newVal.before(ub)) ? newVal : ub;
                    //ub = Math.min(val - 1, ub);
                    break;
                case LESS_THAN_EQUALS:
                    ub = (val.before(ub)) ? val : ub;
                    //ub = Math.min(val, ub);
                    break;
                default:
                    break;
            }
        }
        if(lb.after(ub) || (exact != null && (!lb.equals(exact) || !ub.equals(exact)))) empty = true;
    }

    @Override
    public void merge(AbstractLiteral other) {
        DateLiteral ol = (DateLiteral) other;
        entries.addAll(ol.entries);
    }

    @Override
    public boolean isEmpty() {
        return empty;
    }

}
