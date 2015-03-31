/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.protege;

import org.protege.editor.owl.model.inference.AbstractProtegeOWLReasonerInfo;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

import au.csiro.snorocket.owlapi.SnorocketReasonerFactory;

/**
 * @author Alejandro Metke
 *
 */
public class ProtegeReasonerFactory extends AbstractProtegeOWLReasonerInfo {
    
    private SnorocketReasonerFactory factory = null;
    
    public BufferingMode getRecommendedBuffering() {
        return BufferingMode.BUFFERING;
    }
    
    public OWLReasonerFactory getReasonerFactory() {
        if(factory == null) {
            factory = new SnorocketReasonerFactory();
        }
        
        return factory;
    }

    @Override
    public void initialise() {
        
    }

    @Override
    public void dispose() {
        factory = null;
    }


}
