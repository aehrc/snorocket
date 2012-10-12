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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.snapi.I_Snorocket.I_Callback;

public class TestIncremental {

    private static final String HAS_PROPERTY = "hasProperty";
    private static final String CONCEPT0 = "common_cold";
    private static final String CONCEPT1 = "human";
    private static final String CONCEPT2 = "animal";
    private static final String CONCEPT3 = "disease";
    private static final String ISA = "is_a";

    @Before
    public void setUp() throws Exception {
    }
	
	@Ignore
    @Test
    public void createEmptyInitialState() throws IOException {
        I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        rocket.classify();
        InputStream out = rocket.getStream();
        assertNotNull(out);
    }
	
	/*
	@Ignore
    @Test
    public void createAndRestoreEmptyInitialState() throws IOException {
        I_Snorocket rocket1 = new Snorocket();
        rocket1.setIsa(ISA);
        rocket1.classify();
        InputStream state = rocket1.getStream();
        assertNotNull(state);
        
        I_Snorocket rocket2 = new Snorocket(state);
        rocket2.classify();
        rocket2.getRelationships(new I_Snorocket.I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                fail("No new relationships expected.");
            }
        });
    }
    */
    
	@Ignore
    @Test
    public void createEmptyLocalExtension() {
        I_Snorocket rocket1 = new Snorocket();

        try {
            rocket1.createExtension();
            fail("createExtension before classify() should fail.");
        } catch (IllegalArgumentException e) {
            // ignore expected exception for null classification since we haven't classified yet
        }

        rocket1.setIsa(ISA);
        rocket1.classify();
        I_Snorocket rocket2 = rocket1.createExtension();
        assertNotNull(rocket2);
    }
    
	@Ignore
    @Test
    public void createNonEmptyLocalExtension() {
        System.out.println("---------------------------------- createNonEmptyLocalExtension");
        I_Snorocket rocket1 = getInitialClassifiedRocket();
        System.out.println("----------------------------------");
        
        final I_Snorocket rocket2 = rocket1.createExtension();
        classifyExtraIsaRelationshipWithExistingConcept(rocket2);
    }
	
	/*
	@Ignore
    @Test
    public void createNonEmptySerialisedExtensionWithExistingConceptAndIsa() throws IOException {
        System.out.println("---------------------------------- createNonEmptySerialisedExtensionWithExistingConceptAndIsa");
        final I_Snorocket rocket2 = getExtensionRocket();
        classifyExtraIsaRelationshipWithExistingConcept(rocket2);
    }
    */
    
	/*
	@Ignore
    @Test
    public void createNonEmptySerialisedExtensionWithNewConceptAndIsa() throws IOException {
        System.out.println("---------------------------------- createNonEmptySerialisedExtensionWithNewConceptAndIsa");
        final I_Snorocket rocket2 = getExtensionRocket();
        classifyExtraIsaRelationshipWithNewConcept(rocket2);
    }
    */
    
    /**
     * C [ hasProperty.A
     * B [ hasProperty.C
     */
	/*
	 @Ignore
    @Test
    public void createNonEmptySerialisedExtensionWithHasProperty() throws IOException {
        System.out.println("---------------------------------- createNonEmptySerialisedExtensionWithHasProperty");
        final I_Snorocket rocket2 = getExtensionRocket();
        classifyExtraHasPropertyRelationshipWithExistingConcept(rocket2);
    }
    */
    
    /**
     * C [ hasProperty.A
     * X [ hasProperty.C
     */
	/*
	 @Ignore
    @Test
    public void createNonEmptySerialisedExtensionWithNewConceptAndHasProperty() throws IOException {
        System.out.println("---------------------------------- createNonEmptySerialisedExtensionWithNewConceptAndHasProperty");
        final I_Snorocket rocket2 = getExtensionRocket();
        classifyExtraHasPropertyRelationshipWithNewConcept(rocket2);
    }
    */

    /**
     * C [ hasProperty.A
     */
	 /*
    private I_Snorocket getExtensionRocket() throws IOException {
        I_Snorocket rocket1 = getInitialClassifiedRocket();
        System.out.println("----------------------------------");

        InputStream state = rocket1.getStream();
        assertNotNull(state);
        
        final I_Snorocket rocket2 = new Snorocket(state).createExtension();
        return rocket2;
    }
    */

    /**
     * C [ hasProperty.A
     */
    private void initRocket(I_Snorocket rocket) {
        rocket.addConcept(CONCEPT0, false);      // A
//        rocket.addConcept(CONCEPT1, false);      // B
        rocket.addConcept(CONCEPT2, false);      // C
        rocket.addConcept(HAS_PROPERTY, false);
        rocket.addRelationship(CONCEPT2, HAS_PROPERTY, CONCEPT0, 0);   // C [ hasProperty.A
    }

    /**
     * C [ hasProperty.A
     */
    private I_Snorocket getInitialClassifiedRocket() {
        I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        initRocket(rocket);
        rocket.classify();
        
        checkExpectedRelationshipCount(rocket, 1);
        checkExpectedDistributionRelationshipCount(rocket, 1);
        return rocket;
    }

    private void classifyExtraIsaRelationshipWithExistingConcept(I_Snorocket rocket) {
        rocket.addRelationship(CONCEPT1, ISA, CONCEPT2, 0);      // B [ C
        rocket.classify();

        checkExpectedRelationshipCount(rocket, 2);
        checkExpectedDistributionRelationshipCount(rocket, 2);
    }

    private void classifyExtraIsaRelationshipWithNewConcept(I_Snorocket rocket) {
        rocket.addRelationship(CONCEPT3, ISA, CONCEPT2, 0);      // X [ C
        rocket.classify();

        checkExpectedRelationshipCount(rocket, 2);
        checkExpectedDistributionRelationshipCount(rocket, 2);
    }

    /**
     * B [ hasProperty.C
     */
    private void classifyExtraHasPropertyRelationshipWithExistingConcept(I_Snorocket rocket) {
        rocket.addRelationship(CONCEPT1, HAS_PROPERTY, CONCEPT2, 0);      // B [ hasProperty.C
        rocket.classify();

        checkExpectedRelationshipCount(rocket, 1);
        checkExpectedDistributionRelationshipCount(rocket, 1);
    }

    /**
     * X [ hasProperty.C
     */
    private void classifyExtraHasPropertyRelationshipWithNewConcept(I_Snorocket rocket) {
        rocket.addRelationship(CONCEPT3, HAS_PROPERTY, CONCEPT2, 0);      // X [ hasProperty.C
        rocket.classify();

        checkExpectedRelationshipCount(rocket, 1);
        checkExpectedDistributionRelationshipCount(rocket, 1);
    }

    private void checkExpectedRelationshipCount(I_Snorocket rocket, int expected) {
        final int[] counter = {0};
        counter[0] = 0;
        rocket.getRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                System.out.println(">>>>>>>>>>>>> " + conceptId1 + " " + roleId + " " + conceptId2);
                assertEquals(0, group);
                counter[0]++;
            }
        });
        assertEquals(expected, counter[0]);
    }

    private void checkExpectedDistributionRelationshipCount(I_Snorocket rocket, int expected) {
        final int[] counter = {0};
        counter[0] = 0;
        rocket.getDistributionFormRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                System.out.println("======== " + conceptId1 + " " + roleId + " " + conceptId2);
                assertEquals(0, group);
                counter[0]++;
            }
        });
        assertEquals(expected, counter[0]);
    }
    
}
