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

package au.csiro.snorocket;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Set;

import au.csiro.snorocket.core.AbstractConcept;
import au.csiro.snorocket.core.Concept;
import au.csiro.snorocket.core.Conjunction;
import au.csiro.snorocket.core.Existential;
import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.core.GCI;
import au.csiro.snorocket.core.IConceptSet;
import au.csiro.snorocket.core.Inclusion;
import au.csiro.snorocket.core.RI;
import au.csiro.snorocket.core.Snorocket;
import au.csiro.snorocket.core.SparseConceptSet;
import au.csiro.snorocket.parser.KRSSParser;

public class GenerateStatedForm {

    final Factory factory = new Factory();

    int relCount = 1;
    int group = 0;

    public static void main(String[] args) throws Exception {
        final String fileName = args[0];

        final FileReader reader = new FileReader(fileName);
        final PrintWriter conceptsWriter =
                new PrintWriter(new FileWriter(fileName + "_concepts.txt"));
        final PrintWriter relationshipsWriter =
                new PrintWriter(new FileWriter(fileName + "_relationships.txt"));
        new GenerateStatedForm().generateStatedForm(reader, conceptsWriter,
            relationshipsWriter);
        conceptsWriter.close();
        relationshipsWriter.close();
    }

    public void generateStatedForm(Reader reader, PrintWriter conceptsWriter, PrintWriter relationshipsWriter) throws Exception {
        final IConceptSet fullyDefined = new SparseConceptSet();
        final KRSSParser parser = new KRSSParser();
        
        relationshipsWriter.println("RELATIONSHIPID\tCONCEPTID1\tRELATIONSHIPTYPE\tCONCEPTID2\tCHARACTERISTICTYPE\tREFINABILITY\tRELATIONSHIPGROUP\tCOMMENT");
        
        Set<Inclusion> inclusions = parser.parse(factory, reader);
        
        for (final Inclusion inc: inclusions) {
            group = 0;
            if (inc instanceof GCI) {
                GCI gci = (GCI) inc;

                if (gci.lhs() instanceof Concept) {
                    final String concept1 = factory.lookupConceptId(gci.lhs().hashCode());
//                    System.out.print(concept1 + " [ ");
                    p(relationshipsWriter, concept1, Snorocket.ISA_ROLE, gci.rhs());
//                    System.out.println();
                } else {
                    // These will be the fully-defined concepts:
                    if (gci.rhs() instanceof Concept) {
                        fullyDefined.add(gci.rhs().hashCode());
                    } else {
                        System.err.println(inc);
                    }
                }
            } else {
                RI ri = (RI) inc;
                
                final int[] lhs = ri.getLhs();
                final int rhs = ri.getRhs();
                
                if (lhs.length == 1) {
                    relationshipsWriter.println((relCount++) + "\t" + factory.lookupRoleId(lhs[0]) + "\t" + Snorocket.ISA_ROLE + "\t" + factory.lookupRoleId(rhs) + "\t0\t0\t0");
                } else {
                    System.err.println(ri);
                }
            }
        }
        
        conceptsWriter.println("CONCEPTID\tCONCEPTSTATUS\tFULLYSPECIFIEDNAME\tCTV3ID\tSNOMEDID\tISPRIMITVE");
        for (int i = 0; i < factory.getTotalConcepts(); i++) {
            if (i != Factory.TOP_CONCEPT && i != Factory.BOTTOM_CONCEPT) {
                final String concept = factory.lookupConceptId(i);
                conceptsWriter.println(concept + "\t0\t" + concept + "\t\t\t" + (fullyDefined.contains(i) ? "0" : "1"));
            }
        }
    }

    private void p(PrintWriter writer, String concept1, String rel,
            AbstractConcept ac) {
        if (ac instanceof Concept) {
            writer.println((relCount++) + "\t" + concept1 + "\t" + rel + "\t"
                + factory.lookupConceptId(ac.hashCode()) + "\t0\t0\t" + group);
        } else if (ac instanceof Existential) {
            final Existential e = (Existential) ac;
            final String roleId = factory.lookupRoleId(e.getRole());
            if (Snorocket.ROLE_GROUP.equals(roleId)) {
                group++;
                p(writer, concept1, Snorocket.ISA_ROLE, e.getConcept());
            } else {
                p(writer, concept1, roleId, e.getConcept());
            }
        } else {
            final Conjunction c = (Conjunction) ac;
            //            System.out.print("(");
            final AbstractConcept[] concepts = c.getConcepts();
            for (int i = 0; i < concepts.length; i++) {
                //                if (i > 0) {
                //                    System.out.print(" + ");
                //                }
                p(writer, concept1, rel, concepts[i]);
            }
            //            System.out.print(")");
        }
    }

}
