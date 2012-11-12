/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.protege;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AddAxiom;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLException;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyChange;
import org.semanticweb.owlapi.model.OWLOntologyChangeListener;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.RemoveAxiom;
import org.semanticweb.owlapi.reasoner.AxiomNotInProfileException;
import org.semanticweb.owlapi.reasoner.BufferingMode;
import org.semanticweb.owlapi.reasoner.ClassExpressionNotInProfileException;
import org.semanticweb.owlapi.reasoner.FreshEntitiesException;
import org.semanticweb.owlapi.reasoner.FreshEntityPolicy;
import org.semanticweb.owlapi.reasoner.InconsistentOntologyException;
import org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerConfiguration;
import org.semanticweb.owlapi.reasoner.ReasonerInternalException;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;
import org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNode;
import org.semanticweb.owlapi.reasoner.impl.OWLClassNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLNamedIndividualNodeSet;
import org.semanticweb.owlapi.reasoner.impl.OWLObjectPropertyNode;
import org.semanticweb.owlapi.util.Version;

import au.csiro.ontology.IOntology;
import au.csiro.ontology.IOntology.AxiomForm;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IProgressMonitor;
import au.csiro.ontology.importer.owl.OWLImporter;
import au.csiro.snorocket.core.ClassNode;
import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.PostProcessedData;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;

/**
 * Main classifier class. Communicates with external modules using the
 * org.semanticweb.owlapi classes to represent ontologies. Implements the
 * {@link OWLReasoner} interface. This allows using the reasoner as a Protege
 * plugin or as a standalone application.
 * 
 * @author Alejandro Metke
 * 
 */
public class SnorocketOWLReasoner implements OWLReasoner {

    // SnorocketOWLReasoner name
    static final String REASONER_NAME = "Snorocket";

    // SnorocketOWLReasoner version
    private static final Version REASONER_VERSION = new Version(2, 0, 0, 0);

    // OWL-API ontology manager
    private final OWLOntologyManager manager;

    // OWL-API data factory
    private final OWLDataFactory owlFactory;

    // The OWL-API ontology
    private final OWLOntology owlOntology;

    // Configuration options
    private final OWLReasonerConfiguration config;

    // The progress monitor
    private final IProgressMonitor monitor;

    // Indicates if the reasoner should work in buffering or non-buffering mode
    private final boolean buffering;

    // The reasoner's factory
    private IFactory<String> factory = new Factory<>();

    // The core reasoner
    private NormalisedOntology<String> reasoner = new NormalisedOntology<>(factory);

    // The taxonomy built after the saturation process
    private PostProcessedData<String> ppd = new PostProcessedData<>(factory);

    // List of problems found when doing the ontology classification
    private final List<String> problems = new ArrayList<String>();

    // List of raw changes to the ontology
    private final List<OWLOntologyChange> rawChanges = new ArrayList<OWLOntologyChange>();

    /**
     * 
     * @param ont
     * @param config
     * @param buffering
     */
    public SnorocketOWLReasoner(OWLOntology ont,
            OWLReasonerConfiguration config, boolean buffering) {
        this.owlOntology = ont;
        this.manager = ont.getOWLOntologyManager();
        this.owlFactory = manager.getOWLDataFactory();
        manager.addOntologyChangeListener(ontologyChangeListener);
        this.config = config;
        this.monitor = (config != null) ? 
                new ProgressMonitorWrapper(config.getProgressMonitor()) : 
                new ProgressMonitorWrapper(new NullReasonerProgressMonitor());
        this.buffering = buffering;
    }

    /**
     * Clears the current state and starts the classification process.
     */
    public void synchronise() {
        reset();
        loadAxioms();
        reasoner.classify();
        final IConceptMap<IConceptSet> s = reasoner.getSubsumptions();
        ppd.computeDag(s, monitor);
    }

