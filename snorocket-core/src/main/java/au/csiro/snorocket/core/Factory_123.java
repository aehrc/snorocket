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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.LineReader;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Map&lt;String,Integer&gt;<br>
 * <br>
 * String becomes an array index int.<br>
 * Integer becomes an int.<br>
 * 
 */

final public class Factory_123 implements IFactory_123 {

    @SuppressWarnings("unused")
    private static final int CSIZE_ESTIMATE = 1000000; // :OLD: 3000
    @SuppressWarnings("unused")
    private static final int RSIZE_ESTIMATE = 128; // :OLD: 128

    // Increment 3rd place for upwards/backwards compatible change
    // Increment 2nd place for upwards compatible change
    // Increment 1st place for incompatible change
    private static final Object FILE_VERSION = "3.0.0";

    final private int conceptBase;
    final private int roleBase;

    // conceptNidArray must be provided in sorted order.
    private int conceptNidArray[] = null;

    // String identifier key, Integer conceptNidArry[] index
    private int firstConceptMoreIdx;
    // NIDs assigned automatically for String IDs start at zero.
    private int nextConceptNid = 0;
    final private HashMap<String, Integer> conceptMoreNameMap = new HashMap<String, Integer>();
    final private ArrayList<String> conceptMoreNames = new ArrayList<String>();

    final private IConceptSet virtualConcepts = new SparseConceptSet(
            1000 + 517249 + 1423);

    // roleNidArray must be provided in sorted order.
    private int roleNidArray[] = null;

    // <String identifier key, Integer roleNidArry[] index>
    private int firstRoleMoreIdx;
    // NIDs assigned automatically for String IDs start at zero.
    private int nextRoleNid = 0;
    final private HashMap<String, Integer> roleMoreNameMap = new HashMap<String, Integer>();
    final private ArrayList<String> roleMoreNames = new ArrayList<String>();

    final private RoleSet virtualRoles = new RoleSet();

    /**
     * index of the next available Concept
     */
    private int nextConceptIdx = 0;

    /**
     * index of the next available Role
     */
    private int nextRoleIdx = 0;

    public Factory_123(int[] conceptArray, int nextCIdx, int[] roleArray,
            int nextRIdx) {
        this(0, 0);

        conceptNidArray = conceptArray;
        roleNidArray = roleArray;
        nextConceptIdx = nextCIdx;
        nextRoleIdx = nextRIdx;

        firstConceptMoreIdx = nextCIdx;
        firstRoleMoreIdx = nextRIdx;

        final int top = getConceptIdx(TOP);
        final int bottom = getConceptIdx(BOTTOM);

        assert TOP_CONCEPT == top;
        assert BOTTOM_CONCEPT == bottom;
    }

    Factory_123(final int conceptBase, final int roleBase) {
        this.conceptBase = conceptBase;
        this.roleBase = roleBase;
    }

    public boolean conceptExists(final String key) {
        Integer result = conceptMoreNameMap.get(key);
        if (result == null)
            return false;
        else
            return true;
    }

