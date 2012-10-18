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

final public class DuoFactory implements IFactory {

    final private IFactory base;
    final private IFactory overlay;

    final private int conceptThreshold;
    final private int roleThreshold;
    final private int featureThreshold;

    public DuoFactory(final IFactory base) {
        this.base = base;
        this.conceptThreshold = base.getTotalConcepts();
        this.roleThreshold = base.getTotalRoles();
        this.featureThreshold = base.getTotalFeatures();

        this.overlay = new Factory(conceptThreshold, roleThreshold,
                featureThreshold);
    }

    public boolean conceptExists(String key) {
        return base.conceptExists(key) || overlay.conceptExists(key);
    }

    public boolean roleExists(String key) {
        return base.roleExists(key) || overlay.roleExists(key);
    }

    public int getConcept(String key) {
        return base.conceptExists(key) ? base.getConcept(key) : overlay
                .getConcept(key);
    }

    public int getRole(String key) {
        return base.roleExists(key) ? base.getRole(key) : overlay.getRole(key);
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

    public String lookupConceptId(int id) {
        return id < conceptThreshold ? base.lookupConceptId(id) : overlay
                .lookupConceptId(id);
    }

    public String lookupRoleId(int id) {
        return id < roleThreshold ? base.lookupRoleId(id) : overlay
                .lookupRoleId(id);
    }

    public void printAll(PrintWriter writer) {
        base.printAll(writer);
        overlay.printAll(writer);
    }

    public void setVirtualConcept(int id, boolean isVirtual) {
        if (id < conceptThreshold) {
            throw new IllegalArgumentException(
                    "Cannot change status of base Concept: (" + id + ") "
                            + lookupConceptId(id));
        } else {
            overlay.setVirtualConcept(id, isVirtual);
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

    public boolean featureExists(String key) {
        // TODO Auto-generated method stub
        return false;
    }

    public int getFeature(String key) {
        // TODO Auto-generated method stub
        return 0;
    }

    public int getTotalFeatures() {
        // TODO Auto-generated method stub
        return 0;
    }

    public String lookupFeatureId(int id) {
        // TODO Auto-generated method stub
        return null;
    }

}