    /**
     * Loads the axioms from an OWL ontology and creates the initial axiom
     * index.
     * 
     * @param ont
     *            The OWL ontology.
     * @throws OWLOntologyCreationException
     */
    void loadAxioms() {
        OWLImporter oi = new OWLImporter(owlOntology);
        Map<String, Map<String, IOntology<String>>> res = 
                oi.getOntologyVersions(monitor);
        for(String key : res.keySet()) {
            Map<String, IOntology<String>> map = res.get(key);
            for(String ikey : map.keySet()) {
                IOntology<String> o = map.get(ikey);
                Collection<IAxiom> axioms = o.getAxioms(AxiomForm.STATED);
                reasoner.loadAxioms(new HashSet<IAxiom>(axioms));
            }
        }
        manager.removeOntology(owlOntology);
    }

    /**
     * Clears all current axioms and derivations.
     */
    private void reset() {
        monitor.taskStarted("Clearing state");
        problems.clear();
        factory = new Factory<>();
        reasoner = new NormalisedOntology<>(factory);
        ppd = new PostProcessedData<>(factory);
        monitor.taskEnded();
    }

    /**
     * Returns the {@link OWLClass} for the corresponding internal int
     * representation.
     * 
     * @param c
     * @return
     */
    private OWLClass getOWLClass(int id) {
        // Special cases top and bottom
        if(id == IFactory.TOP_CONCEPT) {
            return owlFactory.getOWLThing();
        } else if(id == IFactory.BOTTOM_CONCEPT) {
            return owlFactory.getOWLNothing();
        } else {
            String iri = factory.lookupConceptId(id);
            return owlFactory.getOWLClass(IRI.create(iri));
        }
    }

    /**
     * Returns the {@link OWLObjectProperty} for the corresponding int
     * representation.
     * 
     * @param r
     * @return
     */
    @SuppressWarnings("unused")
    private OWLObjectProperty getOWLObjectProperty(int r) {
        String iri = factory.lookupRoleId(r);
        return owlFactory.getOWLObjectProperty(IRI.create(iri));
    }

    /**
     * Returns the {@link OWLDataProperty} for the corresponding internal byte
     * array representation.
     * 
     * @param r
     * @return
     */
    @SuppressWarnings("unused")
    private OWLDataProperty getOWLDataProperty(int f) {
        String iri = factory.lookupFeatureId(f);
        return owlFactory.getOWLDataProperty(IRI.create(iri));
    }

    // //////////////////////////////////////////////////////////////////////////
    // OWLReasoner methods
    // //////////////////////////////////////////////////////////////////////////

    @Override
    public String getReasonerName() {
        return REASONER_NAME;
    }

    @Override
    public Version getReasonerVersion() {
        return REASONER_VERSION;
    }

    @Override
    public BufferingMode getBufferingMode() {
        return buffering ? BufferingMode.BUFFERING
                : BufferingMode.NON_BUFFERING;
    }

    /**
     * Classifies the ontology incrementally if no import changes have occurred.
     */
    @Override
    public void flush() {
        if (rawChanges.isEmpty()) {
            return;
        }

        boolean hasImportChange = false;
        List<OWLAxiom> newAxioms = new ArrayList<OWLAxiom>();

        for (OWLOntologyChange change : rawChanges) {
            if (change.isImportChange()) {
                hasImportChange = true;
                break;
            } else if (change.isAxiomChange()) {
                OWLAxiom axiom = change.getAxiom();
                newAxioms.add(axiom);
            } else {
                // Should never happen
                assert (false);
            }
        }

        if (hasImportChange) {
            synchronise();
        } else {
            OWLImporter oi = new OWLImporter(newAxioms);
            Map<String, Map<String, IOntology<String>>> res = oi.getOntologyVersions(monitor);
            for(String key : res.keySet()) {
                Map<String, IOntology<String>> map = res.get(key);
                for(String ikey : map.keySet()) {
                    IOntology<String> o = map.get(ikey);
                    Collection<IAxiom> axioms = o.getAxioms(AxiomForm.STATED);
                    reasoner.classifyIncremental(new HashSet<IAxiom>(axioms));
                }
            }

            final IConceptMap<IConceptSet> n = reasoner.getNewSubsumptions();
            final IConceptMap<IConceptSet> a = reasoner
                    .getAffectedSubsumptions();
            ppd.computeDagIncremental(n, a, monitor);
        }

        rawChanges.clear();
    }

