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

/**
 * Parser for the SNOMED Compositional Grammar. Note that certain parts of the
 * following are redundant and will not match anything in the context they are
 * called (assuming the entry point is expression). These terms are delimited
 * with /
 * 
 * <pre>
 * expression::= concept [/ws/ &quot;+&quot; concept ]* [&quot;:&quot; ws refinements /ws/]
 * concept::= ws conceptId ws [&quot;|&quot; ws term ws &quot;|&quot; ws]
 * conceptId::= digitNonZero digit digit digit digit digit+
 * term::= nonws [nonpipe* nonws]
 * refinements::= attributeSet /ws/ attributeGroup*
 * attributeGroup::= &quot;{&quot; ws attributeSet /ws/ &quot;}&quot; ws
 * attributeSet::= attribute /ws/ (&quot;,&quot; ws attribute /ws/)*
 * attribute::= attributeName /ws/ &quot;=&quot; attributeValue
 * attributeName::= ws attributeNameId ws [&quot;|&quot; ws term ws &quot;|&quot; ws]
 * attributeValue::= concept | (ws &quot;(&quot; expression &quot;)&quot; ws)
 * attributeNameId::= digitNonZero digit digit digit digit digit+
 * 
 * ws: ( space | tab | cr | lf )*
 * nonws::= &lt;any character other than those defined in ws and not &quot;|&quot;&gt;
 * digit::= (&quot;0&quot;|&quot;1&quot;|&quot;2&quot;|&quot;3&quot;|&quot;4&quot;|&quot;5&quot;|&quot;6&quot;|&quot;7&quot;|&quot;8&quot;|&quot;9&quot;)
 * digitNonZero::= (&quot;1&quot;|&quot;2&quot;|&quot;3&quot;|&quot;4&quot;|&quot;5&quot;|&quot;6&quot;|&quot;7&quot;|&quot;8&quot;|&quot;9&quot;)
 * nonpipe::= &lt;any character other than &quot;|&quot;&gt;
 * 
 * space::= &amp;H20
 * tab::= &amp;H09
 * cr::= &amp;H0C
 * lf::= &amp;H0A
 * </pre>
 * 
 * @author law223
 * 
 */
class SnomedExpressionParser {
    
    static IExpression DEFAULT_MATCHER = new MatcherAdapter();

    static class MatcherAdapter implements IMatcher {
        public IConcept conceptMatcher() {
            return this;
        }

        public IRefinement refinementMatcher() {
            return this;
        }

        public IAttributeGroup attributeGroupMatcher() {
            return this;
        }

        public IAttribute attributeMatcher() {
            return this;
        }

        public IAttributeName attributeNameMatcher() {
            return this;
        }

        public IExpression expressionMatcher() {
            return null;
        }

        public void matchConcept(String conceptId, String term) {
        }

        public void matchAttribute(String attributeId, String term) {
        }

        public void matchExpression() {
        }
    }

    private interface IMatcher extends IExpression, IConcept, IRefinement, IAttributeGroup, IAttribute, IAttributeName {
    };
    
    interface IExpression {
        IConcept conceptMatcher();
        IRefinement refinementMatcher();
        void matchExpression();
    };

    interface IConcept {
        void matchConcept(String conceptId, String term);
    };

    interface IRefinement {
        IAttribute attributeMatcher();
        IAttributeGroup attributeGroupMatcher();
    };

    interface IAttributeGroup {
        IAttribute attributeMatcher();
    };

    interface IAttribute {
        IAttributeName attributeNameMatcher();
        IExpression expressionMatcher();
        IConcept conceptMatcher();
    };

    interface IAttributeName {
        void matchAttribute(String attributeId, String term);
    };

    static class Scanner {

        enum TokenType {
            WS(" \t\n\r", true),
            NON_WS(" \t\n\r", false),
            NON_PIPE("|", false),
            DIGIT("0123456789", true),
            DIGIT_NON_ZERO("123456789", true);

            final private String members;
            final private boolean presence;

            TokenType(final String members, final boolean presence) {
                this.members = members;
                this.presence = presence;
            }

            public boolean matches(char c) {
                return presence == (members.indexOf(c) >= 0);
            }
        }

