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

import java.lang.reflect.Field;

import org.junit.*;

import au.csiro.snorocket.core.Factory;

public class TestNestedRoleGrouping {

    private I_Snorocket rocket;
    
    @Before
    public void setUp() throws Exception {
        rocket = new Snorocket(); 
    }
    
    private Factory getFactory() {
        Factory factory = null;
        try {
            Field f = rocket.getClass().getDeclaredField("factory");
            f.setAccessible(true);
            factory = (Factory) f.get(rocket);
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
        return factory;
    }
	
	@Ignore
    @Test
    public void testSimpleNesting() {
        rocket.setIsa("isa");
        
        final String[] concepts = {"a", "b", "c", "d", "e", "base", "has", "likes"};
        for (String c: concepts) {
            rocket.addConcept(c, false);
        }
        
        rocket.addRelationship("a", "has", "b", 1);
        rocket.addRelationship("a", "likes", "c", 1);
        rocket.addRelationship("c", "has", "d", 1);
        rocket.addRelationship("c", "likes", "e", 1);

        if (false) {
            printRoles();
        }
        
        rocket.classify();
        
        final int[] counter = {0};
        rocket.getDistributionFormRelationships(new I_Snorocket.I_Callback() {

            public void addRelationship(String conceptId1, String roleId, String conceptId2, int group) {
//                Assert.fail("No new relationships should be derived.");
                counter[0]++;
            }
            
        });
        
        assertEquals("No new relationships should be derived.", 4, counter[0]);
    }

    private void printRoles() {
        final Factory factory = getFactory();
        final int max = factory.getTotalRoles();
        for (int r = 0; r < max; r++) {
            System.err.println(r+"\t"+factory.lookupRoleId(r));
        }
    }
}
