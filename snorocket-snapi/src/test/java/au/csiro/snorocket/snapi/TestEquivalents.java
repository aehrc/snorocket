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
package au.csiro.snorocket.snapi;

import org.junit.Ignore;
import org.junit.Test;

public class TestEquivalents extends AbstractTest {

    private static final String ISA = "is a";
    private static final String CONCEPT0 = "A";
    private static final String CONCEPT1 = "B";
    private static final String CONCEPT2 = "C";
    private static final String CONCEPT3 = "D";
    private static final String HAS_PROPERTY = "hasProperty";

	@Ignore
    @Test
    public void testSimpleEquivalency() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        rocket.addConcept(CONCEPT0, true);  // A
        rocket.addConcept(CONCEPT1, true);  // B
        rocket.addConcept(CONCEPT2, false); // C

        rocket.addRelationship(CONCEPT0, ISA, CONCEPT2, 0);
        rocket.addRelationship(CONCEPT1, ISA, CONCEPT2, 0);

        rocket.classify();
        
        checkExpectedRelationshipCount(rocket, 6);
        checkExpectedDistributionRelationshipCount(rocket, 0);
        checkExpectedEquivalenceCount(rocket, 1);
    }
	
	@Ignore
    @Test
    public void testNonincrementalEquivalency() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        initRocket(rocket);
        
        rocket.addConcept(CONCEPT3, true);
        rocket.addRelationship(CONCEPT3, HAS_PROPERTY, CONCEPT1, 0);
        rocket.addRelationship(CONCEPT3, ISA, CONCEPT2, 0);
        
        rocket.classify();
        
        checkExpectedRelationshipCount(rocket, 6);
        checkExpectedDistributionRelationshipCount(rocket, 3);
        checkExpectedEquivalenceCount(rocket, 1);
    }
	
	/*
	@Ignore
    @Test
    public void testIncrementalEquivalency() {
        final I_Snorocket rocket = getExtensionRocket();
        
        rocket.addConcept(CONCEPT3, true);
        rocket.addRelationship(CONCEPT3, HAS_PROPERTY, CONCEPT1, 0);
        rocket.addRelationship(CONCEPT3, ISA, CONCEPT2, 0);
        
        rocket.classify();
        
        checkExpectedRelationshipCount(rocket, 4);
        checkExpectedDistributionRelationshipCount(rocket, 3);
        // The following is undefined
//        checkExpectedEquivalenceCount(rocket, 1);
    }
    */
    
	/*
    private I_Snorocket getExtensionRocket() {
        I_Snorocket rocket1 = getInitialClassifiedRocket();
        System.out.println("----------------------------------");

        InputStream state = null;
        try {
            state = rocket1.getStream();
        } catch (IOException e) {
            e.printStackTrace();
            fail(e.toString());
        }
        assertNotNull(state);
        
        final I_Snorocket rocket2 = new Snorocket(state).createExtension();
        return rocket2;
    }
    */

    @SuppressWarnings("unused")
	private I_Snorocket getInitialClassifiedRocket() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        initRocket(rocket);
        rocket.classify();
        
        checkExpectedRelationshipCount(rocket, 2);
        checkExpectedDistributionRelationshipCount(rocket, 2);
        checkExpectedEquivalenceCount(rocket, 0);
        return rocket;
    }

    /**
     * A ][ hasProperty.B + C
     */
    private void initRocket(I_Snorocket rocket) {
        rocket.addConcept(CONCEPT0, true);  // A
        rocket.addConcept(CONCEPT1, false); // B
        rocket.addConcept(CONCEPT2, true);  // C
        rocket.addConcept(HAS_PROPERTY, false);
        rocket.addRelationship(CONCEPT0, HAS_PROPERTY, CONCEPT1, 0);
        rocket.addRelationship(CONCEPT0, ISA, CONCEPT2, 0);
    }

}