        final CharSequence chars;

        int idx;

        Scanner(final CharSequence seq) {
            chars = seq;
            idx = 0;
        }

        boolean EOI() {
            return idx >= chars.length();
        }

        /**
         * Test if current char matches supplied char.
         * @param c
         * @return
         */
        boolean match(char c) {
            return idx < chars.length() && c == chars.charAt(idx);
        }

        /**
         * Test if current char matches supplied token type.
         * @param Scanner.TokenType
         * @return
         */
        boolean match(Scanner.TokenType tokenType) {
            return idx < chars.length() && tokenType.matches(chars.charAt(idx));
        }

        char token() {
            return chars.charAt(idx);
        }

        void nextToken() {
            idx++;
        }
    }

    private Scanner scanner;

    public SnomedExpressionParser(String expression) {
        scanner = new Scanner(expression);
    }

    public void parseExpression(IExpression matcher) {
        assert matcher != null;
        
        if (!scanner.EOI()) {
            expression(matcher);
        }

    }

    /**
     * <pre>
     * expression ::= concept [/ws/ &quot;+&quot; concept ]* [&quot;:&quot; ws refinements /ws/]
     * </pre>
     * 
     * @param matcher
     */
    private void expression(IExpression matcher) {
        concept(matcher.conceptMatcher());

        // first optional section
        while (scanner.match('+')) {
            scanner.nextToken();
            concept(matcher.conceptMatcher());
        }

        // second optional section
        if (scanner.match(':')) {
            scanner.nextToken();
            ws();
            refinements(matcher.refinementMatcher());
        }
        
        matcher.matchExpression();
    }

    /**
     * <pre>
     * concept ::= ws conceptId ws [&quot;|&quot; ws term ws &quot;|&quot; ws]
     * </pre>
     * 
     * @param matcher
     */
    private void concept(IConcept matcher) {
        ws();
        final String conceptId = conceptId();
        ws();

        // optional section
        String term = null;
        if (scanner.match('|')) {
            scanner.nextToken();
            ws();
            term = term();
            // Our term() implementation consumes trailing whitespace
            consume('|');
            ws();
        }
        
        matcher.matchConcept(conceptId, term);
    }

    /**
     * <pre>
     * conceptId ::= digitNonZero digit digit digit digit digit+
     * </pre>
     * 
     * @return
     */
    private String conceptId() {
        final StringBuffer conceptId = new StringBuffer();

        conceptId.append(consume(Scanner.TokenType.DIGIT_NON_ZERO));

        int digitCount = 0;
        while (digitCount < 5) {
            conceptId.append(consume(Scanner.TokenType.DIGIT));
            digitCount++;
        }

        // yes, the following use of exceptions for control-flow is ugly
        while (scanner.match(Scanner.TokenType.DIGIT)) {
            conceptId.append(consume(Scanner.TokenType.DIGIT));
        }

        return conceptId.toString().trim();
    }

    /**
     * <pre>
     * term ::= nonws [nonpipe* nonws]
     * </pre>
     * 
     * @return
     */
    private String term() {
        // This is really a pretty ugly rule to implement; in a general grammar
        // we'd need to be able to backtrack in here, but since we "know" what
        // the full grammar is (i.e., the calling contexts of this rule) we can
        // simplify things by consuming trailing whitespace then trim()ing it
        // from the result.

        final StringBuffer term = new StringBuffer();

        if (!scanner.match(Scanner.TokenType.NON_WS)) {
            error(Scanner.TokenType.NON_WS);
        }
        term.append(consume(Scanner.TokenType.NON_WS));
        while (scanner.match(Scanner.TokenType.NON_PIPE)) {
            term.append(scanner.token());
            scanner.nextToken();
        }

        return term.toString().trim();
    }

    /**
     * <pre>
     * refinements ::= attributeSet /ws/ attributeGroup*
     *               | attributeGroup+
     * </pre>
     * @param matcher
     */
    private void refinements(IRefinement matcher) {
        if (scanner.match('{')) {
            attributeGroup(matcher.attributeGroupMatcher());
        } else {
            attributeSet(matcher.attributeMatcher());
            scanner.nextToken();
        }
        while (scanner.match('{')) {
            attributeGroup(matcher.attributeGroupMatcher());
        }
    }

