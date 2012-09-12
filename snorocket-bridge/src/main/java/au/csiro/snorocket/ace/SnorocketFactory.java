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

package au.csiro.snorocket.ace;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import au.csiro.snorocket.snapi.I_Snorocket;
import au.csiro.snorocket.snapi.Snorocket;

final public class SnorocketFactory implements I_SnorocketFactory {

    final private I_Snorocket rocket;

    public SnorocketFactory() {
        this(new Snorocket());
    }

    public SnorocketFactory(InputStream stream) {
        //this(new Snorocket(stream));
    	// FIXME: need to re-implement loading from stream functionality
    	this(new Snorocket());
    }

    private SnorocketFactory(I_Snorocket rocket) {
        this.rocket = rocket;
    }

    public void setIsa(int isaId) {
        rocket.setIsa(wrap(isaId));
    }

    public void addRoleRoot(int id, boolean inclusive) {
        rocket.addRoleRoot(wrap(id), inclusive);
    }

    public void addRoleNeverGrouped(int id) {
        rocket.addRoleNeverGrouped(wrap(id));
    }

    synchronized public void addConcept(int conceptId, boolean fullyDefined) {
        rocket.addConcept(wrap(conceptId), fullyDefined);
    }

    synchronized public void addRelationship(int c1, int rel, int c2, int group) {
        final String c1Id = wrap(c1);
        final String relId = wrap(rel);
        final String c2Id = wrap(c2);

        // hand-holding for ACE -- ensure all concepts are implicitly defined
        rocket.addConcept(c1Id, false);
        rocket.addConcept(relId, false);
        rocket.addConcept(c2Id, false);
        rocket.addRelationship(c1Id, relId, c2Id, group);
    }

    public void addRoleComposition(int[] lhsIds, int rhsId) {
        int s = lhsIds.length;
        String[] sa = new String[s];
        for (int i = 0; i < s; i++)
            sa[i] = String.valueOf(lhsIds[i]);

        rocket.addRoleComposition(sa, wrap(rhsId));
    }

    public void classify() {
        rocket.classify();
    }

    public void getResults(final I_SnorocketFactory.I_Callback callback) {
        rocket.getDistributionFormRelationships(new I_Snorocket.I_Callback() {

            final public void addRelationship(String cId1, String roleId,
                    String cId2, int group) {
                callback.addRelationship(unwrap(cId1), unwrap(roleId),
                    unwrap(cId2), group);
            }

        });
    }

    // public void getEquivConcepts(final
    // I_SnorocketFactory.I_EquivalentCallback callback)
    public void getEquivConcepts(
            final I_SnorocketFactory.I_EquivalentCallback callback) {
        rocket.getEquivalents(new I_Snorocket.I_EquivalentCallback() {

            final public void equivalent(Collection<String> equivalentConcepts) {
                callback.equivalent(equivalentConcepts);
            }

        });
    }

    static private String wrap(final int id) {
        return String.valueOf(id);
    }

    static private int unwrap(final String id) {
        return Integer.parseInt(String.valueOf(id));
    }

    public I_SnorocketFactory createExtension() {
        return new SnorocketFactory(rocket.createExtension());
    }

    public InputStream getStream() throws IOException {
        return rocket.getStream();
    }

}
