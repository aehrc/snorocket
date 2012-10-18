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

final public class DuoFactory_123 implements IFactory_123 {

    final private IFactory_123 base;
    final private IFactory_123 overlay;

    final private int conceptThreshold;
    final private int roleThreshold;

    public DuoFactory_123(final IFactory_123 base) {
        this.base = base;
        this.conceptThreshold = base.getTotalConcepts();
        this.roleThreshold = base.getTotalRoles();

        this.overlay = new Factory_123(conceptThreshold, roleThreshold);
    }

    public boolean conceptExists(String key) {
        return base.conceptExists(key) || overlay.conceptExists(key);
    }

    public boolean conceptExists(int key) {
        return base.conceptExists(key) || overlay.conceptExists(key);
    }

    public boolean roleExists(String key) {
        return base.roleExists(key) || overlay.roleExists(key);
    }

    public boolean roleExists(int key) {
        return base.roleExists(key) || overlay.roleExists(key);
    }

    public int getConceptIdx(int key) {
        return base.conceptExists(key) ? base.getConceptIdx(key) : overlay
                .getConceptIdx(key);
    }

    public int getRoleIdx(int key) {
        return base.roleExists(key) ? base.getRoleIdx(key) : overlay
                .getRoleIdx(key);
    }

    public int getTotalConcepts() {
        return conceptThreshold + overlay.getTotalConcepts();
    }

    public int getTotalRoles() {
        return roleThreshold + overlay.getTotalRoles();
    }

    public boolean isBaseConcept(int id) {
        return id < conceptThreshold;
    }

    public boolean isBaseRole(int id) {
        return id < roleThreshold;
    }

    public boolean isVirtualConcept(int id) {
        return id < conceptThreshold ? base.isVirtualConcept(id) : overlay
                .isVirtualConcept(id);
    }

    public boolean isVirtualRole(int id) {
        return id < roleThreshold ? base.isVirtualRole(id) : overlay
                .isVirtualRole(id);
    }

    public int lookupConceptId(int id) {
        return id < conceptThreshold ? base.lookupConceptId(id) : overlay
                .lookupConceptId(id);
    }

    public String lookupConceptStrId(int id) {
        return id < conceptThreshold ? base.lookupConceptStrId(id) : overlay
                .lookupConceptStrId(id);
    }

    public int lookupRoleId(int id) {
        return id < roleThreshold ? base.lookupRoleId(id) : overlay
                .lookupRoleId(id);
    }

    public String lookupRoleStrId(int id) {
        return id < roleThreshold ? base.lookupRoleStrId(id) : overlay
                .lookupRoleStrId(id);
    }

    public void printAll(PrintWriter writer) {
        base.printAll(writer);
        overlay.printAll(writer);
    }

    public void setVirtualConceptCIdx(int id, boolean isVirtual) {
        if (id < conceptThreshold) {
            throw new IllegalArgumentException(
                    "Cannot change status of base Concept: (" + id + ") "
                            + lookupConceptId(id));
        } else {
            overlay.setVirtualConceptCIdx(id, isVirtual);
        }
    }

    public void setVirtualRole(int id, boolean isVirtual) {
        if (id < roleThreshold) {
            throw new IllegalArgumentException(
                    "Cannot change status of base Role: (" + id + ") "
                            + lookupRoleId(id));
        } else {
            overlay.setVirtualRole(id, isVirtual);
        }
    }

    public int findConceptIdx(int key) {
        return base.conceptExists(key) ? base.findConceptIdx(key) : overlay
                .findConceptIdx(key);
    }

    public int findRoleIdx(int key) {
        return base.roleExists(key) ? base.findRoleIdx(key) : overlay
                .findRoleIdx(key);
    }

    public int getConceptIdx(String key) {
        return base.conceptExists(key) ? base.getConceptIdx(key) : overlay
                .getConceptIdx(key);
    }

    public int getRoleIdx(String key) {
        return base.roleExists(key) ? base.getRoleIdx(key) : overlay
                .getRoleIdx(key);
    }

    public String toStringStats() {
        return " DuoFactory base=" + base + " overlay=" + overlay;
    }

    public int[] getConceptArray() {
        throw new UnsupportedOperationException();
    }

    public int[] getRoleArray() {
        throw new UnsupportedOperationException();
    }

}
