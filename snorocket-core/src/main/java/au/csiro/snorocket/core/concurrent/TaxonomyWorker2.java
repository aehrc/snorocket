/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.concurrent;

import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import au.csiro.ontology.Node;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

/**
 * Connects the nodes in the taxonomy based on the direct map.
 * 
 * @author Alejandro Metke
 *
 */
public class TaxonomyWorker2<T extends Comparable<T>> implements Runnable {
    
    private final IFactory<T> factory;
    private final Map<T, Node<T>> conceptNodeIndex;
    private final ConcurrentMap<Integer, IConceptSet> direc;
    private final Queue<Node<T>> todo;
    private final Set<Node<T>> nodeSet;
    
    /**
     * 
     */
    public TaxonomyWorker2(IFactory<T> factory, 
            Map<T, Node<T>> conceptNodeIndex, 
            ConcurrentMap<Integer, IConceptSet> direc, Queue<Node<T>> todo, 
            Set<Node<T>> nodeSet) {
        this.factory = factory;
        this.conceptNodeIndex = conceptNodeIndex;
        this.direc = direc;
        this.todo = todo;
        this.nodeSet = nodeSet;
    }

    public void run() {
        while(true) {
            Node<T> node = todo.poll();
            if(node == null) break;
            
            for (T c : node.getEquivalentConcepts()) {
                // Get direct super-concepts
                IConceptSet dc = direc.get(factory.getConcept(c));
                if (dc != null) {
                    for(IntIterator it = dc.iterator(); it.hasNext(); ) {
                        int d = it.next();
                        Node<T> parent = conceptNodeIndex.get(
                                factory.lookupConceptId(d));
                        if (parent != null) {
                            node.getParents().add(parent);
                            parent.getChildren().add(node);
                            nodeSet.remove(parent);
                        }
                    }
                }
            }
        }
    }

}