    public boolean conceptExists(final int key) {
        // :OLD: return conceptNameMap.containsKey(key);
        int idx = Arrays.binarySearch(conceptNidArray, key);
        if (idx >= 0)
            return true;
        else
            return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#roleExists(java.lang.String)
     */
    public boolean roleExists(final int key) {
        // :OLD: return roleNameMap.containsKey(key);
        int idx = Arrays.binarySearch(roleNidArray, key);
        if (idx >= 0)
            return true;
        else
            return false;
    }

    public boolean roleExists(final String key) {
        Integer result = roleMoreNameMap.get(key);
        if (result == null)
            return false;
        else
            return true;
    }

    public int findConceptIdx(final int key) {
        return Arrays.binarySearch(conceptNidArray, key);
    }

    public int findRoleIdx(final int key) {
        return Arrays.binarySearch(roleNidArray, key);
    }

    public int getConceptIdx(final String key) {
        int idx = Integer.MAX_VALUE;
        Integer nid = conceptMoreNameMap.get(key);
        if (null == nid) {
            //
            if (nextConceptIdx == conceptNidArray.length) {
                final int[] newConcepts = new int[nextConceptIdx
                        + firstConceptMoreIdx];
                System.arraycopy(conceptNidArray, 0, newConcepts, 0,
                        nextConceptIdx);
                // Fill array to make binary search work correctly.
                Arrays.fill(newConcepts, nextConceptIdx, newConcepts.length,
                        Integer.MAX_VALUE);
                conceptNidArray = newConcepts;
                Snorocket.getLogger().info(
                        "::: conceptNidArray (Str) resized to: "
                                + conceptNidArray.length);
            }

            conceptNidArray[nextConceptIdx] = nextConceptNid;
            conceptMoreNameMap.put(key, nextConceptNid);
            conceptMoreNames.add(key);
            nextConceptNid++;

            idx = nextConceptIdx;
            nextConceptIdx++;

        } else {
            idx = findConceptIdx(nid);
            if (idx < 0)
                Snorocket
                        .getLogger()
                        .info(String.format(
                                "ERROR, getConceptIdx(%s) key not found ", key));
        }

        return idx + conceptBase;
    }

    public int getConceptIdx(final int key) {
        // Find index of key in the array
        int idx = Arrays.binarySearch(conceptNidArray, key);

        // If key is not found, then insert key to the array.
        if (idx < 0) {
            idx = -idx - 1; // insert_index = -idx - 1

            // If fits into the array....
            if (nextConceptIdx < conceptNidArray.length) {
                // Copy after insertion point (src.., dest.., length)
                System.arraycopy(conceptNidArray, idx, conceptNidArray,
                        idx + 1, nextConceptIdx - idx);

                // Insert value
                conceptNidArray[idx] = key;

            } else {
                // else grow memory linearly ...
                final int[] newConcepts = new int[nextConceptIdx + 500000];

                // Copy up to insertion index
                System.arraycopy(conceptNidArray, 0, newConcepts, 0, idx);

                // Insert value
                conceptNidArray[idx] = key;

                // Copy after insertion index
                // :!!!: v v v v
                System.arraycopy(conceptNidArray, idx, newConcepts, idx + 1,
                        nextConceptIdx - idx);

                // Fill remaining values
                Arrays.fill(newConcepts, nextConceptIdx, newConcepts.length,
                        Integer.MAX_VALUE);

                conceptNidArray = newConcepts;
                Snorocket.getLogger().info(
                        "::: ConceptNidArray (int) resized to: "
                                + conceptNidArray.length);
            }
            nextConceptIdx++;
        }

        // :OLD: return result + conceptBase;
        return idx + conceptBase;
    }

    /**
     * 1. Find in Map&lt;K/String, V/Integer&gt;<br>
     * 2. If not found get next available nextRIdx++, nextCIdx++; and ADD 3. Get
     * nextXIDx, resize array if needed. 4. ADD... resize map if needed.
     */
    public int getRoleIdx(final String key) {
        int idx = Integer.MAX_VALUE;
        Integer nid = roleMoreNameMap.get(key);
        if (null == nid) {
            //
            if (nextRoleIdx == roleNidArray.length) {
                final int[] newRoles = new int[nextRoleIdx + 128];
                System.arraycopy(roleNidArray, 0, newRoles, 0, nextRoleIdx);
                // Fill array to make binary search work correctly.
                Arrays.fill(newRoles, nextRoleIdx, newRoles.length,
                        Integer.MAX_VALUE);
                roleNidArray = newRoles;
                if (Snorocket.DEBUGGING)
                    Snorocket.getLogger().info(
                            "role resize to: " + roleNidArray.length);
            }

            roleNidArray[nextRoleIdx] = nextRoleNid;
            roleMoreNameMap.put(key, nextRoleNid);
            roleMoreNames.add(key);
            nextRoleNid++;

            idx = nextRoleIdx;
            nextRoleIdx++;

        } else {
            idx = findRoleIdx(nid);
            if (idx < 0)
                Snorocket.getLogger().info(
                        String.format("ERROR, getRoleIdx(%s) key not found ",
                                key));
        }

        return idx + roleBase;
    }

    public int getRoleIdx(final int key) {
        // Find index of key in the array
        int idx = Arrays.binarySearch(roleNidArray, key);

        // If key is not found, then insert key to the array.
        if (idx < 0) {
            idx = -idx - 1; // insert_index = -idx - 1

            // If needed, grow the array size.
            if (nextRoleIdx < roleNidArray.length) {
                // Copy after insertion point (src.., dest.., length)
                System.arraycopy(roleNidArray, idx, roleNidArray, idx + 1,
                        nextRoleIdx - idx);

                // Insert value
                roleNidArray[idx] = key;

            } else {
                // grow memory linearly
                final int[] newRoles = new int[nextRoleIdx + 50];

                // Copy up to insertion index
                System.arraycopy(roleNidArray, 0, newRoles, 0, idx);

                // Insert value
                roleNidArray[idx] = key;

                // Copy after insertion index
                // :!!!: v v v v
                System.arraycopy(roleNidArray, idx, newRoles, idx + 1,
                        nextRoleIdx - idx);

                // Fill remaining values
                Arrays.fill(newRoles, nextRoleIdx, newRoles.length,
                        Integer.MAX_VALUE);

                roleNidArray = newRoles;
                if (Snorocket.DEBUGGING)
                    Snorocket.getLogger().info(
                            "Role array resize to: " + roleNidArray.length);
            }
            nextRoleIdx++;
        }

        // :OLD: return result + roleBase;
        return idx + roleBase;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#getTotalConcepts()
     */
    public int getTotalConcepts() {
        return nextConceptIdx;
    }

    /*
     * (non-Javadoc)
     * 
     * @see au.csiro.snorocket.IFactory#getTotalRoles()
     */
    public int getTotalRoles() {
        return nextRoleIdx;
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
    public int lookupConceptId(final int id) {
        assert id >= conceptBase && id < nextConceptIdx + conceptBase;
        return conceptNidArray[id - conceptBase];
    }

    public String lookupConceptStrId(int id) {
        assert id >= conceptBase && id < nextConceptIdx + conceptBase;
        if (id < firstConceptMoreIdx)
            return String.valueOf(conceptNidArray[id - conceptBase]);
        else
            return conceptMoreNames.get(id - firstConceptMoreIdx);
    }

    public int lookupRoleId(final int id) {
        assert id >= roleBase && id < nextRoleIdx + roleBase;
        return roleNidArray[id - roleBase];
    }

    public String lookupRoleStrId(int id) {
        assert id >= roleBase && id < nextRoleIdx + roleBase;
        if (id < firstRoleMoreIdx)
            return String.valueOf(roleNidArray[id - roleBase]);
        else
            return roleMoreNames.get(id - firstRoleMoreIdx);
    }

    public void setVirtualConceptCIdx(int id, boolean isVirtual) {
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
            throw new IllegalStateException(
                    "Cannot convert virtual role into a non-virtual role.");
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

    public void printAll(PrintWriter writer) {
        writer.println(FILE_VERSION);
        writer.println(conceptBase);
        writer.println(roleBase);
        writer.println(getTotalConcepts());
        for (int i = 0; i < nextConceptIdx; i++) {
            writer.println(i + "\t" + conceptNidArray[i] + "\t"
                    + isVirtualConcept(i));
        }
        writer.println(getTotalRoles());
        for (int i = 0; i < nextRoleIdx; i++) {
            writer.println(i + "\t" + roleNidArray[i] + "\t" + isVirtualRole(i));
        }
    }

    static public Factory_123 loadAll(final BufferedReader br)
            throws IOException, ParseException {
        return loadAll(new LineReader(br));
    }

    static Factory_123 loadAll(final LineReader reader) throws IOException,
            ParseException {
        checkVersion(reader);

        // Read in offsets
        final int conceptBase = Integer.parseInt(reader.readLine());
        final int roleBase = Integer.parseInt(reader.readLine());

        final Factory_123 factory = new Factory_123(conceptBase, roleBase);

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
            if (id != factory.nextConceptIdx) {
                throw new AssertionError(
                        "Out of sequence error.  Expected id = "
                                + factory.nextConceptIdx + " got " + id);
            }
            // :NYI: line reader NIDs input not yet implemented & tests.
            String key = line.substring(idx1 + 1, idx2);
            int nid = Integer.parseInt(key);
            final int c = factory.getConceptIdx(nid);
            final boolean isVirtual = Boolean.parseBoolean(line
                    .substring(idx2 + 1));
            // yes, we could just pass isVirtual to the call and avoid this test
            // but there
            // is a not insignificant cost to calling setVirtualConcept
            // unnecessarily.
            if (isVirtual) {
                factory.setVirtualConceptCIdx(c, true);
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
            if (id != factory.nextRoleIdx) {
                throw new IllegalStateException(
                        "Out of sequence error.  Expected id = "
                                + factory.nextRoleIdx + " got " + id);
            }
            // :NYI: line reader NIDs input not yet implemented & tests.
            String key = line.substring(idx1 + 1, idx2);
            int nid = Integer.parseInt(key);
            final int r = factory.getRoleIdx(nid);
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

    public String toStringStats() {
        return " ++concepts=" + conceptMoreNames.size() + " ++roles="
                + roleMoreNames.size();
    }

    public int[] getConceptArray() {
        return conceptNidArray;
    }

    public int[] getRoleArray() {
        return roleNidArray;
    }
}
