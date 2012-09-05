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

package au.csiro.snorocket.snapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Interface to the snorocket classification engine for SNOMED CT
 * 
 * @author law223
 *
 */
public interface I_Snorocket_123 {

    /**
     * Specifies the roleId NID to use as indicating a subsumption relationship 
     *  
     * @param id the roleId to treat as the subsumption (<em>is a</em>) relationship.  May not be null
     * @throws IllegalArgumentException if the parameter {@code id} is null.
     */
    void setIsaNid(int nid);
    
    /**
     * Specifies that the role NID should never be grouped
     *  
     * @param id a roleId that should not be automatically grouped.  May not be null
     * @throws IllegalArgumentException if the parameter {@code id} is null.
     */
    void setRoleNeverGrouped(int nid);

    /**
     * Specifies that all descendants of the role (and the role itself if {@code inclusive} is true) are to be considered as roles. 
     *  
     * @param id a roleId for whom all descendants are to be considered as roles.  May not be null
     * @param inclusive true if {@code id} should also be considered as a role.
     * @throws IllegalArgumentException if the parameter {@code id} is null.
     */
    void setRoleRoot(int nid, boolean inclusive);

    /**
     * Specifies root concept.<br> 
     * <br>
     *  NOTE: the root concept must already have fixed position in the concept array.
     *  
     * @param nid a conceptNid for the root concept.
     */
    // void setConceptRoot(int nid); // NOT IMPLEMENTED

    /**
     * Identifies a concept and indicates whether or not it is fully defined or primitive.
     * 
     * In Description Logic terms, a <em>fullyDefined</em> concept specified as equivalent
     * to the (composition of) the relationships in which it appears as conceptId1
     * 
     * @param conceptId a unique identifier for the concept.
     * @param fullyDefined {@code true} if the concept is fully defined.
     */
    void addConcept(int conceptNid, boolean fullyDefined);
    
    /**
     * Identifies a concept and indicates whether or not it is fully defined or primitive.<br>
     * <br>
     * In Description Logic terms, a <em>fullyDefined</em> concept specified as equivalent
     * to the (composition of) the relationships in which it appears as conceptId1
     * 
     * @param conceptIdx is the <b>ABSOLUTE INDEX</b> for the concept.
     */
    void setConceptIdxAsDefined(int conceptIdx);
    
    /**
     * Specifies a relationship that holds between conceptId1 and conceptId2.
     * 
     * Semantically, this corresponds to a row in the SNOMED CT Relationships table.
     * This includes the meaning of the group id.
     *
     * <h2>Extension Ontologies</h2>
     * 
     * <em>Note:</em>
     * Consider a concept, FDC, that is fully defined: FDC == PC1 + PC2 + R.C2 which means
     * we have the following relationships:
     * <ol>
     * <li>FDC, ISA, PC1, 0
     * <li>FDC, ISA, PC2, 0
     * <li>FDC, R, C2, 0
     * </ol>
     * 
     * You <strong>cannot</strong> add new relationships for FDC in an extension ontology.
     * 
     * @param conceptId1 the focus concept.
     * @param roleId the relationship.
     * @param conceptId2 the relationshipValue.
     * @param group the group the relationship belongs to.
     */
    int addRelationship(int conceptId1, int roleId, int conceptId2, int group);

    /**
     * Specify additional role composition.
     * Use this for left and right identities and for reflexive and transitive roles. 
     * 
     * @param lhsIds lhsIds.length == 0 -> reflexive; == 1 -> role subtyping; >= 2 -> role composition.  May not be null
     * @param rhsId the roleId to treat as the subsumption (<em>is a</em>) relationship.  May not be null
     * @throws IllegalArgumentException if either parameter is null.
     */
    void addRoleComposition(int[] lhsNids, int rhsNid);

    /**
     * Causes the classification engine to construct the stated view in DL and perform classification.
     */
    void classify();

    /**
     * The callback is invoked for <em>all</em> <strong>stated</strong> and <strong>inferred</strong> relationships.
     * This <em>includes</em> the transitive closure of the subsumption hierarchy
     * and all subsumption relationships between equivalent concepts.
     * <p>
     * <em><strong>Note</strong>, exact behaviour varies when working with an incremental classification.</em>
     * 
     * @param callback the object whose {@code addRelationship} method is called.
     * @throws IllegalStateException if {@link #classify()} has not previously been called.
     */
    void getRelationships(I_Callback callback);

    /**
     * Returns the <em>non-redundant defining relationships</em> (i.e., SNOMED CT's distribution view).
     * Subsumption relationships between equivalent concepts will <em>not</em> be returned.
     * <p>
     * <em><strong>Note</strong>, exact behaviour varies when working with an incremental classification.</em>
     * <p>
     * The callback is invoked for only <em>some</em> <strong>stated</strong> and <strong>inferred</strong> relationships:
     * <ul>
     * <li> for subsumption (isa) relationships, only immediate parents are returned; and
     * <li> relationships that are subsumed by other relationships (or relationship groups) are omitted.
     * </ul>
     * 
     * @param callback the object whose {@code addRelationship} method is called.
     * @throws IllegalStateException if {@link #classify()} has not previously been called.
     */
    void getDistributionFormRelationships(I_Callback callback);

    interface I_Callback {
        void addRelationship(int conceptNid1, int roleNid, int conceptNid2, int group);
    }

    /**
     * The callback is invoked for each set of equivalent concepts.
     * <p>
     * <em><strong>Note</strong>, exact behaviour varies when working with an incremental classification.</em>
     * 
     * @param callback the object whose {@code equivalent} method is called
     * @throws IllegalStateException if {@link #classify()} has not previously been called.
     */
    public void getEquivalents(I_EquivalentCallback callback);
    
    interface I_EquivalentCallback {
        void equivalent(ArrayList<Integer> equivalentConcepts);
    }

    /**
     * Returns the serialised internal state of the classification engine.
     * 
     * @return an {@link InputStream} that represents a serialisation of the internal state of the classifier after classification
     *         suitable for later deserialisation.
     * @throws IOException if an I/O error occurs.
     */
    InputStream getStream() throws IOException;

    /**
     * Create a new classification engine that can be used to construct and incrementally classify
     * an extension to the base ontology embodied by this classification engine.
     * 
     * @return a classification engine to which new concepts and relationship can be added and incrementally classified.
     */
    I_Snorocket_123 createExtension();

    /**
     * The callback is invoked query internal concept data.
     */
    public void getInternalDataCon(I_InternalDataConCallback callback);
    
    interface I_InternalDataConCallback {
        void processConData(int cId);
    }

    /**
     * The callback is invoked query internal relationship data.
     */
    public void getInternalDataRel(I_InternalDataRelCallback callback);
    
    interface I_InternalDataRelCallback {
        void processRelData(int c1Id, int roleId, int c2Id, int group);
    }

    /**
     * The callback is invoked query internal role data.
     */
    public void getInternalDataRole(I_InternalDataRoleCallback callback);
    
    interface I_InternalDataRoleCallback {
        void processRoleData(int cId);
    }

}
