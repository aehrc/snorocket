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

package au.csiro.snorocket.ace;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import au.csiro.snorocket.core.Snorocket;

public class TestIncrementalACE {

    private static final String ISA = "isa";
    private static final String DISEASE = "disease";
    private static final String ANIMAL = "animal";
    private static final String HUMAN = "human";
    private static final String COMMON_COLD = "common cold";
    private static final String HAS_PROPERTY = "hasProperty";

    private static final int ISA_ID = ISA.hashCode();
    private static final int DISEASE_ID = DISEASE.hashCode();
    private static final int ANIMAL_ID = ANIMAL.hashCode();
    private static final int HUMAN_ID = HUMAN.hashCode();
    private static final int COMMON_COLD_ID = COMMON_COLD.hashCode();
    private static final int HAS_PROPERTY_ID = HAS_PROPERTY.hashCode();

    private static Map<Integer, String> map = new HashMap<Integer, String>() {
//        @Override
//        public String put(Integer key, String val) {
//            return super.put(key, val + "(" + key + ")");
//        }
    };
    static {
        map.put(ISA_ID, ISA);
        map.put(DISEASE_ID, DISEASE);
        map.put(ANIMAL_ID, ANIMAL);
        map.put(HUMAN_ID, HUMAN);
        map.put(COMMON_COLD_ID, COMMON_COLD);
        map.put(HAS_PROPERTY_ID, HAS_PROPERTY);
    }

    @Before
    public void setUp() throws Exception {
        Snorocket.DEBUGGING = true;
    }
	
	@Ignore
    @Test
    public void createEmptyInitialState() throws IOException {
        I_SnorocketFactory rocket = new SnorocketFactory();
        rocket.setIsa(ISA_ID);
        rocket.classify();
        InputStream out = rocket.getStream();
        assertNotNull(out);
    }
	
	@Ignore
    @Test
    public void createAndRestoreEmptyInitialState() throws IOException {
        I_SnorocketFactory rocket1 = new SnorocketFactory();
        rocket1.setIsa(ISA_ID);
        rocket1.classify();
        InputStream state = rocket1.getStream();
        assertNotNull(state);

        I_SnorocketFactory rocket2 = new SnorocketFactory(state);
        rocket2.classify();
        rocket2.getResults(new I_SnorocketFactory.I_Callback() {
            public void addRelationship(int arg0, int arg1, int arg2, int arg3) {
                fail("No new relationships expected.");
            }
        });
    }
	
	@Ignore
    @Test
    public void createEmptyLocalExtension() {
        I_SnorocketFactory rocket1 = new SnorocketFactory();

        try {
            rocket1.createExtension();
            fail("createExtension before classify() should fail.");
        } catch (IllegalArgumentException e) {
            // ignore expected exception for null classification since we haven't classified yet
        }

        rocket1.setIsa(ISA_ID);
        rocket1.classify();
        I_SnorocketFactory rocket2 = rocket1.createExtension();
        assertNotNull(rocket2);
    }
	
	@Ignore
    @Test
    public void createNonEmptyLocalExtension() {
        System.err.println("---------------------------------- createNonEmptyLocalExtension");
        I_SnorocketFactory rocket1 = getInitialClassifiedRocket();
        System.err.println("----------------------------------");

        final I_SnorocketFactory rocket2 = rocket1.createExtension();
        classifyExtraConcepts(rocket2);
    }
	
	@Ignore
    @Test
    public void createNonEmptySerialisedExtension() throws IOException {
        System.err.println("---------------------------------- createNonEmptySerialisedExtension");
        I_SnorocketFactory rocket1 = getInitialClassifiedRocket();
        System.err.println("----------------------------------");

        InputStream state = rocket1.getStream();
        assertNotNull(state);

        final I_SnorocketFactory rocket2 = new SnorocketFactory(state).createExtension();
        classifyExtraConcepts(rocket2);
    }

    private void initRocket(I_SnorocketFactory rocket) {
        rocket.addConcept(COMMON_COLD_ID, false); // A
        //        rocket.addConcept(HUMAN_ID, false);      // B
        rocket.addConcept(ANIMAL_ID, false); // C
        rocket.addConcept(HAS_PROPERTY_ID, false);
        rocket.addRelationship(ANIMAL_ID, HAS_PROPERTY_ID, COMMON_COLD_ID, 0); // C [ hasProperty.A

        if (false) {
            rocket.addConcept(DISEASE_ID, false); // X
            rocket.addRelationship(COMMON_COLD_ID, ISA_ID, DISEASE_ID, 0); // A [ X
        }
    }

