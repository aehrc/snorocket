/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.protege;

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

    @Override
    public String getReasonerName() {
        return SnorocketReasonerFactory.class.getPackage().getImplementationTitle();
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology) {
        return new SnorocketOWLReasoner(ontology, null, false);
    }

    @Override
    public OWLReasoner createReasoner(OWLOntology ontology) {
        return new SnorocketOWLReasoner(ontology, null, true);
    }

    @Override
    public OWLReasoner createNonBufferingReasoner(OWLOntology ontology,
            OWLReasonerConfiguration config)
                    throws IllegalConfigurationException {
        return new SnorocketOWLReasoner(ontology, config, false);
    }

    @Override
    public OWLReasoner createReasoner(OWLOntology ontology,
            OWLReasonerConfiguration config)
                    throws IllegalConfigurationException {
        return new SnorocketOWLReasoner(ontology, config, true);
    }

}
