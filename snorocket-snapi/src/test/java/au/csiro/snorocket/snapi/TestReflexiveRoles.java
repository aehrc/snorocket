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

import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.core.IFactory;

public class TestReflexiveRoles extends AbstractTest {

    private static final String ISA = "IsA";
    private static final String PART_OF = "partOf";
    private static final String SUB_PART = "subPart";
    private static final String FOOT = "Foot";
    private static final String LOWER_LEG = "Lower_Leg";
    private static final String BODY_PART = "Body_Part";
    private static final String XX = "XX";
    private static final String YY = "YY";
    private static final String AA = "AA";
    private static final String EXACT_LOCATION = "exactLocation";
    private static final String HAS_LOCATION = "hasLocation";
    private static final String FINGER = "Finger";
    private static final String HAND = "Hand";
    private static final String UPPER_LIMB = "Upper_Limb";
    private static final String AMPUTATION = "Amputation";
    private static final String AMPUTATION_OF_FINGER = "Amputation_Of_Finger";
    private static final String AMPUTATION_OF_HAND = "Amputation_Of_Hand";
    private static final String AMPUTATION_OF_UPPER_LIMB = "Amputation_Of_Upper_Limb";
    private static final String INJURY = "Injury";
    private static final String INJURY_TO_FINGER = "Injury_To_Finger";
    private static final String INJURY_TO_HAND = "Injury_To_Hand";
    private static final String INJURY_TO_UPPER_LIMB = "Injury_To_Upper_Limb";
    private static final String HAND_S = "Hand_S";
    private static final String HAND_P = "Hand_P";

    private static final String[] EMPTY_ARRAY = {};
    
    /**
     * Test illustrating the problem of reflexive roles interacting with the role grouping transformation
     * and thus the need for rocket.addRoleUngrouped()
     * 
     * <ul></ul>
     * RAW:<ol>
     * <li>e [ partOf
     * <li>XX [ partOf.YY
     * </ol>
     * 
     * COOKED (SNOMED/SNAPI Transform):<ol>
     * <li>e [ partOf
     * <li>XX [ rg.(partOf.YY)
     * </ol>
     * 
     * NORMALISED:<ol>
     * <li>e [ partOf
     * <li>XX [ rg.AA
     * <li>AA [ partOf.YY
     * </ol>
     * 
     * Thus, AA [ partOf.AA and YY [ partOf.YY but not XX [ partOf.XX
     * 
     */
	 @Ignore
    @Test
    public void simpleTest() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        rocket.addRoleComposition(EMPTY_ARRAY, PART_OF);
        rocket.addRoleNeverGrouped(PART_OF);
        // left identity
        rocket.addRoleComposition(new String[] {IFactory.ROLE_GROUP, PART_OF}, PART_OF);

        rocket.addRelationship(XX, PART_OF, YY, 0);


        rocket.classify();

        checkExpectedDistributionRelationshipCount(rocket, 3);
        expectDistributionRelationship(rocket, XX, PART_OF, YY, 0);
        expectDistributionRelationship(rocket, XX, PART_OF, XX, 0);
        expectDistributionRelationship(rocket, YY, PART_OF, YY, 0);
        checkExpectedDistributionRelationshipCount(rocket, 3);
        
    }
    
	@Ignore
    @Test
    public void simpleSubroleTest() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        rocket.addRoleComposition(EMPTY_ARRAY, PART_OF);        // reflexive
        rocket.addRoleNeverGrouped(PART_OF);
        rocket.addRoleNeverGrouped(SUB_PART);      // should ungrouped property be inherited?
        
//        rocket.addRoleComposition(new String[] {SUB_PART}, PART_OF);  // The other way to say the following
        rocket.addRelationship(SUB_PART, ISA, PART_OF, 0);
        
        rocket.addRelationship(FOOT, ISA, BODY_PART, 0);
        rocket.addRelationship(FOOT, SUB_PART, LOWER_LEG, 0);
        rocket.addRelationship(LOWER_LEG, ISA, BODY_PART, 0);
        
        rocket.classify();

