/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.importer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import au.csiro.snorocket.core.IFactory_123;
import au.csiro.snorocket.core.axioms.GCI_123;
import au.csiro.snorocket.core.axioms.Inclusion_123;
import au.csiro.snorocket.core.axioms.RI_123;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Existential;

/**
 * Transforms the native RF2 files used in SNOMED into the native format used by
 * Snorocket.
 * 
 * @author Alejandro Metke
 * 
 */
public class RF1Importer_123 {

    private final IFactory_123 factory;
    private String conceptsFile;
    private String relationshipsFile;

    public static final String conceptModelAttId = "410662002";
    public static final String isAId = "116680003";
    public static final String attrId = "246061005";
    private final List<String> neverGroupedIds = new ArrayList<>();

    // direct-substance o has-active-ingredient -> direct-substance
    private final Map<String, String> rightId = new HashMap<>();

    // private static final String coreModuleId = "900000000000207008";
    // private static final String metadataModuleId = "900000000000012004";
    // private static final String conceptDefinedId = "900000000000073002";
    // private static final String conceptPrimitiveId = "900000000000074008";
    // private static final String FSNId = "900000000000003001";

    public static final String definingRelationship = "900000000000006009";
    public static final String some = "900000000000451002";
    public static final String all = "900000000000452009";

    private final List<String> problems = new ArrayList<>();

    private final Map<String, String> primitive = new HashMap<>();
    private final Map<String, Set<String>> parents = new HashMap<>();
    private final Map<String, Set<String>> children = new HashMap<>();
    private final Map<String, List<String[]>> rels = new HashMap<>();
    private final Map<String, Map<String, String>> roles = new HashMap<>();

    public final static String PRE = "SCT_";

    /**
     * Constructor.
     * 
     * @param conceptsFile
     * @param relationshipsFile
     */
    public RF1Importer_123(IFactory_123 factory, String conceptsFile,
            String relationshipsFile) {
        this.factory = factory;
        this.conceptsFile = conceptsFile;
        this.relationshipsFile = relationshipsFile;
        neverGroupedIds.add("123005000");
        neverGroupedIds.add("127489000");
        neverGroupedIds.add("272741003");
        neverGroupedIds.add("411116001");
        rightId.put("363701004", "127489000");
    }

    @SuppressWarnings("resource")
    public List<Inclusion_123> transform(ReasonerProgressMonitor monitor) {
        monitor.reasonerTaskStarted("Loading axioms");
        final List<Inclusion_123> axioms = new ArrayList<>();

        BufferedReader br = null;

        try {
            // We process the concepts file to determine if a concept is fully
            // defined or primitive and to exclude inactive concepts
            br = new BufferedReader(new FileReader(conceptsFile));
            String line;
            while (null != (line = br.readLine())) {
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');
                int idx2 = line.indexOf('\t', idx1 + 1);
                int idx3 = line.indexOf('\t', idx2 + 1);
                int idx4 = line.indexOf('\t', idx3 + 1);
                int idx5 = line.indexOf('\t', idx4 + 1);
                int idx6 = line.indexOf('\t', idx5 + 1);

                // 0..idx1 == conceptid
                // idx1+1..idx2 == status
                // idx2+1..idx3 == fully specified name
                // idx3+1..idx4 == CTV3ID
                // idx4+1..idx5 == SNOMEDID
                // idx5+1..idx6 == isPrimitive

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0) {
                    throw new RuntimeException(
                            "Concepts: Mis-formatted "
                                    + "line, expected at least 6 tab-separated fields, "
                                    + "got: " + line);
                }

                final String id = line.substring(0, idx1);
                final String status = line.substring(idx1 + 1, idx2);
                final String isPrimitive = (idx6 > 0) ? line.substring(
                        idx5 + 1, idx6) : line.substring(idx5 + 1);

                if (!"CONCEPTID".equals(id) && "0".equals(status)) {
                    primitive.put(id, isPrimitive);
                }
            }

            br.close();

            // Process relationships
            br = new BufferedReader(new FileReader(relationshipsFile));

            while (null != (line = br.readLine())) {
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');
                int idx2 = line.indexOf('\t', idx1 + 1);
                int idx3 = line.indexOf('\t', idx2 + 1);
                int idx4 = line.indexOf('\t', idx3 + 1);
                int idx5 = line.indexOf('\t', idx4 + 1);
                int idx6 = line.indexOf('\t', idx5 + 1);
                int idx7 = line.indexOf('\t', idx6 + 1);

                // 0..idx1 == relationshipid
                // idx1+1..idx2 == conceptid1
                // idx2+1..idx3 == RELATIONSHIPTYPE
                // idx3+1..idx4 == conceptid2
                // idx4+1..idx5 == CHARACTERISTICTYPE
                // idx5+1..idx6 == REFINABILITY
                // idx6+1..idx7 == RELATIONSHIPGROUP

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0
                        || idx6 < 0) {
                    throw new RuntimeException(
                            "Relationships: Mis-formatted "
                                    + "line, expected at least 7 tab-separated fields, "
                                    + "got: " + line);
                }

                final String id = line.substring(idx1);
                final String concept1 = line.substring(idx1 + 1, idx2);
                final String role = line.substring(idx2 + 1, idx3);
                final String concept2 = line.substring(idx3 + 1, idx4);
                final String characteristicType = line
                        .substring(idx4 + 1, idx5);
                final String group = (idx7 > 0) ? line
                        .substring(idx6 + 1, idx7) : line.substring(idx6 + 1);

                // only process active concepts and defining relationships
                if (!"RELATIONSHIPID".equals(id)
                        && "0".equals(characteristicType)) {
                    if (isAId.equals(role)) {
                        populateParent(concept1, concept2);
                        populateChildren(concept2, concept1);
                    } else {
                        // Populate relationships
                        populateRels(concept1, role, concept2, group);
                    }
                }
            }

