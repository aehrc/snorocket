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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

//import au.csiro.snorocket.core.Factory; :!!!:zzz:
import au.csiro.snorocket.core.Factory_123;
import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.core.NormalisedOntology_123;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.PostProcessedData_123;
import au.csiro.snorocket.core.R;
import au.csiro.snorocket.core.NormalisedOntology_123.Classification;
import au.csiro.snorocket.core.axioms.GCI_123;
import au.csiro.snorocket.core.axioms.Inclusion_123;
import au.csiro.snorocket.core.axioms.RI_123;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;

public class Snorocket_123 implements I_Snorocket_123 {

    // Increment 3rd place for upwards/backwards compatible change
    // Increment 2nd place for upwards compatible change
    // Increment 1st place for incompatible change
    private static final String FILE_VERSION = "1.0.0";

    private static final int INCLUSIVE = 0;
    private static final int EXCLUSIVE = 1;

    private static final Logger LOGGER = au.csiro.snorocket.core.Snorocket.getLogger();

    private int rootConceptNid = Integer.MAX_VALUE;
    // :OLD: String isaId
    private int isaNid = Integer.MAX_VALUE;
    transient private int isaRIdx;
    transient final private int roleGroupRIdx;

    private Classification classification = null;
    private PostProcessedData_123 postProcessedData = null;

    final private List<Row> rowList = new ArrayList<Row>();

    transient private IConceptSet ungroupedRoleIdxs = new SparseConceptSet();

    /**
    * The Concept ids for the roots of the role hierarchy.
    * roleRootCIdxs[INCLUSIVE] and roleRootCIdxs[EXCLUSIVE]
    */
    transient protected Set roleRootCIdxs[] = { new HashSet<Integer>(), new HashSet<Integer>() };

    transient private Collection<RI_123> roleCompositions = new ArrayList<RI_123>();

    transient final private Classification baseClassification;

    transient final protected IFactory_123 factory;

    transient private IConceptSet fullyDefinedCIdxs = new SparseConceptSet();

    transient private int nestedRoleGroupCount;

    public Snorocket_123(int[] conceptArray, int nextCIdx, int[] roleArray, int nextRIdx,
            int rootCNid) {
        LOGGER.info("::: Snorocket_123(int[], int, int[], int, int)");
        baseClassification = null;
        factory = new Factory_123(conceptArray, nextCIdx, roleArray, nextRIdx);
        roleGroupRIdx = factory.getRoleIdx(IFactory_123.ROLE_GROUP); // :@@@:
        // "roleGroup"
        rootConceptNid = rootCNid;

        assert factory.conceptExists(rootCNid);
    }

    // called by createExtension
    protected Snorocket_123(final Classification classification, int isaNid) {
        LOGGER.info("::: Snorocket_123(final Classification classification, int isaNid)");
        if (null == classification) {
            throw new IllegalArgumentException("classification can not be null");
        }

        baseClassification = classification;
        factory = baseClassification.getExtensionFactory();
        roleGroupRIdx = factory.getRoleIdx(IFactory_123.ROLE_GROUP); // :@@@:
        // "roleGroup"
        setIsaNid(isaNid);
    }

