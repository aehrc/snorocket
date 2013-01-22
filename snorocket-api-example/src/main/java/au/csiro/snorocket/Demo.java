/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import au.csiro.ontology.Factory;
import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.snorocket.core.SnorocketReasoner;

/**
 * @author Alejandro Metke
 *
 */
public class Demo {
    
    public Demo() {
        
    }
    
    @SuppressWarnings("unchecked")
    public void start() {
        
        Factory f = new Factory();
        
        // 1. Load the base state
        SnorocketReasoner<Integer> reasoner = SnorocketReasoner.load(
                this.getClass().getResourceAsStream("/classifier.state"));
        
        IOntology<Integer> ont = reasoner.getClassifiedOntology();
        int i = 0;
        for(Node<Integer> n : ont.getTopNode().getChildren()) {
            System.out.println(n);
        }
        
        // 2. Add test axiom
        //IAxiom a1 = f.createConceptInclusion(lhs, rhs)
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        Demo d = new Demo();
        d.start();
    }

}
