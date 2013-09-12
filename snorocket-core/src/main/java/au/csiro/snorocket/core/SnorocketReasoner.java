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

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Axiom;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.ConceptInclusion;
import au.csiro.ontology.model.Conjunction;
import au.csiro.ontology.model.Datatype;
import au.csiro.ontology.model.Existential;
import au.csiro.ontology.model.Feature;
import au.csiro.ontology.model.Literal;
import au.csiro.ontology.model.NamedConcept;
import au.csiro.ontology.model.NamedFeature;
import au.csiro.ontology.model.NamedRole;
import au.csiro.ontology.model.Operator;
import au.csiro.snorocket.core.concurrent.CR;
import au.csiro.snorocket.core.concurrent.Context;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.FloatLiteral;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.model.StringLiteral;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.RoleSet;

/**
 * This class represents an instance of the reasoner. It uses the internal
 * ontology model. If you need to use an OWL model refer to the
 * {@link SnorocketOWLReasoner} class.
 *
 * @author Alejandro Metke
 *
 */
final public class SnorocketReasoner implements IReasoner, Serializable {

    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(SnorocketReasoner.class);

    public static final int BUFFER_SIZE = 10;

    private NormalisedOntology no = null;
    private IFactory factory = null;
    private boolean isClassified = false;

    /**
     * Loads a saved instance of a {@link SnorocketReasoner} from an input
     * stream.
     *
     * @param in
     * @return
     */
    public static SnorocketReasoner load(InputStream in) {
        SnorocketReasoner res;
        ObjectInputStream ois = null;
        try {
            ois = new ObjectInputStream(in);
            res = (SnorocketReasoner)ois.readObject();
        } catch(Exception e) {
            log.error("Problem loading reasoner." + e);
            throw new RuntimeException(e);
        } finally {
            if(ois != null) {
                try { ois.close(); } catch(Exception e) {}
            }
        }

        Context.init(res.no);
        res.no.buildTaxonomy();
        return res;
    }

    /**
     * Creates an instance of Snorocket using the given base ontology.
     *
     * @param ontology The base ontology to classify.
     */
    public SnorocketReasoner() {
        factory = new CoreFactory();
        no = new NormalisedOntology(factory);
    }

