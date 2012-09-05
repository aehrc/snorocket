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

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

import au.csiro.snorocket.core.AbstractConcept;
import au.csiro.snorocket.core.Concept;
import au.csiro.snorocket.core.Conjunction;
import au.csiro.snorocket.core.Existential;
import au.csiro.snorocket.core.GCI;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.Inclusion;
import au.csiro.snorocket.core.LineReader;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.RI;

public class SnorocketParser {
    static boolean FILTER_PCDEFS = false;

    private static final String DELIMITERS = "][.+() " + // valid delimiters
                                             ",:;{}";    // invalid delimiters

    private static enum InclusionToken {
        EQUIVALENCE (" \u2250 "),
        LEFT_INCLUSION (" \u2291 "),
        RIGHT_INCLUSION (" \u2292 ");

        final String label;

        InclusionToken(String label) {
            this.label = label;
        }

        public String toString() {
            return label;
        }
    }

    private IFactory _factory;

    public Set<Inclusion> parse(final IFactory factory, final String ontology) throws ParseException, FileNotFoundException {
        final FileReader ontologyReader = new FileReader(ontology);
        FileReader terminologyReader = null;
        try {
            final int idx = ontology.lastIndexOf('.');
            if (idx >= 0) {
                terminologyReader = new FileReader(ontology.substring(0, idx) + ".trm");
            }
        } catch (FileNotFoundException e) {
            // ignore
        }
        return parse(factory, new LineReader(terminologyReader), new LineReader(ontologyReader));
    }

    private Set<Inclusion> parse(final IFactory factory, final LineReader terminologyReader, final LineReader ontologyReader) throws ParseException {
        try {
            _factory = factory;
            final Set<Inclusion> result = new HashSet<Inclusion>();

            readTerminology(terminologyReader, result);

            parse(ontologyReader, result);
            return result;
        } finally {
            _factory = null;
        }
    }

    private void readTerminology(LineReader terminologyReader, final Set<Inclusion> result) throws ParseException {
        if (null != terminologyReader) {
            try {
                String line = terminologyReader.readLine();
                while (null != line) {
                    if (line.trim().startsWith("#")) {
                        // It's a comment line
                        continue;
                    }

                    final String[] fields = line.split(":");

                    // Trim leading and trailing whitespace
                    for (int i = 0; i < fields.length; i++) {
                        fields[i] = fields[i].trim();
                    }

                    final String id = fields[0];
                    final String term = fields[1];
                    if (fields.length == 2) {	// maps id to string
                        getConcept(id);
                    } else if (fields.length == 3) {	// defines synonym (we create equivalent Concepts)
                        final String syn = term.substring(0, term.indexOf(" "));
                        final Concept c1 = getConcept(syn);
                        final Concept c2 = getConcept(id);

                        result.add(new GCI(c1, c2));
                        result.add(new GCI(c2, c1));
                    } else if (fields.length > 0){
                        throw new ParseException("Malformed terminology, wrong number of fields.", terminologyReader);
                    }

                    line = terminologyReader.readLine();
                }
            } catch (IOException e) {
                throw new ParseException("Problem reading terminology file", terminologyReader, e);
            }
        }
    }

    private Concept getConcept(final String id) {
        return new Concept(_factory.getConcept(id));
    }

    public Set<Inclusion> parse(final IFactory factory, final Reader ontologyReader) throws ParseException {
        return parse(factory, null, new LineReader(ontologyReader));
    }