    /**
     * Pre-load from stored state
     * 
     * @param snomedVersion
     * @throws RuntimeException
     *             if version is not available
     */
    public Snorocket_123(InputStream state) {
        LOGGER.info("::: Snorocket_123(InputStream state)");
        try {
            baseClassification = null;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(state));

            classification = NormalisedOntology_123.loadClassification(reader);
            factory = classification.getExtensionFactory();
            roleGroupRIdx = factory.getRoleIdx(IFactory_123.ROLE_GROUP); // :@@@:
            // "roleGroup"

            final String isaConcept = reader.readLine();
            setIsaNid(Integer.parseInt(isaConcept));

            String line = reader.readLine();
            if (null != line) {

                if (!line.contains(".")) { // no '.'s means not a version string
                    // Handle deprecated file format in an unreleased version.
                    // TODO consider removing code for this rare case at end of
                    // 2009
                    final int roleRootCount = Integer.parseInt(line);
                    for (int i = 0; i < roleRootCount; i++) {
                        String roleRoot = reader.readLine();
                        setRoleRoot(Integer.parseInt(roleRoot), false);
                    }
                } else {
                    // check compatible version
                    if (!FILE_VERSION.equals(line)) {
                        // TODO - choose a better exception to throw
                        throw new Error(
                                "Malformed SNOMED Resource: Unsupported file format version, found "
                                        + line + ", expected " + FILE_VERSION + " or compatible.");
                    }

                    { // ungrouped roles
                        line = reader.readLine();
                        final int count = Integer.parseInt(line);
                        for (int i = 0; i < count; i++) {
                            String role = reader.readLine();
                            ungroupedRoleIdxs.add(Integer.parseInt(role));
                        }
                    }

                    { // role roots INCLUSIVE
                        line = reader.readLine();
                        final int count = Integer.parseInt(line);
                        for (int i = 0; i < count; i++) {
                            line = reader.readLine();
                            roleRootCIdxs[INCLUSIVE].add(Integer.parseInt(line));
                        }
                    }
                    { // role roots EXCLUSIVE
                        line = reader.readLine();
                        final int count = Integer.parseInt(line);
                        for (int i = 0; i < count; i++) {
                            line = reader.readLine();
                            roleRootCIdxs[EXCLUSIVE].add(Integer.parseInt(line));
                        }
                    }

                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new Error("Malformed SNOMED Resource: " + e.getLocalizedMessage(), e);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new Error("Malformed SNOMED Resource: " + e.getLocalizedMessage(), e);
        }
    }

    public void setIsaNid(int nid) {
        int rIdx = factory.getRoleIdx(nid);

        if (rIdx < 0) {
            String message = String.format("ERROR, setIsa(%d) concept not defined as role ", nid);
            LOGGER.info(message);
            return;
        }

        isaNid = nid; // isaNid
        isaRIdx = rIdx;
    }

    public void setRoleNeverGrouped(int nid) {
        int rIdx = factory.getRoleIdx(nid);

        if (rIdx < 0) {
            String message = String.format("ERROR, setRoleNeverGrouped(%d) has undefined role ",
                    nid);
            LOGGER.info(message);
            return;
        }

        ungroupedRoleIdxs.add(rIdx);
    }

    public void setRoleRoot(int nid, boolean inclusive) {
        int cIdx = factory.getConceptIdx(nid);
        if (cIdx < 0) {
            String message = String.format("ERROR, setRoleRoot(%d) has undefined concept ", nid);
            LOGGER.info(message);
            return;
        }

        if (inclusive) {
            roleRootCIdxs[INCLUSIVE].add(cIdx);
        } else {
            roleRootCIdxs[EXCLUSIVE].add(cIdx);
        }
    }

    public void addConcept(int conceptNid, boolean fullyDefined) {
        addConcept(conceptNid, fullyDefined, false);
    }

    private void addConcept(int conceptNid, boolean fullyDefined, boolean isVirtual) {
        final int conceptIdx = factory.getConceptIdx(conceptNid);
        if (isVirtual) {
            factory.setVirtualConceptCIdx(conceptIdx, true);
        }
        if (fullyDefined) {
            this.fullyDefinedCIdxs.add(conceptIdx);
        }
    }

    public void setConceptIdxAsDefined(int conceptIdx) {
        this.fullyDefinedCIdxs.add(conceptIdx);
    }

    public int addRelationship(int conceptNid1, int relNid, int conceptNid2, int group) {
        final int c1Idx = factory.findConceptIdx(conceptNid1);
        final int relRIdx = factory.findRoleIdx(relNid);
        final int c2Idx = factory.findConceptIdx(conceptNid2);

        if (c1Idx < 0 || relRIdx < 0 || c2Idx < 0) {
//            LOGGER.info("WARN: addRelationship( c1=" + conceptNid1 + "@" + c1Idx + ", role="
//                    + relNid + "@" + relRIdx + ", c2=" + conceptNid2 + "@" + c2Idx + ", group="
//                    + group + " ) concept(s) and/or role not in base array ");
            int err = 0;
            if (c1Idx < 0)
                err = 1;
            if (relRIdx < 0)
                err += 2;
            if (c2Idx < 0)
                err += 4;
            // if role or concepts if missing,
            // then the path filter for initially constructing int[]
            // should be checked. 
            return err;
        }

        // TODO make this check more efficient (cache the subsumptions)
        if (null != baseClassification && baseClassification.getSubsumptions().containsKey(c1Idx)) {
            throw new IllegalArgumentException(
                    "Cannot add new relationships for concepts defined in base ontology: "
                            + conceptNid1);
        }

        rowList.add(new Row(c1Idx, relRIdx, c2Idx, group));
        return 0;
    }

    public void addRoleComposition(int[] lhsNids, int rhsNid) {
        final int[] lhs = new int[lhsNids.length];
        final int rhs = factory.getRoleIdx(rhsNid);

        for (int i = 0; i < lhsNids.length; i++) {
            lhs[i] = factory.getRoleIdx(lhsNids[i]);
        }

        roleCompositions.add(new RI_123(lhs, rhs));
    }

    public void classify() {
        NormalisedOntology_123.setBatchMode(true); // Just in case

        long start = System.currentTimeMillis();
        final NormalisedOntology_123 ontology = populateOntology();
        // :!!!:zzz:
//        LOGGER.info("::: NormalisedOntology_123 ontology = populateOntology();");
//        ontology.printStats();
        
        LOGGER.info("populate time: " + (System.currentTimeMillis() - start) / 1000.0 + "s"
                + factory.toStringStats());
        start = System.currentTimeMillis();
        classification = ontology.getClassification();
        LOGGER.info("classify time: " + (System.currentTimeMillis() - start) / 1000.0 + "s");
    }

    // ----------------------------------------------------------------

    private class Populater {

        public final Set<Inclusion_123> ontology = new HashSet<Inclusion_123>();

        private int currentId = Integer.MIN_VALUE;
        private int currentGroup = 0;
        private int lhs = -1;
        private List<AbstractConcept> rhs = null;
        private List<Existential> rhsGroup = null;

        /**
         * 
         * @param totalConcepts
         * @param rootConcepts
         */
        public Populater(final int totalConcepts, final int[] rootConcepts) {
            final Row[] rowArray = rowList.toArray(new Row[rowList.size()]);
            Arrays.sort(rowArray);

            // :!!!:zzz:
//            LOGGER.info("::: final Row[] rowArray" + 
//            		" ==>> SIZE= " + rowArray.length);

            for (Row row : rowArray) {
                processConceptRow(row);
            }

            // :!!!:zzz:
//            LOGGER.info("::: processConceptRow()... ontology" + 
//            		" ==>> SIZE= " + ontology.size());

            if (null != rhs) {
                addGroup(rhs, rhsGroup);
                store(ontology, lhs, rhs);
            }

            ontology.addAll(roleCompositions);

            // :!!!:zzz:
//            LOGGER.info("::: roleCompositions.size()" + 
//            		" ==>> SIZE= " + roleCompositions.size());
            // :!!!:zzz:
//            LOGGER.info("::: ontology.addAll(roleCompositions)... ontology" + 
//            		" ==>> SIZE= " + ontology.size());

            // We have to loop here in case processRoleRow(...) identifies
            // extra concepts to treat as roles and thus changes the outcome
            // of the roleExists tests.
            int extraRoleCount;
            do {
                int roleMax = factory.getTotalRoles();

                for (Row row : rowArray) {
                    final int concept1 = factory.lookupConceptId(row.concept1);
                    final int concept2 = factory.lookupConceptId(row.concept2);

                    if (factory.roleExists(concept1) || factory.roleExists(concept2)) {
                        processRoleRow(row);
                    }
                }

                extraRoleCount = factory.getTotalRoles() - roleMax;
                // :!!!:zzz:
//                LOGGER.info("::: extraRoleCount" + 
//                		" ==>> == " + extraRoleCount);
            } while (extraRoleCount > 0);

            // :!!!:zzz:
//            LOGGER.info("::: (Row row : rowArray)... ontology" + 
//            		" ==>> SIZE= " + ontology.size());

        }

        private void processRoleRow(final Row row) {
            if (isaRIdx == row.role) {
                if (roleRootCIdxs[INCLUSIVE].contains(row.concept1)
                        || roleRootCIdxs[EXCLUSIVE].contains(row.concept1)) {
                    // The child is a roleRoot so the inheritance from its
                    // parent
                    // is not a subRole relationship
                    return;
                }
                if (roleRootCIdxs[EXCLUSIVE].contains(row.concept2)) {
                    // The parent is a roleRoot but not a role so the
                    // inheritance to its child
                    // is not a subRole relationship
                    return;
                }
                // EKM
                LOGGER.info("Role inclusion: " + factory.lookupConceptId(row.concept1) + " "
                        + factory.lookupConceptId(row.concept2));
                int[] lhs = { factory.getRoleIdx(factory.lookupConceptId(row.concept1)) };
                int rhs = factory.getRoleIdx(factory.lookupConceptId(row.concept2));
                ontology.add(new RI_123(lhs, rhs));
            } else {
                throw new AssertionError("only valid relationship for roles is 'Is a', not rIdx="
                        + row.role + " (role is " + row.concept1 + ")" 
                        + " please check C1:" + factory.lookupConceptStrId(row.concept1)
                        + " ROLE:" + factory.lookupRoleStrId(row.role)
                        + " C2:" + factory.lookupConceptStrId(row.concept2));
            }
        }

        private void processConceptRow(final Row row) {
            if (row.concept1 < currentId) {
                throw new AssertionError("concept1 mis-sorted; expected >= " + currentId + ", got "
                        + row.concept1);
            }

            if (row.group != 0 && ungroupedRoleIdxs.contains(row.role)) {
                throw new AssertionError("Role " + factory.lookupRoleId(row.role)
                        + " is marked as never grouped, but occurs in a grouped relationship: "
                        + row.toString(factory));
            }

            // check for beginning of a concept definition
            if (row.concept1 != currentId) {
                // check that there was a previous concept whose definition
                // needs storing
                if (null != rhs) {
                    // store definition
                    addGroup(rhs, rhsGroup);
                    store(ontology, lhs, rhs);
                }
                currentId = row.concept1;
                currentGroup = row.group;
                lhs = row.concept1;
                rhs = new ArrayList<AbstractConcept>();
                rhsGroup = new ArrayList<Existential>();
            } else if (row.group < currentGroup) {
                throw new AssertionError("group mis-sorted; expected >= " + currentGroup + ", got "
                        + row.group);
            }

            final Concept c2 = new Concept(row.concept2);

            if (isaRIdx == row.role) {
                assert row.group == 0;

                rhs.add(c2);
            } else {
                if ((row.group == 0 || row.group > currentGroup) && rhsGroup.size() > 0) {
                    addGroup(rhs, rhsGroup);
                    rhsGroup = new ArrayList<Existential>();
                }
                currentGroup = row.group;

                rhsGroup.add(new Existential(row.role, c2));
            }
        }

        private void addGroup(final List<AbstractConcept> rhs, final List<Existential> rhsGroup) {
            if (rhsGroup.size() > 0) {
                final AbstractConcept groupConcept = getConcept(rhsGroup);

                if (groupConcept instanceof Existential
                        && ungroupedRoleIdxs.contains(((Existential) groupConcept).getRole())) {

                    rhs.add(groupConcept);
                } else {
                    rhs.add(new Existential(roleGroupRIdx, groupConcept));
                }
            }
        }

        private AbstractConcept getConcept(final List<? extends AbstractConcept> rhs) {
            if (rhs.size() > 1) {
                return new Conjunction(rhs);
            } else {
                return rhs.get(0);
            }
        }

        private void store(final Set<Inclusion_123> ontology, final int lhs,
                final List<AbstractConcept> rhsList) {
            final AbstractConcept rhs = getConcept(rhsList);
            final GCI_123 gci = new GCI_123(lhs, rhs);

            // if (isDebugging()) {
            // StringBuilder sb = new StringBuilder();
            // sb.append(factory.lookupConceptId(lhs)).append(" [ ");
            // p(sb, rhs);
            // LOGGER.info(sb.toString());
            // }

            // System.err.print(factory.lookupConceptId(lhs) + "\t[ ");
            // p(rhs);
            // System.err.println();

            ontology.add(gci);

            if (fullyDefinedCIdxs.contains(lhs)) {
                ontology.add(new GCI_123(rhs, lhs));
            }
        }

    }

    private NormalisedOntology_123 populateOntology() {
        final Set<Inclusion_123> ontology = getInclusions();
        
        // :!!!:zzz:
//        LOGGER.info("::: Set<Inclusion_123> ontology = getInclusions(); --> SIZE= " + ontology.size());

        if (null != baseClassification) {
            //return baseClassification.getExtensionOntology(factory, ontology);
            return new NormalisedOntology_123(factory, ontology);
        } else {
            return new NormalisedOntology_123(factory, ontology);
        }
    }

    protected Set<Inclusion_123> getInclusions() {
        // CONFIRM THAT ISA IS SET
        if (isaNid == Integer.MAX_VALUE) {
            throw new AssertionError("No ISA id has been specified with setIsa(int nid).");
        }

        // SETUP CONCEPT ROOT
        final int[] rootConceptCIdxs = { factory.getConceptIdx(rootConceptNid) };
        // :!!!:zzz:
//        LOGGER.info("::: rootConceptCIdxs int[] ==> " + rootConceptCIdxs);

        // POPULATE
        return new Populater(factory.getTotalConcepts(), rootConceptCIdxs).ontology;
    }

    final protected static class Row implements Comparable<Row> {

        final int concept1;
        final int role;
        final int concept2;
        final int group;

        Row(int concept1, int role, int concept2, int group) {
            this.concept1 = concept1;
            this.role = role;
            this.concept2 = concept2;
            this.group = group;
        }

        @Override
        public int hashCode() {
            return (concept1 ^ role ^ concept2 ^ group);
        }

        @Override
        public boolean equals(Object o) {
            Row other = (Row) o;
            return concept1 == other.concept1 && concept2 == other.concept2 && role == other.role
                    && group == other.group;
        }

        public int compareTo(Row other) {
            return concept1 == other.concept1 ? (group == other.group ? (role == other.role ? (compareTo(
                    concept2, other.concept2))
                    : compareTo(role, other.role))
                    : compareTo(group, other.group))
                    : compareTo(concept1, other.concept1);
        }

        private static int compareTo(int lhs, int rhs) {
            return lhs < rhs ? -1 : (lhs > rhs ? 1 : 0);
        }

        public String toString(IFactory_123 factory) {
            return factory.lookupConceptId(concept1) + ",\t" + factory.lookupRoleId(role) + ",\t"
                    + factory.lookupConceptId(concept2) + ",\t" + group;
        }

        @Override
        public String toString() {
            return concept1 + ",\t" + role + ",\t" + concept2 + ",\t" + group;
        }

    }

    /**
     * Only need to record role and value since concept is constant and always
     * known in context.
     * 
     * @author law223
     */
    final private static class Rel implements Comparable<Rel> {
        final int role;
        final int concept2;

        Rel(final int role, final int concept2) {
            this.role = role;
            this.concept2 = concept2;
        }

        @Override
        public int hashCode() {
            return (role ^ concept2);
        }

        @Override
        public boolean equals(Object o) {
            Rel other = (Rel) o;
            return concept2 == other.concept2 && role == other.role;
        }

        public int compareTo(Rel other) {
            return role == other.role ? (compareTo(concept2, other.concept2)) : compareTo(role,
                    other.role);
        }

        private static int compareTo(int lhs, int rhs) {
            return lhs < rhs ? -1 : (lhs > rhs ? 1 : 0);
        }

        @Override
        public String toString() {
            return role + ",\t" + concept2;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * <p>
     * <em><strong>Note</strong>, results are currently undefined for incremental classification.</em>
     * 
     * @see {@link I_Snorocket#getEquivalents(au.csiro.snorocket.snapi.I_Snorocket.I_EquivalentCallback)}
     */
    public void getEquivalents(I_EquivalentCallback callback) {
        final IConceptMap<IConceptSet> equivalents = getPostProcessedData().getEquivalents();
        for (final IntIterator keyItr = equivalents.keyIterator(); keyItr.hasNext();) {
            final int key = keyItr.next();

            if (skip(key)) {
                continue;
            }

            final IConceptSet equivalentsConceptSet = equivalents.get(key);

            if (equivalentsConceptSet.size() > 0) {
                final ArrayList<Integer> equivalentConcepts = new ArrayList<Integer>();
                equivalentConcepts.add(factory.lookupConceptId(key));

                for (final IntIterator valItr = equivalentsConceptSet.iterator(); valItr.hasNext();) {
                    final int val = valItr.next();

                    if (skip(val)) {
                        continue;
                    }

                    equivalentConcepts.add(factory.lookupConceptId(val));
                }

                if (equivalentConcepts.size() > 1) {
                    callback.equivalent(equivalentConcepts);
                }
            }
        }
    }

    public void getRelationships(I_Callback callback) {
        returnRelationships(callback, false);
    }

    public void getDistributionFormRelationships(final I_Callback callback) {
        returnRelationships(callback, true);
    }

    private void returnRelationships(final I_Callback callback, final boolean filterRedundant) {
        if (null == classification) {
            throw new IllegalStateException("Ontology has not been classified.");
        }
        nestedRoleGroupCount = 0;

        final IConceptMap<IConceptSet> subsumptions = classification.getSubsumptions();
        final R rels = classification.getRelationships();
        final IConceptMap<IConceptSet> filteredSubsumptions = filterRedundant ? getPostProcessedData()
                .getParents()
                : subsumptions;

        final int limit = factory.getTotalConcepts();
        for (int concept = 0; concept < limit; concept++) {

            if (skip(concept)) {
                continue;
            }

            returnIsaRelationships(callback, filteredSubsumptions, concept);

            if (!factory.isBaseConcept(concept)) {
                returnOtherRelationships(callback, classification, rels, concept, filterRedundant);

                // handle roleGroupRIdx special case
                final IConceptSet roleValues = rels.lookupB(concept, roleGroupRIdx);
                final IConceptSet candidateValues = getLeaves(roleValues);
                returnGroupedRelationships(callback, classification, rels, concept,
                        candidateValues, filterRedundant);
            }

        }

        if (nestedRoleGroupCount > 0) {
            LOGGER.warning("SNOMED CT should not contain nested role groups, but detected "
                    + nestedRoleGroupCount);
        }
    }

    private PostProcessedData_123 getPostProcessedData() {
        if (null == classification) {
            throw new IllegalStateException("Ontology has not been classified.");
        }

        final IConceptMap<IConceptSet> subsumptions = classification.getSubsumptions();

        if (null == postProcessedData) {
            postProcessedData = null == baseClassification ? new PostProcessedData_123(factory,
                    subsumptions) : new PostProcessedData_123(factory, baseClassification
                    .getSubsumptions(), subsumptions);
        }

        return postProcessedData;
    }

    /**
     * Invokes callback for all isa relationships for concept identified by key
     * that were not part of the original stated form. That is, only returns
     * new, inferred subsumptions.
     * 
     * @param callback
     * @param dag
     * @param concept
     */
    private void returnIsaRelationships(final I_Callback callback,
            final IConceptMap<IConceptSet> dag, final int concept) {
        final int conceptNid = factory.lookupConceptId(concept);

        final IConceptSet conceptSet = dag.get(concept);
        if (null == conceptSet) {
            return;
        }

        for (final IntIterator itr = conceptSet.iterator(); itr.hasNext();) {
            final int parent = itr.next();
            // if (isDebugging()) {
            // System.err.println("ISA: " + conceptNid + " [ " +
            // factory.lookupConceptId(parent)); // FIXME delete
            // }

            if (concept == parent || skip(parent)) {
                continue;
            }

            final int parentNid = factory.lookupConceptId(parent);

            callback.addRelationship(conceptNid, isaNid, parentNid, 0);
        }
    }

    /**
     * Invokes callback for all non-isa relationships for concept.
     * 
     * @param callback
     * @param classification
     * @param rels
     * @param concept
     * @param filterRedundant
     */
    private void returnOtherRelationships(final I_Callback callback,
            final Classification classification, final R rels, final int concept,
            final boolean filterRedundant) {

        final RVGroup rvGroup = computeRoleValues(classification, rels, concept, filterRedundant);

        // return role values
        final int conceptNid = factory.lookupConceptId(concept);

        rvGroup.map(new RVCallback() {
            public void map(int role, int value) {
                final int roleNid = factory.lookupRoleId(role);
                if (!skip(value)) {
                    final int valueNid = factory.lookupConceptId(value);
                    callback.addRelationship(conceptNid, roleNid, valueNid, 0);
                }
            }
        });
        // for (int role = 0; role < roleValuesMap.length; role++) {
        // final String roleNid = factory.lookupRoleId(role);
        // final IConceptSet values = roleValuesMap[role];
        // returnUngroupedRelationships(callback, concept, conceptNid, role,
        // roleNid, values);
        // }
    }

    private interface RVCallback {
        void map(int role, int value);
    }

    private class RVGroup {
        final private Classification classification;

        final IConceptSet[] roleValuesMap = new IConceptSet[factory.getTotalRoles()];
        Collection<Rel> _rels = null;

        RVGroup(final Classification classification) {
            this.classification = classification;
        }

        void add(int rel, int val) {
            if (null == roleValuesMap[rel]) {
                roleValuesMap[rel] = IConceptSet.FACTORY.createConceptSet();
            }
            roleValuesMap[rel].add(val);
        }

        void map(RVCallback cb) {
            for (int role = 0; role < roleValuesMap.length; role++) {
                final IConceptSet values = roleValuesMap[role];
                if (null != values) {
                    for (final IntIterator itr = values.iterator(); itr.hasNext();) {
                        cb.map(role, itr.next());
                    }
                }
            }
        }

        boolean containsAll(final RVGroup other) {
            for (final Rel otherRel : other.getRels()) {
                boolean contained = false;
                for (final Rel ourRel : getRels()) {
                    if (contains(ourRel, otherRel)) {
                        contained = true;
                        break;
                    }
                }
                if (!contained) {
                    return false;
                }
            }

            return true;
        }

        void filterRedundant() {
            for (int role = 0; role < roleValuesMap.length; role++) {
                final IConceptSet values = roleValuesMap[role];
                if (null != values) {
                    final RoleSet parentRoles = classification.getRoleClosure(role);
                    for (int parentRole = parentRoles.first(); parentRole >= 0; parentRole = parentRoles
                            .next(parentRole + 1)) {

                        if (role == parentRole) {
                            continue;
                        }

                        if (null != roleValuesMap[parentRole]) {

                            int beforeSize = roleValuesMap[parentRole].size();
                            roleValuesMap[parentRole].removeAll(values);
                        }
                    }
                }
            }
        }

        private boolean contains(final Rel lhs, final Rel rhs) {
            IConceptSet set;
            return (lhs.role == rhs.role || classification.getRoleClosure(lhs.role).contains(
                    rhs.role))
                    && (lhs.concept2 == rhs.concept2 || (null != (set = classification
                            .getSubsumptions().get(lhs.concept2)) && set.contains(rhs.concept2)));
        }

        private Collection<Rel> getRels() {
            if (null == _rels) {
                _rels = new ArrayList<Rel>();
                map(new RVCallback() {
                    public void map(int role, int value) {
                        _rels.add(new Rel(role, value));
                    }
                });
            }
            return _rels;
        }

        int size() {
            return getRels().size();
        }
    }

    /**
     * Returns a map of roles to values, possible filtering redundant values.
     * 
     * @param classification
     * @param rels
     * @param concept
     * @param filterRedundant
     * @return
     */
    private RVGroup computeRoleValues(final Classification classification, final R rels,
            final int concept, final boolean filterRedundant) {
        final int maxRole = factory.getTotalRoles();

        // map from role to concept
        final RVGroup rvGroup = new RVGroup(classification);
        // final IConceptSet[] roleValuesMap = new IConceptSet[maxRole];
        // roleValuesMap[roleGroupRIdx] = IConceptSet.EMPTY_SET;

        for (int role = 0; role < maxRole; role++) {
            if (roleGroupRIdx == role) {
                continue; // Handle this outside the loop
            }

            final IConceptSet candidateValues = getLeaves(rels.lookupB(concept, role));

            // final int numValues = candidateValues.size();
            // if (numValues > 0) {
            // roleValuesMap[role] =
            // IConceptSet.FACTORY.createConceptSet(numValues);
            // } else {
            // roleValuesMap[role] = IConceptSet.EMPTY_SET;
            // continue;
            // }

            for (final IntIterator valueItr = candidateValues.iterator(); valueItr.hasNext();) {
                final int value = valueItr.next();

                if (skip(value)) {
                    continue;
                }

                // We now have concept [ role.value but want to avoid also
                // returning concept [ role2.value for some role [ role2
                // roleValuesMap[role].add(value);
                rvGroup.add(role, value);
            }
        }

        if (filterRedundant) {
            // Filter redundant role values
            rvGroup.filterRedundant();
            // for (int role = 0; role < roleValuesMap.length; role++) {
            // final IConceptSet values = roleValuesMap[role];
            // final IConceptSet parentRoles = subsumptions.get(role);
            // for (final IntIterator itr = parentRoles.iterator();
            // itr.hasNext(); ) {
            // final int parentRole = itr.next();
            //
            // int beforeSize = roleValuesMap[parentRole].size();
            // roleValuesMap[parentRole].removeAll(values);
            // if (beforeSize > roleValuesMap[parentRole].size()) {
            // System.err.println("FILTER REDUNDANT"); // FIXME delete
            // }
            // }
            // }
        }

        return rvGroup;
    }

    /**
     * Before invoking the callback we check that the (ungrouped) relationship
     * was not part of the stated view.
     * 
     * @param callback
     * @param conceptNid
     * @param rString
     * @param roleValues
     */
    private void returnUngroupedRelationships(I_Callback callback, final int concept,
            final int conceptNid, final int role, final int roleNid, final IConceptSet roleValues) {
        for (final IntIterator valueItr = roleValues.iterator(); valueItr.hasNext();) {
            final int value = valueItr.next();

            if (skip(value)) {
                continue;
            }

            final int valueNid = factory.lookupConceptId(value);
            callback.addRelationship(conceptNid, roleNid, valueNid, 0);
        }
    }

    /**
     * For each group of inferred relationships, we check if an identical group
     * was part of the stated view. If so, then we skip it, otherwise we add it
     * to the collection of newRelGroups.
     * 
     * Finally, for each group of inferred relationships in newRelGroups we
     * invoke the callback.
     * 
     * The new groupIds are allocated from 1 more than the number of re-used
     * ids.
     * 
     * Need to watch out for following case:
     * <code>X [ rg.(r1.Y + r2.Z) + rg.(r1.Y + r3.Z) where r3 [ r2</code> since
     * this will manifest as
     * <code>X [ rg.(r1.Y + r2.Z) + rg.(r1.Y + r2.Z + r3.Z) where r3 [ r2</code>
     * Code does following: <code>
     * newRelGroups = [{r1.Y, r2.Z}]
     * {r1.Y,r2.Z}.containsAll({r1.Y,r2.Z,r3.Z})?
     * FALSE -> {r1.Y,r2.Z,r3.Z}.containsAll({r1.Y,r2.Z})?
     * TRUE -> red.add({r1.Y,r2.Z})
     * newRelGroups.removeAll(red)  // newRel = []
     * newRelGroups = [{r1.Y, r2.Z, r3.Z}]
     * </code> Can't filter out r2.Z until this point
     * 
     * @param callback
     *            The object to send the grouped relationships to
     * @param rels
     *            All computed relationships
     * @param concept
     *            The concept that is the subject of the relationships
     * @param roleValues
     *            The concepts corresponding to the grouped relationships
     *            (should all be virtual?)
     * @param filterRedundant
     * @param filterStated
     */
    private void returnGroupedRelationships(final I_Callback callback,
            final Classification classification, final R rels, final int concept,
            final IConceptSet roleValues, final boolean filterRedundant) {

        final Collection<RVGroup> newRVGroups = new ArrayList<RVGroup>();

        int groupCount = 0;

        final int conceptNid = factory.lookupConceptId(concept);
        for (final IntIterator valueItr = roleValues.iterator(); valueItr.hasNext();) {
            final int groupedValue = valueItr.next();

            // invariant: valueConcept is a composite concept:
            assert factory.isVirtualConcept(groupedValue);
            if (isDebugging() && !factory.isVirtualConcept(groupedValue)) {
                throw new AssertionError("Internal error: non-virtual grouped concepts found: "
                        + factory.lookupConceptId(groupedValue));
            }

            // check we don't have nested role groups
            final IConceptSet nestedCandidateValues = getLeaves(rels.lookupB(groupedValue,
                    roleGroupRIdx));
            if (nestedCandidateValues.size() > 0) {
                nestedRoleGroupCount++;
                if (isDebugging()) {
                    handleNestedRoleGroups(conceptNid, nestedCandidateValues);
                }
            }

            final RVGroup rvGroup = computeRoleValues(classification, rels, groupedValue,
                    filterRedundant);

            final Collection<RVGroup> redundant = new ArrayList<RVGroup>();

            boolean duplicate = false;

            for (final RVGroup rvg : newRVGroups) {
                if (rvg.containsAll(rvGroup)) {
                    duplicate = true;
                    break;
                } else if (rvGroup.containsAll(rvg)) {
                    redundant.add(rvg);
                }
            }

            newRVGroups.removeAll(redundant);

            if (!duplicate) {
                newRVGroups.add(rvGroup);
            }

            groupCount++;
        }

        // calculate the initial groupId for the newly inferred relationship
        // groups
        // (need to avoid re-using group ids)
        //
        // int groupId = groupCount; // - newRelGroups.size();

        for (RVGroup rvGroup : newRVGroups) {
            // If there's only one item in a group, don't "group" it.
            final int groupId = rvGroup.size() > 1 ? groupCount++ : 0;

            rvGroup.map(new RVCallback() {
                public void map(int role, int value) {
                    callback.addRelationship(conceptNid, factory.lookupRoleId(role), factory
                            .lookupConceptId(value), groupId);
                }
            });
            // for (Rel rel: group) {
            // callback.addRelationship(conceptNid,
            // factory.lookupRoleId(rel.role),
            // factory.lookupConceptId(rel.concept2), groupId);
            // }
        }
    }

    private void handleNestedRoleGroups(final int conceptNid,
            final IConceptSet nestedCandidateValues) {
        StringBuilder detail = new StringBuilder(conceptNid);
        detail.append(" [ ");
        for (final IntIterator nestedValueItr = nestedCandidateValues.iterator(); nestedValueItr
                .hasNext();) {
            final int nestedValue = nestedValueItr.next();

            detail.append("  <");
            detail.append(factory.lookupConceptId(nestedValue));
            detail.append(">");
        }

        // throw new
        // AssertionError("SNOMED should not contain nested role groups:" +
        // detail);
        LOGGER.warning("SNOMED should not contain nested role groups: " + detail.toString());
    }

    /**
     * Given a set of concepts, computes the subset such that no member of the
     * subset is subsumed by another member.
     * 
     * result = {c | c in bs and not c' in b such that c' [ c}
     * 
     * @param s
     *            subsumption relationships
     * @param bs
     *            set of subsumptions to filter
     * @return
     */
    private IConceptSet getLeaves(final IConceptSet bs) {
        final IConceptMap<IConceptSet> subsumptions = classification.getSubsumptions();
        final IConceptMap<IConceptSet> baseSubsumptions = null == baseClassification ? null
                : baseClassification.getSubsumptions();

        final IConceptSet leafBs = IConceptSet.FACTORY.createConceptSet(bs);

        for (final IntIterator bItr = bs.iterator(); bItr.hasNext();) {
            final int b = bItr.next();

            final IConceptSet ancestors = IConceptSet.FACTORY.createConceptSet(subsumptions.get(b));
            if (null != baseSubsumptions) {
                final IConceptSet set = baseSubsumptions.get(b);
                if (null != set) {
                    ancestors.addAll(set);
                }
            }
            ancestors.remove(b);
            leafBs.removeAll(ancestors);
        }
        return leafBs;
    }

    private boolean skip(int id) {
        return (id == Factory_123.TOP_CONCEPT || id == Factory_123.BOTTOM_CONCEPT || factory
                .isVirtualConcept(id));
    }

    public InputStream getStream() throws IOException {
        final PipedInputStream result = new PipedInputStream();
        final PipedOutputStream out = new PipedOutputStream(result);

        new Thread(new Runnable() {
            public void run() {
                final PrintWriter printWriter = new PrintWriter(out);
                try {
                    classification.printClassification(printWriter);

                    printWriter.println(isaNid);
                    printWriter.println(FILE_VERSION);

                    // ungrouped roles
                    printWriter.println(ungroupedRoleIdxs.size());
                    for (final IntIterator itr = ungroupedRoleIdxs.iterator(); itr.hasNext();) {
                        final int role = itr.next();
                        printWriter.println(role);
                    }

                    // role roots
                    printWriter.println(roleRootCIdxs[INCLUSIVE].size());
                    for (Integer role : (Set<Integer>) roleRootCIdxs[INCLUSIVE]) {
                        printWriter.println(factory.lookupConceptId(role));
                    }
                    printWriter.println(roleRootCIdxs[EXCLUSIVE].size());
                    for (Integer role : (Set<Integer>) roleRootCIdxs[EXCLUSIVE]) {
                        printWriter.println(factory.lookupConceptId(role));
                    }

                } catch (Throwable t) {
                    t.printStackTrace();
                } finally {
                    printWriter.close();
                }
            }
        }).start();

        return result;
    }

    public I_Snorocket_123 createExtension() {
        return createExtension(classification, isaNid);
    }

    protected I_Snorocket_123 createExtension(Classification classification, int isaId) {
        return new Snorocket_123(classification, isaId);
    }

    static boolean isDebugging() {
        return au.csiro.snorocket.core.Snorocket.DEBUGGING;
    }
    
    static boolean isDebugDumping() {
        return au.csiro.snorocket.core.Snorocket.DEBUG_DUMP;
    }
    
    public void getInternalDataCon(I_InternalDataConCallback callback) {
        int[] ca = factory.getConceptArray();
        
        // Find data stop index.
        int stopIdx = ca.length - 1;
        while (stopIdx > 0 && ca[stopIdx] == Integer.MAX_VALUE) 
            stopIdx--;

        // check order
        for (int i = 1; i <= stopIdx; i++)
            if (ca[i-1] > ca[i]) 
                LOGGER.info("::: ERROR Internal ConceptArray Data Not In Order: " + ca[i] + " @ " + i);
            else if (ca[i] == ca[i-1])
                LOGGER.info("::: ERROR Internal ConceptArray Data Duplicate Value: " + ca[i] + " @ " + i);
        
        for (int i = 0; i <= stopIdx; i++)
            callback.processConData(ca[i]);
    }

    public void getInternalDataRel(I_InternalDataRelCallback callback) {
        for (Row r : rowList)
            callback.processRelData(r.concept1, r.role, r.concept2, r.group);
    }

    public void getInternalDataRole(I_InternalDataRoleCallback callback) {
        int[] ra = factory.getRoleArray();
        
        // Find data stop index.
        int stopIdx = ra.length - 1;
        while (stopIdx > 0 && ra[stopIdx] == Integer.MAX_VALUE) 
            stopIdx--;

        // check order
        for (int i = 1; i <= stopIdx; i++)
            if (ra[i-1] > ra[i]) 
                LOGGER.info("::: ERROR Internal RoleArray Data Not In Order: " + ra[i] + " @ " + i);
            else if (ra[i] == ra[i-1])
                LOGGER.info("::: ERROR Internal RoleArray Data Duplicate Value: " + ra[i] + " @ " + i);

        for (int i = 0; i <= stopIdx; i++)
            callback.processRoleData(ra[i]);
    }
}
