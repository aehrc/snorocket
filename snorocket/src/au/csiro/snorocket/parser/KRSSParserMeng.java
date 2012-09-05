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

package au.csiro.snorocket.parser;

import java.util.Set;

import au.csiro.snorocket.core.AbstractConcept;
import au.csiro.snorocket.core.GCI;
import au.csiro.snorocket.core.Inclusion;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.RI;

/**
 * Handle Meng's KRSS Syntax 
 * 
 * @author law223
 *
 */
public class KRSSParserMeng extends AbstractKRSSParser {

    private static final String PARENT = ":parent";
    private static final String TRANSITIVE = ":transitive";
    private static final String REFLEXIVE = ":reflexive";

    /**
     * <pre>
     * stmt ::= '(' ( PRIMITIVE_ROLE role (REFLEXIVE bool)? (TRANSITIVE bool)? (PARENT role)? (RIGHT_IDENTITY role)?
     *              | (PRIMITIVE_CONCEPT | FULLY_DEFINED_CONCEPT)
     *                concept expression
     *              )
     *          ')';
     * expressions ::= ( concept
     *                 | '(' ( AND (expression)* | SOME role expression ) ')'
     *                 );
     * role ::= ID;
     * concept ::= ID;
     * </pre>
     * @param ontology
     * @throws ParseException 
     */
    void parseStatement(Set<Inclusion> ontology) throws ParseException {
        expect("(");
        consume();
        
        final String declaration = token();

        if (PRIMITIVE_ROLE.equals(declaration) /* || "role-inclusion".equals(declaration) */) {
            parseRoleDefinition(ontology);
//        } else if ("transitive".equals(declaration)) {
//            consume();
//
//            final Role role = parseRole();
//
//            ont.addRI(new RI(new Role[] {role, role}, role));
        } else {
            final boolean isFullyDefinedConcept = FULLY_DEFINED_CONCEPT.equals(declaration) || EQUIVALENT.equals(declaration);
            if (isFullyDefinedConcept || PRIMITIVE_CONCEPT.equals(declaration) || IMPLIES.equals(declaration)) {
                consume();
                final AbstractConcept lhs;
                if (IMPLIES.equals(declaration) || EQUIVALENT.equals(declaration)) {
                    lhs = parseExpression();
                } else {
                    lhs = parseConcept(token());
                }

                AbstractConcept rhs = parseExpression();

                ontology.add(new GCI(lhs, rhs));
                if (isFullyDefinedConcept) {
                    ontology.add(new GCI(rhs, lhs));
                }
            } else {
                throw new ParseException("Unexpected token: '" + declaration + "'", lineReader);
            }
        }

        expect(")");
        consume();
    }

    /**
     * <pre>
     * roleDef ::= PRIMITIVE_ROLE role
     *                 (REFLEXIVE bool)?
     *                 (TRANSITIVE bool)?
     *                 (PARENT role)?
     *                 (RIGHT_IDENTITY role)?
     * </pre>
     * @param ontology
     * @throws ParseException
     */
    private void parseRoleDefinition(Set<Inclusion> ontology) throws ParseException {
        consume();
        final int childRole = parseRole();

        String parameter = token();
        while (!parameter.equals(")")) {
            consume();

            if (RIGHT_IDENTITY.equals(parameter)) {
                final int identity = parseRole();

                ontology.add(new RI(new int[] {childRole, identity}, childRole));
//            } else if (LEFT_IDENTITY.equals(parameter)) {
//                final int left = parseRole();
//
//                ont.addRI(new RI(new int[] {left, childRole}, childRole));
            } else if (TRANSITIVE.equals(parameter)) {
                if (parseBool()) {
                    ontology.add(new RI(new int[] {childRole, childRole}, childRole));
                }
            } else if (PARENT.equals(parameter)) {
                final String parent = token();

                if (!TOP.equals(parent)) {
                    final int parentRole = parseRole();

                    ontology.add(new RI(new int[] {childRole}, parentRole));
                } else {
                    consume();
                }
            } else if (REFLEXIVE.equals(parameter)) {
                parseBool();
            } else {
                throw new ParseException("Unexpected token: '" + parameter + "'", lineReader);
            }

            parameter = token();
        }
    }
    
    private boolean parseBool() throws ParseException {
        final boolean result = token().equals("t");
        consume();
        return result;
    }
    
}
