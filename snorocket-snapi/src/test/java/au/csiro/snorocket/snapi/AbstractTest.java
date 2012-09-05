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
import static org.junit.Assert.assertTrue;

import java.util.Collection;

import au.csiro.snorocket.snapi.I_Snorocket.I_Callback;

public abstract class AbstractTest {

    protected void expectRelationship(I_Snorocket rocket, final String concept1, final String rel, final String concept2, final int group) {
        final int[] counter = {0};
        final String[] groupMismatch = {""};
        
        rocket.getRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int grp) {
//System.out.println(conceptId1 + " [ " + roleId + "." + conceptId2 + "\t" + grp);
                if (concept1.equals(conceptId1)
                        && rel.equals(roleId)
                        && concept2.equals(conceptId2)) {
                    if ((0 == group && 0 == grp) || (0 != group && 0 != grp)) {
                        counter[0]++;
                    } else {
                        groupMismatch[0] = " (group=" + grp + " cf. " + group + ")";
                    }
                }
            }
        });
        assertTrue("Expected relationship not found: " + concept1 + " " + rel + " " + concept2 + " " + group + groupMismatch[0],
                0 < counter[0]);
    }

    protected void expectDistributionRelationship(I_Snorocket rocket, final String concept1, final String rel, final String concept2, final int group) {
        final int[] counter = {0};
        final String[] groupMismatch = {""};
        
        rocket.getDistributionFormRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int grp) {
                if (concept1.equals(conceptId1)
                        && rel.equals(roleId)
                        && concept2.equals(conceptId2)) {
                    if ((0 == group && 0 == grp) || (0 != group && 0 != grp)) {
                        counter[0]++;
                    } else {
                        groupMismatch[0] = " (group=" + grp + " cf. " + group + ")";
                    }
                }
            }
        });
        assertTrue("Expected relationship not found: " + concept1 + " " + rel + " " + concept2 + " " + group + groupMismatch[0],
                0 < counter[0]);
    }

    protected void checkExpectedRelationshipCount(I_Snorocket rocket, int expected) {
        final int[] counter = {0};

        rocket.getRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                counter[0]++;
                System.out.println(counter[0] + ". " + conceptId1 + " " + roleId + " " + conceptId2 + " " + group);
            }
        });
        assertEquals("Incorrect number of relationships found", expected, counter[0]);
    }
    
    protected void checkExpectedDistributionRelationshipCount(I_Snorocket rocket, int expected) {
        final int[] counter = {0};

        rocket.getDistributionFormRelationships(new I_Callback() {
            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
                counter[0]++;
                System.out.println(counter[0] + ". " + conceptId1 + " " + roleId + " " + conceptId2 + " " + group);
            }
        });
        assertEquals("Incorrect number of relationships found", expected, counter[0]);
    }

    protected void checkExpectedEquivalenceCount(I_Snorocket rocket, int expected) {
        final int[] counter = {0};

        rocket.getEquivalents(new I_Snorocket.I_EquivalentCallback() {
            public void equivalent(Collection<String> equivalentConcepts) {
                counter[0]++;
                System.out.println(counter[0] + ". " + equivalentConcepts);
            }
        });
        assertEquals("Incorrect number of equivalencies found", expected, counter[0]);
    }
}
