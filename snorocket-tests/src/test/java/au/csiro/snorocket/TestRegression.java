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

public class TestRegression {
    
    /**
     * Small ontology test, mainly used to test the OWL test infrastructure.
     */
    @Ignore
    @Test
    public void testSmall() {
        System.out.println("Running testSmall");
        testOWLOntology(this.getClass().getResourceAsStream("/small_stated.owl"), 
                this.getClass().getResourceAsStream("/small_inferred.owl"), 
                false);
    }
    
    /**
     * Tests an anatomy ontology that uncovered a bug in the original Snorocket
     * implementation.
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
     * Tests the classification process using the 2011 version of SNOMED-CT and
     * the RF1 loader. The original SNOMED distribution files are used as input.
     */
    @Ignore
    @Test
    public void testSnomed_20110731_RF1() {
        System.out.println("Running testSnomed_20110731_RF1");      
        InputStream concepts = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/" +
                "sct1_Concepts_Core_INT_20110731.txt");
        InputStream descriptions = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/" +
                "sct1_Descriptions_en_INT_20110731.txt");
        InputStream relations = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/OtherResources/StatedRelationships/" +
                "res1_StatedRelationships_Core_INT_20110731.txt");
        InputStream canonical = this.getClass().getResourceAsStream(
                "/snomed_int_rf1/Terminology/Content/" +
                "sct1_Relationships_Core_INT_20110731.txt");
        
        testRF1Ontology(concepts, descriptions, relations, canonical, 
                "20110731");
    }
    
    /**
     * Tests the classification process using the January 2012 version of SNOMED-CT international edition and the RF2 
     * loader. The original SNOMED distribution files are used as input.
     * @throws ImportException 
     */
    @Ignore
    @Test
    public void testSnomed_20130131_RF2() throws ImportException {
        System.out.println("Running testSnomed_20130131_RF2"); 
        try {
            testRF2Ontology(this.getClass().getResourceAsStream(
                    "/config-snomed.xml"), "20130131");
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
    
    /**
     * Classifies the stated version of an ontology in RF2 format and compares
     * it to a correctly classified version (available in the canonical table).
     * This method assumes that only a single ontology is defined in the input
     * configuration file.
     * 
     * @param config The input object.
     * @throws JAXBException 
     * @throws ImportException 
     */
    private void testRF2Ontology(InputStream config, String version) throws JAXBException, ImportException {
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
    
    private void compareWithCanonical(Map<String, Set<String>> canonicalParents, 
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
     * Classifies the stated version of an ontology in RF1 format and compares
     * it to a correctly classified version (available in the canonical table).
     * 
     * @param concepts
     * @param relations
     * @param canonical
     */
    private void testRF1Ontology(InputStream concepts, InputStream descriptions,
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
     * Classifies the stated version of an ontology and compares it to a
     * correctly classified version. All the classes of the ontologies are
     * traversed and their direct parents are compared.
     * 
     * @param stated
     *            The {@link InputStream} of the stated ontology.
     * @param inferred
     *            The {@link InputStream} of the classified ontology.
     * @param ignoreBottom
     *            Indicates if the bottom node should be ignored in the
     *            comparison (some generated inferred files do not connect
     *            bottom).
     */
    private void testOWLOntology(InputStream stated, InputStream inferred, 
            boolean ignoreBottom) {
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
            OWLOntology ont2 = manager2.loadOntologyFromOntologyDocument(
                    inferred);

            System.out.println("Testing parent equality");
            int numOk = 0;
            int numWrong = 0;
            for (OWLClass cl : ont2.getClassesInSignature()) {

                // Ignore owl:nothing - some generated inferred files do not
                // connect childless nodes to owl:nothing
                if (ignoreBottom
                        && cl.toStringID().equals(
                                "http://www.w3.org/2002/07/owl#Nothing"))
                    continue;

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

    private String formatClassSet(Set<OWLClass> set, OWLOntology ont) {
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
