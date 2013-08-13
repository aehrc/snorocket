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

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import au.csiro.ontology.model.NamedConcept;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

final public class CoreFactory implements IFactory {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Logger.
     */
    private final static Logger log = Logger.getLogger(CoreFactory.class);
    
    private static final int SIZE_ESTIMATE = 3000;

    final private int conceptBase;
    final private int roleBase;
    final private int featureBase;

    private Object[] concepts = new Object[SIZE_ESTIMATE];
    final private Map<Object, Integer> conceptMap = new HashMap<Object, Integer>();
    final private IConceptSet virtualConcepts = new SparseConceptSet(SIZE_ESTIMATE);

    private Object[] roles = new Object[128];
    final private Map<Object, Integer> roleMap = new HashMap<Object, Integer>();
    final private RoleSet virtualRoles = new RoleSet();

    private String[] features = new String[128];
    final private Map<String, Integer> featureNameMap = new HashMap<String, Integer>();

    /**
     * Index of the next available Concept.
     */
    private int conceptIdCounter = 0;

    /**
     * Index of the next available Role.
     */
    private int roleIdCounter = 0;

    /**
     * Index of the next available Feature.
     */
    private int featureIdCounter = 0;
    
    /**
     * Creates a new factory.
     * 
     * @param top The object to represent top.
     * @param bottom The object to represent bottom.
     * @param roleGroup The object to represent role group.
     */
    public CoreFactory() {
        this(0, 0, 0);

        final int topId = getConcept(NamedConcept.TOP);
        final int bottomId = getConcept(NamedConcept.BOTTOM);

        assert TOP_CONCEPT == topId;
        assert BOTTOM_CONCEPT == bottomId;
    }

    CoreFactory(final int conceptBase, final int roleBase, final int featureBase) {
        this.conceptBase = conceptBase;
        this.roleBase = roleBase;
        this.featureBase = featureBase;
    }
    
    public int getTotalConcepts() {
        return conceptIdCounter;
    }
    
    public int getTotalRoles() {
        return roleIdCounter;
    }
    
    public int getTotalFeatures() {
        return featureIdCounter;
    }

    public String lookupFeatureId(int id) {
        assert id >= featureBase && id <= featureIdCounter + featureBase;
        return features[id - featureBase];
    }

    public Object lookupConceptId(final int id) {
        assert id >= conceptBase && id <= conceptIdCounter + conceptBase;
        return concepts[id - conceptBase];
    }

    public Object lookupRoleId(final int id) {
        assert id >= roleBase && id <= roleIdCounter + roleBase;
        return roles[id - roleBase];
    }

    public boolean isVirtualConcept(int id) {
        return virtualConcepts.contains(id - conceptBase);
    }

    public boolean isVirtualRole(int id) {
        return virtualRoles.contains(id - roleBase);
    }

    public boolean conceptExists(Object key) {
        return conceptMap.containsKey(key);
    }

    public boolean roleExists(Object key) {
        return roleMap.containsKey(key);
    }

    public boolean featureExists(String key) {
        return featureNameMap.containsKey(key);
    }

    public int getConcept(Object key) {
        if (null == key) {
            throw new IllegalArgumentException("Concept key must not be null");
        }

        Integer result = conceptMap.get(key);
        if (null == result) {
            if (conceptIdCounter == concepts.length) {
                final Object[] newConcepts = new Object[conceptIdCounter * 2];
                System.arraycopy(concepts, 0, newConcepts, 0, conceptIdCounter);
                concepts = newConcepts;
                if (log.isTraceEnabled())
                    log.trace("Resizing concepts array to: " + concepts.length);
            }
            concepts[conceptIdCounter] = key;
            result = conceptIdCounter++;
            conceptMap.put(key, result);
        }
        return result + conceptBase;
    }

    public int getRole(Object key) {
        Integer result = roleMap.get(key);
        if (null == result) {
            if (roleIdCounter == roles.length) {
                final Object[] newRoles = new Object[roleIdCounter * 2];
                System.arraycopy(roles, 0, newRoles, 0, roleIdCounter);
                roles = newRoles;
                if (log.isTraceEnabled()) {
                    log.trace("role resize to: " + roles.length);
                }
            }
            roles[roleIdCounter] = key;
            result = roleIdCounter++;
            roleMap.put(key, result);
        }
        return result + roleBase;
    }

    public int getFeature(String key) {
        Integer result = featureNameMap.get(key);
        if (null == result) {
            if (featureIdCounter == features.length) {
                final String[] newFeatures = new String[featureIdCounter * 2];
                System.arraycopy(features, 0, newFeatures, 0, featureIdCounter);
                features = newFeatures;
                if (log.isTraceEnabled()) {
                    log.trace("feature resize to: " + features.length);
                }
            }
            features[featureIdCounter] = key;
            result = featureIdCounter++;
            featureNameMap.put(key, result);
        }
        return result + featureBase;
    }
    
    public void setVirtualConcept(int id, boolean isVirtual) {
        if (isVirtual) {
            virtualConcepts.add(id - conceptBase);
        } else {
            virtualConcepts.remove(id - conceptBase);
        }
    }

}