            populateRoles(children.get(conceptModelAttId), "");

            SortedSet<String> classes = new TreeSet<String>();
            SortedSet<String> props = new TreeSet<String>();
            // SortedSet<String> feats = new TreeSet<String>();

            // Add RoleGroup - doesn't use prefix
            props.add("RoleGroup");

            // Differs from the script for historical reasons: the factory in
            // the ELK version of Snorocket requires the concepts, roles, and
            // features to be created before adding axioms
            for (String role : roles.keySet()) {
                props.add(PRE + role);
            }

            for (String c : primitive.keySet()) {
                if (roles.get(c) == null) {
                    classes.add(PRE + c);
                    Set<String> par = parents.get(c);
                    if (par != null)
                        for (String pa : par)
                            classes.add(PRE + pa);
                }
            }

            for (String cl : classes) {
                factory.getConceptIdx(cl);
            }

            for (String pr : props) {
                factory.getRoleIdx(pr);
            }

            /*
             * for(String feat : feats) { factory.getFeatureIdx(feat); }
             */

            // Add the role axioms
            for (String r1 : roles.keySet()) {
                String parentRole = roles.get(r1).get("parentrole");

                if (!"".equals(parentRole)) {
                    axioms.add(new RI_123(new int[] { factory.getRoleIdx(PRE
                            + r1) }, factory.getRoleIdx(PRE + parentRole)));
                }

                String rightId = roles.get(r1).get("rightID");
                if (!"".equals(rightId)) {
                    axioms.add(new RI_123(new int[] {
                            factory.getRoleIdx(PRE + r1),
                            factory.getRoleIdx(PRE + rightId) }, factory
                            .getRoleIdx(PRE + r1)));
                }
            }

