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
package au.csiro.snorocket.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Ignore;
import org.junit.Test;

import au.csiro.snorocket.core.axioms.NF1a;
import au.csiro.snorocket.core.axioms.NF2;
import au.csiro.snorocket.core.axioms.NF3;
import au.csiro.snorocket.core.axioms.NF4;
import au.csiro.snorocket.core.axioms.NF5;
import au.csiro.snorocket.core.axioms.NF6;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

public class TestReflexiveRoles {

    @Test
    public void testPartWhole() {
        IFactory factory = new Factory();
        NormalisedOntology o = new NormalisedOntology(factory);

        // roles
        int partOf = factory.getRole("part-of");
        int partOrWholeOf = factory.getRole("part-whole-of");

        o.addTerm(new NF4(partOf, partOrWholeOf));
        o.addTerm(new NF5(partOf, partOf, partOf)); // transitive
        o.addTerm(new NF6(partOrWholeOf)); // reflexive

        // concepts
        int gastroTract = factory.getConcept("Gastrointestinal-Tract");
        int stomach = factory.getConcept("Stomach");
        int intestine = factory.getConcept("Intestine");
        int smallIntestine = factory.getConcept("Small-Intestine");
        int largeIntestine = factory.getConcept("Large-Intestine");
        int appendix = factory.getConcept("Appendix");

        o.addTerm(NF2.getInstance(appendix, partOf, largeIntestine));
        o.addTerm(NF2.getInstance(largeIntestine, partOf, intestine));
        o.addTerm(NF2.getInstance(smallIntestine, partOf, intestine));
        o.addTerm(NF2.getInstance(intestine, partOf, gastroTract));
        o.addTerm(NF2.getInstance(stomach, partOf, gastroTract));

        int allParts = factory
                .getConcept("|All anatomical parts of the gastrointestinal tract|");
        int allPartsOrWhole = factory
                .getConcept("|All anatomical parts or whole of the gastrointestinal tract|");

        // allParts == partOf.gastroTract
        o.addTerm(NF2.getInstance(allParts, partOf, gastroTract));
        o.addTerm(NF3.getInstance(partOf, gastroTract, allParts));

        // allPartsOrWhole == partOrWholeOf.gastroTract
        o.addTerm(NF2.getInstance(allPartsOrWhole, partOrWholeOf, gastroTract));
        o.addTerm(NF3.getInstance(partOrWholeOf, gastroTract, allPartsOrWhole));

        o.classify();
        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        final int count = printAll(factory, s);

        final IConceptSet gtParents = s.get(gastroTract);
        assertTrue("Gastro Tract should be all partsOrWhole of itself",
                gtParents.contains(allPartsOrWhole));
        assertEquals("Wrong number of ancestors for Gastro Tract", 3,
                gtParents.size());

        assertEquals("Incorrect number of subsumption edges", 31, count);

    }

    @Ignore
    @Test
    public void simpleReflexive() {
        IFactory factory = new Factory();
        NormalisedOntology o = new NormalisedOntology(factory);

        // roles
        int partOf = factory.getRole("partOf");
        int subPart = factory.getRole("subPart");

        o.addTerm(new NF4(subPart, partOf));
        o.addTerm(new NF6(partOf)); // reflexive

        // concepts
        int foot = factory.getConcept("Foot");
        int lowerLeg = factory.getConcept("Lower_Leg");
        int bodyPart = factory.getConcept("Body_Part");
        int xx = factory.getConcept("XX");
        int yy = factory.getConcept("YY");

        // relationships

        o.addTerm(NF2.getInstance(foot, subPart, lowerLeg));
        o.addTerm(NF2.getInstance(xx, partOf, yy));
        o.addTerm(NF1a.getInstance(lowerLeg, bodyPart));
        o.addTerm(NF1a.getInstance(foot, bodyPart));

        final IConceptMap<IConceptSet> s = o.getSubsumptions();

        printAll(factory, s);

        final R r = o.getRelationships();

        for (int concept = 0; concept < factory.getTotalConcepts(); concept++) {
            for (int role = 0; role < factory.getTotalRoles(); role++) {
                final IConceptSet Bs = r.lookupB(concept, role);
                for (final IntIterator itr = Bs.iterator(); itr.hasNext();) {
                    final int b = itr.next();
                    System.out.println(factory.lookupConceptId(concept) + " [ "
                            + factory.lookupRoleId(role) + "."
                            + factory.lookupConceptId(b));
                }
            }
        }
    }

    private int printAll(IFactory factory, final IConceptMap<IConceptSet> s) {
        int count = 0;
        for (final IntIterator keyItr = s.keyIterator(); keyItr.hasNext();) {
            final int key = keyItr.next();

            System.out.print(factory.lookupConceptId(key) + " :");
            for (final IntIterator valItr = s.get(key).iterator(); valItr
                    .hasNext();) {
                final int val = valItr.next();

                System.out.print(" " + factory.lookupConceptId(val));
                count++;
            }

            System.out.println();
        }

        return count;
    }
}
