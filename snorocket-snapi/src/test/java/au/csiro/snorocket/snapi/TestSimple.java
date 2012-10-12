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

import static org.junit.Assert.*;

import java.lang.reflect.Field;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.snapi.I_Snorocket.I_Callback;

/**
 * These aren't real tests yet...
 * 
 * @author law223
 *
 */
public class TestSimple {

    private static final String HAS_PROPERTY = "hasProperty";

    private static final String SNOMED_ISA = "116680003";

    final String CONCEPT0 = "concept 0";
    final String CONCEPT1 = "concept 1";
    final String CONCEPT2 = "concept 2";
    final String CONCEPT3 = "concept 3";

    private Snorocket rocket;

    private I_Callback printer;

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception {
        System.err.println("------------------------------");
        rocket = new Snorocket();
        rocket.setIsa(SNOMED_ISA);
        
        printer = new I_Callback() {

            public void addRelationship(String conceptId1, String roleId,
                                        String conceptId2, int group) {
                System.err.println(conceptId1 + " " + roleId + " " + conceptId2 + (group > 0 ? "\tG: " + group : ""));
            }
            
        };
    }

    /**
     * Test method for {@link au.csiro.snorocket.snapi.Snorocket#addConcept(java.lang.String, boolean)}.
     */
	@Ignore
    @Test
    public void testAddConceptStringBoolean() {
        rocket.addConcept(CONCEPT0, true);
        rocket.addConcept(CONCEPT1, true);
        rocket.addConcept(CONCEPT2, true);
        rocket.addConcept(HAS_PROPERTY, false);
        rocket.addRelationship(CONCEPT2, HAS_PROPERTY, CONCEPT0, 0);
        rocket.addRelationship(CONCEPT1, SNOMED_ISA, CONCEPT2, 0);
        
        final Factory factory = getFactory(rocket);

        final int total = factory.getTotalConcepts();
        for (int i = 0; i < total; i++) {
            System.err.println(i + "\t" + factory.lookupConceptId(i));
        }
        assertEquals("Expected Concepts: TOP, BOTTOM, our three concepts, and two roles", 7, total);

        rocket.classify();
        
        assertTrue(factory.conceptExists(CONCEPT1));
        assertTrue(factory.conceptExists(CONCEPT2));
        assertFalse(factory.conceptExists(CONCEPT3));

        int c1 = factory.getConcept(CONCEPT1);
        int r = factory.getRole(SNOMED_ISA);
        int c2 = factory.getConcept(CONCEPT2);
        System.err.println(c1 + " " + r + " " + c2);
        
        rocket.getDistributionFormRelationships(new I_Snorocket.I_Callback() {

            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                System.err.println(conceptId1 + " " + roleId + " " + conceptId2);
                
                assertTrue(factory.conceptExists(conceptId1));
                assertTrue(factory.roleExists(roleId));
                assertTrue(factory.conceptExists(conceptId2));
                
                int c1 = factory.getConcept(conceptId1);
                int r = factory.getRole(roleId);
                int c2 = factory.getConcept(conceptId2);
                System.err.println(c1 + " " + r + " " + c2);
            }
            
        });
    }

    private Factory getFactory(final I_Snorocket rocket) {
        Factory fX = null;
        try {
            Field f = rocket.getClass().getDeclaredField("factory");
            f.setAccessible(true);
            fX  = (Factory) f.get(rocket);
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final Factory factory = fX;
        return factory;
    }
	
	@Ignore
    @Test
    public void subsumptionInProperties() {
        rocket.addConcept("sick_animal", true);
        rocket.addConcept("animal", false);
        rocket.addConcept("person", false);
        rocket.addConcept("disease", false);
        rocket.addConcept("hasProperty", false);
        rocket.addConcept("cold", false);
        rocket.addConcept("tiger", false);
        rocket.addConcept("tiger with cold", false);

        rocket.addRelationship("person", SNOMED_ISA, "animal", 0);
        rocket.addRelationship("tiger", SNOMED_ISA, "animal", 0);
        rocket.addRelationship("sick_animal", SNOMED_ISA, "animal", 0);
        rocket.addRelationship("sick_animal", "hasProperty", "disease", 0);
        rocket.addRelationship("cold", SNOMED_ISA, "disease", 0);
        rocket.addRelationship("tiger_with_cold", SNOMED_ISA, "tiger", 0);
        rocket.addRelationship("tiger_with_cold", "hasProperty", "cold", 0);
        
        rocket.classify();
        
        rocket.getDistributionFormRelationships(printer);
    }
}
