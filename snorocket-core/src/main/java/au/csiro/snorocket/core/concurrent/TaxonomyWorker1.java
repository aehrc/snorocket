/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.concurrent;

import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

/**
 * 
 * Creates equivalent and direct sets using a context.
 * 
 * @author Alejandro Metke
 *
 */
public class TaxonomyWorker1<T> implements Runnable {
    
    private final IConceptMap<Context> contextIndex;
    
    private final ConcurrentMap<Integer, Set<Integer>> equiv;
    
    private final ConcurrentMap<Integer, Set<Integer>> direc;
    
    private final IFactory<T> factory;
    
    private final Queue<Integer> todo;
    
    /**
     * 
     */
    public TaxonomyWorker1(IConceptMap<Context> contextIndex, 
            ConcurrentMap<Integer, Set<Integer>> equiv, 
            ConcurrentMap<Integer, Set<Integer>> direc, IFactory<T> factory,
            Queue<Integer> todo) {
        this.contextIndex = contextIndex;
        this.equiv = equiv;
        this.direc = direc;
        this.factory = factory;
        this.todo = todo;
    }
    
    public void run() {
        while(true) {
            Integer aInt = todo.poll();
            if(aInt == null) break;
            
            // Get the context
            int a = aInt.intValue();
            Context ctx = contextIndex.get(a);
            
            // Ignore if virtual
            if(factory.isVirtualConcept(a)) continue;
            
            // For every non-virtual parent get its parents
            for (IntIterator it = ctx.getS().iterator(); it.hasNext(); ) {
                int c = it.next();
                if(factory.isVirtualConcept(c)) continue;
                
                if (c == IFactory.BOTTOM_CONCEPT) {
                    addToSet(equiv, a, c);
                    addToSet(equiv, c, a);
                    continue;
                }
                
                // TODO: why is the context for BOTTOM including TOP?
                if(a == IFactory.BOTTOM_CONCEPT && c == IFactory.TOP_CONCEPT) {
                    continue;
                }
                
                IConceptSet cs = contextIndex.get(c).getS();
                if(cs != null && cs.contains(a)) {
                    addToSet(equiv, a, c);
                } else {
                    boolean isDirect = true;
                    Set<Integer> d = direc.get(a);
                    if (d != null) {
                        Set<Integer> toRemove = new HashSet<Integer>();
                        for (int b : d) {
                            IConceptSet bs = contextIndex.get(b).getS();
                            if (bs != null && bs.contains(c)) {
                                isDirect = false;
                                break;
                            }
                            if (cs != null && cs.contains(b)) {
                                toRemove.add(b);
                            }
                        }
                        d.removeAll(toRemove);
                    }
                    if (isDirect) {
                        addToSet(direc, a, c);
                    }
                }
            }
        }
    }
    
    public static void addToSet(ConcurrentMap<Integer, Set<Integer>> set, 
            int key, int val) {
        Set<Integer> temp = new HashSet<Integer>();
        Set<Integer> valSet = set.putIfAbsent(key, temp);
        if(valSet == null) {
            temp.add(val);
        } else {
            valSet.add(val);
        }
    }

}
