/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;

import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.importer.ImportException;
import au.csiro.ontology.importer.rf1.RF1Importer;
import au.csiro.ontology.importer.rf2.RF2Importer;
import au.csiro.ontology.importer.rf2.RelationshipRow;
import au.csiro.ontology.input.Inputs;
import au.csiro.ontology.input.ModuleInfo;
import au.csiro.ontology.input.RF2Input;
import au.csiro.ontology.input.Version;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.NamedConcept;
import au.csiro.ontology.util.NullProgressMonitor;
import au.csiro.snorocket.core.CoreFactory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.owlapi.SnorocketOWLReasoner;
import au.csiro.snorocket.owlapi.util.DebugUtils;

/**
 * @author Alejandro Metke
 *
 */
public abstract class AbstractTest {
    
    /**
     * Classifies the stated version of an ontology in RF2 format and compares it to a correctly classified version 
     * (available in the canonical table). This method assumes that only a single ontology is defined in the input
     * configuration file.
     * 
     * @param config The input object.
     * @throws JAXBException 
     * @throws ImportException 
     */
    protected void testRF2Ontology(InputStream config, String version) throws JAXBException, ImportException {
        Inputs in = Inputs.load(config);
        
        // Classify ontology from stated form
        System.out.println("Classifying ontology");
        IFactory factory = new CoreFactory();
        NormalisedOntology no = new NormalisedOntology(factory);
        System.out.println("Importing axioms");
        
        RF2Importer imp = new RF2Importer(in);
        
        Iterator<Ontology> it = imp.getOntologyVersions(new NullProgressMonitor());
        
        while(it.hasNext()) {
            Ontology ont = it.next();
            if(ont.getVersion().equals(version)) {
                System.out.println("Loading axioms");
                no.loadAxioms(new HashSet<Axiom>((Collection<? extends Axiom>) ont.getStatedAxioms()));
                System.out.println("Running classification");
                no.classify();
                System.out.println("Computing taxonomy");
                no.buildTaxonomy();
                System.out.println("Done");
                
                System.gc();
                
                RF2Input rf2In = in.getRf2Inputs().get(0);
                ModuleInfo modInfo = rf2In.getModules().get(0);
                Version ver = modInfo.getVersions().get(0);
                InputStream canonical = this.getClass().getResourceAsStream(
                        rf2In.getRelationshipsFiles().iterator().next());
                
                System.out.println("Comparing with canonical ontology");
                String isAId = ver.getMetadata().get("isAId");
                List<String> problems = new ArrayList<String>();
                
                System.out.println("Loading rows from canonical table");
                Map<String, List<RelationshipRow>> allRows = new HashMap<String, List<RelationshipRow>>();
                BufferedReader br = null;
                try {
                    br = new BufferedReader(new InputStreamReader(canonical));
                    String line = br.readLine(); // skip first line
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
                        int idx8 = line.indexOf('\t', idx7 + 1);
                        int idx9 = line.indexOf('\t', idx8 + 1);

                        // 0..idx1 == id
                        // idx1+1..idx2 == effectiveTime
                        // idx2+1..idx3 == active
                        // idx3+1..idx4 == moduleId
                        // idx4+1..idx5 == sourceId
                        // idx5+1..idx6 == destinationId
                        // idx6+1..idx7 == relationshipGroup
                        // idx7+1..idx8 == typeId
                        // idx8+1..idx9 == characteristicTypeId
                        // idx9+1..end == modifierId

                        if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || 
                                idx5 < 0 || idx6 < 0 || idx7 < 0 || idx8 < 0 || 
                                idx9 < 0) {
                            throw new RuntimeException(
                                    "Concepts: Mis-formatted "
                                    + "line, expected 10 tab-separated fields, "
                                    + "got: " + line);
                        }
                        
                        final String id = line.substring(0, idx1);
                        final String effectiveTime = line.substring(idx1+1, 
                                idx2);
                        final String active = line.substring(idx2+1,  idx3);
                        final String modId = line.substring(idx3+1, idx4);
                        final String conceptId1 = line.substring(idx4 + 1, 
                                idx5);
                        final String conceptId2 = line.substring(idx5 + 1, 
                                idx6);
                        //final String relGroup = line.substring(idx6+1, idx7);
                        final String relId = line.substring(idx7 + 1, idx8);
                        //final String charTypeId = line.substring(idx8+1, idx9);
                        //final String modifierId = line.substring(idx9+1);
                        
                        List<RelationshipRow> l = allRows.get(id+"_"+modId);
                        if(l == null) {
                            l = new ArrayList<RelationshipRow>();
                            allRows.put(id+"_"+modId, l);
                        }
                        l.add(new RelationshipRow(id, effectiveTime, active, 
                                modId, conceptId1, conceptId2, "", relId, 
                                "", ""));
                    }
                } catch(IOException e) {
                    e.printStackTrace();
                    Assert.assertTrue(false);
                } finally {
                    if(br != null) {
                        try { br.close(); } catch(Exception e) {}
                    }
                }
                
                System.gc();
                
                System.out.println("Filtering rows");
                // Remove old versions - has to be module-aware
                List<RelationshipRow> filteredRows = 
                        new ArrayList<RelationshipRow>();
                
                for(String key : allRows.keySet()) {
                    List<RelationshipRow> rows = allRows.get(key);
                    int mostRecent = Integer.MIN_VALUE;
                    RelationshipRow theOne = null;
                    for(RelationshipRow row : rows) {
                        int time = Integer.parseInt(row.getEffectiveTime());
                        if(time > mostRecent) {
                            mostRecent = time;
                            theOne = row;
                        }
                    }
                    if(theOne.getActive().equals("1") && 
                            theOne.getTypeId().equals(isAId)) {
                        filteredRows.add(theOne);
                    }
                }
                
                allRows = null;
                System.gc();
                
                System.out.println("Building canonical parents");
                Map<String, Set<String>> canonicalParents = 
                        new TreeMap<String, Set<String>>();
                
                for(RelationshipRow row : filteredRows) {
                    Set<String> parents = canonicalParents.get(
                            row.getSourceId());
                    if (parents == null) {
                        parents = new HashSet<String>();
                        canonicalParents.put(row.getSourceId(), parents);
                    }
                    parents.add(row.getDestinationId());
                }
                
                compareWithCanonical(canonicalParents, no, isAId, problems);
                
                for (String problem : problems) {
                    System.err.println(problem);
                }

                Assert.assertTrue(problems.isEmpty());
            }
            break;
        }
    }
    
    protected void compareWithCanonical(Map<String, Set<String>> canonicalParents, 
            NormalisedOntology no, String isAId, 
            List<String> problems) {
        System.out.println("Build taxonomy from canonical table");

        final String top = "_top_";
        final String bottom = "_bottom_";
        Map<String, Set<String>> canonicalEquivs = 
                new TreeMap<String, Set<String>>();
        Set<String> topSet = new HashSet<String>();
        topSet.add(top);
        canonicalEquivs.put(top, topSet);
        for (String key : canonicalParents.keySet()) {
            Set<String> eq = new TreeSet<String>();
            eq.add(key);
            canonicalEquivs.put(key, eq);
            Set<String> parents = canonicalParents.get(key);
            if (parents == null) {
                // Create the equivalent set with key
                Set<String> val = new TreeSet<String>();
                val.add(key);
                canonicalEquivs.put(key, val);
                continue;
            }
            for (String parent : parents) {
                Set<String> grandpas = canonicalParents.get(parent);
                if (grandpas != null && grandpas.contains(key)) {
                    // Concepts are equivalent
                    Set<String> equivs1 = canonicalEquivs.get(parent);
                    if (equivs1 == null)
                        equivs1 = new TreeSet<String>();
                    equivs1.add(key);
                    equivs1.add(parent);
                    Set<String> equivs2 = canonicalEquivs.get(key);
                    if (equivs2 == null)
                        equivs2 = new TreeSet<String>();
                    equivs2.add(key);
                    equivs2.add(parent);
                    equivs1.addAll(equivs2);
                    canonicalEquivs.put(key, equivs1);
                    canonicalEquivs.put(parent, equivs1);
                }
            }
        }
        
        // Compare canonical and classified
        Map<String, Node> tax = no.getTaxonomy();
        
        for (Object key : tax.keySet()) {
            
            String concept = null;
            if(key == au.csiro.ontology.model.NamedConcept.TOP) {
                concept = top;
            } else if(key == au.csiro.ontology.model.NamedConcept.BOTTOM){
                concept = bottom;
            } else {
                concept = (String) key; 
            }
            
            Node ps = null;
            
            if(key instanceof String) {
                ps = no.getEquivalents((String)key);
            } else if(key == NamedConcept.TOP) {
                ps = no.getTopNode();
            } else if(key == NamedConcept.BOTTOM) {
                ps = no.getBottomNode();
            }

            // Actual equivalents set
            Set<String> aeqs = new HashSet<String>();

            for (Object cid : ps.getEquivalentConcepts()) {
                if(cid == NamedConcept.TOP)
                    aeqs.add(top);
                else if(cid == NamedConcept.BOTTOM)
                    aeqs.add(bottom);
                else
                    aeqs.add((String)cid);
            }

            // Actual parents set
            Set<String> aps = new HashSet<String>();
            Set<Node> parents = ps.getParents();
            for (Node parent : parents) {
                for (Object pid : parent.getEquivalentConcepts()) {
                    if(pid == NamedConcept.TOP)
                        aps.add(top);
                    else if(pid == NamedConcept.BOTTOM)
                        aps.add(bottom);
                    else
                        aps.add((String)pid);
                }
            }
             
            // FIXME: BOTTOM is not connected and TOP is not assigned as a
            // parent of SNOMED_CT_CONCEPT
            if (bottom.equals(concept)
                    || "138875005".equals(concept))
                continue;

            Set<String> cps = canonicalParents.get(concept);
            Set<String> ceqs = canonicalEquivs.get(concept);

            // Compare both sets
            if (cps == null) {
                cps = Collections.emptySet();
            }

            if (cps.size() != aps.size()) {
                problems.add("Problem with concept " + concept
                        + ": canonical parents size = " + cps.size() + " ("
                        + cps.toString() + ")" + " actual parents size = "
                        + aps.size() + " (" + aps.toString() + ")");
                continue;
            }

            for (String s : cps) {
                if (!aps.contains(s)) {
                    problems.add("Problem with concept " + concept
                            + ": parents do not contain concept " + s);
                }
            }

            if (ceqs == null) {
                ceqs = Collections.emptySet();
            }

            // Add the concept to its set of equivalents (every concept is
            // equivalent to itself)
            aeqs.add(concept);
            if (ceqs.size() != aeqs.size()) {
                problems.add("Problem with concept " + concept
                        + ": canonical equivalents size = " + ceqs.size()
                        + " actual equivalents size = " + aeqs.size());
            }
            for (String s : ceqs) {
                if (!aeqs.contains(s)) {
                    problems.add("Problem with concept " + concept
                            + ": equivalents do not contain concept " + s);
                }
            }
        }

    }
    
    /**
     * Classifies the stated version of an ontology in RF1 format and compares it to a correctly classified version 
     * (available in the canonical table).
     * 
     * @param concepts
     * @param relations
     * @param canonical
     */
    protected void testRF1Ontology(InputStream concepts, InputStream descriptions,
            InputStream relations, InputStream canonical, String version) {
        System.out.println("Classifying ontology");
        IFactory factory = new CoreFactory();
        NormalisedOntology no = new NormalisedOntology(factory);
        System.out.println("Importing axioms");
        RF1Importer imp = new RF1Importer(concepts, relations, version);
        Iterator<Ontology> it = imp.getOntologyVersions(new NullProgressMonitor());
        Ontology ont = it.next();
        
        System.out.println("Loading axioms");
        no.loadAxioms(new HashSet<Axiom>(ont.getStatedAxioms()));
        System.out.println("Running classification");
        no.classify();
        System.out.println("Computing taxonomy");
        no.buildTaxonomy();
        System.out.println("Done");
        
        System.out.println("Comparing with canonical ontology");
        String isAId = imp.getMetadata().getIsAId();
        List<String> problems = new ArrayList<String>();
        
        Map<String, Set<String>> canonicalParents = new TreeMap<String, Set<String>>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(canonical));
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

                // 0..idx1 == relationship id
                // idx1+1..idx2 == concept id1
                // idx2+1..idx3 == relationship type
                // idx3+1..idx4 == concept id2
                // idx4+1..idx5 == characteristic type
                // idx5+1..idx6 == refinability
                // idx6+1..end == relationship group

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0
                        || idx6 < 0) {
                    throw new RuntimeException("Concepts: Mis-formatted "
                            + "line, expected 7 tab-separated fields, "
                            + "got: " + line);
                }

                final String conceptId1 = line.substring(idx1 + 1, idx2);
                final String relId = line.substring(idx2 + 1, idx3);
                final String conceptId2 = line.substring(idx3 + 1, idx4);

                if (relId.equals(isAId)) {
                    Set<String> parents = canonicalParents.get(conceptId1);
                    if (parents == null) {
                        parents = new HashSet<String>();
                        canonicalParents.put(conceptId1, parents);
                    }
                    parents.add(conceptId2);
                }
            }
        } catch(IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        } finally {
            if(br != null) {
                try { br.close(); } catch(Exception e) {}
            }
        }
        
        compareWithCanonical(canonicalParents, no, isAId, problems);
        
        for (String problem : problems) {
            System.err.println(problem);
        }

        Assert.assertTrue(problems.isEmpty());
    }
    
    /**
     * Classifies the stated version of an ontology and compares it to a correctly classified version. All the classes 
     * of the ontologies are traversed and their direct parents are compared.
     * 
     * @param stated
     *            The {@link InputStream} of the stated ontology.
     * @param inferred
     *            The {@link InputStream} of the classified ontology.
     * @param ignoreBottom
     *            Indicates if the bottom node should be ignored in the comparison (some generated inferred files do not
     *            connect bottom).
     */
    protected void testOWLOntology(InputStream stated, InputStream inferred, boolean ignoreBottom) {
        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntologyFromOntologyDocument(stated);
            // Classify ontology from stated form
            SnorocketOWLReasoner c = new SnorocketOWLReasoner(ont, null, false);

            System.out.println("Classifying");
            c.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            // Load ontology from inferred form
            System.out.println("Loading inferred ontology");
            OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
            OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(inferred);

            System.out.println("Testing parent equality");
            int numOk = 0;
            int numWrong = 0;
            for (OWLClass cl : ont2.getClassesInSignature()) {

                // Ignore owl:nothing - some generated inferred files do not
                // connect childless nodes to owl:nothing
                if (ignoreBottom && cl.toStringID().equals("http://www.w3.org/2002/07/owl#Nothing"))
                    continue;

                Set<OWLClass> truth = new HashSet<OWLClass>();

                Set<OWLClassExpression> parents = cl.getSuperClasses(ont2);
                for (OWLClassExpression ocl : parents) {
                    if (!ocl.isAnonymous() && !ocl.isTopEntity()) {
                        truth.add(ocl.asOWLClass());
                    }
                }

                Set<OWLClass> classified = new HashSet<OWLClass>();
                NodeSet<OWLClass> otherParents = c.getSuperClasses(cl, true);
                classified.addAll(otherParents.getFlattened());
                
                // Remove top if present
                classified.remove(ont.getOWLOntologyManager().getOWLDataFactory().getOWLThing());

                // Assert parents are equal
                if (truth.size() != classified.size()) {
                    numWrong++;
                    System.out.println(cl.toStringID() + "("
                            + DebugUtils.getLabel(cl, ont) + ")");
                    System.out.println("Truth: " + formatClassSet(truth, ont));
                    System.out.println("Classified: " + formatClassSet(classified, ont));
                } else {
                    truth.removeAll(classified);

                    if (truth.isEmpty()) {
                        numOk++;
                    } else {
                        numWrong++;
                        System.out.println(cl.toStringID() + "(" + DebugUtils.getLabel(cl, ont) + ")");
                        System.out.println("Truth: " + formatClassSet(truth, ont));
                        System.out.println("Classified: " + formatClassSet(classified, ont));
                    }
                }
            }
            assertTrue("Num OK: " + numOk + " Num wrong: " + numWrong, numWrong == 0);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            assertTrue("Error loading ontologies", false);
        }
    }

    protected String formatClassSet(Set<OWLClass> set, OWLOntology ont) {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (OWLClass c : set) {
            sb.append(c.toStringID());
            sb.append("(");
            sb.append(DebugUtils.getLabel(c, ont));
            sb.append(") ");
        }
        sb.append("]");
        return sb.toString();
    }
}
