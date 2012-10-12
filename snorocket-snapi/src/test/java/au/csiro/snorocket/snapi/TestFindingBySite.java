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

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.snapi.I_Snorocket.I_Callback;

public class TestFindingBySite extends AbstractTest {

    private static final String ISA = "isa";
    private static final String FINDING = "finding";
    private static final String FINDING_BY_SITE = "findingBySite";
    private static final String BODY_STRUCTURE = "bodyStructure";
    private static final String LIMB_STRUCTURE = "limbStructure";
    private static final String LEG_STRUCTURE = "legStructure";
    private static final String ARM_STRUCTURE = "armStructure";
    private static final String FINDING_SITE = "findingSite";
    private static final String FRACTURE = "fracture";
    private static final String BROKEN_LEG = "broken leg";
    private static final String BROKEN_ARM = "broken arm";

    private static final String ASSOCIATED_MORPHOLOGY = "associatedMorphology";
    private static final String FRACTURED_BONE = "fracturedBone";

    @Before
    public void setUp() throws Exception {
        au.csiro.snorocket.core.Snorocket.DEBUGGING = true;
    }
	
	@Ignore
    @Test
    public void testSimple() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        rocket.addConcept(BROKEN_LEG, true);
        rocket.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 0);
        rocket.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        rocket.classify();

        /*
         *  1. fracture      isa finding
         *  2. findingBySite isa finding
         *  3. findingBySite findingSite bodyStructure
         *  4. limbStructure isa bodyStructure
         *  5. legStructure  isa limbStructure
         *  6. legStructure  isa bodyStructure
         *  7. broken leg    isa findingBySite
         *  8. broken leg    isa fracture
         *  9. broken leg    isa finding
         * 10. broken leg    findingSite bodyStructure
         * 
         */
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        expectRelationship(rocket, BROKEN_LEG, ISA, FRACTURE, 0);
        expectRelationship(rocket, BROKEN_LEG, ISA, FINDING, 0);
        expectRelationship(rocket, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);
        expectRelationship(rocket, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 0);
        expectRelationship(rocket, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        checkExpectedRelationshipCount(rocket, 12);
    }

    /**
     * NCCH are seeing strange behaviour where flipping from primitive to fullyDefined after an incremental
     * classification gives different results from just doing incremental on fullyDefined.
     * 
     * This was a manifestation of a *documented* (!) limitation of ExtensionOntology (documented in a FIXME in the source code)
     * that resulted in a corruption of the base state's *NF structures.
     */
	@Ignore
    @Test
    public void testIncrementalNCCHBug2() {
        au.csiro.snorocket.core.Snorocket.DEBUGGING = false;
        I_Snorocket rocket = new Snorocket();

        rocket.addConcept(ISA, false);
        rocket.addConcept(FINDING, false);
        rocket.addConcept(FRACTURE, false);
        rocket.addConcept(FINDING_BY_SITE, true);
        rocket.addConcept(BODY_STRUCTURE, false);
        rocket.addConcept(LIMB_STRUCTURE, false);
        rocket.addConcept(LEG_STRUCTURE, false);
        rocket.addConcept(FINDING_SITE, false);
        
        rocket.setIsa(ISA);
        
        rocket.addRelationship(FRACTURE, ISA, FINDING, 0);
        rocket.addRelationship(FINDING_BY_SITE, ISA, FINDING, 0);
        rocket.addRelationship(LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        rocket.addRelationship(LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);

        rocket.addRelationship(FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 1);
        rocket.addRelationship(FINDING_BY_SITE, ASSOCIATED_MORPHOLOGY, FRACTURE, 1);

        rocket.classify();

        step2(rocket, "heel fracture", false);
        step2(rocket, "heel fracture 2", true);
    }

    private void step2(I_Snorocket rocket, String newConcept, boolean fullyDefined) {
        final I_Snorocket ext = rocket.createExtension();
        
        ext.addConcept(newConcept, fullyDefined);
        ext.addRelationship(newConcept, ISA, FINDING, 0);
        ext.addRelationship(newConcept, FINDING_SITE, LEG_STRUCTURE, 1);
        ext.addRelationship(newConcept, ASSOCIATED_MORPHOLOGY, FRACTURE, 1);
        
        ext.classify();
        checkExpectedRelationshipCount(ext, 4);
        expectRelationship(ext, newConcept, ISA, FINDING, 0);
        expectRelationship(ext, newConcept, ISA, FINDING_BY_SITE, 0);
        expectRelationship(ext, newConcept, FINDING_SITE, LEG_STRUCTURE, 1);
        expectRelationship(ext, newConcept, ASSOCIATED_MORPHOLOGY, FRACTURE, 1);
    }
	
	@Ignore
    @Test
    public void testIncrementalNCCHBug() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.classify();

        /*
         * 1. fracture      isa finding
         * 2. findingBySite isa finding
         * 3. findingBySite findingSite bodyStructure
         * 4. limbStructure isa bodyStructure
         * 5. legStructure  isa limbStructure
         * 6. legStructure  isa bodyStructure
         * 
         */
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        checkExpectedRelationshipCount(rocket, 6);
        
        doNCCHBugStep2(rocket, "MyNewFindingBySite", true);
        doNCCHBugStep2(rocket, "MyOTHERNewFindingBySite", true);
    }

    private void doNCCHBugStep2(final I_Snorocket rocket, final String newConcept, final boolean fullyDefined) {
        final I_Snorocket ext = rocket.createExtension();
        
        ext.addConcept(newConcept, fullyDefined);
        ext.addRelationship(newConcept, ISA, FINDING, 0);
        ext.addRelationship(newConcept, FINDING_SITE, BODY_STRUCTURE, 0);
        
        ext.classify();
        checkExpectedRelationshipCount(ext, 4);
        expectRelationship(ext, newConcept, ISA, FINDING, 0);
        expectRelationship(ext, newConcept, ISA, FINDING_BY_SITE, 0);
        expectRelationship(ext, newConcept, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(ext, FINDING_BY_SITE, ISA, newConcept, 0);
    }
	
	@Ignore
    @Test
    public void testIncremental() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        rocket.classify();

        /*
         * 1. fracture      isa finding
         * 2. findingBySite isa finding
         * 3. findingBySite findingSite bodyStructure
         * 4. limbStructure isa bodyStructure
         * 5. legStructure  isa limbStructure
         * 6. legStructure  isa bodyStructure
         * 
         */
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        checkExpectedRelationshipCount(rocket, 7);

        incrementalTestStage2(rocket);
        incrementalTestStage2(rocket);
    }

    private void incrementalTestStage2(I_Snorocket rocket) {
        I_Snorocket rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG, true);
        rocket2.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket2.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 0);
        rocket2.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        expectRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);
        expectRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 0);
        expectRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);

        checkExpectedRelationshipCount(rocket2, 5);
    }
	
	@Ignore
    @Test
    public void testSimpleGrouped() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.addConcept(BROKEN_LEG, true);
        rocket.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        rocket.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.classify();

        /*
         *  1. fracture      isa finding
         *  2. findingBySite isa finding
         *  3. findingBySite findingSite bodyStructure
         *  4. limbStructure isa bodyStructure
         *  5. legStructure  isa limbStructure
         *  6. legStructure  isa bodyStructure
         *  7. broken leg    isa findingBySite
         *  8. broken leg    isa fracture
         *  9. broken leg    isa finding
         * 10. broken leg    findingSite bodyStructure
         * 11. fracture      associatedMorphology fracturedBine
         * 12. broken leg    associatedMorphology fracturedBine
         * 
         */
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        expectRelationship(rocket, BROKEN_LEG, ISA, FRACTURE, 0);
        expectRelationship(rocket, BROKEN_LEG, ISA, FINDING, 0);
        expectRelationship(rocket, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);
        expectRelationship(rocket, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        expectRelationship(rocket, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        checkExpectedRelationshipCount(rocket, 12);
    }
	
	@Ignore
    @Test
    public void testIncrementalPostGrouped() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.classify();

        /*
         * 1. fracture      isa finding
         * 2. findingBySite isa finding
         * 3. findingBySite findingSite bodyStructure
         * 4. limbStructure isa bodyStructure
         * 5. legStructure  isa limbStructure
         * 6. legStructure  isa bodyStructure
         * 
         */
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        checkExpectedRelationshipCount(rocket, 7);

        I_Snorocket rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG, false);
        rocket2.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket2.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        rocket2.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        expectRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);
        expectRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        expectRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);

        checkExpectedRelationshipCount(rocket2, 5);
    }
	
	@Ignore
    @Test
    public void testIncrementalPreGrouped() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.addConcept(BROKEN_ARM, false);
        rocket.addRelationship(BROKEN_ARM, ISA, FRACTURE, 0);
        rocket.addRelationship(BROKEN_ARM, FINDING_SITE, ARM_STRUCTURE, 1);
        rocket.addRelationship(BROKEN_ARM, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);
        rocket.addRelationship(ARM_STRUCTURE, ISA, LIMB_STRUCTURE, 0);

        rocket.classify();

        /*
         * 1. fracture      isa finding
         * 2. findingBySite isa finding
         * 3. findingBySite findingSite bodyStructure
         * 4. limbStructure isa bodyStructure
         * 5. legStructure  isa limbStructure
         * 6. legStructure  isa bodyStructure
         * 
         */
        int relCount = 0;
        expectRelationship(rocket, FRACTURE, ISA, FINDING, 0);  relCount++;
        expectRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);  relCount++;
        expectRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);  relCount++;
        expectRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);  relCount++;
        expectRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);  relCount++;
        expectRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);  relCount++;
        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);  relCount++;
        expectRelationship(rocket, ARM_STRUCTURE, ISA, LIMB_STRUCTURE, 0);  relCount++;
        expectRelationship(rocket, ARM_STRUCTURE, ISA, BODY_STRUCTURE, 0);  relCount++;

        expectRelationship(rocket, BROKEN_ARM, ISA, FRACTURE, 0);  relCount++;
        expectRelationship(rocket, BROKEN_ARM, ISA, FINDING, 0);  relCount++;
        expectRelationship(rocket, BROKEN_ARM, FINDING_SITE, ARM_STRUCTURE, 1);  relCount++;
        expectRelationship(rocket, BROKEN_ARM, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);  relCount++;
        expectRelationship(rocket, BROKEN_ARM, ISA, FINDING_BY_SITE, 0);  relCount++;

        checkExpectedRelationshipCount(rocket, relCount);

        I_Snorocket rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG, false);
        rocket2.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket2.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        rocket2.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        relCount = 0;
        expectRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);  relCount++;
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);  relCount++;
        expectRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);  relCount++;
        expectRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);  relCount++;
        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);  relCount++;

        checkExpectedRelationshipCount(rocket2, relCount);
    }
	
	@Ignore
    @Test
    public void testIncrementalPreGroupedDistribution() {
        I_Snorocket rocket = new Snorocket();

        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.addConcept(BROKEN_ARM, false);
        rocket.addConcept(ARM_STRUCTURE, false);
        rocket.addRelationship(BROKEN_ARM, ISA, FRACTURE, 0);
        rocket.addRelationship(BROKEN_ARM, FINDING_SITE, ARM_STRUCTURE, 1);
        rocket.addRelationship(BROKEN_ARM, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);
        rocket.addRelationship(ARM_STRUCTURE, ISA, LIMB_STRUCTURE, 0);

        rocket.classify();

        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FINDING_BY_SITE, 0);

        /*
         * 1. fracture      isa finding
         * 2. findingBySite isa finding
         * 3. findingBySite findingSite bodyStructure
         * 4. limbStructure isa bodyStructure
         * 5. legStructure  isa limbStructure
         * 6. legStructure  isa bodyStructure
         * 
         */
        int relCount = 0;
        expectDistributionRelationship(rocket, FRACTURE, ISA, FINDING, 0);  relCount++;
        expectDistributionRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);  relCount++;
        expectDistributionRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);  relCount++;
        expectDistributionRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);  relCount++;
        expectDistributionRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);  relCount++;
        expectDistributionRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);  relCount++;
        expectDistributionRelationship(rocket, ARM_STRUCTURE, ISA, LIMB_STRUCTURE, 0);  relCount++;
        //        expectRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);  relCount++;

        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FRACTURE, 0);  relCount++;
        //        expectRelationship(rocket, BROKEN_ARM, ISA, FINDING, 0);  relCount++;
        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FINDING_BY_SITE, 0);  relCount++;
        expectDistributionRelationship(rocket, BROKEN_ARM, FINDING_SITE, ARM_STRUCTURE, 1);  relCount++;
        expectDistributionRelationship(rocket, BROKEN_ARM, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);  relCount++;

        checkExpectedDistributionRelationshipCount(rocket, relCount);

        I_Snorocket rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG, false);
        rocket2.addRelationship(BROKEN_LEG, ISA, FRACTURE, 0);
        rocket2.addRelationship(BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        rocket2.addRelationship(BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        relCount = 0;
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);  relCount++;
        //        expectRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);  relCount++;
        expectDistributionRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);  relCount++;
        expectDistributionRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);  relCount++;
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);  relCount++;

        checkExpectedDistributionRelationshipCount(rocket2, relCount);
    }

    private void initRocketFindings(I_Snorocket rocket) {
        rocket.addConcept(ISA, false);
        rocket.addConcept(FINDING, false);
        rocket.addConcept(FRACTURE, false);
        rocket.addConcept(FINDING_BY_SITE, true);
        rocket.addConcept(BODY_STRUCTURE, false);
        rocket.addConcept(LIMB_STRUCTURE, false);
        rocket.addConcept(LEG_STRUCTURE, false);
        rocket.addConcept(FINDING_SITE, false);

        rocket.setIsa(ISA);

        rocket.addRelationship(FRACTURE, ISA, FINDING, 0);
        rocket.addRelationship(FINDING_BY_SITE, ISA, FINDING, 0);
        rocket.addRelationship(FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        rocket.addRelationship(LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        rocket.addRelationship(LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
    }
	
	@Ignore
    @Test
    public void testGrouping() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);

        rocket.addConcept(BODY_STRUCTURE, false);
        rocket.addConcept(FINDING, false);
        rocket.addConcept(FRACTURE, false);
        rocket.addConcept(FRACTURED_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addConcept(FINDING_SITE, false);

        rocket.addRelationship(FRACTURE, ISA, FINDING, 0);
        rocket.addRelationship(FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 3);
        rocket.addRelationship(FRACTURE, FINDING_SITE, BODY_STRUCTURE, 3);

        rocket.classify();

        // check that assocMorph and findingSite relationships have same (non-zero) group
        rocket.getRelationships(new I_Callback() {
            int counter = 0;
            int prevGroup = -1;

            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                counter++;
                System.out.println(counter + ". " + conceptId1 + " " + roleId + " " + conceptId2 + " " + group);

                if (prevGroup < 0) {
                    if (!roleId.equals(ISA)) {
                        prevGroup = group;
                    }
                } else {
                    assertEquals("Mismatched value for group", prevGroup, group);
                }
            }
        });

        // check that assocMorph and findingSite relationships have same (non-zero) group 
        rocket.getDistributionFormRelationships(new I_Callback() {
            int counter = 0;
            int prevGroup = -1;

            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                counter++;
                System.out.println(counter + ". " + conceptId1 + " " + roleId + " " + conceptId2 + " " + group);

                if (prevGroup < 0) {
                    if (!roleId.equals(ISA)) {
                        prevGroup = group;
                    }
                } else {
                    assertEquals("Mismatched value for group", prevGroup, group);
                }
            }
        });
    }
}