    private I_SnorocketFactory getInitialClassifiedRocket() {
        final int[] counter = { 0 };

        I_SnorocketFactory rocket = new SnorocketFactory();
        rocket.setIsa(ISA_ID);
        initRocket(rocket);
        rocket.classify();
        rocket.getResults(new I_SnorocketFactory.I_Callback() {
            public void addRelationship(int conceptId1, int roleId, int conceptId2, int group) {
                System.err.println("======== " + map.get(conceptId1) + " " + map.get(roleId) + " "
                        + map.get(conceptId2));
                assertEquals(0, group);
                counter[0]++;
            }
        });
        assertEquals(1, counter[0]);
        return rocket;
    }

    private void classifyExtraConcepts(I_SnorocketFactory rocket2) {
        rocket2.addRelationship(HUMAN_ID, ISA_ID, ANIMAL_ID, 0); // B [ C
        rocket2.classify();
        assertNotNull(rocket2);
        final int[] counter2 = { 0 };
        counter2[0] = 0;
        rocket2.getResults(new I_SnorocketFactory.I_Callback() {
            public void addRelationship(int conceptId1, int roleId, int conceptId2, int group) {
                System.err.println(">>>>>>>>>>>>> " + map.get(conceptId1) + " " + map.get(roleId) + " "
                        + map.get(conceptId2));
                assertEquals(0, group);
                counter2[0]++;
            }
        });
        assertEquals(2, counter2[0]);
    }

    private static final String FINDING = "finding";
    private static final String FINDING_BY_SITE = "finding by site";
    private static final String BODY_STRUCTURE = "body structure";
    private static final String LEG_STRUCTURE = "leg structure";
    private static final String ARM_STRUCTURE = "arm structure";
    private static final String LIMB_STRUCTURE = "limb structure";
    private static final String FINDING_SITE = "finding site";
    private static final String FRACTURE = "fracture";
    private static final String BROKEN_LEG = "broken leg";
    private static final String BROKEN_ARM = "broken arm";

    private static final String ASSOCIATED_MORPHOLOGY = "associatedMorphology";
    private static final String FRACTURED_BONE = "fracturedBone";

    private static final int FINDING_ID = FINDING.hashCode();
    private static final int BODY_STRUCTURE_ID = BODY_STRUCTURE.hashCode();
    private static final int FINDING_BY_SITE_ID = FINDING_BY_SITE.hashCode();
    private static final int LEG_STRUCTURE_ID = LEG_STRUCTURE.hashCode();
    private static final int ARM_STRUCTURE_ID = ARM_STRUCTURE.hashCode();
    private static final int LIMB_STRUCTURE_ID = LIMB_STRUCTURE.hashCode();
    private static final int FINDING_SITE_ID = FINDING_SITE.hashCode();
    private static final int FRACTURE_ID = FRACTURE.hashCode();
    private static final int BROKEN_LEG_ID = BROKEN_LEG.hashCode();
    private static final int BROKEN_ARM_ID = BROKEN_ARM.hashCode();

    private static final int ASSOCIATED_MORPHOLOGY_ID = ASSOCIATED_MORPHOLOGY.hashCode();
    private static final int FRACTURED_BONE_ID = FRACTURED_BONE.hashCode();

    static {
        map.put(FINDING_ID, FINDING);
        map.put(BODY_STRUCTURE_ID, BODY_STRUCTURE);
        map.put(FINDING_BY_SITE_ID, FINDING_BY_SITE);
        map.put(LEG_STRUCTURE_ID, LEG_STRUCTURE);
        map.put(ARM_STRUCTURE_ID, ARM_STRUCTURE);
        map.put(LIMB_STRUCTURE_ID, LIMB_STRUCTURE);
        map.put(FINDING_SITE_ID, FINDING_SITE);
        map.put(FRACTURE_ID, FRACTURE);
        map.put(BROKEN_LEG_ID, BROKEN_LEG);
        map.put(BROKEN_ARM_ID, BROKEN_ARM);

        map.put(ASSOCIATED_MORPHOLOGY_ID, ASSOCIATED_MORPHOLOGY);
        map.put(FRACTURED_BONE_ID, FRACTURED_BONE);
    }
	