    @Override
    public List<OWLOntologyChange> getPendingChanges() {
        return rawChanges;
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomAdditions() {
        Set<OWLAxiom> added = new HashSet<OWLAxiom>();
        for (OWLOntologyChange change : rawChanges) {
            if (change instanceof AddAxiom) {
                added.add(change.getAxiom());
            }
        }
        return added;
    }

    @Override
    public Set<OWLAxiom> getPendingAxiomRemovals() {
        Set<OWLAxiom> removed = new HashSet<OWLAxiom>();
        for (OWLOntologyChange change : rawChanges) {
            if (change instanceof RemoveAxiom) {
                removed.add(change.getAxiom());
            }
        }
        return removed;
    }

    @Override
    public OWLOntology getRootOntology() {
        return owlOntology;
    }

    @Override
    public void interrupt() {
        // TODO: implement
    }

    @Override
    public void precomputeInferences(InferenceType... inferenceTypes)
            throws ReasonerInterruptedException, TimeOutException,
            InconsistentOntologyException {
        for (InferenceType inferenceType : inferenceTypes) {
            if (inferenceType.equals(InferenceType.CLASS_HIERARCHY)) {
                synchronise();
            }
        }
    }

    @Override
    public boolean isPrecomputed(InferenceType inferenceType) {
        if (inferenceType.equals(InferenceType.CLASS_HIERARCHY)) {
            if (ppd != null) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return Collections.singleton(InferenceType.CLASS_HIERARCHY);
    }

    /**
     * TODO: deal with ReasonerInterruptedException and TimeOutException. TODO:
     * is this version of Snorocket using the problem list to report
     * inconsistencies?
     */
    @Override
    public boolean isConsistent() throws ReasonerInterruptedException,
            TimeOutException {
        if (ppd != null) {
            if (problems.isEmpty()) {
                return true;
            } else {
                return false;
            }
        } else {
            synchronise();
            if (problems.isEmpty()) {
                return true;
            } else {
                return false;
            }
        }
    }

    @Override
    public boolean isSatisfiable(OWLClassExpression classExpression)
            throws ReasonerInterruptedException, TimeOutException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            InconsistentOntologyException {
        if (classExpression.isAnonymous()) {
            return true;
        } else {
            // If the node that contains OWLNothing contains this OWLClass then
            // it is not satisfiable
            int c = factory.getConcept(classExpression.asOWLClass()
                    .toStringID());
            ClassNode bottom = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
            return !bottom.getEquivalentConcepts().contains(c);

        }
    }

    @Override
    public Node<OWLClass> getUnsatisfiableClasses()
            throws ReasonerInterruptedException, TimeOutException,
            InconsistentOntologyException {
        ClassNode bottom = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        return nodeToOwlClassNode(bottom);
    }

    @Override
    public boolean isEntailed(OWLAxiom axiom)
            throws ReasonerInterruptedException,
            UnsupportedEntailmentTypeException, TimeOutException,
            AxiomNotInProfileException, FreshEntitiesException,
            InconsistentOntologyException {
        throw new UnsupportedEntailmentTypeException(axiom);
    }

    @Override
    public boolean isEntailed(Set<? extends OWLAxiom> axioms)
            throws ReasonerInterruptedException,
            UnsupportedEntailmentTypeException, TimeOutException,
            AxiomNotInProfileException, FreshEntitiesException,
            InconsistentOntologyException {
        throw new UnsupportedEntailmentTypeException(axioms.iterator().next());
    }

    @Override
    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return false;
    }

    @Override
    public Node<OWLClass> getTopClassNode() {
        ClassNode top = ppd.getEquivalents(IFactory.TOP_CONCEPT);
        return nodeToOwlClassNode(top);
    }

    @Override
    public Node<OWLClass> getBottomClassNode() {
        ClassNode bottom = ppd.getEquivalents(IFactory.BOTTOM_CONCEPT);
        return nodeToOwlClassNode(bottom);
    }

    @Override
    public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
            throws ReasonerInterruptedException, TimeOutException,
            FreshEntitiesException, InconsistentOntologyException,
            ClassExpressionNotInProfileException {
        if (!problems.isEmpty()) {
            throw new InconsistentOntologyException();
        }
        if (ce.isAnonymous()) {
            throw new ReasonerInternalException("Expected a named class, got "
                    + ce);
        }
        OWLClass c = ce.asOWLClass();

        // Get the corresponding concept in the internal representation
        int cc = factory.getConcept(c.toStringID());

        // Use the post processed data to answer the query
        ClassNode n = ppd.getEquivalents(cc);
        Set<ClassNode> children = n.getChildren();

        if (direct) {
            // Transform the response back into owlapi objects
            return nodesToOwlClassNodeSet(children);
        } else {
            Set<ClassNode> res = new HashSet<>();
            Queue<Set<ClassNode>> todo = new LinkedList<>();
            todo.add(children);
            while (!todo.isEmpty()) {
                Set<ClassNode> items = todo.remove();
                res.addAll(items);
                for (ClassNode item : items) {
                    Set<ClassNode> cn = item.getChildren();
                    if (!cn.isEmpty())
                        todo.add(cn);
                }
            }

            return nodesToOwlClassNodeSet(res);
        }
    }

    @Override
    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
            boolean direct) throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        if (!problems.isEmpty()) {
            throw new InconsistentOntologyException();
        }
        if (ce.isAnonymous()) {
            throw new ReasonerInternalException("Expected a named class, got "
                    + ce);
        }
        
