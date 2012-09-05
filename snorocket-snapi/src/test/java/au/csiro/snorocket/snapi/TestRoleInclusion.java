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

import org.junit.Before;
import org.junit.Test;

public class TestRoleInclusion extends AbstractTest {

    private static final String ISA = "ISA";
    private static final String BODY_STRUCTURE = "bodyStructure";
    private static final String FINDING_SITE = "findingSite";
    private static final String FINDING_SITE_DIRECT = "findingSiteDirect";
    private static final String FINDING_BY_SITE = "findingBySite";
    private static final String BROKEN_BONE = "brokenBone";
    private static final String FRACTURED_BONE = "fracturedBone";
    private static final String ASSOCIATED_MORPHOLOGY = "associatedMorphology";

    @Before
    public void setUp() throws Exception {
        au.csiro.snorocket.core.Snorocket.DEBUGGING = true;
    }

    /**
     * Example input -> output for distribution form
     * where Y [ Z and r2 [ r1
     * 
     * Input:  X [ r1.Y + r2.Y + r2.Z
     * Output: X [ r2.Y
     */
    @Test
    public void testSimpleRoleInclusionRedundancy() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);
        rocket.addConcept("X", false);
        rocket.addConcept("Y", false);
        rocket.addConcept("Z", false);
        rocket.addConcept("r1", false);
        rocket.addConcept("r2", false);
        
        rocket.addRelationship("Y", ISA, "Z", 0);
        rocket.addRelationship("r2", ISA, "r1", 0);

        rocket.addRelationship("X", "r1", "Y", 0);
        rocket.addRelationship("X", "r2", "Y", 0);
        rocket.addRelationship("X", "r2", "Z", 0);
        
        rocket.classify();
        
        checkExpectedDistributionRelationshipCount(rocket, 3);
        expectDistributionRelationship(rocket, "Y", ISA, "Z", 0);
        expectDistributionRelationship(rocket, "r2", ISA, "r1", 0);
        expectDistributionRelationship(rocket, "X", "r2", "Y", 0);
        checkExpectedDistributionRelationshipCount(rocket, 3);
    }
    
    /**
     * Example input -> output for distribution form
     * where Y [ Z and r2 [ r1
     * 
     * Input:  A [ rg.(r.B + r1.Z) + rg.(r.B + r2.Y)
     * Output: A [ rg.(r.B + r2.Y)
     */
    @Test
    public void testGroupedRoleInclusionRedundancy() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);
        rocket.addConcept("A", false);
        rocket.addConcept("B", false);
        rocket.addConcept("Y", false);
        rocket.addConcept("Z", false);
        rocket.addConcept("r", false);
        rocket.addConcept("r1", false);
        rocket.addConcept("r2", false);
        
        rocket.addRelationship("Y", ISA, "Z", 0);
        rocket.addRelationship("r2", ISA, "r1", 0);

        rocket.addRelationship("A", "r", "B", 1);
        rocket.addRelationship("A", "r1", "Z", 1);
        rocket.addRelationship("A", "r", "B", 2);
        rocket.addRelationship("A", "r2", "Y", 2);
        
        rocket.classify();
        
        expectDistributionRelationship(rocket, "Y", ISA, "Z", 0);
        expectDistributionRelationship(rocket, "r2", ISA, "r1", 0);
        expectDistributionRelationship(rocket, "A", "r", "B", 1);
        expectDistributionRelationship(rocket, "A", "r2", "Y", 1);
        checkExpectedDistributionRelationshipCount(rocket, 4);
    }
    
    /**
     * Example input -> output for distribution form
     * where Y [ Z and r2 [ r1
     * 
     * Input:  A [ rg.(r.B + r1.Y) + rg.(r.B + r2.Z)
     * Output: A [ rg.(r.B + r1.Y) + rg.(r.B + r2.Z)
     */
    @Test
    public void testGroupedRoleInclusionNoRedundancy() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);
        rocket.addConcept("A", false);
        rocket.addConcept("B", false);
        rocket.addConcept("Y", false);
        rocket.addConcept("Z", false);
        rocket.addConcept("r", false);
        rocket.addConcept("r1", false);
        rocket.addConcept("r2", false);
        
        rocket.addRelationship("Y", ISA, "Z", 0);
        rocket.addRelationship("r2", ISA, "r1", 0);

        rocket.addRelationship("A", "r", "B", 1);
        rocket.addRelationship("A", "r1", "Y", 1);
        rocket.addRelationship("A", "r", "B", 2);
        rocket.addRelationship("A", "r2", "Z", 2);
        
        rocket.classify();
        
        expectDistributionRelationship(rocket, "Y", ISA, "Z", 0);
        expectDistributionRelationship(rocket, "r2", ISA, "r1", 0);
        expectDistributionRelationship(rocket, "A", "r", "B", 1);
        expectDistributionRelationship(rocket, "A", "r2", "Z", 1);
        expectDistributionRelationship(rocket, "A", "r", "B", 2);
        expectDistributionRelationship(rocket, "A", "r1", "Y", 2);
        checkExpectedDistributionRelationshipCount(rocket, 6);
    }
    
    @Test
    public void testOneGroup() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);
        
        rocket.addConcept(FINDING_BY_SITE, true);
        rocket.addConcept(FINDING_SITE, false);
        rocket.addConcept(FINDING_SITE_DIRECT, false);
        rocket.addConcept(BROKEN_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addConcept(BODY_STRUCTURE, false);
        rocket.addConcept(FRACTURED_BONE, false);

        rocket.addRelationship(FINDING_SITE_DIRECT, ISA, FINDING_SITE, 0);

        rocket.addRelationship(FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        rocket.addRelationship(BROKEN_BONE, FINDING_SITE_DIRECT, BODY_STRUCTURE, 1);
        rocket.addRelationship(BROKEN_BONE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.classify();

        checkExpectedRelationshipCount(rocket, 6);
        checkExpectedDistributionRelationshipCount(rocket, 5);
        expectRelationship(rocket, BROKEN_BONE, ISA, FINDING_BY_SITE, 0);
        expectRelationship(rocket, BROKEN_BONE, FINDING_SITE, BODY_STRUCTURE, 1);
//        expectRelationship(rocket, BROKEN_BONE, ISA, FINDING_BY_SITE, 0);
    }
 
//    @Test
//    public void testRoleGroupMerging() {
//        I_Snorocket rocket = new Snorocket();
//        
//        rocket.setIsa(ISA);
//
//        final String D = "disease (finding)";
//        final String TB_F = "thermal burn (finding)";
//        final String TB_M = "thermal burn (morphology)";
//        final String BOS_F = "burn of skin (finding)";
//        final String BI_M = "burn injury (morphology)";
//        final String AB_M = "acid burn (morphology)";
//        final String AS = "anatomical structure (body structure)";
//        final String SS = "skin structure (body structure)";
//
//        final String M = "morphology";
//        final String FS = "site";
//        
//        rocket.addConcept(D, false);
//        rocket.addConcept(TB_F, false);
//        rocket.addConcept(TB_M, false);
//        rocket.addConcept(BOS_F, false);
//        rocket.addConcept(BI_M, false);
//        rocket.addConcept(SS, false);
//        
//        rocket.addConcept(M, false);
//        rocket.addConcept(FS, false);
//        
//        // TB_F [ D + rg.(M.TB_M)
//        rocket.addRelationship(TB_F, ISA, D, 0);
//        rocket.addRelationship(TB_F, M, TB_M, 1);
////        rocket.addRelationship(TB_F, FS, AS, 1);
//        // BOS_F [ D + rg.(M.BI_M + FS.SS)
//        rocket.addRelationship(BOS_F, ISA, D, 0);
//        rocket.addRelationship(BOS_F, M, BI_M, 1);
//        rocket.addRelationship(BOS_F, FS, SS, 1);
//
//        // PROXY1 == M.BI_M + FS.SS
//        final String PROXY1 = "(Burn of Skin)";
//        rocket.addConcept(PROXY1, true);
//        rocket.addRelationship(PROXY1, M, BI_M, 1);
//        rocket.addRelationship(PROXY1, FS, SS, 1);
//        // PROXY2 == M.BI_M + FS.SS
//        final String PROXY2 = "(Acid Burn of Skin)";
//        rocket.addConcept(PROXY2, true);
//        rocket.addRelationship(PROXY2, M, AB_M, 1);
//        rocket.addRelationship(PROXY2, FS, SS, 1);
//        // PROXY2 == M.BI_M + FS.SS
//        final String PROXY3 = "(Thermal Burn of Skin)";
//        rocket.addConcept(PROXY3, true);
//        rocket.addRelationship(PROXY3, M, TB_M, 1);
//        rocket.addRelationship(PROXY3, FS, SS, 1);
//        
//        
//        // SS [ AS
//        rocket.addRelationship(SS, ISA, AS, 0);
//        
//        // TB_M [ BI_M
//        rocket.addRelationship(TB_M, ISA, BI_M, 0);
//        // AB_M [ BI_M
//        rocket.addRelationship(AB_M, ISA, BI_M, 0);
//        
//        final String FBOS = "Food burn of skin (finding)";
//
//        // FBOS [ TB_F + BOS_F
//        rocket.addConcept(FBOS, false);
//        rocket.addRelationship(FBOS, ISA, TB_F, 0);
//        rocket.addRelationship(FBOS, ISA, BOS_F, 0);
//        
//        final String CBOS = "Chemical burn of skin (finding)";
//
//        // CBOS [ BOS_F + rg.(M.AB_M + FS.SS)   -- TODO find the bug
//        rocket.addConcept(CBOS, false);
//        rocket.addRelationship(CBOS, ISA, BOS_F, 0);
//        rocket.addRelationship(CBOS, M, AB_M, 2);
//        rocket.addRelationship(CBOS, FS, SS, 2);
//        
//        rocket.classify();
//        
//        rocket.getRelationships(new I_Snorocket.I_Callback() {
//            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
//                System.err.println(conceptId1 + " " + roleId + " " + conceptId2 + " " + group);
//            }
//        });
//    }
}
