/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.benchmark;

import java.io.File;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import au.csiro.ontology.util.Statistics;
import au.csiro.snorocket.owlapi.SnorocketReasonerFactory;

/**
 * @author Alejandro Metke
 *
 */
public class OWLBenchmark {

    /**
     * 
     */
    public OWLBenchmark() {
        
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        
        // loading the root ontology
        OWLOntology root = null;
        try {
            root = man.loadOntologyFromOntologyDocument(new File(
                    "C:\\dev\\ontologies\\owl\\snomed_20130131_stated.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return;
        }
        System.out.println("owl-api loading: "+(System.currentTimeMillis()-start));
        
        // Create a Snorocket reasoner and classify
        OWLReasonerFactory reasonerFactory = new SnorocketReasonerFactory();
        OWLReasoner reasoner = reasonerFactory.createNonBufferingReasoner(root);
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        reasoner.dispose();
        
        System.out.println(Statistics.INSTANCE.getStatistics());
        System.out.println("Total time: "+Statistics.INSTANCE.getTotalTime());
    }

}