            // Add concept axioms
            for (String c1 : primitive.keySet()) {
                if (roles.get(c1) != null)
                    continue;
                Set<String> prs = parents.get(c1);
                int numParents = (prs != null) ? prs.size() : 0;

                List<String[]> relsVal = rels.get(c1);
                int numRels = 0;
                if (relsVal != null)
                    numRels = 1;

                int numElems = numParents + numRels;

                if (numElems == 0) {
                    // do nothing
                } else if (numElems == 1) {
                    axioms.add(new GCI_123(factory.getConceptIdx(PRE + c1),
                            new Concept(factory.getConceptIdx(PRE
                                    + prs.iterator().next()))));
                } else {
                    List<AbstractConcept> conjs = new ArrayList<>();

                    for (String pr : prs) {
                        conjs.add(new Concept(factory.getConceptIdx(PRE + pr)));
                    }

                    if (relsVal != null) {
                        for (Set<RoleValuePair> rvs : groupRoles(relsVal)) {
                            if (rvs.size() > 1) {
                                List<AbstractConcept> innerConjs = new ArrayList<>();
                                for (RoleValuePair rv : rvs) {
                                    int role = factory
                                            .getRoleIdx(PRE + rv.role);
                                    int filler = factory.getConceptIdx(PRE
                                            + rv.value);
                                    Existential exis = new Existential(role,
                                            new Concept(filler));
                                    innerConjs.add(exis);
                                }
                                // Wrap with a role group
                                conjs.add(new Existential(factory
                                        .getRoleIdx("RoleGroup"),
                                        new Conjunction(innerConjs)));
                            } else {
                                RoleValuePair first = rvs.iterator().next();
                                int role = factory.getRoleIdx(PRE + first.role);
                                int filler = factory.getConceptIdx(PRE
                                        + first.value);
                                Existential exis = new Existential(role,
                                        new Concept(filler));
                                if (neverGroupedIds.contains(first.role)) {
                                    // Does not need a role group
                                    conjs.add(exis);
                                } else {
                                    // Needs a role group
                                    conjs.add(new Existential(factory
                                            .getRoleIdx("RoleGroup"), exis));
                                }
                            }
                        }
                    }

                    axioms.add(new GCI_123(factory.getConceptIdx(PRE + c1),
                            new Conjunction(conjs)));

                    if (primitive.get(c1).equals("0")) {
                        axioms.add(new GCI_123(new Conjunction(conjs), factory
                                .getConceptIdx(PRE + c1)));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (br != null)
                try {
                    br.close();
                } catch (Exception e) {
                }
        }

        return axioms;
    }

    private void populateParent(String src, String tgt) {
        Set<String> prs = parents.get(src);
        if (prs == null) {
            prs = new TreeSet<>();
            parents.put(src, prs);
        }
        prs.add(tgt);
    }

    private void populateChildren(String src, String tgt) {
        Set<String> prs = children.get(src);
        if (prs == null) {
            prs = new TreeSet<>();
            children.put(src, prs);
        }
        prs.add(tgt);
    }

    private void populateRels(String src, String role, String tgt, String group) {
        List<String[]> val = rels.get(src);
        if (val == null) {
            val = new ArrayList<>();
            rels.put(src, val);
        }
        val.add(new String[] { role, tgt, group });
    }

    private void populateRoles(Set<String> roles, String parentSCTID) {
        for (String role : roles) {
            Set<String> cs = children.get(role);
            if (cs != null) {
                populateRoles(cs, role);
            }
            String ri = rightId.get(role);
            if (ri != null) {
                populateRoleDef(role, ri, parentSCTID);
            } else {
                populateRoleDef(role, "", parentSCTID);
            }
        }
    }

    private void populateRoleDef(String code, String rightId, String parentRole) {
        Map<String, String> vals = roles.get(code);
        if (vals == null) {
            vals = new HashMap<>();
            roles.put(code, vals);
        }
        vals.put("rightID", rightId);
        vals.put("parentrole", parentRole);
    }

    private Set<Set<RoleValuePair>> groupRoles(List<String[]> groups) {
        Map<String, Set<RoleValuePair>> roleGroups = new HashMap<>();

        for (String[] group : groups) {
            String roleGroup = group[2];
            Set<RoleValuePair> lrvp = roleGroups.get(roleGroup);
            if (lrvp == null) {
                lrvp = new HashSet<>();
                roleGroups.put(group[2], lrvp);
            }
            lrvp.add(new RoleValuePair(group[0], group[1]));
        }

        Set<Set<RoleValuePair>> res = new HashSet<>();
        for (String roleGroup : roleGroups.keySet()) {
            Set<RoleValuePair> val = roleGroups.get(roleGroup);

            // 0 indicates not grouped
            if ("0".equals(roleGroup)) {
                for (RoleValuePair rvp : val) {
                    Set<RoleValuePair> sin = new HashSet<>();
                    sin.add(rvp);
                    res.add(sin);
                }
            } else {
                Set<RoleValuePair> item = new HashSet<>();
                for (RoleValuePair trvp : val) {
                    item.add(trvp);
                }
                res.add(item);
            }
        }
        return res;
    }

    public void clear() {
        problems.clear();
        primitive.clear();
        parents.clear();
        children.clear();
        rels.clear();
        roles.clear();
    }

    public List<String> getProblems() {
        return problems;
    }

    private class RoleValuePair {
        String role;
        String value;

        RoleValuePair(String role, String value) {
            this.role = role;
            this.value = value;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + getOuterType().hashCode();
            result = prime * result + ((role == null) ? 0 : role.hashCode());
            result = prime * result + ((value == null) ? 0 : value.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            RoleValuePair other = (RoleValuePair) obj;
            if (!getOuterType().equals(other.getOuterType()))
                return false;
            if (role == null) {
                if (other.role != null)
                    return false;
            } else if (!role.equals(other.role))
                return false;
            if (value == null) {
                if (other.value != null)
                    return false;
            } else if (!value.equals(other.value))
                return false;
            return true;
        }

        private RF1Importer_123 getOuterType() {
            return RF1Importer_123.this;
        }
    }

    public boolean usesConcreteDomains() {
        return false;
    }

}