    void parse(final LineReader ontologyReader, final Set<Inclusion> ontology) throws ParseException {

        String line;

        try {
            while ((line = ontologyReader.readLine()) != null && !"--".equals(line)) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                if (line.contains("<")) {
                    processRI(ontology, line, ontologyReader.getLineNumber());
                } else {
                    processGCI(ontology, line, ontologyReader.getLineNumber());
                }
            }

            while ((line = ontologyReader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0 || line.startsWith("#")) {
                    continue;
                }
                processRI(ontology, line, ontologyReader.getLineNumber());
            }
        } catch (IOException e) {
            throw new ParseException("Problem reading ontology file.", ontologyReader, e);
        }
    }

    private void processGCI(Set<Inclusion> ontology, String line, int lineNumber) throws ParseException {
        this.lineNumber = lineNumber;
        final Set<GCI> gcis = parseGCI(line);
        for (GCI gci: gcis) {
            ontology.add(gci);
        }
    }

    private void processRI(Set<Inclusion> ontology, String line, int lineNumber) throws ParseException {
        this.lineNumber = lineNumber;
        final RI ri = parseRI(line);
        ontology.add(ri);
    }

    /**
     * @param statement
     * @throws ParseException 
     */
    RI parseRI(final String statement) throws ParseException {
        StringTokenizer st = new StringTokenizer(statement, DELIMITERS, true);

        List<Integer> lhsList = new ArrayList<Integer>();

        lhsList.add(role(nextToken(st)));
        while (!lookahead(st, "[") && !lookahead(st, "<")) {
            lhsList.add(role(nextToken(st)));
        }
        consumeToken(st);
        int rhs = role(nextToken(st));

        endOfInput(st);

//      System.out.println(lhs + " [ " + rhs);
        int[] lhs = new int[lhsList.size()];
        int i = 0;
        for (Integer role: lhsList) {
            lhs[i++] = role;
        }

        return new RI(lhs, rhs);
    }

    /**
     * @param statement
     * @throws ParseException 
     */
    Set<GCI> parseGCI(final String statement) throws ParseException {
        StringTokenizer st = new StringTokenizer(statement, DELIMITERS, true);

        if (FILTER_PCDEFS && !statement.contains("][")) return Collections.emptySet();

        final AbstractConcept lhs = expression(st);
        final InclusionToken inclusion = inclusion(st);
        final AbstractConcept rhs = expression(st);

        endOfInput(st);

//      System.out.println(lhs + String.valueOf(inclusion) + rhs);

        final Set<GCI> result = new HashSet<GCI>();

        switch (inclusion) {
        case EQUIVALENCE:
            result.add(new GCI(lhs, rhs));
            result.add(new GCI(rhs, lhs));
            break;

        case LEFT_INCLUSION:
            result.add(new GCI(lhs, rhs));
            break;

        case RIGHT_INCLUSION:
            result.add(new GCI(rhs, lhs));
            break;

        default:
            throw new AssertionError(lineNumber + ": Unknown Inclusion: " + inclusion);
        }

        return result;
    }

    /**
     * Parse one of '[', ']', or ']['
     * 
     * @param st
     * @return
     * @throws ParseException 
     */
    private InclusionToken inclusion(StringTokenizer st) throws ParseException {
        String tok = nextToken(st);
        final InclusionToken result;
        if ("]".equals(tok)) {
            if (lookahead(st, "[")) {
                nextToken(st);
                result = InclusionToken.EQUIVALENCE;
            } else {
                result = InclusionToken.RIGHT_INCLUSION;
            }
        } else {
            expect(tok, "[");
            result = InclusionToken.LEFT_INCLUSION;
        }
        return result;
    }

    /**
     * st should contain a complete expression (an SCTConcept or an SCTExpression)
     * 
     * @param st
     * @param expected
     * @return
     * @throws ParseException 
     */
    private AbstractConcept expression(StringTokenizer st) throws ParseException {
        String tok = nextToken(st);
        if (null == tok) {
            return null;
        }

        final AbstractConcept value;
        if ("(".equals(tok)) {
            value = expression(st);
            expect(nextToken(st), ")");
        } else if (lookahead(st, ".")) {
            int role = role(tok);
            consumeToken(st);
            if (lookahead(st, "(")) {
                consumeToken(st);
                AbstractConcept concept = expression(st);
                expect(nextToken(st), ")");
                value = new Existential(role, concept);
            } else {
                AbstractConcept concept = concept(nextToken(st));
                value = new Existential(role, concept);
            }
        } else {
            value = concept(tok);
        }

        return optionalConjunction(st, value);
    }

    private AbstractConcept optionalConjunction(StringTokenizer st, AbstractConcept concept) throws ParseException {
        final AbstractConcept value;
        if (lookahead(st, "+")) {
            final Collection<AbstractConcept> concepts = new ArrayList<AbstractConcept>();
            concepts.add(concept);

            while (lookahead(st, "+")) {
                consumeToken(st);
                concepts.add(expression(st));
            }

            value = new Conjunction(concepts);
        } else {
            value = concept;
        }
        return value;
    }

    private Concept concept(String tok) {
        int concept;
//      try {
        if ("top".equals(tok)) {
            // SNOMED CT Concept (SNOMED RT+CTV3)
            concept = IFactory.TOP_CONCEPT;
//          } else if ("123456789".contains(tok.subSequence(0, 1))) {
//          // probably a number
//          long conceptId = Long.parseLong(tok);
//          concept = Concept.getInstance("S_" + conceptId);
        } else if ("bottom".equals(tok)) {
            concept = IFactory.BOTTOM_CONCEPT;
        } else {
            concept = _factory.getConcept(tok);
        }
//      } catch (NumberFormatException e) {
//      concept = Concept.getInstance(tok);
//      }
        return new Concept(concept);
    }


    private int role(String tok) {
        int role = _factory.getRole(tok);
//      try {
//      long roleId = Long.parseLong(tok);
//      role = Role.getInstance(roleId);
//      } catch (NumberFormatException e) {
//      role = Role.getInstance(tok);
//      }
        return role;
    }

    private void expect(String token, String expected) throws ParseException {
        if (null == token || null == expected || !expected.contains(token)) {
            throw new ParseException("Expected '" + expected + "', but found '" + token + "'", lineNumber, null);
        }
    }


    private String _nextToken = null;
    private int lineNumber = -1;

    private boolean lookahead(final StringTokenizer st, final String string) {
        _findNextToken(st);
        return string.equals(_nextToken);
    }

    private void consumeToken(final StringTokenizer st) {
        _findNextToken(st);
        _nextToken = null;
    }

    private String nextToken(final StringTokenizer st) {
        final String tok = _findNextToken(st);
        _nextToken = null;
        return tok;
    }

    private void endOfInput(StringTokenizer st) throws ParseException {
        final String next;
        if (null != (next = nextToken(st))) {
            throw new ParseException("Expected end of input, but found '" + next + "'", lineNumber, null);
        }
    }

    private String _findNextToken(final StringTokenizer st) {
        if (null == _nextToken) {
            while (st.hasMoreTokens() && " ".equals(_nextToken = st.nextToken())) {
                // skip whitespace
            }
            if (" ".equals(_nextToken)) {
                _nextToken = null;
            }
        }
        return _nextToken;
    }

}
