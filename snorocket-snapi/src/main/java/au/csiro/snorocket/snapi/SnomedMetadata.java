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

import java.util.HashMap;
import java.util.Map;

import au.csiro.snorocket.core.Snorocket;

/**
 * Utility that encapsulates SNOMED-specific knowledge.
 * <p>
 * <b>CAUTION:</b> The values for these parameters depend on the particular release of SNOMED CT.<br>
 * Do not assume they remain stable across different releases.
 * These values are valid for 20080731, 20090131, and 20090731
 * 
 * @author law223
 */
final public class SnomedMetadata {

    private final static Map<String, SnomedMetadata> METADATA = new HashMap<String, SnomedMetadata>() {
        {
            final SnomedMetadata V20080731 =
                new SnomedMetadata(Snorocket.ISA_ROLE,
                        new String[] {
                        Snorocket.ISA_ROLE,
                        "410662002"     // Concept Model Attribute
                },
                new String[] {
                        "123005000",	// part-of
                        "272741003",	// laterality
                        "127489000",	// has-active-ingredient
                        "411116001"	// has-dose-form
                },
                new String[][] {
                        // direct-substance o has-active-ingredient -> direct-substance
                        {"363701004", "127489000"}
                });

            put("20080731", V20080731);
            put("20090131", V20080731);
            put("20090731", V20080731);
        }
    };

    public final String ISA;
    public final String[] ROLE_ROOTS;
    public final String[] NEVER_GROUPED;
    public final String[][] RIGHT_IDENTITIES;

    private SnomedMetadata(String isa, String[] roleRoots, String[] neverGrouped, String[][] rightIdentities) {
        ISA = isa;
        ROLE_ROOTS = roleRoots;
        NEVER_GROUPED = neverGrouped;
        RIGHT_IDENTITIES = rightIdentities;
    }

    /**
     * Use the encapsulated SNOMED metadata to initialise Snorocket.
     * 
     * @param rocket
     * @param version
     * @throws IllegalArgumentException if an unknown version is supplied.
     */
    public static void configureSnorocket(I_Snorocket rocket, String version) {
        SnomedMetadata md = METADATA.get(version);

        if (null == md) {
            throw new IllegalArgumentException();
        }

        rocket.setIsa(md.ISA);
        for (final String role: md.ROLE_ROOTS) {
            rocket.addRoleRoot(role, false);
        }
        for (final String role: md.NEVER_GROUPED) {
            rocket.addRoleNeverGrouped(role);
        }

        for (final String[] composition: md.RIGHT_IDENTITIES) {
            rocket.addRoleComposition(composition, composition[0]);
        }
    }

}