	@Ignore
    @Test
    public void testIncremental() {
        I_SnorocketFactory rocket = new SnorocketFactory();
        rocket.setIsa(ISA_ID);
        initRocketFindings(rocket);

        rocket.classify();

        expectDistributionRelationship(rocket, FRACTURE_ID, ISA_ID, FINDING_ID, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE_ID, ISA_ID, FINDING_ID, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE_ID, FINDING_SITE_ID, BODY_STRUCTURE_ID, 0);
        expectDistributionRelationship(rocket, LIMB_STRUCTURE_ID, ISA_ID, BODY_STRUCTURE_ID, 0);
        expectDistributionRelationship(rocket, LEG_STRUCTURE_ID, ISA_ID, LIMB_STRUCTURE_ID, 0);
        //        expectRelationship(rocket, LEG_STRUCTURE_ID, ISA_ID, BODY_STRUCTURE_ID, 0);

        checkExpectedDistributionRelationshipCount(rocket, 5);

        I_SnorocketFactory rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG_ID, true);
        rocket2.addRelationship(BROKEN_LEG_ID, ISA_ID, FRACTURE_ID, 0);
        rocket2.addRelationship(BROKEN_LEG_ID, FINDING_SITE_ID, LEG_STRUCTURE_ID, 0);

        rocket2.classify();

        expectDistributionRelationship(rocket2, BROKEN_LEG_ID, ISA_ID, FRACTURE_ID, 0);
        //        expectRelationship(rocket2, BROKEN_LEG_ID, ISA_ID, FINDING_ID, 0);
        expectDistributionRelationship(rocket2, BROKEN_LEG_ID, ISA_ID, FINDING_BY_SITE_ID, 0);
        expectDistributionRelationship(rocket2, BROKEN_LEG_ID, FINDING_SITE_ID, LEG_STRUCTURE_ID, 0);

