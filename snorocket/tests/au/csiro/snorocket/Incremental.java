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

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.R;
//import au.csiro.snorocket.core.NormalisedOntology.Classification;
import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

public class Incremental {

    private static final String CONCEPT0 = "A";
    private static final String CONCEPT1 = "B";
    private static final String CONCEPT2 = "C";

    private static final String HAS_PROPERTY = "hasProperty";

    @Before
    public void setUp() throws Exception {
        System.err.println("----------------------");
    }
    
    /*
    @Test
    public void createSimpleExtension() {
        IFactory f1 = new Factory();
        Set<Inclusion> set1 = new HashSet<Inclusion>();
        set1.add(new GCI(f1.getConcept("A"), new Concept(f1.getConcept("B"))));
        NormalisedOntology ont1 = new NormalisedOntology(f1, set1);
        Classification c1 = ont1.getClassification();
        
        assertEquals(4, f1.getTotalConcepts());
        assertEquals(4, getSize("createSimpleExtension, c1", f1, c1.getSubsumptions())); // TOP, BOTTOM, "A", "B"
        
        IFactory f2 = c1.getExtensionFactory();
        Set<Inclusion> set2 = new HashSet<Inclusion>();
        set2.add(new GCI(f2.getConcept("A"), new Concept(f2.getConcept("C"))));
        NormalisedOntology ont2 = c1.getExtensionOntology(f2, set2);
        Classification c2 = ont2.getClassification();
        
        assertEquals(4, f1.getTotalConcepts());
        for (int i = 0; i < f2.getTotalConcepts(); i++) {
            System.err.println(i+"\t"+f2.lookupConceptId(i));
        }
        assertEquals(5, f2.getTotalConcepts());
//        assertEquals(5, getSize("createSimpleExtension, c2", f2, c2.getSubsumptions())); // TOP, BOTTOM, "A", "B", "C"
    }
    */

    private int getSize(final String label, final IFactory f, final IConceptMap<IConceptSet> s1) {
        System.err.println(label);
        System.err.println("====");
        int size = 0;
        for (final IntIterator itr = s1.keyIterator(); itr.hasNext(); ) {
            int k = itr.next();
            size++;
            System.err.print(f.lookupConceptId(k) + " isa ");

            final IConceptSet conceptSet = s1.get(k);
            for (final IntIterator itr2 = conceptSet.iterator(); itr2.hasNext(); ) {
                System.err.print(f.lookupConceptId(itr2.next()));
                if (itr2.hasNext()) {
                    System.err.print(", ");
                }
            }
            
            System.err.println();
        }
        System.err.println("====");
        
        return size;
    }
    
    private int printRels(IFactory f, R rels) {
        final int numConcepts = f.getTotalConcepts();
        final int numRoles = f.getTotalRoles();
        
        final StringBuilder sb = new StringBuilder();
        
        int size = 0;
        for (int i = 0; i < numConcepts; i++) {
            for (int j = 0; j < numRoles; j++) {
                final IConceptSet set = rels.lookupB(i, j);
                
                for (final IntIterator itr = set.iterator(); itr.hasNext(); ) {
                    int k = itr.next();
                    sb
                        .append(f.lookupConceptId(i))
                        .append(" ")
                        .append(f.lookupRoleId(j))
                        .append(" ")
                        .append(f.lookupConceptId(k))
                        .append("\n");
                    size++;
                }
            }
        }
        
        System.err.println("RELS:");
        System.err.println(sb);
        
        return size;
    }
    
    /*
    @Test
    public void createNonEmptyExtension() {
        IFactory f1 = new Factory();
        Set<Inclusion> set1 = new HashSet<Inclusion>();
        set1.add(new GCI(f1.getConcept("A"), new Concept(f1.getConcept("B"))));
        NormalisedOntology ont1 = new NormalisedOntology(f1, set1);
        Classification c1 = ont1.getClassification();
        
        IFactory f2 = c1.getExtensionFactory();
        final Set<Inclusion> set2 = new HashSet<Inclusion>();
        set2.add(getInclusion2(f2));
        set2.add(getInclusion1(f2));
        NormalisedOntology ont2 = c1.getExtensionOntology(f2, set2);
        Classification c2 = ont2.getClassification();

        printRels(f2, c2.getRelationships());
        
        assertEquals(3, getSize("createNonEmptyExtension, c2", f2, c2.getSubsumptions()));      // A, B, C
    }
    */
    
    private GCI getInclusion1(IFactory f) {
        return new GCI(f.getConcept(CONCEPT1), new Concept(f.getConcept(CONCEPT2)));
    }

    private GCI getInclusion2(IFactory f) {
        return new GCI(f.getConcept(CONCEPT2), new Existential(f.getRole(HAS_PROPERTY), new Concept(f.getConcept(CONCEPT0))));
    }
    
    /*
    @Test
    public void createExtension() {
        IFactory f1 = new Factory();
        Set<Inclusion> set1 = new HashSet<Inclusion>();
        set1.add(getInclusion1(f1));
        NormalisedOntology ont1 = new NormalisedOntology(f1, set1);
        Classification c1 = ont1.getClassification();

        assertEquals(4, getSize("pre-extension", f1, c1.getSubsumptions()));
        assertEquals(0, printRels(f1, c1.getRelationships()));
        
        IFactory f2 = c1.getExtensionFactory();
        final Set<Inclusion> set2 = new HashSet<Inclusion>();
        set2.add(getInclusion2(f2));
        NormalisedOntology ont2 = c1.getExtensionOntology(f2, set2);
        Classification c2 = ont2.getClassification();

        assertEquals(1, getSize("extension", f2, c2.getSubsumptions()));
        assertEquals(2, printRels(f2, c2.getRelationships()));
    }
	*/
    
    
}
