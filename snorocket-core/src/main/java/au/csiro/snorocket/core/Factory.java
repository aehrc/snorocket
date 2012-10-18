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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.LineReader;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

final public class Factory implements IFactory {

    private static final int SIZE_ESTIMATE = 3000; // + 517249 + 1423;

    // Increment 3rd place for upwards/backwards compatible change
    // Increment 2nd place for upwards compatible change
    // Increment 1st place for incompatible change
    private static final Object FILE_VERSION = "3.0.0";

    final private int conceptBase;
    final private int roleBase;
    final private int featureBase;

    private String[] concepts = new String[SIZE_ESTIMATE];
    final private Map<String, Integer> conceptNameMap = new HashMap<String, Integer>();
    final private IConceptSet virtualConcepts = new SparseConceptSet(
            1000 + 517249 + 1423);

    private String[] roles = new String[128]; // Sufficient for FULL-GALEN
    final private Map<String, Integer> roleNameMap = new HashMap<String, Integer>();
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

    public Factory() {
        this(0, 0, 0);

        final int top = getConcept(TOP);
        final int bottom = getConcept(BOTTOM);

        assert TOP_CONCEPT == top;
        assert BOTTOM_CONCEPT == bottom;
    }

    Factory(final int conceptBase, final int roleBase, final int featureBase) {
        this.conceptBase = conceptBase;
        this.roleBase = roleBase;
        this.featureBase = featureBase;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#conceptExists(java.lang.String)
     */
    public boolean conceptExists(final String key) {
        return conceptNameMap.containsKey(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#roleExists(java.lang.String)
     */
    public boolean roleExists(final String key) {
        return roleNameMap.containsKey(key);
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#getConcept(java.lang.String)
     */
    public int getConcept(final String key) {
        if (null == key) {
            throw new IllegalArgumentException("Concept key must not be null");
        }

        Integer result = conceptNameMap.get(key);
        if (null == result) {
            if (conceptIdCounter == concepts.length) {
                final String[] newConcepts = new String[conceptIdCounter * 2];
                System.arraycopy(concepts, 0, newConcepts, 0, conceptIdCounter);
                concepts = newConcepts;
                if (Snorocket.DEBUGGING)
                    Snorocket.getLogger().info(
                            "concept resize to: " + concepts.length);
            }
            concepts[conceptIdCounter] = key;
            result = conceptIdCounter++;
            conceptNameMap.put(key, result);
        }
        return result + conceptBase;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#getRole(java.lang.String)
     */
    public int getRole(final String key) {
        Integer result = roleNameMap.get(key);
        if (null == result) {
            if (roleIdCounter == roles.length) {
                final String[] newRoles = new String[roleIdCounter * 2];
                System.arraycopy(roles, 0, newRoles, 0, roleIdCounter);
                roles = newRoles;
                if (Snorocket.DEBUGGING) {
                    Snorocket.getLogger().info(
                            "role resize to: " + roles.length);
                }
            }
            roles[roleIdCounter] = key;
            result = roleIdCounter++;
            roleNameMap.put(key, result);
        }
        return result + roleBase;
    }

    public int getTotalConcepts() {
        return conceptIdCounter;
    }

    public int getTotalRoles() {
        return roleIdCounter;
    }

    public boolean featureExists(String key) {
        return featureNameMap.containsKey(key);
    }

    public int getFeature(String key) {
        Integer result = featureNameMap.get(key);
        if (null == result) {
            if (featureIdCounter == features.length) {
                final String[] newFeatures = new String[featureIdCounter * 2];
                System.arraycopy(features, 0, newFeatures, 0, featureIdCounter);
                features = newFeatures;
                if (Snorocket.DEBUGGING) {
                    Snorocket.getLogger().info(
                            "feature resize to: " + features.length);
                }
            }
            features[featureIdCounter] = key;
            result = featureIdCounter++;
            featureNameMap.put(key, result);
        }
        return result + featureBase;
    }

    public int getTotalFeatures() {
        return featureIdCounter;
    }

    public String lookupFeatureId(int id) {
        assert id >= featureBase && id <= featureIdCounter + featureBase;
        return features[id - featureBase];
    }

    public boolean isBaseConcept(int id) {
        return false;
    }

    public boolean isBaseRole(int id) {
        return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#lookupConceptId(int)
     */
    public String lookupConceptId(final int id) {
        assert id >= conceptBase && id <= conceptIdCounter + conceptBase;
        return concepts[id - conceptBase];
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#lookupRoleId(int)
     */
    public String lookupRoleId(final int id) {
        // FIXME: why is this assertion failing with Endocarditis example +
        // logging
        // assert id >= roleBase && id <= roleIdCounter + roleBase;
        return roles[id - roleBase];
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#setVirtualConcept(int, boolean)
     */
    public void setVirtualConcept(int id, boolean isVirtual) {
        if (isVirtual) {
            virtualConcepts.add(id - conceptBase);
        } else {
            virtualConcepts.remove(id - conceptBase);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#isVirtualConcept(int)
     */
    public boolean isVirtualConcept(int id) {
        return virtualConcepts.contains(id - conceptBase);
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#setVirtualRole(int, boolean)
     */
    public void setVirtualRole(int id, boolean isVirtual) {
        if (isVirtual) {
            virtualRoles.add(id - roleBase);
        } else if (virtualRoles.contains(id - roleBase)) {
            throw new IllegalStateException("Cannot convert virtual role "
                    + "into a non-virtual role.");
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#isVirtualRole(int)
     */
    public boolean isVirtualRole(int id) {
        return virtualRoles.contains(id - roleBase);
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#printAll(java.io.PrintWriter)
     */
    public void printAll(PrintWriter writer) {
        writer.println(FILE_VERSION);
        writer.println(conceptBase);
        writer.println(roleBase);
        writer.println(getTotalConcepts());
        for (int i = 0; i < conceptIdCounter; i++) {
            writer.println(i + "\t" + concepts[i] + "\t" + isVirtualConcept(i));
        }
        writer.println(getTotalRoles());
        for (int i = 0; i < roleIdCounter; i++) {
            writer.println(i + "\t" + roles[i] + "\t" + isVirtualRole(i));
        }
    }

    static public IFactory loadAll(final BufferedReader br) throws IOException,
            ParseException {
        return loadAll(new LineReader(br));
    }

    static IFactory loadAll(final LineReader reader) throws IOException,
            ParseException {
        checkVersion(reader);

        // Read in offsets
        final int conceptBase = Integer.parseInt(reader.readLine());
        final int roleBase = Integer.parseInt(reader.readLine());

        // TODO: update this to read featureBase...
        final Factory factory = new Factory(conceptBase, roleBase, 0);

        // LOAD CONCEPTS...
        final int numConcepts = Integer.parseInt(reader.readLine());
        Snorocket.getLogger().fine("loading " + numConcepts + " concepts ...");

        // // Skip over TOP and BOTTOM
        // reader.readLine();
        // reader.readLine();

        for (int i = 0; i < numConcepts; i++) {
            final String line = reader.readLine();
            if (null == line) {
                throw new AssertionError("EOF reached unexpectedly.  "
                        + (numConcepts - i) + " more concepts expected.");
            }
            final int idx1 = line.indexOf('\t');
            final int idx2 = line.indexOf('\t', idx1 + 1);
            final int id = Integer.parseInt(line.substring(0, idx1));
            if (id != factory.conceptIdCounter) {
                throw new AssertionError(
                        "Out of sequence error.  Expected id = "
                                + factory.conceptIdCounter + " got " + id);
            }
            final int c = factory.getConcept(line.substring(idx1 + 1, idx2));
            final boolean isVirtual = Boolean.parseBoolean(line
                    .substring(idx2 + 1));
            // yes, we could just pass isVirtual to the call and avoid this test
            // but there
            // is a not insignificant cost to calling setVirtualConcept
            // unnecessarily.
            if (isVirtual) {
                factory.setVirtualConcept(c, true);
            }
        }

        // LOAD ROLES...
        final int numRoles = Integer.parseInt(reader.readLine());
        Snorocket.getLogger().fine("loading " + numRoles + " roles ...");

        for (int i = 0; i < numRoles; i++) {
            final String line = reader.readLine();
            if (null == line) {
                throw new AssertionError("EOF reached unexpectedly.  "
                        + (numConcepts - i) + " more concepts expected.");
            }
            final int idx1 = line.indexOf('\t');
            final int idx2 = line.indexOf('\t', idx1 + 1);
            final int id = Integer.parseInt(line.substring(0, idx1));
            if (id != factory.roleIdCounter) {
                throw new IllegalStateException(
                        "Out of sequence error.  Expected id = "
                                + factory.roleIdCounter + " got " + id);
            }
            final int r = factory.getRole(line.substring(idx1 + 1, idx2));
            factory.setVirtualRole(r,
                    Boolean.parseBoolean(line.substring(idx2 + 1)));
        }

        return factory;
    }

    private static void checkVersion(LineReader reader) throws IOException,
            ParseException {
        String line = reader.readLine(); // Read file version
        if (!FILE_VERSION.equals(line)) {
            throw new ParseException("Unsupported file format version, found "
                    + line + ", expected " + FILE_VERSION + " or compatible.",
                    reader);
        }
    }

}
