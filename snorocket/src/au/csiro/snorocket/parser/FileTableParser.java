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
import java.io.PrintWriter;
import java.io.Reader;

import au.csiro.snorocket.core.LineReader;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.Snorocket;
import au.csiro.snorocket.snapi.I_Snorocket;

import java.util.Set;
import java.util.HashSet;

public class FileTableParser {

    private Set<String> inactive = new HashSet<String>(100000);

    private I_Snorocket _rocket;

    public void parse(final I_Snorocket rocket, final boolean skipHeader, final Reader concepts, final Reader relationships, PrintWriter printWriter) throws ParseException {
        try {
            _rocket = rocket;

            loadConcepts(skipHeader, concepts);
            loadRelationships(skipHeader, relationships, printWriter);
        } finally {
            // avoid mem leak in parser
            _rocket = null;
        }
    }

    private void loadRelationships(final boolean skipHeader, final Reader relationships, final PrintWriter printWriter) throws ParseException {
        final LineReader lineReader = new LineReader(relationships);

        boolean firstLine = true;
        
        try {
            if (skipHeader) {
                final String line = lineReader.readLine();	// skip header line
                firstLine = false;
                
                int count = 0;
                for (int idx = line.indexOf('\t'); idx >= 0; idx = line.indexOf('\t', idx + 1)) {
                    count++;
                }
                if (count < 6) {
                    Snorocket.getLogger().warning("Header line contains fewer than expected 7 tab-separated columns: " + line);
                }
            }

            String line;
            while (null != (line = lineReader.readLine())) {
                if (firstLine && !skipHeader && line.contains("RELATIONSHIPID")) {
                    Snorocket.getLogger().warning("First line of relationships input looks like a header line: " + line);
                }
                firstLine = false;
                
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');		// 0..idx1 == relationshipid
                int idx2 = line.indexOf('\t', idx1+1);	// idx1+1..idx2 == conceptid1
                int idx3 = line.indexOf('\t', idx2+1);	// idx2+1..idx3 == RELATIONSHIPTYPE
                int idx4 = line.indexOf('\t', idx3+1);	// idx3+1..idx4 == conceptid2
                int idx5 = line.indexOf('\t', idx4+1);	// idx4+1..idx5 == CHARACTERISTICTYPE
                int idx6 = line.indexOf('\t', idx5+1);	// idx5+1..idx6 == REFINABILITY
                int idx7 = line.indexOf('\t', idx6+1);	// idx6+1..idx7 == RELATIONSHIPGROUP

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0 || idx6 < 0) {
                    throw new ParseException("Relationships: Mis-formatted line, expected at least 7 tab-separated fields, got: " + line, lineReader);
                }

                final String concept1 = line.substring(idx1+1, idx2);
                final String role = line.substring(idx2+1, idx3);
                final String concept2 = line.substring(idx3+1, idx4);
                final String characteristicType = line.substring(idx4+1, idx5);

                // only process active concepts and defining relationships
                if ("0".equals(characteristicType) && !inactiveRelationship(concept1, role, concept2)) {
                    final int group = idx7 < 0
                            ? Integer.parseInt(line.substring(idx6+1))
                            : Integer.parseInt(line.substring(idx6+1, idx7));

                    _rocket.addRelationship(concept1, role, concept2, group);
                } else if (null != printWriter) {
                    printWriter.println(line);
                }
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Relationships: Malformed number.", lineReader, e);
        } catch (IOException e) {
            throw new ParseException("Relationships: Problem reading relationships file.", lineReader, e);
        }
//        if (Snorocket.DEBUGGING) System.err.println("Number of rows = " + rowList.size());

    }

    private boolean inactiveRelationship(final String concept1, final String role, final String concept2) {
        return inactive.contains(concept1) ||
                inactive.contains(role) ||
                inactive.contains(concept2);
    }
    
    private void loadConcepts(final boolean skipHeader, final Reader concepts) throws ParseException {
        final LineReader lineReader = new LineReader(concepts);

        int totalConcepts = 0;
		int fullyDefinedCount = 0;

		try {
            if (skipHeader) {
                lineReader.readLine();	// skip header line
            }

            String line;
            while (null != (line = lineReader.readLine())) {
                if (line.trim().length() < 1) {
                    continue;
                }
                int idx1 = line.indexOf('\t');		// 0..idx1 == conceptid
                int idx2 = line.indexOf('\t', idx1+1);	// idx1+1..idx2 == status
                int idx3 = line.indexOf('\t', idx2+1);	// idx2+1..idx3 == fully specified name
                int idx4 = line.indexOf('\t', idx3+1);	// idx3+1..idx4 == CTV3ID
                int idx5 = line.indexOf('\t', idx4+1);	// idx4+1..idx5 == SNOMEDID
                int idx6 = line.indexOf('\t', idx5+1);	// idx5+1..idx6 == isPrimitive

                if (idx1 < 0 || idx2 < 0 || idx3 < 0 || idx4 < 0 || idx5 < 0) {
                    throw new ParseException("Concepts: Mis-formatted line, expected at least 6 tab-separated fields, got: " + line, lineReader);
                }

                final String status = line.substring(idx1+1, idx2);
                
                if ("0".equals(status) || "6".equals(status) || "11".equals(status)) {
                    // status one of 0, 6, 11 means the concept is active; we skip inactive concepts

                    final int isPrimitive = idx6 < 0
                            ? Integer.parseInt(line.substring(idx5+1))
                            : Integer.parseInt(line.substring(idx5+1, idx6));
                            
                    final boolean fullyDefined = 0 == isPrimitive;
					_rocket.addConcept(line.substring(0, idx1), fullyDefined);
                    totalConcepts++;
                    if (fullyDefined) {
                    	fullyDefinedCount ++;
                    }
                } else {
                    // We don't classify inactive concepts or relationships that refer to them
                    inactive.add(line.substring(0, idx1));
                }
            }
        } catch (NumberFormatException e) {
            throw new ParseException("Concepts: Malformed number.", lineReader, e);
        } catch (IOException e) {
            throw new ParseException("Concepts: Problem reading concepts file", lineReader, e);
        }

        if (Snorocket.DEBUGGING) {
            Snorocket.getLogger().info("Loaded " + totalConcepts + " concepts, " + fullyDefinedCount + " are fully defined.");
        }
    }

}
