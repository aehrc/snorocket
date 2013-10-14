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

package au.csiro.snorocket;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;

import au.csiro.ontology.importer.ImportException;
import au.csiro.snorocket.owlapi.SnorocketOWLReasoner;
import au.csiro.snorocket.owlapi.util.DebugUtils;

public class TestRegression extends AbstractTest {
    
    /**
     * Small ontology test, mainly used to test the OWL test infrastructure.
     */
    @Test
    public void testSmall() {
        System.out.println("Running testSmall");
        testOWLOntology(this.getClass().getResourceAsStream("/small_stated.owl"), 
                this.getClass().getResourceAsStream("/small_inferred.owl"), 
                false);
    }
    
    /**
     * Tests an anatomy ontology that uncovered a bug in the original Snorocket implementation.
     * 
     * TODO: move these files to external projects and add dependency.
     */
    @Ignore
    @Test
    public void testAnatomy2012() {
        System.out.println("Running testAnatomy2012");
        InputStream stated = this.getClass().getResourceAsStream("/anatomy_2012_stated.owl");
        InputStream inferred = this.getClass().getResourceAsStream("/anatomy_2012_inferred.owl");
        testOWLOntology(stated, inferred, false);
    }
    
    /**
     * Tests the classification process using the 2011 version of SNOMED-CT and the RF1 loader. The original SNOMED 
     * distribution files are used as input.
     */
    @Test
    public void testSnomed_20110731_RF1() {
        System.out.println("Running testSnomed_20110731_RF1");      
        InputStream concepts = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/sct1_Concepts_Core_INT_20110731.txt");
        InputStream descriptions = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/sct1_Descriptions_en_INT_20110731.txt");
        InputStream relations = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/OtherResources/StatedRelationships/res1_StatedRelationships_Core_INT_20110731.txt");
        InputStream canonical = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/sct1_Relationships_Core_INT_20110731.txt");
        
        testRF1Ontology(concepts, descriptions, relations, canonical, "20110731");
    }
    
