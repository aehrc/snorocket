/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.owlapi;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.IllegalConfigurationException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * Factory used to create the OWL-API version of the Snorocket reasoner.
 * 
 * @author Alejandro Metke
 *
 */
public class SnorocketReasonerFactory implements OWLReasonerFactory {

    public String getReasonerName() {
        return SnorocketReasonerFactory.class.getPackage().getImplementationTitle();
    }

    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
        return new SnorocketOWLReasoner(ontology, null, false);
    }

    public OWLReasoner createReasoner(OWLOntology ontology) {
        return new SnorocketOWLReasoner(ontology, null, true);
    }

    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology,
            OWLReasonerConfiguration config)
                    throws IllegalConfigurationException {
        return new SnorocketOWLReasoner(ontology, config, false);
    }

    public OWLReasoner createReasoner(OWLOntology ontology,
            OWLReasonerConfiguration config)
                    throws IllegalConfigurationException {
        return new SnorocketOWLReasoner(ontology, config, true);
    }

}
