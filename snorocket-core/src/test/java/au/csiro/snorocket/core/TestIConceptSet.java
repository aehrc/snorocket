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

package au.csiro.snorocket.core;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import au.csiro.snorocket.core.util.IConceptSet;

public abstract class TestIConceptSet {

    private static final int RANGE = 500000;

    abstract IConceptSet createSet(final int capacity);

    /**
     * The following test is designed to trigger a potentially pathalogical
     * condition.
     */
    @Test
    public void testDoubleAdd() {
        final int capacity = 32;
        final IConceptSet set = createSet(capacity);
        final int val1 = 1;
        final int val2 = val1 + capacity; // ensure a re-probe when val2 is
                                          // inserted after val1

        set.add(val1);
        assertTrue(set.contains(val1));

        set.add(val2);
        assertTrue(set.contains(val2));

        checkRemove(set, val1, "");

        set.add(val2);
        assertTrue(set.contains(val2));

        checkRemove(set, val2, "");
    }

    @Test
    public void testAdd() {
        final int limit1 = 10;
        final int limit2 = 200;

        for (int capacity = 1; capacity < limit1; capacity++) {
            final IConceptSet set = createSet(capacity);
            for (int i = 0; i < limit2; i++) {
                final int val = (int) (Math.random() * RANGE);
                set.add(val);
                assertTrue("Set should contain " + val, set.contains(val));

                perturb(set);
            }
        }
    }

    @Test
    public void testAddSequence() {
        final int limit1 = 100;
        final int limit2 = 20;

        for (int capacity = 1; capacity < limit1; capacity++) {
            final IConceptSet set = createSet(capacity);
            for (int i = 0; i < limit2; i++) {
                final int val = i;
                set.add(val);
                assertTrue("Set should contain " + val, set.contains(val));
            }
        }

        for (int capacity = 1; capacity < limit1; capacity++) {
            final IConceptSet set = createSet(capacity);
            for (int i = limit2; i > 0; i--) {
                final int val = i;
                set.add(val);
                assertTrue("Set should contain " + val, set.contains(val));
            }
        }
    }

    private void perturb(final IConceptSet set) {
        for (int i = 0; i < 100; i++) {
            final int val = (int) (Math.random() * RANGE);
            set.add(val);
            assertTrue("Set should contain " + val, set.contains(val));
            String messageSuffix = " (iteration " + i + ")";
            checkRemove(set, val, messageSuffix);
        }
    }

    private void checkRemove(final IConceptSet set, final int val,
            String messageSuffix) {
        if (supportsRemove()) {
            set.remove(val);
            if (set.contains(val)) {
                System.err.println();
            }
            assertFalse("Set should not contain " + val + messageSuffix,
                    set.contains(val));
        }
    }

    abstract boolean supportsRemove();

}
