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

import java.io.PrintWriter;

public interface IFactory {

    public static final String TOP = "_top_";
    public static final String BOTTOM = "_bottom_";
    // FIXME: this shouldn't be here... is specific to SNOMED
    public static final String ROLE_GROUP = "_role_group_";

    public static final int TOP_CONCEPT = 0;
    public static final int BOTTOM_CONCEPT = 1;

    boolean conceptExists(final String key);

    boolean roleExists(final String key);

    boolean featureExists(final String key);

    int getConcept(final String key);

    int getRole(final String key);

    int getFeature(final String key);

    int getTotalConcepts();

    int getTotalRoles();

    int getTotalFeatures();

    String lookupConceptId(final int id);

    String lookupRoleId(final int id);

    String lookupFeatureId(final int id);

    boolean isBaseConcept(int id);

    boolean isBaseRole(int id);

    void setVirtualConcept(int id, boolean isVirtual);

    boolean isVirtualConcept(int id);

    void setVirtualRole(int id, boolean isVirtual);

    boolean isVirtualRole(int id);

    void printAll(PrintWriter writer);

}
