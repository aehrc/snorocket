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

package au.csiro.snorocket.printer;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import au.csiro.snorocket.snapi.I_Snorocket;

public class FileTablePrinter implements I_Snorocket.I_Callback {

    private final static String[] HEADER =
            { "RELATIONSHIPID", "CONCEPTID1", "RELATIONSHIPTYPE", "CONCEPTID2",
             "CHARACTERISTICTYPE", "REFINABILITY", "RELATIONSHIPGROUP",
             "RELATIONSHIPUUID", "CONCEPTUUID1", "RELATIONSHIPTYPEUUID",
             "CONCEPTUUID2", "CHARACTERISTICTYPEUUID", "REFINABILITYUUID",
             "RELATIONSHIPSTATUSUUID", "EFFECTIVETIME" };

    private final int columnCount = 7;

    final private PrintWriter _writer;

    final private String _date =
            new SimpleDateFormat("yyyyMMdd hh:mm:ss").format(new Date());

    private int _relid = 1;

    public FileTablePrinter(final PrintWriter writer) {
        this._writer = writer;

        printline(_writer, HEADER);
    }

    public void addRelationship(String conceptId1, String roleId,
            String conceptId2, int group) {
        final Object[] elements =
                { _relid++, conceptId1, roleId, conceptId2, 0, 0, group,
                 UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                 UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                 UUID.randomUUID(), _date };

        printline(_writer, elements);
    }

    private void printline(final PrintWriter w, final Object[] elements) {
        w.print(elements[0]);
        for (int i = 1; i < columnCount; i++) {
            w.print("\t");
            w.print(elements[i]);
        }
        w.println();
    }

}
