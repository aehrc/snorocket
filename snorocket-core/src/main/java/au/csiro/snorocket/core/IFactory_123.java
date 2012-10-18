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

public interface IFactory_123 {

    // INDENTIFIER VALUE
    public static final int TOP = Integer.MIN_VALUE + 1; // "_top_" root concept
    public static final int BOTTOM = Integer.MIN_VALUE + 2; // "_bottom_"
    public static final String ROLE_GROUP = "_role_group_";

    // ARRAY INDEX
    public static final int TOP_CONCEPT = 0; // 0 index of root concept
    public static final int BOTTOM_CONCEPT = 1; // 1 index

    boolean conceptExists(final String key);

    boolean conceptExists(final int key);

    boolean roleExists(final String key);

    boolean roleExists(final int key);

    int findConceptIdx(final int key);

    int findRoleIdx(final int key);

    int getConceptIdx(final String key);

    int getConceptIdx(final int key);

    int[] getConceptArray();

    int getRoleIdx(final String key);

    int getRoleIdx(final int key);

    int[] getRoleArray();

    int getTotalConcepts();

    int getTotalRoles();

    int lookupConceptId(final int id);

    String lookupConceptStrId(final int id);

    // IN: id is idx; OUT: NID
    int lookupRoleId(final int id);

    String lookupRoleStrId(final int id);

    boolean isBaseConcept(int id);

    boolean isBaseRole(int id);

    void setVirtualConceptCIdx(int id, boolean isVirtual);

    boolean isVirtualConcept(int id);

    void setVirtualRole(int id, boolean isVirtual);

    boolean isVirtualRole(int id);

    void printAll(PrintWriter writer);

    String toStringStats();

}
