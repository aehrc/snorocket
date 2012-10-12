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

import java.util.Arrays;

import org.junit.Test;
import org.junit.Ignore;

public class TestExpressionParser {
	
	@Ignore
    @Test
    public void testSingleConcept() {
        final String concept = "10006000";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept);
        parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
            public void matchConcept(String conceptId, String term) {
                assertEquals(concept, conceptId);
                assertNull(term);
            }

            public void matchAttribute(String attributeId, String term) {
                fail("No Attributes supplied");
            }

            public void matchExpression() {
            }
        });
    }
	
	@Ignore
    @Test
    public void testShortConcept() {
        final String concept = "6000";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept);
        try {
            parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
                public void matchConcept(String conceptId, String term) {
                    assertEquals(concept, conceptId);
                    assertNull(term);
                }

                public void matchAttribute(String attributeId, String term) {
                    fail("No Attributes supplied");
                }

                public void matchExpression() {
                }
            });
            
            fail("Should have had parse error");
        } catch (SnomedExpressionParser.ScannerException e) {
        }
    }
	
	@Ignore
    @Test
    public void testSingleConceptWithTerm() {
        final String conceptStr = "10006000";
        final String termStr = "some value";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(conceptStr + "|" + termStr + "|");
        parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
            public void matchConcept(String conceptId, String term) {
                assertEquals(conceptStr, conceptId);
                assertEquals(termStr, term);
            }

            public void matchAttribute(String attributeId, String term) {
                fail("No Attributes supplied");
            }

            public void matchExpression() {
            }
        });
    }
	
	@Ignore
    @Test
    public void testShortConceptAndTerm() {
        final String concept = "10006000 |fjdkfj";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept);
        try {
            parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
                public void matchConcept(String conceptId, String term) {
                    assertEquals(concept, conceptId);
                    assertNull(term);
                }

                public void matchAttribute(String attributeId, String term) {
                    fail("No Attributes supplied");
                }

                public void matchExpression() {
                }
            });
            
            fail("Should have had parse error");
        } catch (SnomedExpressionParser.ScannerException e) {
            assertTrue(e.toString().contains("found [<end of input>]"));
        }
    }
	
	@Ignore
    @Test
    public void testMultipleConcepta() {
        String base = "10006000";
        
        for (int i = 1; i < 4; i++) {
            base = base + " + " + base + i;
            final String concept = base;
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept);
        parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
            public void matchConcept(String conceptId, String term) {
//                assertEquals(concept, conceptId);
                System.out.println(conceptId);
                assertNull(term);
            }

            public void matchAttribute(String attributeId, String term) {
                fail("No Attributes supplied");
            }

            public void matchExpression() {
            }
        });
        
        }
    }
	
	@Ignore
    @Test
    public void testConceptWithAttributeSet() {
        final String[] concept = {"10006000", "83983298"};
        final String attr = "5794857894";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept[0] + ":" + attr + "=" + concept[1]);
        parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
            public void matchConcept(String conceptId, String term) {
                assertTrue(Arrays.asList(concept).contains(conceptId));
                assertNull(term);
            }

            public void matchAttribute(String attributeId, String term) {
                assertEquals(attr, attributeId);
                assertNull(term);
            }

            public void matchExpression() {
            }
        });
    }
	
	@Ignore
    @Test
    public void testConceptWithAttributeGroup() {
        final String[] concept = {"10006000", "83983298"};
        final String attr = "5794857894";
        
        SnomedExpressionParser parser = new SnomedExpressionParser(concept[0] + ": {" + attr + "=" + concept[1] + "}");
        parser.parseExpression(new SnomedExpressionParser.MatcherAdapter() {
            public void matchConcept(String conceptId, String term) {
                assertTrue(Arrays.asList(concept).contains(conceptId));
                assertNull(term);
            }

            public void matchAttribute(String attributeId, String term) {
                assertEquals(attr, attributeId);
                assertNull(term);
            }

            public void matchExpression() {
            }
        });
    }

}