    /**
     * <pre>
     * attributeGroup ::= &quot;{&quot; ws attributeSet /ws/ &quot;}&quot; ws
     * </pre>
     * @param matcher 
     */
    private void attributeGroup(IAttributeGroup matcher) {
        consume('{');
        ws();
        attributeSet(matcher.attributeMatcher());
        consume('}');
        ws();
    }

    /**
     * <pre>
     * attributeSet ::= attribute /ws/ (&quot;,&quot; ws attribute /ws/)*
     * </pre>
     * @param matcher
     */
    private void attributeSet(IAttribute matcher) {
        attribute(matcher);

        // optional section
        while (scanner.match(',')) {
            scanner.nextToken();
            ws();
            attribute(matcher);
        }
    }

    /**
     * <pre>
     * attribute ::= attributeName /ws/ &quot;=&quot; attributeValue
     * </pre>
     * @param matcher
     */
    private void attribute(IAttribute matcher) {
        attributeName(matcher.attributeNameMatcher());
        consume('=');
        attributeValue(matcher);
    }

    /**
     * <pre>
     * attributeName ::= ws attributeNameId ws [&quot;|&quot; ws term ws &quot;|&quot; ws]
     * </pre>
     * @return
     */
    private void attributeName(IAttributeName matcher) {
        ws();
        final String attributeId = attributeNameId();
        ws();

        // optional section
        String term = null;
        if (scanner.match('|')) {
            scanner.nextToken();
            ws();
            term = term();
            // Our term() implementation consumes trailing whitespace
            consume('|');
            ws();
        }
        
        matcher.matchAttribute(attributeId, term);
    }

    /**
     * <pre>
     * attributeValue ::= concept | (ws &quot;(&quot; expression &quot;)&quot; ws)
     * </pre>
     * @param expression
     */
    private void attributeValue(IAttribute matcher) {
        ws();
        if (scanner.match('(')) {
            // expression option
            scanner.nextToken();
            expression(matcher.expressionMatcher());
            consume(')');
            ws();
        } else {
            // concept option
            concept(matcher.conceptMatcher());
        }
    }

    /**
     * <pre>
     * attributeNameId ::= digitNonZero digit digit digit digit digit+
     * </pre>
     * @return
     */
    private String attributeNameId() {
        // the grammar here is identical to that of conceptId
        return conceptId();
    }

    

    /**
     * Match and discard whitespace.
     * 
     * <pre>
     * ws ::= (space | tab | cr | lf)*
     * </pre>
     */
    private void ws() {
        while (scanner.match(Scanner.TokenType.WS)) {
            scanner.nextToken(); // discard whitespace
        }
    }

    
    
    private char consume(Scanner.TokenType expectedToken) throws ScannerException {
        if (!scanner.match(expectedToken)) {
            error(expectedToken);
        }
        final char result = scanner.token();
        scanner.nextToken();
        return result;
    }

    private char consume(char expectedToken) throws ScannerException {
        if (!scanner.match(expectedToken)) {
            error(expectedToken);
        }
        final char result = scanner.token();
        scanner.nextToken();
        return result;
    }

    private void error(final Scanner.TokenType expected) throws ScannerException {
        final String current = scanner.EOI() ? "<end of input>" : "" + scanner.token();
        throw new ScannerException(current, String.valueOf(expected));
    }

    private void error(final char expected) throws ScannerException {
        final String current = scanner.EOI() ? "<end of input>" : "" + scanner.token();
        throw new ScannerException(current, String.valueOf(expected));
    }
    
    @SuppressWarnings("serial")
    static class ScannerException extends IllegalStateException {

        public ScannerException(String current, String expected) {
            super("Error in expression - found [" + current + "] but expecting [" + expected + "]");
        }
        
    }

    public static void main(String args[]) {
        SnomedExpressionParser exprParser = new SnomedExpressionParser(
                "187372000 | some  text | + 187372000|and yet more| ");
        exprParser.parseExpression(DEFAULT_MATCHER);
    }

}
