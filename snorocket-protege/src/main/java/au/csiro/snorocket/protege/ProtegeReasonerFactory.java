/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.protege;

import org.protege.editor.owl.model.inference.AbstractProtegeOWLReasonerInfo;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;

/**
 * @author Alejandro Metke
 *
 */
public class ProtegeReasonerFactory extends AbstractProtegeOWLReasonerInfo {
    
    private SnorocketReasonerFactory factory = null;
    
    @Override
    public BufferingMode getRecommendedBuffering() {
        return BufferingMode.BUFFERING;
    }
    
    @Override
    public OWLReasonerFactory getReasonerFactory() {
        if(factory == null) {
            factory = new SnorocketReasonerFactory();
        }
        
        return factory;
    }

    @Override
    public void initialise() throws Exception {
        
    }

    @Override
    public void dispose() throws Exception {
        
    }

}
