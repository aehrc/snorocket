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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import au.csiro.snorocket.core.AbstractConcept;
import au.csiro.snorocket.core.Concept;
import au.csiro.snorocket.core.Conjunction;
import au.csiro.snorocket.core.Existential;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.Inclusion;
import au.csiro.snorocket.core.LineReader;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.Snorocket;

abstract class AbstractKRSSParser {

    private static final String DELIMITERS = "( \t)'|";

    protected static final String TOP = "top";
    protected static final String PRIMITIVE_CONCEPT = "define-primitive-concept";
    protected static final String FULLY_DEFINED_CONCEPT = "define-concept";
    protected static final String EQUIVALENT = "equivalent";
    protected static final String IMPLIES = "implies";
    protected static final String PRIMITIVE_ROLE = "define-primitive-role";
    protected static final String RIGHT_IDENTITY = ":right-identity";
    protected static final String LEFT_IDENTITY = ":left-identity";
    protected static final String AND = "and";
    protected static final String SOME = "some";

    protected LineReader lineReader;
    private String line;
    private String token;
    private StringTokenizer st;

    private Set<String> maps = new HashSet<String>();

    private Set<String> missedMaps = new HashSet<String>();

    private Map<String, String> cMap = new HashMap<String, String>();

    protected IFactory _factory;

    abstract void parseStatement(Set<Inclusion> result) throws ParseException;

    protected void expect(String expected) throws ParseException {
        if (!expected.equals(token())) {
            throw new ParseException("Expected '" + expected + "', but found '" + token + "'", lineReader);
        }
    }

    /**
     * Returns the current token or null at end of input
     * 
     * @return
     * @throws ParseException 
     */
    protected String token() throws ParseException {
        if (null == token) {
            findNextToken();
        }
        return token;
    }

    private void findNextToken() throws ParseException {
        if (null == line || !st.hasMoreElements()) {
            try {
                do {
                    line = lineReader.readLine();
                } while (null != line && line.startsWith(";;"));
                //              System.err.println(lineCount + "\t" + line);
                if (null != line) {
                    st = new StringTokenizer(line, DELIMITERS, true);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (null != line) {
            while (st.hasMoreElements() && isWhitespace(token = st.nextToken())) {
                // skip whitespace
            }
            if (null == token || isWhitespace(token)) {
                findNextToken();
            }

            // handle quoting
            handleQuoting();
        }
    }

    private static boolean isWhitespace(final String token) {
        return " ".equals(token) || "\t".equals(token);
    }

    private void handleQuoting() throws ParseException {
        if ("'".equals(token) || "|".equals(token)) {
            final String quote = token;
            token = "";
            String fragment = null;
            while (st.hasMoreElements() && !quote.equals(fragment = st.nextToken())) {
                token += fragment;
            }
            if (null == fragment) {
                throw new ParseException("Could't find closing quote symbol (" + quote + ") for token starting with " + token, lineReader);
            }
        }
    }

    /**
     * Consumes current token and reads next token (skips whitespace)
     * @throws ParseException 
     *
     */
    protected void consume() throws ParseException {
        if (null == token) {
            findNextToken();
        }
        token = null;
    }
    
    protected Concept parseConcept(String string) throws ParseException {
        final int concept;

        if (cMap.containsKey(string)) {
            concept = _factory.getConcept(cMap.get(string));
            maps.add(string);
        } else {
            concept = _factory.getConcept(string);
            if (string.startsWith("C")) {
//                System.err.println("Oops, no mapping for " + string);
                missedMaps.add(string);
            }
        }
        consume();
        return new Concept(concept);
    }
    
    protected int parseRole() throws ParseException {
        final String string = token();
        final int role;
        if (cMap.containsKey(string)) {
            role = _factory.getRole(cMap.get(string));
        } else {
            role = _factory.getRole(string);
        }
        consume();
        return role;
    }

    /**
     * <pre>
     * expressions ::= ( concept
     *                 | '(' ( AND (expression)*
     *                       | SOME role expression
     *                       ) ')'
     *             ;
     * </pre>
     * @return
     * @throws ParseException 
     */
    protected AbstractConcept parseExpression() throws ParseException {
        AbstractConcept result;
        String next = token();
        if ("(".equals(next)) {
            consume();
            if (AND.equals(token())) {
                result = parseAnd();
            } else if (SOME.equals(token())) {
                result = parseSome();
            } else {
                throw new ParseException("Expected '" + AND + "' or '" + SOME + "', but found '" + token() + "'", lineReader);
            }

            expect(")");
            consume();
        } else {
            result = parseConcept(next);
        }
        return result;
    }

    /**
     * Called when token = SOME
     * some ::= role expression
     *      ;
     * @return
     * @throws ParseException 
     */
    private AbstractConcept parseSome() throws ParseException {
        AbstractConcept result;
        consume();

        int role = parseRole();

        result = new Existential(role, parseExpression());
        return result;
    }

    /**
     * Called when token = AND
     * Expects at least one expression in the conjunction
     * and ::= (expression)*
     *     ;
     * @return
     * @throws ParseException 
     */
    private AbstractConcept parseAnd() throws ParseException {
        consume();

        List<AbstractConcept> concepts = new ArrayList<AbstractConcept>();
        concepts.add(parseExpression());

        while (!")".equals(token())) {
            concepts.add(parseExpression());
        }
        
        //      if (concepts.size() < 1) {
        //      error("Empty conjunction");
        //      }
        
        if (concepts.size() == 1) {
            return concepts.get(0);
        } else {
            return new Conjunction(concepts);
        }
    }

    public Set<Inclusion> parse(final IFactory factory, final Reader reader) throws ParseException {
        final Set<Inclusion> result = new HashSet<Inclusion>();
    
        _factory = factory;
        lineReader = new LineReader(reader);
        maps.clear();
        missedMaps.clear();
    
        long stmtCount;
        for (stmtCount = 0; null != token(); stmtCount++) {
            parseStatement(result);
        }
    
        if (Snorocket.DEBUGGING) {
            System.err.println("Parsed " + stmtCount + " statements");
            System.err.println("Missed " + missedMaps.size() + " maps, succeeded " + maps.size());
        }
    
        _factory = null;
        lineReader = null;
    
        return result;
    }

}
