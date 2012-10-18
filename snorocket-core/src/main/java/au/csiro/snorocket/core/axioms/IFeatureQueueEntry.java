/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.axioms;

import au.csiro.snorocket.core.model.Datatype;

/**
 * Represents queue entry: -> f.(o, v)
 * 
 * @author Alejandro Metke
 * 
 */
public interface IFeatureQueueEntry {

    /**
     * Returns the datatype.
     * 
     * @return Datatype The datatype.
     */
    Datatype getD();

}
