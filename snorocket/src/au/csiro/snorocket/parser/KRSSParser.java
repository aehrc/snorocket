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

import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.axioms.RI;
import au.csiro.snorocket.core.model.AbstractConcept;

public class KRSSParser extends AbstractKRSSParser {

    public KRSSParser() {
    }

    /**
     * <pre>
     * stmt ::= '(' ( PRIMITIVE_ROLE role role (RIGHT_IDENTITY role)?
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
        
        String declaration = token();

        if (PRIMITIVE_ROLE.equals(declaration) || "role-inclusion".equals(declaration)) {
            consume();
            final int childRole = parseRole();

            final String parent = token();
            consume();

            if (!TOP.equals(parent)) {
                final int parentRole = _factory.getRole(parent);

                ontology.add(new RI(new int[] {childRole}, parentRole));
            }

            final String parameter = token();
            if (RIGHT_IDENTITY.equals(parameter)) {
                consume();
                final int identity = parseRole();

                ontology.add(new RI(new int[] {childRole, identity}, childRole));
            } else if (LEFT_IDENTITY.equals(parameter)) {
                consume();
                final int left = parseRole();

                ontology.add(new RI(new int[] {left, childRole}, childRole));
            }
            
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
            } else if ("transitive".equals(declaration)) {
                consume();

                final int role = parseRole();

                ontology.add(new RI(new int[] {role, role}, role));
            } else {
                throw new ParseException("Unexpected token: '" + declaration + "'", lineReader);
            }
        }

        expect(")");
        consume();
    }

}
