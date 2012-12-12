/**
 * Copyright (c) 2009 International Health Terminology Standards Development
 * Organisation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */

package au.csiro.snorocket.core;

import java.io.Serializable;

/**
 * Interface for the factory used to create concepts, roles and features
 * internally.
 * 
 * @author Alejandro Metke
 *
 * @param <T> The typeof the key used in the factory. The type should support
 * identity based on equals and hashcode.
 */
public interface IFactory<T> extends Serializable {
    
    /**
     * Internal id used for the top concept.
     */
    public static final int TOP_CONCEPT = 0;
    
    /**
     * Internal id used for the bottom concept.
     */
    public static final int BOTTOM_CONCEPT = 1;
    
    /**
     * Indicates if a concept, either named or virtual, exists.
     * 
     * @param key
     * @return
     */
    boolean conceptExists(final Object key);

    /**
     * Indicates if a role, either named or virtual, exists.
     * 
     * @param key
     * @return
     */
    boolean roleExists(final Object key);
    
    /**
     * Indicates if a feature exists.
     * 
     * @param key
     * @return
     */
    boolean featureExists(final T key);
    
    /**
     * Returns the internal id of a concept.
     * 
     * @param key
     * @return
     */
    int getConcept(final Object key);
    
    /**
     * Returns the internal id of a role.
     * 
     * @param key
     * @return
     */
    int getRole(final Object key);

    /**
     * Returns the internal id of a feature.
     * 
     * @param key
     * @return
     */
    int getFeature(final T key);
    
    /**
     * Returns the total number of concepts.
     * 
     * @return
     */
    int getTotalConcepts();
    
    /**
     * Returns the total number of roles.
     * 
     * @return
     */
    int getTotalRoles();
    
    /**
     * Returns the total number of features.
     * 
     * @return
     */
    int getTotalFeatures();
    
    /**
     * Returns the external id of a concept given its internal id.
     * 
     * @param id
     * @return
     */
    T lookupConceptId(final int id);
    
    /**
     * Returns the external id of a role given its internal id.
     * 
     * @param id
     * @return
     */
    T lookupRoleId(final int id);
    
    /**
     * Returns a feature given its id.
     * 
     * @param id
     * @return
     */
    T lookupFeatureId(final int id);
    
    /**
     * Indicates if a concept, identified by its internal id, is virtual or
     * named.
     * 
     * @param id
     * @return
     */
    boolean isVirtualConcept(int id);
    
    /**
     * Indicates if a role, identified by its internal id, is virtual or named.
     * 
     * @param id
     * @return
     */
    boolean isVirtualRole(int id);
    
    /**
     * Flags a concept as virtual.
     * 
     * @param id
     * @param isVirtual
     */
    public void setVirtualConcept(int id, boolean isVirtual);

}