    /**
     * Tests the classification process using the January 2012 version of SNOMED-CT international edition and the RF2 
     * loader. The original SNOMED distribution files are used as input.
     * @throws ImportException 
     */
    @Test
    public void testSnomed_20130131_RF2() throws ImportException {
        System.out.println("Running testSnomed_20130131_RF2"); 
        try {
            testRF2Ontology(this.getClass().getResourceAsStream("/config-snomed.xml"), "20130131");
        } catch (JAXBException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
    }

    /**
     * Tests the classification of the AMT v3 20121231 ontology by using the OWL functional exported file and comparing
     * the classification output with the classification results using Fact++.
     */
    @Test
    public void testAMT_20130204() {
        System.out.println("Running testAMT_20130204");
        //InputStream stated = this.getClass().getResourceAsStream("/amtv3int.owl");
        InputStream stated = this.getClass().getResourceAsStream("/amt_v3_owl/amtv3.owl");
        InputStream inferred = this.getClass().getResourceAsStream("/amt_v3_owl/amtv3_inferred.owl");
        testOWLOntology(stated, inferred, false);
    }

    /**
     * Tests the sub-second incremental classification functionality by doing
     * the following:
     * 
     * <ol>
     * <li>The concept "Concretion of appendix" is removed from SNOMED.</li>
     * <li>This ontology is classified.</li>
     * <li>The axioms that were removed are added programmatically to the
     * ontology (see axioms below).</li>
     * <li>The new ontology is reclassified and the time taken to do so is
     * measured.</li>
     * <li>If time is below 1 second the test is successful.</li>
     * </ol>
     * 
     * Declaration(Class(:SCT_24764000)) AnnotationAssertion(rdfs:label
     * :SCT_24764000 "Concretion of appendix (disorder)" )
     * EquivalentClasses(:SCT_24764000 ObjectIntersectionOf(:SCT_18526009
     * ObjectSomeValuesFrom(:RoleGroup ObjectIntersectionOf(
     * ObjectSomeValuesFrom(:SCT_116676008 :SCT_56381008)
     * ObjectSomeValuesFrom(:SCT_363698007 :SCT_66754008) ) ) ) )
     * 
     * TODO: move these files to external projects and add dependency.
     */
    @Ignore
    @Test
    public void testSnomed_20120131_Incremental() {
        System.out.println("Running testSnomed_20120131_Incremental");
        
        InputStream stated = this.getClass().getResourceAsStream("/snomed_20120131_inc_stated.owl");
        InputStream inferred = this.getClass().getResourceAsStream("/snomed_20120131_inferred.owl");

        try {
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntologyFromOntologyDocument(stated);

            // Classify ontology from stated form
            SnorocketOWLReasoner c = new SnorocketOWLReasoner(ont, null, true);

            System.out.println("Classifying");
            c.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            // Add additional axioms
            PrefixManager pm = new DefaultPrefixManager(
                    "http://www.ihtsdo.org/");
            OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
            OWLClass concr = df.getOWLClass(":SCT_24764000", pm);
            OWLAxiom a1 = df
                    .getOWLAnnotationAssertionAxiom(
                            concr.getIRI(),
                            df.getOWLAnnotation(
                                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL
                                            .getIRI()),
                                    df.getOWLLiteral("Concretion of appendix (disorder)")));
            OWLAxiom a2 = df.getOWLDeclarationAxiom(concr);
            OWLAxiom a3 = df
                    .getOWLEquivalentClassesAxiom(
                            concr,
                            df.getOWLObjectIntersectionOf(
                                    df.getOWLClass(":SCT_18526009", pm),
                                    df.getOWLObjectSomeValuesFrom(
                                            df.getOWLObjectProperty(
                                                    ":RoleGroup", pm),
                                            df.getOWLObjectIntersectionOf(
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(
                                                                    ":SCT_116676008",
                                                                    pm),
                                                            df.getOWLClass(
                                                                    ":SCT_56381008",
                                                                    pm)),
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(
                                                                    ":SCT_363698007",
                                                                    pm),
                                                            df.getOWLClass(
                                                                    ":SCT_66754008",
                                                                    pm))))));
            manager.addAxiom(ont, a1);
            manager.addAxiom(ont, a2);
            manager.addAxiom(ont, a3);

            // Classify again
            long start = System.currentTimeMillis();
            c.flush();

            // Measure time
            long time = System.currentTimeMillis() - start;
            System.out.println("Classified incrementally in: " + time+"ms");
            // Assert.assertTrue("Incremental classification took longer than 1 "
            // + "second: "+time, time < 1000);

            // Load ontology from inferred form to test for correctness
            OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
            OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(inferred);