    @Override
    public void prune() {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    @Override
    public Ontology getClassifiedOntology() {
        // Check ontology is classified
        if(!isClassified) classify();

        log.info("Building taxonomy");
        no.buildTaxonomy();
        Map<String, Node> taxonomy = no.getTaxonomy();
        Set<Node> affectedNodes = no.getAffectedNodes();

        return new Ontology(null, null, null, taxonomy, affectedNodes);
    }

    @Override
    public Ontology getClassifiedOntology(Ontology ont) {
        // Check ontology is classified
        if(!isClassified) classify();

        log.info("Building taxonomy");
        no.buildTaxonomy();
        Map<String, Node> nodeMap = no.getTaxonomy();
        Set<Node> affectedNodes = no.getAffectedNodes();

        ont.setNodeMap(nodeMap);
        ont.setAffectedNodes(affectedNodes);

        return ont;
    }

    /**
     * Ideally we'd return some kind of normal form axioms here.  However, in
     * the presence of GCIs this is not well defined (as far as I know -
     * Michael).
     * <p>
     * Instead, we will return stated form axioms for Sufficient conditions (
     * i.e. for INamedConcept on the RHS), and SNOMED CT DNF-based axioms for
     * Necessary conditions. The former is just a filter over the stated axioms,
     * the latter requires looking at the Taxonomy and inferred relationships.
     * <p>
     * Note that there will be <i>virtual</i> INamedConcepts that need to be
     * skipped/expanded and redundant IExistentials that need to be filtered.
     *
     * @return
     */
    public Collection<Axiom> getInferredAxioms() {
        final Collection<Axiom> inferred = new HashSet<Axiom>();

        if(!isClassified) classify();

        if (!no.isTaxonomyComputed()) {
            log.info("Building taxonomy");
            no.buildTaxonomy();
        }

        final Map<String, Node> taxonomy = no.getTaxonomy();
        final IConceptMap<Context> contextIndex = no.getContextIndex();
        final IntIterator itr = contextIndex.keyIterator();
        while (itr.hasNext()) {
            final int key = itr.next();
            final String id = factory.lookupConceptId(key).toString();

            if (factory.isVirtualConcept(key) || NamedConcept.BOTTOM == id) {
                continue;
            }

            Concept rhs = getNecessary(contextIndex, taxonomy, key);

            final Concept lhs = new NamedConcept(factory.lookupConceptId(key).toString());
            if (!lhs.equals(rhs) && !rhs.equals(NamedConcept.TOP)) { // skip trivial axioms
                inferred.add(new ConceptInclusion(lhs, rhs));
            }
        }

        return inferred;
    }

    protected Concept getNecessary(IConceptMap<Context> contextIndex, Map<String, Node> taxonomy, int key) {
        final Object id = factory.lookupConceptId(key);
        final List<Concept> result = new ArrayList<Concept>();

        final Node node = taxonomy.get(id);
        if (node != null) {
            for (final Node parent: node.getParents()) {
                final String parentId = parent.getEquivalentConcepts().iterator().next();
                if (!NamedConcept.TOP.equals(parentId)) {      // Top is redundant
                    result.add(new NamedConcept(parentId));
                }
            }
            // Look for Datatype concepts
            final IntIterator ancestorItr = contextIndex.get(key).getS().iterator();
            while (ancestorItr.hasNext()) {
                int anc = ancestorItr.next();
                if (factory.isVirtualConcept(anc)) {
                    Object c = factory.lookupConceptId(anc);
                    if (c instanceof au.csiro.snorocket.core.model.Datatype) {
                        au.csiro.snorocket.core.model.Datatype d = (au.csiro.snorocket.core.model.Datatype) c;
                        Feature feature = new NamedFeature(factory.lookupFeatureId(d.getFeature()));
                        Operator operator = Operator.EQUALS;
                        Literal literal;
                        if (d.getLiteral() instanceof FloatLiteral) {
                            literal = new au.csiro.ontology.model.FloatLiteral(((FloatLiteral) d.getLiteral()).getLowerBound());
                        } else if (d.getLiteral() instanceof IntegerLiteral) {
                            literal = new au.csiro.ontology.model.IntegerLiteral(((IntegerLiteral) d.getLiteral()).getLowerBound());
                        } else if (d.getLiteral() instanceof StringLiteral) {
                            literal = new au.csiro.ontology.model.StringLiteral(((StringLiteral) d.getLiteral()).getValue());
                        } else {
                            throw new UnsupportedOperationException("Literals of type " + d.getLiteral().getClass().getName() + " not yet supported");
                        }
                        result.add(new Datatype(feature, operator, literal));
                    }
                }
            }
        } else if (id instanceof au.csiro.snorocket.core.model.Conjunction) {
            // In this case, we have a result of normalisation so we reach inside and grab out the parents
            for (AbstractConcept conjunct: ((au.csiro.snorocket.core.model.Conjunction) id).getConcepts()) {
                if (conjunct instanceof au.csiro.snorocket.core.model.Concept) {
                    final int conjunctInt = ((au.csiro.snorocket.core.model.Concept) conjunct).hashCode();
                    final String conjunctId = factory.lookupConceptId(conjunctInt).toString();
                    result.add(new NamedConcept(conjunctId));
                }
            }
            throw new IllegalStateException("This else should never be reached since we've already filtered virtual concepts");
        } else {
            throw new IllegalStateException("This else should never be reached since every key should have a node");
        }

        final Context ctx = contextIndex.get(key);
        CR succ = ctx.getSucc();
        for (int roleId: succ.getRoles()) {
            NamedRole role = new NamedRole(factory.lookupRoleId(roleId).toString());
            IConceptSet values = getLeaves(succ.lookupConcept(roleId));
            for (IntIterator itr2 = values.iterator(); itr2.hasNext(); ) {
                int valueInt = itr2.next();
                if (!factory.isVirtualConcept(valueInt)) {
                    final String valueId = factory.lookupConceptId(valueInt).toString();
                    final Existential x = new Existential(role, new NamedConcept(valueId));
                    result.add(x);
                } else {
                    final Concept valueConcept = getNecessary(contextIndex, taxonomy, valueInt);
                    final Existential x = new Existential(role, Builder.build(no, valueConcept));
                    result.add(x);
                }
            }
        }
        //        System.err.println("gN: " + id + "\t" + factory.isVirtualConcept(key) + "\t" + result);

        if (result.size() == 0) {
            return NamedConcept.TOP_CONCEPT;
        } else if (result.size() == 1) {
            return result.get(0);
        } else {
            return new Conjunction(result);
        }
    }

    /**
     * Given a set of concepts, computes the subset such that no member of the subset is subsumed by another member.
     *
     * result = {c | c in bs and not c' in b such that c' [ c}
     *
     * @param concepts set of subsumptions to filter
     * @return
     */
    private IConceptSet getLeaves(final IConceptSet concepts) {
        final IConceptSet leafBs = IConceptSet.FACTORY.createConceptSet(concepts);
        final IConceptSet set = IConceptSet.FACTORY.createConceptSet(leafBs);

        for (final IntIterator bItr = set.iterator(); bItr.hasNext(); ) {
            final int b = bItr.next();

            final IConceptSet ancestors = IConceptSet.FACTORY.createConceptSet(getAncestors(no, b));
            ancestors.remove(b);
            leafBs.removeAll(ancestors);
        }
        return leafBs;
    }

    private static IConceptSet getAncestors(NormalisedOntology no, int conceptInt) {
        return no.getContextIndex().get(conceptInt).getS();
    }

    final static class Builder {
        final private NormalisedOntology no;
        final private IFactory factory;
        final private Map<Integer, RoleSet> rc;

        final private Set<Existential> items = new HashSet<Existential>();

        private Builder(NormalisedOntology no) {
            this.no = no;
            this.factory = no.factory;
            this.rc = no.getRoleClosureCache();
        }

        static Concept build(NormalisedOntology no, Concept... concepts) {
            final List<Concept> list = new ArrayList<Concept>();
            final Builder b = new Builder(no);

            for (final Concept member: concepts) {
                if (member instanceof Existential) {
                    final Existential existential = (Existential) member;

                    b.build((NamedRole) existential.getRole(), build(no, existential.getConcept()));
                } else {
                    list.add(buildOne(no, member));
                }
            }

            list.addAll(b.get());

            if (list.size() == 1) {
                return list.get(0);
            } else {
                return new Conjunction(list);
            }
        }

        private static Concept buildOne(NormalisedOntology no, Concept concept) {
System.err.println("buildOne: " + concept);
            if (concept instanceof Existential) {
                final Existential existential = (Existential) concept;

                return new Existential(existential.getRole(), buildOne(no, existential.getConcept()));
            } else if (concept instanceof Conjunction) {
                return build(no, ((Conjunction) concept).getConcepts());
            } else if (concept instanceof NamedConcept) {
System.err.println("NC: " + concept);
                return concept;
            } else if (concept instanceof Datatype) {
System.err.println("DT: " + concept);
                return concept;
            } else {
                throw new RuntimeException("Unexpected type: " + concept);
            }
        }

        /**
         * Two cases to handle:<ol>
         * <li> We are trying to add something that is redundant
         * <li> We are trying to add something that makes an already-added thing redundant
         * </ol>
         */
        private void build(NamedRole role, Concept concept) {
System.err.println("build: " + concept);
            if (!(concept instanceof NamedConcept)) {
                log.info("WARNING: pass through of complex value: " + concept);
                doAdd(role, concept);
                return;
            }
            if (log.isTraceEnabled()) log.trace("check for subsumption: " + role + "." + concept);

            final int cInt = factory.getConcept(((NamedConcept) concept).getId());
            final IConceptSet cAncestorSet = getAncestors(no, cInt);
            final int rInt = factory.getRole(role.getId());
            final RoleSet rSet = rc.get(rInt);

            final List<Existential> remove = new ArrayList<Existential>();
            boolean subsumed = false;

            for (Existential candidate: items) {
                final Concept value = candidate.getConcept();
                if (!(value instanceof NamedConcept)) {
                    log.warn("WARNING: pass through of nested complex value: " + value);
                    continue;
                }

                final int dInt = factory.getConcept(((NamedConcept) value).getId());
                final IConceptSet dAncestorSet = getAncestors(no, dInt);
                final int sInt = factory.getRole(((NamedRole) candidate.getRole()).getId());
                final RoleSet sSet = rc.get(sInt);

                if (rInt == sInt && cInt == dInt) {
                    subsumed = true;
                } else {
                    if (rSet.contains(sInt)) {
                        if (cAncestorSet.contains(dInt)) {
                            remove.add(candidate);
                            if (log.isTraceEnabled()) log.trace("\tremove " + candidate);
                        }
                    }

                    if (sSet.contains(rInt)) {
                        if (dAncestorSet.contains(cInt)) {
                            subsumed = true;
                            if (log.isTraceEnabled()) log.trace("\tsubsumed");
                        }
                    }
                }
            }

            if (subsumed && !remove.isEmpty()) {
                throw new AssertionError("Should not have items to remove if item to be added is already subsumed.");
            }

            items.removeAll(remove);
            if (!subsumed) {
                doAdd(role, concept);
            }
        }

        private Collection<Existential> get() {
            return items;
        }

        private void doAdd(NamedRole role, Concept concept) {
            items.add(new Existential(role, concept));
        }

    }

    @Override
    public void save(OutputStream out) {
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(out);
            oos.writeObject(this);
            oos.flush();
        } catch(Exception e) {
            log.error("Problem saving reasoner.", e);
            throw new RuntimeException(e);
        } finally {
            if(oos != null) {
                try { oos.close(); } catch(Exception e) {}
            }
        }
    }

    @Override
    public boolean isClassified() {
        return isClassified;
    }

    @Override
    public void loadAxioms(Set<Axiom> axioms) {
        if(!isClassified) {
            no.loadAxioms(axioms);
        } else {
            no.loadIncremental(axioms);
        }
    }

    @Override
    public void loadAxioms(Iterator<Axiom> axioms) {
        Set<Axiom> axiomSet = new HashSet<Axiom>();
        while(axioms.hasNext()) {
            Axiom axiom = axioms.next();
            if(axiom == null) continue;
            axiomSet.add(axiom);
            if(axiomSet.size() == BUFFER_SIZE) {
                loadAxioms(axiomSet);
                axiomSet.clear();
            }
        }

        if(!axiomSet.isEmpty()) {
            loadAxioms(axiomSet);
        }
    }

    @Override
    public void loadAxioms(Ontology ont) {
        loadAxioms(new HashSet<Axiom>(ont.getStatedAxioms()));
    }

    @Override
    public IReasoner classify() {
        if(!isClassified) {
            no.classify();
            isClassified = true;
        } else {
            no.classifyIncremental();
        }
        return this;
    }

}