        OWLClass c = ce.asOWLClass();
        
        // Get the corresponding concept in the internal representation
        int cc = factory.getConcept(c.toStringID());

        // Use the post processed data to answer the query

        ClassNode n = ppd.getEquivalents(cc);
        if (n == null) {
            return new OWLClassNodeSet();
        }
        Set<ClassNode> parents = n.getParents();

        if (direct) {
            // Transform the response back into owlapi objects
            return nodesToOwlClassNodeSet(parents);
        } else {
            Set<ClassNode> res = new HashSet<>();
            Queue<Set<ClassNode>> todo = new LinkedList<>();
            todo.add(parents);
            while (!todo.isEmpty()) {
                Set<ClassNode> items = todo.remove();
                res.addAll(items);
                for (ClassNode item : items) {
                    Set<ClassNode> cn = item.getParents();
                    if (!cn.isEmpty())
                        todo.add(cn);
                }
            }

            return nodesToOwlClassNodeSet(res);
        }

    }

    @Override
    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
            throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        if (!problems.isEmpty()) {
            throw new InconsistentOntologyException();
        }
        if (ce.isAnonymous()) {
            throw new ReasonerInternalException("Expected a named class, got "
                    + ce);
        }
        OWLClass c = ce.asOWLClass();
        // Get the corresponding concept in the internal representation
        int cc = factory.getConcept(c.toStringID());

        // Use the post processed data to answer the query
        ClassNode n = ppd.getEquivalents(cc);
        return nodeToOwlClassNode(n);
    }

    @Override
    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
            throws ReasonerInterruptedException, TimeOutException,
            FreshEntitiesException, InconsistentOntologyException {
        throw new ReasonerInternalException(
                "getDisjointClasses not implemented");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        return new OWLObjectPropertyNode(owlFactory.getOWLTopObjectProperty());
    }

    @Override
    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        return new OWLObjectPropertyNode(
                owlFactory.getOWLBottomObjectProperty());
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSubObjectProperties not implemented");
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSuperObjectProperties not implemented");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        return new OWLObjectPropertyNode();
    }

    @Override
    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getDisjointObjectProperties not implemented");
    }

    @Override
    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getInverseObjectProperties not implemented");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyDomains(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyDomains not implemented");
    }

    @Override
    public NodeSet<OWLClass> getObjectPropertyRanges(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyRanges not implemented");
    }

    @Override
    public Node<OWLDataProperty> getTopDataPropertyNode() {
        throw new ReasonerInternalException(
                "getTopDataPropertyNode not implemented");
    }

    @Override
    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        throw new ReasonerInternalException(
                "getBottomDataPropertyNode not implemented");
    }

    @Override
    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getSubDataProperties not implemented");
    }

    @Override
    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getSuperDataProperties not implemented");
    }

    @Override
    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getEquivalentDataProperties not implemented");
    }

    @Override
    public NodeSet<OWLDataProperty> getDisjointDataProperties(
            OWLDataPropertyExpression pe) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDisjointDataProperties not implemented");
    }

    @Override
    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDataPropertyDomains not implemented");
    }

    @Override
    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException("getTypes not implemented");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
            boolean direct) throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        return new OWLNamedIndividualNodeSet();
    }

    @Override
    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
            OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyValues not implemented");
    }

    @Override
    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
            OWLDataProperty pe) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        return Collections.emptySet();
    }

    @Override
    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSameIndividuals not implemented");
    }

    @Override
    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
            OWLNamedIndividual ind) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDifferentIndividuals not implemented");
    }

    @Override
    public long getTimeOut() {
        return (config != null) ? config.getTimeOut() : 0;
    }

    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return (config != null) ? config.getFreshEntityPolicy()
                : FreshEntityPolicy.DISALLOW;
    }

    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        return (config != null) ? config.getIndividualNodeSetPolicy()
                : IndividualNodeSetPolicy.BY_NAME;
    }

    @Override
    public void dispose() {
        owlOntology.getOWLOntologyManager().removeOntologyChangeListener(
                ontologyChangeListener);
        rawChanges.clear();
        reset();
    }

    /**
     * Handles raw changes in the ontology.
     * 
     * @param changes
     */
    private synchronized void handleRawOntologyChanges(
            List<? extends OWLOntologyChange> changes) {
        rawChanges.addAll(changes);
        if (!buffering) {
            flush();
        }
    }

    /**
     * Change listener used to handle changes in the ontology.
     */
    private OWLOntologyChangeListener ontologyChangeListener = new OWLOntologyChangeListener() {
        public void ontologiesChanged(List<? extends OWLOntologyChange> changes)
                throws OWLException {
            handleRawOntologyChanges(changes);
        }
    };

    /**
     * Transforms a {@link ClassNode} into a {@link Node} of {@link OWLClass}es.
     * 
     * @param n
     * @return
     */
    private Node<OWLClass> nodeToOwlClassNode(ClassNode n) {
        org.semanticweb.owlapi.reasoner.Node<OWLClass> node = new OWLClassNode();

        for (IntIterator it = n.getEquivalentConcepts().iterator(); it
                .hasNext();) {
            int eq = it.next();
            node.getEntities().add(getOWLClass(eq));
        }
        return node;
    }

    /**
     * Transforms a set of {@link ClassNode}s into a {@link NodeSet}.
     * 
     * @param nodes
     * @return
     */
    private NodeSet<OWLClass> nodesToOwlClassNodeSet(Set<ClassNode> nodes) {
        Set<org.semanticweb.owlapi.reasoner.Node<OWLClass>> temp = new HashSet<>();
        for (ClassNode n : nodes) {
            temp.add(nodeToOwlClassNode(n));
        }
        return new OWLClassNodeSet(temp);
    }

    // //////////////////////////////////////////////////////////////////////////
    // Main method to use stand alone
    // //////////////////////////////////////////////////////////////////////////

    /**
     * @param args
     */
    public static void main(String[] args) {
        String filename = args[0];
        File f = new File(filename);
        IRI iri = IRI.create(f.getAbsoluteFile());

        try {
            long start = System.currentTimeMillis();
            System.out.println("Loading OWL ontology");
            OWLOntologyManager mgr = OWLManager.createOWLOntologyManager();
            OWLOntology ont = mgr.loadOntologyFromOntologyDocument(iri);
            SnorocketOWLReasoner c = new SnorocketOWLReasoner(ont, null, false);
            System.out.println("Took " + (System.currentTimeMillis() - start)
                    + "ms");

            start = System.currentTimeMillis();
            System.out.println("Importing axioms");
            c.loadAxioms();
            System.out.println("Took " + (System.currentTimeMillis() - start)
                    + "ms");

            start = System.currentTimeMillis();
            System.out.println("Classifying");
            // c.saturateConcepts();
            System.out.println("Took " + (System.currentTimeMillis() - start)
                    + "ms");
            start = System.currentTimeMillis();
            System.out.println("Building taxonomy");
            // c.calculateTransitiveReduction();
            System.out.println("Took " + (System.currentTimeMillis() - start)
                    + "ms");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

}