        checkExpectedDistributionRelationshipCount(rocket2, 3);
    }
	
	@Ignore
    @Test
    public void testIncrementalPostGrouped() {
        I_SnorocketFactory rocket = new SnorocketFactory();
        rocket.setIsa(ISA_ID);
        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE_ID, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY_ID, false);
        rocket.addRelationship(FRACTURE_ID, ASSOCIATED_MORPHOLOGY_ID, FRACTURED_BONE_ID, 1);

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
        expectDistributionRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectDistributionRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectDistributionRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectDistributionRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);

        checkExpectedDistributionRelationshipCount(rocket, 6);

        I_SnorocketFactory rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG_ID, false);
        rocket2.addRelationship(BROKEN_LEG_ID, ISA_ID, FRACTURE_ID, 0);
        rocket2.addRelationship(BROKEN_LEG_ID, FINDING_SITE_ID, LEG_STRUCTURE_ID, 1);
        rocket2.addRelationship(BROKEN_LEG_ID, ASSOCIATED_MORPHOLOGY_ID, FRACTURED_BONE_ID, 1);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);
        //        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);
        expectDistributionRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        expectDistributionRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        checkExpectedDistributionRelationshipCount(rocket2, 4);
    }
	
	@Ignore
    @Test
    public void testIncrementalPreGrouped() {
        I_SnorocketFactory rocket = new SnorocketFactory();
        //        rocket.setIsa(ISA_ID);
        initRocketFindings(rocket);

        rocket.addConcept(FRACTURED_BONE_ID, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY_ID, false);
        rocket.addRelationship(FRACTURE_ID, ASSOCIATED_MORPHOLOGY_ID, FRACTURED_BONE_ID, 1);

        rocket.addConcept(BROKEN_ARM_ID, false);
        rocket.addConcept(ARM_STRUCTURE_ID, false);
        rocket.addRelationship(BROKEN_ARM_ID, ISA_ID, FRACTURE_ID, 0);
        rocket.addRelationship(BROKEN_ARM_ID, FINDING_SITE_ID, ARM_STRUCTURE_ID, 1);
        rocket.addRelationship(BROKEN_ARM_ID, ASSOCIATED_MORPHOLOGY_ID, FRACTURED_BONE_ID, 1);
        rocket.addRelationship(ARM_STRUCTURE_ID, ISA_ID, LIMB_STRUCTURE_ID, 0);

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
        expectDistributionRelationship(rocket, FRACTURE, ISA, FINDING, 0);
        expectDistributionRelationship(rocket, FRACTURE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE, ISA, FINDING, 0);
        expectDistributionRelationship(rocket, FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        expectDistributionRelationship(rocket, LIMB_STRUCTURE, ISA, BODY_STRUCTURE, 0);
        expectDistributionRelationship(rocket, LEG_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        expectDistributionRelationship(rocket, ARM_STRUCTURE, ISA, LIMB_STRUCTURE, 0);
        //        expectDistributionRelationship(rocket, LEG_STRUCTURE, ISA, BODY_STRUCTURE, 0);

        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FRACTURE, 0);
        //        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FINDING, 0);
        expectDistributionRelationship(rocket, BROKEN_ARM, ISA, FINDING_BY_SITE, 0);
        expectDistributionRelationship(rocket, BROKEN_ARM, FINDING_SITE, ARM_STRUCTURE, 1);
        expectDistributionRelationship(rocket, BROKEN_ARM, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        checkExpectedDistributionRelationshipCount(rocket, 11);

        I_SnorocketFactory rocket2 = rocket.createExtension();

        rocket2.addConcept(BROKEN_LEG_ID, false);
        rocket2.addRelationship(BROKEN_LEG_ID, ISA_ID, FRACTURE_ID, 0);
        rocket2.addRelationship(BROKEN_LEG_ID, FINDING_SITE_ID, LEG_STRUCTURE_ID, 1);
        rocket2.addRelationship(BROKEN_LEG_ID, ASSOCIATED_MORPHOLOGY_ID, FRACTURED_BONE_ID, 1);

        rocket2.classify();

        /*
         * 1. broken leg isa findingBySite
         * 2. broken leg isa fracture
         * 3. broken leg isa finding
         * 4. broken leg findingSite legStructure
         * 
         */
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FRACTURE, 0);
        //        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FINDING, 0);
        expectDistributionRelationship(rocket2, BROKEN_LEG, FINDING_SITE, LEG_STRUCTURE, 1);
        expectDistributionRelationship(rocket2, BROKEN_LEG, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);
        expectDistributionRelationship(rocket2, BROKEN_LEG, ISA, FINDING_BY_SITE, 0);

        checkExpectedDistributionRelationshipCount(rocket2, 4);
    }

    private void initRocketFindings(I_SnorocketFactory rocket) {
        rocket.addConcept(ISA_ID, false);
        rocket.addConcept(FINDING_ID, false);
        rocket.addConcept(FRACTURE_ID, false);
        rocket.addConcept(FINDING_BY_SITE_ID, true);
        rocket.addConcept(BODY_STRUCTURE_ID, false);
        rocket.addConcept(LIMB_STRUCTURE_ID, false);
        rocket.addConcept(LEG_STRUCTURE_ID, false);
        rocket.addConcept(FINDING_SITE_ID, false);

        rocket.setIsa(ISA_ID);

        rocket.addRelationship(FRACTURE_ID, ISA_ID, FINDING_ID, 0);
        rocket.addRelationship(FINDING_BY_SITE_ID, ISA_ID, FINDING_ID, 0);
        rocket.addRelationship(FINDING_BY_SITE_ID, FINDING_SITE_ID, BODY_STRUCTURE_ID, 0);
        rocket.addRelationship(LIMB_STRUCTURE_ID, ISA_ID, BODY_STRUCTURE_ID, 0);
        rocket.addRelationship(LEG_STRUCTURE_ID, ISA_ID, LIMB_STRUCTURE_ID, 0);
    }

    private void expectDistributionRelationship(I_SnorocketFactory rocket, final String concept1, final String rel,
            final String concept2, final int group) {
        expectDistributionRelationship(rocket, concept1.hashCode(), rel.hashCode(), concept2.hashCode(), group);
    }

    private void expectDistributionRelationship(I_SnorocketFactory rocket, final int concept1, final int rel,
            final int concept2, final int group) {
        final int[] counter = { 0 };
        final String[] groupMismatch = { "" };

        rocket.getResults(new I_SnorocketFactory.I_Callback() {
            public void addRelationship(int conceptId1, int roleId, int conceptId2, int grp) {
//                System.err.println("eDR.addRel: " + map.get(conceptId1) + " " + map.get(roleId) + " " + map.get(conceptId2) + " " + grp);
                if (concept1 == conceptId1 && rel == roleId && concept2 == conceptId2) {
                    if ((0 == group && 0 == grp) || (0 != group && 0 != grp)) {
                        counter[0]++;
                    } else {
                        groupMismatch[0] = " (group=" + grp + " cf. " + group + ")";
                    }
                }
            }
        });
        assertTrue("Expected relationship not found: " + map.get(concept1) + " " + map.get(rel) + " "
                + map.get(concept2) + " " + group + groupMismatch[0], 0 < counter[0]);
    }

    private void checkExpectedDistributionRelationshipCount(I_SnorocketFactory rocket, int expected) {
        final int[] counter = { 0 };
        counter[0] = 0;
        rocket.getResults(new I_SnorocketFactory.I_Callback() {
            public void addRelationship(int conceptId1, int roleId, int conceptId2, int group) {
                System.out.println(">>>>>>>>>>>>> " + map.get(conceptId1) + " " + map.get(roleId) + " "
                        + map.get(conceptId2) + " " + group);
                counter[0]++;
            }
        });
        assertEquals(expected, counter[0]);
    }

}
