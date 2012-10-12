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
import org.junit.Ignore;

public class TestRoleGrouping extends AbstractTest {

    private static final String ISA = "ISA";
    private static final String BODY_STRUCTURE = "bodyStructure";
    private static final String FINDING_SITE = "findingSite";
    private static final String FINDING_BY_SITE = "findingBySite";
    private static final String BROKEN_BONE = "brokenBone";
    private static final String FRACTURED_BONE = "fracturedBone";
    private static final String ASSOCIATED_MORPHOLOGY = "associatedMorphology";

    @Before
    public void setUp() throws Exception {
        au.csiro.snorocket.core.Snorocket.DEBUGGING = true;
    }
	
	@Ignore
    @Test
    public void testOneGroup() {
        I_Snorocket rocket = new Snorocket();

        rocket.setIsa(ISA);
        
        rocket.addConcept(FINDING_BY_SITE, true);
        rocket.addConcept(FINDING_SITE, false);
        rocket.addConcept(BROKEN_BONE, false);
        rocket.addConcept(ASSOCIATED_MORPHOLOGY, false);
        rocket.addConcept(BODY_STRUCTURE, false);
        rocket.addConcept(FRACTURED_BONE, false);

        rocket.addRelationship(FINDING_BY_SITE, FINDING_SITE, BODY_STRUCTURE, 0);
        rocket.addRelationship(BROKEN_BONE, FINDING_SITE, BODY_STRUCTURE, 1);
        rocket.addRelationship(BROKEN_BONE, ASSOCIATED_MORPHOLOGY, FRACTURED_BONE, 1);

        rocket.classify();
        
        expectRelationship(rocket, BROKEN_BONE, ISA, FINDING_BY_SITE, 0);
    }
	
	@Ignore
    @Test
    public void testRoleGroupMerging() {
        I_Snorocket rocket = new Snorocket();
        
        rocket.setIsa(ISA);

        final String D = "disease (finding)";
        final String TB_F = "thermal burn (finding)";
        final String TB_M = "thermal burn (morphology)";
        final String BOS_F = "burn of skin (finding)";
        final String BI_M = "burn injury (morphology)";
        final String AB_M = "acid burn (morphology)";
        final String AS = "anatomical structure (body structure)";
        final String SS = "skin structure (body structure)";

        final String M = "morphology";
        final String FS = "site";
        
        rocket.addConcept(D, false);
        rocket.addConcept(TB_F, false);
        rocket.addConcept(TB_M, false);
        rocket.addConcept(BOS_F, false);
        rocket.addConcept(BI_M, false);
        rocket.addConcept(SS, false);
        
        rocket.addConcept(M, false);
        rocket.addConcept(FS, false);
        
        // TB_F [ D + rg.(M.TB_M)
        rocket.addRelationship(TB_F, ISA, D, 0);
        rocket.addRelationship(TB_F, M, TB_M, 1);
//        rocket.addRelationship(TB_F, FS, AS, 1);
        // BOS_F [ D + rg.(M.BI_M + FS.SS)
        rocket.addRelationship(BOS_F, ISA, D, 0);
        rocket.addRelationship(BOS_F, M, BI_M, 1);
        rocket.addRelationship(BOS_F, FS, SS, 1);

        // PROXY1 == M.BI_M + FS.SS
        final String PROXY1 = "(Burn of Skin)";
        rocket.addConcept(PROXY1, true);
        rocket.addRelationship(PROXY1, M, BI_M, 1);
        rocket.addRelationship(PROXY1, FS, SS, 1);
        // PROXY2 == M.BI_M + FS.SS
        final String PROXY2 = "(Acid Burn of Skin)";
        rocket.addConcept(PROXY2, true);
        rocket.addRelationship(PROXY2, M, AB_M, 1);
        rocket.addRelationship(PROXY2, FS, SS, 1);
        // PROXY2 == M.BI_M + FS.SS
        final String PROXY3 = "(Thermal Burn of Skin)";
        rocket.addConcept(PROXY3, true);
        rocket.addRelationship(PROXY3, M, TB_M, 1);
        rocket.addRelationship(PROXY3, FS, SS, 1);
        
        
        // SS [ AS
        rocket.addRelationship(SS, ISA, AS, 0);
        
        // TB_M [ BI_M
        rocket.addRelationship(TB_M, ISA, BI_M, 0);
        // AB_M [ BI_M
        rocket.addRelationship(AB_M, ISA, BI_M, 0);
        
        final String FBOS = "Food burn of skin (finding)";

        // FBOS [ TB_F + BOS_F
        rocket.addConcept(FBOS, false);
        rocket.addRelationship(FBOS, ISA, TB_F, 0);
        rocket.addRelationship(FBOS, ISA, BOS_F, 0);
        
        final String CBOS = "Chemical burn of skin (finding)";

        // CBOS [ BOS_F + rg.(M.AB_M + FS.SS)   -- TODO find the bug
        rocket.addConcept(CBOS, false);
        rocket.addRelationship(CBOS, ISA, BOS_F, 0);
        rocket.addRelationship(CBOS, M, AB_M, 2);
        rocket.addRelationship(CBOS, FS, SS, 2);
        
        rocket.classify();
        
        rocket.getRelationships(new I_Snorocket.I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                System.err.println(conceptId1 + " " + roleId + " " + conceptId2 + " " + group);
            }
        });
    }
}