            System.out.println("Testing parent equality");
            int numOk = 0;
            int numWrong = 0;
            for (OWLClass cl : ont2.getClassesInSignature()) {
                Set<OWLClass> truth = new HashSet<OWLClass>();

                Set<OWLClassExpression> parents = cl.getSuperClasses(ont2);
                for (OWLClassExpression ocl : parents) {
                    if (!ocl.isAnonymous()) {
                        truth.add(ocl.asOWLClass());
                    }
                }

                Set<OWLClass> classified = new HashSet<OWLClass>();
                NodeSet<OWLClass> otherParents = c.getSuperClasses(cl, true);
                classified.addAll(otherParents.getFlattened());

                // Assert parents are equal
                if (truth.size() != classified.size()) {
                    numWrong++;
                    System.out.println(cl.toStringID() + "("
                            + DebugUtils.getLabel(cl, ont) + ")");
                    System.out.println("Truth: " + formatClassSet(truth, ont));
                    System.out.println("Classified: "
                            + formatClassSet(classified, ont));
                } else {
                    truth.removeAll(classified);

                    if (truth.isEmpty()) {
                        numOk++;
                    } else {
                        numWrong++;
                        System.out.println(cl.toStringID() + "("
                                + DebugUtils.getLabel(cl, ont) + ")");
                        System.out.println("Truth: "
                                + formatClassSet(truth, ont));
                        System.out.println("Classified: "
                                + formatClassSet(classified, ont));
                    }
                }
            }
            assertTrue("Num OK: " + numOk + " Num wrong: " + numWrong,
                    numWrong == 0);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            assertTrue("Error loading ontologies", false);
        }
    }

    /**
     * Similar to testIncremental() but removes two concepts from the original
     * ontology. The removed axioms are:
     * 
     * Declaration(Class(:SCT_24764000)) AnnotationAssertion(rdfs:label
     * :SCT_24764000 "Concretion of appendix (disorder)" )
     * EquivalentClasses(:SCT_24764000 ObjectIntersectionOf(:SCT_18526009
     * ObjectSomeValuesFrom(:RoleGroup ObjectIntersectionOf(
     * ObjectSomeValuesFrom(:SCT_116676008 :SCT_56381008)
     * ObjectSomeValuesFrom(:SCT_363698007 :SCT_66754008) ) ) ) )
     * 
     * Declaration(Class(:SCT_300307005)) AnnotationAssertion(rdfs:label
     * :SCT_300307005 "Finding of appendix (finding)" )
     * EquivalentClasses(:SCT_300307005 ObjectIntersectionOf(:SCT_118436003
     * ObjectSomeValuesFrom(:RoleGroup ObjectSomeValuesFrom(:SCT_363698007
     * :SCT_66754008) ) ) ) SubClassOf(:SCT_300308000
     * ObjectIntersectionOf(:SCT_300307005 ObjectSomeValuesFrom(:RoleGroup
     * ObjectSomeValuesFrom(:SCT_363698007 :SCT_66754008) ) ) )
     * SubClassOf(:SCT_300309008 ObjectIntersectionOf(:SCT_300307005
     * ObjectSomeValuesFrom(:RoleGroup ObjectSomeValuesFrom(:SCT_363698007
     * :SCT_66754008) ) ) ) SubClassOf(:SCT_422989001
     * ObjectIntersectionOf(:SCT_300307005 :SCT_395557000) )
     * 
     * TODO: move these files to external projects and add dependency.
     */
    @Ignore
    @Test
    public void testSnomed_20120131_Incremental2() {
        System.out.println("Running testSnomed_20120131_Incremental2");
        
        InputStream stated = this.getClass().getResourceAsStream(
                "/snomed_20120131_inc2_stated.owl");
        InputStream inferred = this.getClass().getResourceAsStream(
                "/snomed_20120131_inferred.owl");

        try {
            System.out.println("Loading stated ontology");
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntologyFromOntologyDocument(stated);

            // Classify ontology from stated form
            SnorocketOWLReasoner c = new SnorocketOWLReasoner(ont, null, true);

            System.out.println("Classifying");
            c.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            // Add additional axioms
            PrefixManager pm = new DefaultPrefixManager(
                    "http://www.ihtsdo.org/");
            OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
            OWLClass concr = df.getOWLClass(":SCT_24764000", pm);
            OWLAxiom a1 = df
                    .getOWLAnnotationAssertionAxiom(
                            concr.getIRI(),
                            df.getOWLAnnotation(
                                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL
                                            .getIRI()),
                                    df.getOWLLiteral("Concretion of appendix (disorder)")));
            OWLAxiom a2 = df.getOWLDeclarationAxiom(concr);
            OWLAxiom a3 = df
                    .getOWLEquivalentClassesAxiom(
                            concr,
                            df.getOWLObjectIntersectionOf(
                                    df.getOWLClass(":SCT_18526009", pm),
                                    df.getOWLObjectSomeValuesFrom(
                                            df.getOWLObjectProperty(
                                                    ":RoleGroup", pm),
                                            df.getOWLObjectIntersectionOf(
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(
                                                                    ":SCT_116676008",
                                                                    pm),
                                                            df.getOWLClass(
                                                                    ":SCT_56381008",
                                                                    pm)),
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(
                                                                    ":SCT_363698007",
                                                                    pm),
                                                            df.getOWLClass(
                                                                    ":SCT_66754008",
                                                                    pm))))));

            OWLClass concr2 = df.getOWLClass(":SCT_300307005", pm);
            OWLAxiom a4 = df
                    .getOWLAnnotationAssertionAxiom(
                            concr2.getIRI(),
                            df.getOWLAnnotation(
                                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL
                                            .getIRI()),
                                    df.getOWLLiteral("Finding of appendix (finding)")));

            OWLAxiom a5 = df.getOWLDeclarationAxiom(concr2);

            OWLAxiom a6 = df.getOWLEquivalentClassesAxiom(df.getOWLClass(
                    ":SCT_300307005", pm), df.getOWLObjectIntersectionOf(df
                    .getOWLClass(":SCT_118436003", pm), df
                    .getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(
                            ":RoleGroup", pm), df.getOWLObjectSomeValuesFrom(
                            df.getOWLObjectProperty(":SCT_363698007", pm),
                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a7 = df.getOWLSubClassOfAxiom(df.getOWLClass(
                    ":SCT_300308000", pm), df.getOWLObjectIntersectionOf(df
                    .getOWLClass("SCT_300307005", pm), df
                    .getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(
                            ":RoleGroup", pm), df.getOWLObjectSomeValuesFrom(
                            df.getOWLObjectProperty(":SCT_363698007", pm),
                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a8 = df.getOWLSubClassOfAxiom(df.getOWLClass(
                    ":SCT_300309008", pm), df.getOWLObjectIntersectionOf(df
                    .getOWLClass("SCT_300307005", pm), df
                    .getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(
                            ":RoleGroup", pm), df.getOWLObjectSomeValuesFrom(
                            df.getOWLObjectProperty(":SCT_363698007", pm),
                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a9 = df.getOWLSubClassOfAxiom(
                    df.getOWLClass(":SCT_422989001", pm),
                    df.getOWLObjectIntersectionOf(
                            df.getOWLClass("SCT_300307005", pm),
                            df.getOWLClass(":SCT_395557000", pm)));

            manager.addAxiom(ont, a1);
            manager.addAxiom(ont, a2);
            manager.addAxiom(ont, a3);
            manager.addAxiom(ont, a4);
            manager.addAxiom(ont, a5);
            manager.addAxiom(ont, a6);
            manager.addAxiom(ont, a7);
            manager.addAxiom(ont, a8);
            manager.addAxiom(ont, a9);

            // Classify again
            long start = System.currentTimeMillis();
            c.flush();

            // Measure time
            long time = System.currentTimeMillis() - start;
            System.out.println("Classified incrementally in: " + time+"ms");

            // Load ontology from inferred form to test for correctness
            System.out.println("Loading inferred ontology");
            OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
            OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(inferred);

            System.out.println("Testing parent equality");
            int numOk = 0;
            int numWrong = 0;
            for (OWLClass cl : ont2.getClassesInSignature()) {
                Set<OWLClass> truth = new HashSet<OWLClass>();

                Set<OWLClassExpression> parents = cl.getSuperClasses(ont2);
                for (OWLClassExpression ocl : parents) {
                    if (!ocl.isAnonymous()) {
                        truth.add(ocl.asOWLClass());
                    }
                }

                Set<OWLClass> classified = new HashSet<OWLClass>();
                NodeSet<OWLClass> otherParents = c.getSuperClasses(cl, true);
                classified.addAll(otherParents.getFlattened());

                // Assert parents are equal
                if (truth.size() != classified.size()) {
                    numWrong++;
                    System.out.println(cl.toStringID() + "("
                            + DebugUtils.getLabel(cl, ont) + ")");
                    System.out.println("Truth: " + formatClassSet(truth, ont));
                    System.out.println("Classified: "
                            + formatClassSet(classified, ont));
                } else {
                    truth.removeAll(classified);

                    if (truth.isEmpty()) {
                        numOk++;
                    } else {
                        numWrong++;
                        System.out.println(cl.toStringID() + "("
                                + DebugUtils.getLabel(cl, ont) + ")");
                        System.out.println("Truth: "
                                + formatClassSet(truth, ont));
                        System.out.println("Classified: "
                                + formatClassSet(classified, ont));
                    }
                }
            }
            assertTrue("Num OK: " + numOk + " Num wrong: " + numWrong,
                    numWrong == 0);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            assertTrue("Error loading ontologies", false);
        }
    }

    /**
     * Similar to testIncremental2() but only one concept (:SCT_24764000) is
     * removed from the original ontology. This means that redundant axioms are
     * added programmatically.
     * 
     * TODO: move these files to external projects and add dependency.
     */
    @Ignore
    @Test
    public void testSnomed_20120131_Incremental3() {
        System.out.println("Running testSnomed_20120131_Incremental3");
        
        InputStream stated = this.getClass().getResourceAsStream("/snomed_20120131_inc_stated.owl");
        InputStream inferred = this.getClass().getResourceAsStream("/snomed_20120131_inferred.owl");

        try {
            System.out.println("Loading stated ontology");
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntologyFromOntologyDocument(stated);

            // Classify ontology from stated form
            SnorocketOWLReasoner c = new SnorocketOWLReasoner(ont, null, true);

            System.out.println("Classifying");
            c.precomputeInferences(InferenceType.CLASS_HIERARCHY);

            // Add additional axioms
            PrefixManager pm = new DefaultPrefixManager("http://www.ihtsdo.org/");
            OWLDataFactory df = ont.getOWLOntologyManager().getOWLDataFactory();
            OWLClass concr = df.getOWLClass(":SCT_24764000", pm);
            OWLAxiom a1 = df
                    .getOWLAnnotationAssertionAxiom(
                            concr.getIRI(),
                            df.getOWLAnnotation(
                                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL
                                            .getIRI()),
                                    df.getOWLLiteral("Concretion of appendix (disorder)")));
            OWLAxiom a2 = df.getOWLDeclarationAxiom(concr);
            OWLAxiom a3 = df
                    .getOWLEquivalentClassesAxiom(
                            concr,
                            df.getOWLObjectIntersectionOf(
                                    df.getOWLClass(":SCT_18526009", pm),
                                    df.getOWLObjectSomeValuesFrom(
                                            df.getOWLObjectProperty(
                                                    ":RoleGroup", pm),
                                            df.getOWLObjectIntersectionOf(
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(":SCT_116676008", pm),
                                                            df.getOWLClass(":SCT_56381008", pm)),
                                                    df.getOWLObjectSomeValuesFrom(
                                                            df.getOWLObjectProperty(":SCT_363698007", pm),
                                                            df.getOWLClass(":SCT_66754008", pm))))));

            OWLClass concr2 = df.getOWLClass(":SCT_300307005", pm);
            OWLAxiom a4 = df
                    .getOWLAnnotationAssertionAxiom(
                            concr2.getIRI(),
                            df.getOWLAnnotation(
                                    df.getOWLAnnotationProperty(OWLRDFVocabulary.RDFS_LABEL.getIRI()),
                                    df.getOWLLiteral("Finding of appendix (finding)")));

            OWLAxiom a5 = df.getOWLDeclarationAxiom(concr2);

            OWLAxiom a6 = df.getOWLEquivalentClassesAxiom(df.getOWLClass(
                    ":SCT_300307005", pm), df.getOWLObjectIntersectionOf(df
                    .getOWLClass(":SCT_118436003", pm), df
                    .getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(":RoleGroup", pm), 
                            df.getOWLObjectSomeValuesFrom(
                            df.getOWLObjectProperty(":SCT_363698007", pm),
                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a7 = df.getOWLSubClassOfAxiom(df.getOWLClass(
                    ":SCT_300308000", pm), df.getOWLObjectIntersectionOf(df.getOWLClass("SCT_300307005", pm), 
                            df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(
                                    ":RoleGroup", pm), df.getOWLObjectSomeValuesFrom(
                                            df.getOWLObjectProperty(":SCT_363698007", pm),
                                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a8 = df.getOWLSubClassOfAxiom(df.getOWLClass(
                    ":SCT_300309008", pm), df.getOWLObjectIntersectionOf(df.getOWLClass("SCT_300307005", pm), 
                            df.getOWLObjectSomeValuesFrom(df.getOWLObjectProperty(
                                    ":RoleGroup", pm), df.getOWLObjectSomeValuesFrom(
                                            df.getOWLObjectProperty(":SCT_363698007", pm),
                                            df.getOWLClass(":SCT_66754008", pm)))));

            OWLAxiom a9 = df.getOWLSubClassOfAxiom(
                    df.getOWLClass(":SCT_422989001", pm),
                    df.getOWLObjectIntersectionOf(
                            df.getOWLClass("SCT_300307005", pm),
                            df.getOWLClass(":SCT_395557000", pm)));

            manager.addAxiom(ont, a1);
            manager.addAxiom(ont, a2);
            manager.addAxiom(ont, a3);
            manager.addAxiom(ont, a4);
            manager.addAxiom(ont, a5);
            manager.addAxiom(ont, a6);
            manager.addAxiom(ont, a7);
            manager.addAxiom(ont, a8);
            manager.addAxiom(ont, a9);

            // Classify again
            long start = System.currentTimeMillis();
            c.flush();

            // Measure time
            long time = System.currentTimeMillis() - start;
            System.out.println("Classified incrementally in: " + time+"ms");

            // Assert.assertTrue("Incremental classification took longer than 1 "
            // + "second: "+time, time < 1000);

            // Load ontology from inferred form to test for correctness
            System.out.println("Loading inferred ontology");
            OWLOntologyManager manager2 = OWLManager.createOWLOntologyManager();
            OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(inferred);

            System.out.println("Testing parent equality");
            int numOk = 0;
            int numWrong = 0;
            for (OWLClass cl : ont2.getClassesInSignature()) {
                Set<OWLClass> truth = new HashSet<OWLClass>();

                Set<OWLClassExpression> parents = cl.getSuperClasses(ont2);
                for (OWLClassExpression ocl : parents) {
                    if (!ocl.isAnonymous()) {
                        truth.add(ocl.asOWLClass());
                    }
                }

                Set<OWLClass> classified = new HashSet<OWLClass>();
                NodeSet<OWLClass> otherParents = c.getSuperClasses(cl, true);
                classified.addAll(otherParents.getFlattened());

                // Assert parents are equal
                if (truth.size() != classified.size()) {
                    numWrong++;
                    System.out.println(cl.toStringID() + "("
                            + DebugUtils.getLabel(cl, ont) + ")");
                    System.out.println("Truth: " + formatClassSet(truth, ont));
                    System.out.println("Classified: "
                            + formatClassSet(classified, ont));
                } else {
                    truth.removeAll(classified);

                    if (truth.isEmpty()) {
                        numOk++;
                    } else {
                        numWrong++;
                        System.out.println(cl.toStringID() + "("
                                + DebugUtils.getLabel(cl, ont) + ")");
                        System.out.println("Truth: "
                                + formatClassSet(truth, ont));
                        System.out.println("Classified: "
                                + formatClassSet(classified, ont));
                    }
                }
            }
            assertTrue("Num OK: " + numOk + " Num wrong: " + numWrong,
                    numWrong == 0);
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            assertTrue("Error loading ontologies", false);
        }
    }

}