//        checkExpectedDistributionRelationshipCount(rocket, 6);
        expectDistributionRelationship(rocket, FOOT, PART_OF, FOOT, 0);
        expectDistributionRelationship(rocket, FOOT, SUB_PART, LOWER_LEG, 0);
        expectDistributionRelationship(rocket, LOWER_LEG, PART_OF, LOWER_LEG, 0);
        checkExpectedDistributionRelationshipCount(rocket, 6);
        
    }
    
    /**
     * From "Replacing SEP-Triplets in SNOMED CT using Tractable Description Logic Operators", Fig 2
     * 
     * @see http://lat.inf.tu-dresden.de/research/papers/2007/SunBaaSchSpa-AIME-07.pdf
     */
	@Ignore
    @Test
    public void spackmanSEPTest() {
        final I_Snorocket rocket = new Snorocket();
        rocket.setIsa(ISA);
        
        rocket.addConcept(AMPUTATION_OF_FINGER, true);
        rocket.addConcept(AMPUTATION_OF_HAND, true);
        rocket.addConcept(AMPUTATION_OF_UPPER_LIMB, true);
        rocket.addConcept(INJURY_TO_FINGER, true);
        rocket.addConcept(INJURY_TO_HAND, true);
        rocket.addConcept(INJURY_TO_UPPER_LIMB, true);
        
        rocket.addRelationship(FINGER, ISA, BODY_PART, 0);                      // 1a
        rocket.addRelationship(FINGER, SUB_PART, HAND, 0);                      // 1b
        rocket.addRelationship(HAND, ISA, BODY_PART, 0);                        // 2a
        rocket.addRelationship(HAND, SUB_PART, UPPER_LIMB, 0);                  // 2b
        rocket.addRelationship(UPPER_LIMB, ISA, BODY_PART, 0);                  // 3
        rocket.addRelationship(AMPUTATION_OF_FINGER, ISA, AMPUTATION, 0);       // 4a
        rocket.addRelationship(AMPUTATION_OF_FINGER, EXACT_LOCATION, FINGER, 0);// 4b
        rocket.addRelationship(AMPUTATION_OF_HAND, ISA, AMPUTATION, 0);         // 5a
        rocket.addRelationship(AMPUTATION_OF_HAND, EXACT_LOCATION, HAND, 0);    // 5b
        rocket.addRelationship(AMPUTATION_OF_UPPER_LIMB, ISA, AMPUTATION, 0);   // 6a
        rocket.addRelationship(AMPUTATION_OF_UPPER_LIMB, EXACT_LOCATION, UPPER_LIMB, 0);    // 6b
        rocket.addRelationship(INJURY_TO_FINGER, ISA, INJURY, 0);               // 7a
        rocket.addRelationship(INJURY_TO_FINGER, HAS_LOCATION, FINGER, 0);      // 7b
        rocket.addRelationship(INJURY_TO_HAND, ISA, INJURY, 0);                 // 8a
        rocket.addRelationship(INJURY_TO_HAND, HAS_LOCATION, HAND, 0);          // 8b
        rocket.addRelationship(INJURY_TO_UPPER_LIMB, ISA, INJURY, 0);           // 9a
        rocket.addRelationship(INJURY_TO_UPPER_LIMB, HAS_LOCATION, UPPER_LIMB, 0);// 9b
        
        rocket.addRoleComposition(new String[] {SUB_PART, SUB_PART}, SUB_PART); // 10
        rocket.addRelationship(SUB_PART, ISA, PART_OF, 0);                      // 11
        
        rocket.addRoleComposition(new String[] {PART_OF, PART_OF}, PART_OF);    // 12
        rocket.addRoleComposition(EMPTY_ARRAY, PART_OF);                        // 13
        rocket.addRelationship(EXACT_LOCATION, ISA, HAS_LOCATION, 0);           // 14
        rocket.addRoleComposition(new String[] {HAS_LOCATION, SUB_PART}, HAS_LOCATION); // 15

        rocket.addRoleNeverGrouped(PART_OF);
        rocket.addRoleNeverGrouped(SUB_PART);
        rocket.addRoleNeverGrouped(HAS_LOCATION);
        rocket.addRoleNeverGrouped(EXACT_LOCATION);
        
        
        rocket.addConcept(HAND_S, true);
        rocket.addConcept(HAND_P, true);
        rocket.addRelationship(HAND_S, PART_OF, HAND, 0);
        rocket.addRelationship(HAND_P, SUB_PART, HAND, 0);
        
        rocket.classify();

        checkExpectedDistributionRelationshipCount(rocket, 37);

        expectRelationship(rocket, HAND, PART_OF, HAND, 0);
        expectRelationship(rocket, HAND_P, ISA, HAND_S, 0);
        expectRelationship(rocket, HAND, ISA, HAND_S, 0);
        expectRelationship(rocket, HAND_P, SUB_PART, HAND, 0);
        expectRelationship(rocket, HAND_P, PART_OF, HAND, 0);
        expectRelationship(rocket, HAND_P, ISA, HAND_S, 0);
        
        checkExpectedDistributionRelationshipCount(rocket, 37);
        
    }
    
}
