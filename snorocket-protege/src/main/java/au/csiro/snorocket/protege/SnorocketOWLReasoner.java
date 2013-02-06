/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.protege;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
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
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IProgressMonitor;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.importer.owl.OWLImporter;
import au.csiro.ontology.model.Concept;
import au.csiro.snorocket.core.ClassNode;
import au.csiro.snorocket.core.SnorocketReasoner;

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
    private Version REASONER_VERSION;

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

    // List of problems found when doing the ontology classification
    private final List<String> problems = new ArrayList<String>();

    // List of raw changes to the ontology
    private final List<OWLOntologyChange> rawChanges = new ArrayList<OWLOntologyChange>();
    
    // The reasoner
    private IReasoner<String> reasoner = new SnorocketReasoner<>();
    
    // The taxonomy
    private IOntology<String> taxonomy = null;

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
    
    ////////////////////////////////////////////////////////////////////////////
    // Private methods
    ////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the {@link OWLClass} for the corresponding internal object
     * representation.
     * 
     * @param c
     * @return
     */
    private OWLClass getOWLClass(Object id) {
        // Special cases top and bottom
        if(id == Concept.TOP) {
            return owlFactory.getOWLThing();
        } else if(id == Concept.BOTTOM) {
            return owlFactory.getOWLNothing();
        } else {
            String iri = (String)id;
            return owlFactory.getOWLClass(IRI.create(iri));
        }
    }
    
    /**
     * Returns the identifier for an {@link OWLClass}. If the owl class is 
     * anonymous then it returns null.
     * 
     * @param oc
     * @return
     */
    private Object getId(OWLClass oc) {
        if(oc.isAnonymous()) return null;
        
        String id = oc.toStringID();
        
        if(id.equals("<"+OWLImporter.THING_IRI+">") || 
                id.equals(OWLImporter.THING_IRI)) {
            return Concept.TOP;
        } else if(id.equals("<"+OWLImporter.NOTHING_IRI+">") || 
                id.equals(OWLImporter.NOTHING_IRI)) {
            return Concept.BOTTOM;
        } else {
            return id;
        }
    }
    
    /**
     * Returns the {@link au.csiro.ontology.Node} for an {@link OWLClass}.
     * If the owl class is anonymous then it throws a {@link RuntimeException}.
     * 
     * @param oc
     * @return
     */
    private au.csiro.ontology.Node<String> getNode(OWLClass oc) {
        final Object id = getId(oc);
        
        final au.csiro.ontology.Node<String> n;
        
        if(id instanceof String) {
            n = getTaxonomy().getNode((String)id);
        } else if(id == Concept.TOP) {
            n = getTaxonomy().getTopNode();
        } else if(id == Concept.BOTTOM) {
            n = getTaxonomy().getBottomNode();
        } else {
            throw new RuntimeException("Unexpected id "+id);
        }

        return n;
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
    private Node<OWLClass> nodeToOwlClassNode(au.csiro.ontology.Node<String> n) {
        assert n != null;
        
        Node<OWLClass> node = new OWLClassNode();
        
        if(n == null) return node;
        
        for(Object eq : n.getEquivalentConcepts()) {
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
    private NodeSet<OWLClass> nodesToOwlClassNodeSet(Set<au.csiro.ontology.Node<String>> nodes) {
        Set<Node<OWLClass>> temp = new HashSet<>();
        for (au.csiro.ontology.Node<String> n : nodes) {
            temp.add(nodeToOwlClassNode(n));
        }
        return new OWLClassNodeSet(temp);
    }
    
    /**
     * Performs a full classification on the current ontology.
     */
    private void classify() {
        
        // Transform the axioms into the canonical model
        Set<IAxiom> canAxioms = new HashSet<>();
        OWLImporter oi = new OWLImporter(owlOntology);
        Map<String, Map<String, IOntology<String>>> res = 
                oi.getOntologyVersions(monitor);
        for(String key : res.keySet()) {
            Map<String, IOntology<String>> map = res.get(key);
            for(String ikey : map.keySet()) {
                IOntology<String> o = map.get(ikey);
                canAxioms.addAll(o.getStatedAxioms());
            }
        }
        
        // Classify
        monitor.taskStarted("Classifying");
        monitor.taskBusy();
        reasoner = reasoner.classify(canAxioms);
        monitor.taskEnded();
        monitor.taskStarted("Building taxonomy");
        monitor.taskBusy();
        taxonomy = reasoner.getClassifiedOntology();
        monitor.taskEnded();
    }
    
    /**
     * Returns the taxonomy.
     * 
     * @return
     */
    private IOntology<String> getTaxonomy() {
        if(taxonomy == null) {
            taxonomy = reasoner.getClassifiedOntology();
        }
        
        return taxonomy;
    }

    ////////////////////////////////////////////////////////////////////////////
    // OWLReasoner methods
    ////////////////////////////////////////////////////////////////////////////

    @Override
    public String getReasonerName() {
        return REASONER_NAME;
    }

    @Override
    public Version getReasonerVersion() {
        if (null == REASONER_VERSION) {
            // load properties from plugin.properties
            Properties p = new Properties();
            try {
                p.load(getClass().getResourceAsStream("/plugin.properties"));
                String[] versions = p.getProperty("plugin.version").split("[-.]");         // X.Y.Z
                int major = Integer.parseInt(versions[0]);
                int minor = Integer.parseInt(versions[1]);
                int patch = Integer.parseInt(versions[2]);
                int build = (int) (System.currentTimeMillis() / 1000);          // FIXME
                REASONER_VERSION = new Version(major, minor, patch, build);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return REASONER_VERSION;
    }

    @Override
    public BufferingMode getBufferingMode() {
        return buffering ? BufferingMode.BUFFERING
                : BufferingMode.NON_BUFFERING;
    }

    /**
     * Classifies the ontology incrementally if no import changes have occurred.
     * 
     * Flushes any changes stored in the buffer, which causes the reasoner to 
     * take into consideration the changes the current root ontology specified 
     * by the changes. If the reasoner buffering mode is
     * {@link org.semanticweb.owlapi.reasoner.BufferingMode#NON_BUFFERING}
     * then this method will have no effect.
     */
    @Override
    public void flush() {
        if (rawChanges.isEmpty() || !buffering) {
            return;
        }
        
        // Get the changed axioms
        List<OWLAxiom> newAxioms = new ArrayList<OWLAxiom>();
        for (OWLOntologyChange change : rawChanges) {
            OWLAxiom axiom = change.getAxiom();
            newAxioms.add(axiom);
        }
        
        // Transform the axioms into the canonical model
        Set<IAxiom> canAxioms = new HashSet<>();
        OWLImporter oi = new OWLImporter(newAxioms);
        Map<String, Map<String, IOntology<String>>> res = oi.getOntologyVersions(monitor);
        for(String key : res.keySet()) {
            Map<String, IOntology<String>> map = res.get(key);
            for(String ikey : map.keySet()) {
                IOntology<String> o = map.get(ikey);
                canAxioms.addAll(o.getStatedAxioms());
            }
        }
        
        // Classify
        monitor.taskStarted("Classifying incrementally");
        monitor.taskBusy();
        reasoner = reasoner.classify(canAxioms);
        monitor.taskEnded();
        
        monitor.taskStarted("Calculating taxonomy incrementally");
        monitor.taskBusy();
        taxonomy = reasoner.getClassifiedOntology();
        monitor.taskEnded();
        
        rawChanges.clear();
    }
    
    /**
     * Gets the pending changes which need to be taken into consideration by the
     * reasoner so that it is up to date with the root ontology imports closure.
     * After the {@link #flush()} method is called the set of pending changes
     * will be empty.
     * 
     * @return A set of changes. Note that the changes represent the raw changes
     *         as applied to the imports closure of the root ontology.
     */
    @Override
    public List<OWLOntologyChange> getPendingChanges() {
        return rawChanges;
    }
    
    /**
     * Gets the axioms that as a result of ontology changes need to be added to
     * the reasoner to synchronise it with the root ontology imports closure. If
     * the buffering mode is
     * {@link org.semanticweb.owlapi.reasoner.BufferingMode#NON_BUFFERING} then
     * there will be no pending axiom additions.
     * 
     * @return The set of axioms that need to added to the reasoner to the
     *         reasoner to synchronise it with the root ontology imports
     *         closure.
     */
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
    
    /**
     * Gets the axioms that as a result of ontology changes need to removed to
     * the reasoner to synchronise it with the root ontology imports closure. If
     * the buffering mode is
     * {@link org.semanticweb.owlapi.reasoner.BufferingMode#NON_BUFFERING} then
     * there will be no pending axiom additions.
     * 
     * @return The set of axioms that need to added to the reasoner to the
     *         reasoner to synchronise it with the root ontology imports
     *         closure.
     */
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
    
    /**
     * Gets the "root" ontology that is loaded into this reasoner. The reasoner
     * takes into account the axioms in this ontology and its imports closure,
     * plus the axioms returned by {@link #getPendingAxiomRemovals()}, minus the
     * axioms returned by {@link #getPendingAxiomAdditions()} when reasoning.
     * </p> 
     * Note that the root ontology is set at reasoner creation time and
     * cannot be changed thereafter. Clients that want to add ontologies to and
     * remove ontologies from the reasoner after creation time should create a
     * "dummy" ontology that imports the "real" ontologies and then specify the
     * dummy ontology as the root ontology at reasoner creation time.
     * 
     * @return The root ontology that is loaded into the reasoner.
     */
    @Override
    public OWLOntology getRootOntology() {
        return owlOntology;
    }
    
    /**
     * Asks the reasoner to interrupt what it is currently doing. An
     * ReasonerInterruptedException will be thrown in the thread that invoked
     * the last reasoner operation. The OWL API is not thread safe in general,
     * but it is likely that this method will be called from another thread than
     * the event dispatch thread or the thread in which reasoning takes place.
     * </p> 
     * Note that the reasoner will periodically check for interrupt
     * requests. Asking the reasoner to interrupt the current process does not
     * mean that it will be interrupted immediately. However, clients can expect
     * to be able to interrupt individual consistency checks, satisfiability
     * checks etc.
     */
    @Override
    public void interrupt() {
        // TODO: implement
    }
    
    /**
     * Asks the reasoner to precompute certain types of inferences. Note that it
     * is NOT necessary to call this method before asking any other queries -
     * the reasoner will answer all queries correctly regardless of whether
     * inferences are precomputed or not. For example, if the imports closure of
     * the root ontology entails <code>SubClassOf(A B)</code> then the result of
     * <code>getSubClasses(B)</code> will contain <code>A</code>, regardless of
     * whether
     * <code>precomputeInferences({@link InferenceType#CLASS_HIERARCHY})</code>
     * has been called.
     * <p>
     * If the reasoner does not support the precomputation of a particular type
     * of inference then it will silently ignore the request.
     * 
     * @param inferenceTypes
     *            Suggests a list of the types of inferences that should be
     *            precomputed. If the list is empty then the reasoner will
     *            determine which types of inferences are precomputed. Note that
     *            the order of the list is unimportant - the reasoner will
     *            determine the order in which inferences are computed.
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public void precomputeInferences(InferenceType... inferenceTypes)
            throws ReasonerInterruptedException, TimeOutException,
            InconsistentOntologyException {
        for (InferenceType inferenceType : inferenceTypes) {
            if (inferenceType.equals(InferenceType.CLASS_HIERARCHY)) {
                classify();
            }
        }
    }
    
    /**
     * Determines if a specific set of inferences have been precomputed.
     * @param inferenceType The type of inference to check for.
     * @return <code>true</code> if the specified type of inferences have been 
     * precomputed, otherwise <code>false</code>.
     */
    @Override
    public boolean isPrecomputed(InferenceType inferenceType) {
        if (inferenceType.equals(InferenceType.CLASS_HIERARCHY)) {
            return reasoner.isClassified();
        } else {
            return false;
        }
    }
    
    /**
     * Returns the set of {@link org.semanticweb.owlapi.reasoner.InferenceType}s
     * that are precomputable by reasoner.
     * 
     * @return A set of {@link org.semanticweb.owlapi.reasoner.InferenceType}s
     *         that can be precomputed by this reasoner.
     */
    @Override
    public Set<InferenceType> getPrecomputableInferenceTypes() {
        return Collections.singleton(InferenceType.CLASS_HIERARCHY);
    }

    /**
     * Inconsistent Ontology - this occurs when the axioms in an ontology 
     * contain a contradiction which prevents the ontology from having a model, 
     * e.g., when the ontology asserts that an individual belongs to an 
     * unsatisfiable concept.
     * 
     * Snorocket does not currently support individuals so this method always
     * returns true. TODO: if there is a syntactic issue such as a datatype
     * being declared with a literal value of a different type than the one
     * declared in a restriction, should this method return false?
     * 
     * 
     * Determines if the set of reasoner axioms is consistent. Note that this
     * method will NOT throw an
     * {@link org.semanticweb.owlapi.reasoner.InconsistentOntologyException}
     * even if the root ontology imports closure is inconsistent.
     * 
     * @return <code>true</code> if the imports closure of the root ontology is
     *         consistent, or <code>false</code> if the imports closure of the
     *         root ontology is inconsistent.
     * 
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process).
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.           
     */
    @Override
    public boolean isConsistent() throws ReasonerInterruptedException,
            TimeOutException {
        return true;
    }
    
    /**
     * A convenience method that determines if the specified class expression is
     * satisfiable with respect to the reasoner axioms.
     * 
     * @param classExpression
     *            The class expression
     * @return <code>true</code> if classExpression is satisfiable with respect
     *         to the set of axioms, or <code>false</code> if classExpression is
     *         unsatisfiable with respect to the axioms.
     * 
     * @throws InconsistentOntologyException
     *             if the set of reasoner axioms is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>classExpression</code> is not within the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the classExpression is not contained
     *             within the signature of the set of reasoner axioms.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
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
            Object id = getId(classExpression.asOWLClass());
            au.csiro.ontology.Node<String> bottom = 
                    getTaxonomy().getBottomNode();
            return !bottom.getEquivalentConcepts().contains(id);
        }
    }
    
    /**
     * A convenience method that obtains the classes in the signature of the
     * root ontology that are unsatisfiable.
     * 
     * @return A <code>Node</code> that is the bottom node in the class
     *         hierarchy. This node represents <code>owl:Nothing</code> and
     *         contains <code>owl:Nothing</code> itself plus classes that are
     *         equivalent to <code>owl:Nothing</code>.
     * 
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @throws InconsistentOntologyException
     *             if the set of reasoner axioms is inconsistent
     */
    @Override
    public Node<OWLClass> getUnsatisfiableClasses()
            throws ReasonerInterruptedException, TimeOutException,
            InconsistentOntologyException {
        return nodeToOwlClassNode(getTaxonomy().getBottomNode());
    }
    
    /**
     * A convenience method that determines if the specified axiom is entailed
     * by the set of reasoner axioms.
     * 
     * @param axiom
     *            The axiom
     * @return <code>true</code> if {@code axiom} is entailed by the reasoner
     *         axioms or <code>false</code> if {@code axiom} is not entailed by
     *         the reasoner axioms. <code>true</code> if the set of reasoner
     *         axioms is inconsistent.
     * 
     * @throws FreshEntitiesException
     *             if the signature of the axiom is not contained within the
     *             signature of the imports closure of the root ontology.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @throws UnsupportedEntailmentTypeException
     *             if the reasoner cannot perform a check to see if the
     *             specified axiom is entailed
     * @throws AxiomNotInProfileException
     *             if <code>axiom</code> is not in the profile that is supported
     *             by this reasoner.
     * @throws InconsistentOntologyException
     *             if the set of reasoner axioms is inconsistent
     * @see #isEntailmentCheckingSupported(org.semanticweb.owlapi.model.AxiomType)
     */
    @Override
    public boolean isEntailed(OWLAxiom axiom)
            throws ReasonerInterruptedException,
            UnsupportedEntailmentTypeException, TimeOutException,
            AxiomNotInProfileException, FreshEntitiesException,
            InconsistentOntologyException {
        throw new UnsupportedEntailmentTypeException(axiom);
    }
    
    /**
     * Determines if the specified set of axioms is entailed by the reasoner
     * axioms.
     * 
     * @param axioms
     *            The set of axioms to be tested
     * @return <code>true</code> if the set of axioms is entailed by the axioms
     *         in the imports closure of the root ontology, otherwise
     *         <code>false</code>. If the set of reasoner axioms is inconsistent
     *         then <code>true</code>.
     * 
     * @throws FreshEntitiesException
     *             if the signature of the set of axioms is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @throws UnsupportedEntailmentTypeException
     *             if the reasoner cannot perform a check to see if the
     *             specified axiom is entailed
     * @throws AxiomNotInProfileException
     *             if <code>axiom</code> is not in the profile that is supported
     *             by this reasoner.
     * @throws InconsistentOntologyException
     *             if the set of reasoner axioms is inconsistent
     * @see #isEntailmentCheckingSupported(org.semanticweb.owlapi.model.AxiomType)
     */
    @Override
    public boolean isEntailed(Set<? extends OWLAxiom> axioms)
            throws ReasonerInterruptedException,
            UnsupportedEntailmentTypeException, TimeOutException,
            AxiomNotInProfileException, FreshEntitiesException,
            InconsistentOntologyException {
        throw new UnsupportedEntailmentTypeException(axioms.iterator().next());
    }
    
    /**
     * Determines if entailment checking for the specified axiom type is
     * supported.
     * 
     * @param axiomType
     *            The axiom type
     * @return <code>true</code> if entailment checking for the specified axiom
     *         type is supported, otherwise <code>false</code>. If
     *         <code>true</code> then asking
     *         {@link #isEntailed(org.semanticweb.owlapi.model.OWLAxiom)} will
     *         <em>not</em> throw an exception of
     *         {@link org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException}
     *         . If <code>false</code> then asking
     *         {@link #isEntailed(org.semanticweb.owlapi.model.OWLAxiom)}
     *         <em>will</em> throw an
     *         {@link org.semanticweb.owlapi.reasoner.UnsupportedEntailmentTypeException}
     *         .
     */
    @Override
    public boolean isEntailmentCheckingSupported(AxiomType<?> axiomType) {
        return false;
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the top node (containing
     * <code>owl:Thing</code>) in the class hierarchy.
     * 
     * @return A <code>Node</code> containing <code>owl:Thing</code> that is the
     *         top node in the class hierarchy. This <code>Node</code> is
     *         essentially equal to the <code>Node</code> returned by calling
     *         {@link #getEquivalentClasses(org.semanticweb.owlapi.model.OWLClassExpression)}
     *         with a parameter of <code>owl:Thing</code>.
     */
    @Override
    public Node<OWLClass> getTopClassNode() {
        au.csiro.ontology.Node<String> top = getTaxonomy().getTopNode();
        return nodeToOwlClassNode(top);
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the bottom node (containing
     * <code>owl:Nothing</code>) in the class hierarchy.
     * 
     * @return A <code>Node</code> containing <code>owl:Nothing</code> that is
     *         the bottom node in the class hierarchy. This <code>Node</code> is
     *         essentially equal to the <code>Node</code> that will be returned
     *         by calling
     *         {@link #getEquivalentClasses(org.semanticweb.owlapi.model.OWLClassExpression)}
     *         with a parameter of <code>owl:Nothing</code>.
     */
    @Override
    public Node<OWLClass> getBottomClassNode() {
        return nodeToOwlClassNode(getTaxonomy().getBottomNode());
    }
    
    /**
     * Gets the set of named classes that are the strict (potentially direct)
     * subclasses of the specified class expression with respect to the reasoner
     * axioms. Note that the classes are returned as a
     * {@link NodeSet}.
     * 
     * @param ce
     *            The class expression whose strict (direct) subclasses are to
     *            be retrieved.
     * @param direct
     *            Specifies if the direct subclasses should be retrived (
     *            <code>true</code>) or if the all subclasses (descendant)
     *            classes should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> such that
     *         for each class <code>C</code> in the <code>NodeSet</code> the set
     *         of reasoner axioms entails <code>DirectSubClassOf(C, ce)</code>.
     *         </p> If direct is <code>false</code>, a <code>NodeSet</code> such
     *         that for each class <code>C</code> in the <code>NodeSet</code>
     *         the set of reasoner axioms entails
     *         <code>StrictSubClassOf(C, ce)</code>. </p> If <code>ce</code> is
     *         equivalent to <code>owl:Nothing</code> then the empty
     *         <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>classExpression</code> is not within the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the classExpression is not contained
     *             within the signature of the imports closure of the root
     *             ontology and the undeclared entity policy of this reasoner is
     *             set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getSubClasses(OWLClassExpression ce, boolean direct)
            throws ReasonerInterruptedException, TimeOutException,
            FreshEntitiesException, InconsistentOntologyException,
            ClassExpressionNotInProfileException {
        checkOntologyConsistent();
        checkNamedClass(ce);
        
        au.csiro.ontology.Node<String> n = getNode(ce.asOWLClass());
        if(n == null) {
            // TODO: add logging and warn!
            return new OWLClassNodeSet();
        }
        
        Set<au.csiro.ontology.Node<String>> children = n.getChildren();

        if (direct) {
            // Transform the response back into owlapi objects
            return nodesToOwlClassNodeSet(children);
        } else {
            Set<au.csiro.ontology.Node<String>> res = new HashSet<>();
            Queue<Set<au.csiro.ontology.Node<String>>> todo = new LinkedList<>();
            todo.add(children);
            while (!todo.isEmpty()) {
                Set<au.csiro.ontology.Node<String>> items = todo.remove();
                res.addAll(items);
                for (au.csiro.ontology.Node<String> item : items) {
                    Set<au.csiro.ontology.Node<String>> cn = item.getChildren();
                    if (!cn.isEmpty())
                        todo.add(cn);
                }
            }

            return nodesToOwlClassNodeSet(res);
        }
    }

    private void checkNamedClass(OWLClassExpression ce)
            throws ReasonerInternalException {
        if (ce.isAnonymous()) {
            throw new ReasonerInternalException(
                    "Expected a named class, got " + ce);
        }
    }

    private void checkOntologyConsistent() throws InconsistentOntologyException {
        if (!problems.isEmpty()) {
            throw new InconsistentOntologyException();
        }
    }
    
    /**
     * Gets the set of named classes that are the strict (potentially direct)
     * super classes of the specified class expression with respect to the
     * imports closure of the root ontology. Note that the classes are returned
     * as a {@link NodeSet}.
     * 
     * @param ce
     *            The class expression whose strict (direct) super classes are
     *            to be retrieved.
     * @param direct
     *            Specifies if the direct super classes should be retrived (
     *            <code>true</code>) or if the all super classes (ancestors)
     *            classes should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> such that
     *         for each class <code>C</code> in the <code>NodeSet</code> the set
     *         of reasoner axioms entails <code>DirectSubClassOf(ce, C)</code>.
     *         </p> If direct is <code>false</code>, a <code>NodeSet</code> such
     *         that for each class <code>C</code> in the <code>NodeSet</code>
     *         the set of reasoner axioms entails
     *         <code>StrictSubClassOf(ce, C)</code>. </p> If <code>ce</code> is
     *         equivalent to <code>owl:Thing</code> then the empty
     *         <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>classExpression</code> is not within the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the classExpression is not contained
     *             within the signature of the imports closure of the root
     *             ontology and the undeclared entity policy of this reasoner is
     *             set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getSuperClasses(OWLClassExpression ce,
            boolean direct) throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        checkOntologyConsistent();
        checkNamedClass(ce);
        
        au.csiro.ontology.Node<String> n = getNode(ce.asOWLClass());
        if(n == null) {
            // TODO: add logging and warn!
            return new OWLClassNodeSet();
        }
        Set<au.csiro.ontology.Node<String>> parents = n.getParents();

        if (direct) {
            // Transform the response back into owlapi objects
            return nodesToOwlClassNodeSet(parents);
        } else {
            Set<au.csiro.ontology.Node<String>> res = new HashSet<>();
            Queue<Set<au.csiro.ontology.Node<String>>> todo = new LinkedList<>();
            todo.add(parents);
            while (!todo.isEmpty()) {
                Set<au.csiro.ontology.Node<String>> items = todo.remove();
                res.addAll(items);
                for (au.csiro.ontology.Node<String> item : items) {
                    Set<au.csiro.ontology.Node<String>> cn = item.getParents();
                    if (!cn.isEmpty())
                        todo.add(cn);
                }
            }

            return nodesToOwlClassNodeSet(res);
        }
    }
    
    /**
     * Gets the set of named classes that are equivalent to the specified class
     * expression with respect to the set of reasoner axioms. The classes are
     * returned as a {@link Node}.
     * 
     * @param ce
     *            The class expression whose equivalent classes are to be
     *            retrieved.
     * @return A node containing the named classes such that for each named
     *         class <code>C</code> in the node the root ontology imports
     *         closure entails <code>EquivalentClasses(ce C)</code>. If
     *         <code>ce</code> is not a class name (i.e. it is an anonymous
     *         class expression) and there are no such classes <code>C</code>
     *         then the node will be empty. </p> If <code>ce</code> is a named
     *         class then <code>ce</code> will be contained in the node. </p> If
     *         <code>ce</code> is unsatisfiable with respect to the set of
     *         reasoner axioms then the node representing and containing
     *         <code>owl:Nothing</code>, i.e. the bottom node, will be returned.
     *         </p> If <code>ce</code> is equivalent to <code>owl:Thing</code>
     *         with respect to the set of reasoner axioms then the node
     *         representing and containing <code>owl:Thing</code>, i.e. the top
     *         node, will be returned </p>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>classExpression</code> is not within the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the classExpression is not contained
     *             within the signature of the imports closure of the root
     *             ontology and the undeclared entity policy of this reasoner is
     *             set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public Node<OWLClass> getEquivalentClasses(OWLClassExpression ce)
            throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        checkOntologyConsistent();
        checkNamedClass(ce);

        au.csiro.ontology.Node<String> n = getNode(ce.asOWLClass());
        if(n == null) {
            throw new ReasonerInternalException("Named class "+ ce +
                    " not found!");
        }
        return nodeToOwlClassNode(n);
    }
    
    /**
     * Gets the classes that are disjoint with the specified class expression
     * <code>ce</code>. The classes are returned as a
     * {@link NodeSet}.
     * 
     * @param ce
     *            The class expression whose disjoint classes are to be
     *            retrieved.
     * @return The return value is a <code>NodeSet</code> such that for each
     *         class <code>D</code> in the <code>NodeSet</code> the set of
     *         reasoner axioms entails
     *         <code>EquivalentClasses(D, ObjectComplementOf(ce))</code> or
     *         <code>StrictSubClassOf(D, ObjectComplementOf(ce))</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>classExpression</code> is not within the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the classExpression is not contained
     *             within the signature of the imports closure of the root
     *             ontology and the undeclared entity policy of this reasoner is
     *             set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getDisjointClasses(OWLClassExpression ce)
            throws ReasonerInterruptedException, TimeOutException,
            FreshEntitiesException, InconsistentOntologyException {
        throw new ReasonerInternalException(
                "getDisjointClasses not implemented");
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the top node (containing
     * <code>owl:topObjectProperty</code>) in the object property hierarchy.
     * 
     * @return A <code>Node</code> containing <code>owl:topObjectProperty</code>
     *         that is the top node in the object property hierarchy. This
     *         <code>Node</code> is essentially equivalent to the
     *         <code>Node</code> returned by calling
     *         {@link #getEquivalentObjectProperties(org.semanticweb.owlapi.model.OWLObjectPropertyExpression)}
     *         with a parameter of <code>owl:topObjectProperty</code>.
     */
    @Override
    public Node<OWLObjectPropertyExpression> getTopObjectPropertyNode() {
        return new OWLObjectPropertyNode(owlFactory.getOWLTopObjectProperty());
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the bottom node (containing
     * <code>owl:bottomObjectProperty</code>) in the object property hierarchy.
     * 
     * @return A <code>Node</code>, containing
     *         <code>owl:bottomObjectProperty</code>, that is the bottom node in
     *         the object property hierarchy. This <code>Node</code> is
     *         essentially equal to the <code>Node</code> that will be returned
     *         by calling
     *         {@link #getEquivalentObjectProperties(org.semanticweb.owlapi.model.OWLObjectPropertyExpression)}
     */
    @Override
    public Node<OWLObjectPropertyExpression> getBottomObjectPropertyNode() {
        return new OWLObjectPropertyNode(
                owlFactory.getOWLBottomObjectProperty());
    }
    
    /**
     * Gets the set of <a href="#spe">simplified object property expressions</a>
     * that are the strict (potentially direct) subproperties of the specified
     * object property expression with respect to the imports closure of the
     * root ontology. Note that the properties are returned as a
     * {@link NodeSet}.
     * 
     * @param pe
     *            The object property expression whose strict (direct)
     *            subproperties are to be retrieved.
     * @param direct
     *            Specifies if the direct subproperties should be retrived (
     *            <code>true</code>) or if the all subproperties (descendants)
     *            should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> of <a
     *         href="#spe">simplified object property expressions</a>, such that
     *         for each <a href="#spe">simplified object property
     *         expression</a>, <code>P</code>, in the <code>NodeSet</code> the
     *         set of reasoner axioms entails
     *         <code>DirectSubObjectPropertyOf(P, pe)</code>. </p> If direct is
     *         <code>false</code>, a <code>NodeSet</code> of <a
     *         href="#spe">simplified object property expressions</a>, such that
     *         for each <a href="#spe">simplified object property
     *         expression</a>, <code>P</code>, in the <code>NodeSet</code> the
     *         set of reasoner axioms entails
     *         <code>StrictSubObjectPropertyOf(P, pe)</code>. </p> If
     *         <code>pe</code> is equivalent to
     *         <code>owl:bottomObjectProperty</code> then the empty
     *         <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLObjectPropertyExpression> getSubObjectProperties(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSubObjectProperties not implemented");
    }
    
    /**
     * Gets the set of <a href="#spe">simplified object property expressions</a>
     * that are the strict (potentially direct) super properties of the
     * specified object property expression with respect to the imports closure
     * of the root ontology. Note that the properties are returned as a
     * {@link NodeSet}.
     * 
     * @param pe
     *            The object property expression whose strict (direct) super
     *            properties are to be retrieved.
     * @param direct
     *            Specifies if the direct super properties should be retrived (
     *            <code>true</code>) or if the all super properties (ancestors)
     *            should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> of <a
     *         href="#spe">simplified object property expressions</a>, such that
     *         for each <a href="#spe">simplified object property
     *         expression</a>, <code>P</code>, in the <code>NodeSet</code>, the
     *         set of reasoner axioms entails
     *         <code>DirectSubObjectPropertyOf(pe, P)</code>. </p> If direct is
     *         <code>false</code>, a <code>NodeSet</code> of <a
     *         href="#spe">simplified object property expressions</a>, such that
     *         for each <a href="#spe">simplified object property
     *         expression</a>, <code>P</code>, in the <code>NodeSet</code>, the
     *         set of reasoner axioms entails
     *         <code>StrictSubObjectPropertyOf(pe, P)</code>. </p> If
     *         <code>pe</code> is equivalent to
     *         <code>owl:topObjectProperty</code> then the empty
     *         <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLObjectPropertyExpression> getSuperObjectProperties(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSuperObjectProperties not implemented");
    }
    
    /**
     * Gets the set of <a href="#spe">simplified object property expressions</a>
     * that are equivalent to the specified object property expression with
     * respect to the set of reasoner axioms. The properties are returned as a
     * {@link Node}.
     * 
     * @param pe
     *            The object property expression whose equivalent properties are
     *            to be retrieved.
     * @return A node containing the <a href="#spe">simplified object property
     *         expressions</a> such that for each <a href="#spe">simplified
     *         object property expression</a>, <code>P</code>, in the node, the
     *         set of reasoner axioms entails
     *         <code>EquivalentObjectProperties(pe P)</code>. </p> If
     *         <code>pe</code> is a <a href="#spe">simplified object property
     *         expression</a> then <code>pe</code> will be contained in the
     *         node. </p> If <code>pe</code> is unsatisfiable with respect to
     *         the set of reasoner axioms then the node representing and
     *         containing <code>owl:bottomObjectProperty</code>, i.e. the bottom
     *         node, will be returned. </p> If <code>pe</code> is equivalent to
     *         <code>owl:topObjectProperty</code> with respect to the set of
     *         reasoner axioms then the node representing and containing
     *         <code>owl:topObjectProperty</code>, i.e. the top node, will be
     *         returned </p>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public Node<OWLObjectPropertyExpression> getEquivalentObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        return new OWLObjectPropertyNode();
    }
    
    /**
     * Gets the <a href="#spe">simplified object property expressions</a> that
     * are disjoint with the specified object property expression
     * <code>pe</code>. The object properties are returned as a
     * {@link NodeSet}.
     * 
     * @param pe
     *            The object property expression whose disjoint object
     *            properties are to be retrieved.
     * @return The return value is a <code>NodeSet</code> of <a
     *         href="#spe">simplified object property expressions</a>, such that
     *         for each <a href="#spe">simplified object property
     *         expression</a>, <code>P</code>, in the <code>NodeSet</code> the
     *         set of reasoner axioms entails
     *         <code>EquivalentObjectProperties(P, ObjectPropertyComplementOf(pe))</code>
     *         or
     *         <code>StrictSubObjectPropertyOf(P, ObjectPropertyComplementOf(pe))</code>
     *         .
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>object propertyExpression</code> is not within the
     *             profile that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of <code>pe</code> is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.and the undeclared entity
     *             policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLObjectPropertyExpression> getDisjointObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getDisjointObjectProperties not implemented");
    }
    
    /**
     * Gets the set of <a href="#spe">simplified object property expressions</a>
     * that are the inverses of the specified object property expression with
     * respect to the imports closure of the root ontology. The properties are
     * returned as a {@link NodeSet}
     * 
     * @param pe
     *            The property expression whose inverse properties are to be
     *            retrieved.
     * @return A <code>NodeSet</code> of <a href="#spe">simplified object
     *         property expressions</a>, such that for each simplified object
     *         property expression <code>P</code> in the nodes set, the set of
     *         reasoner axioms entails
     *         <code>InverseObjectProperties(pe, P)</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public Node<OWLObjectPropertyExpression> getInverseObjectProperties(
            OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getInverseObjectProperties not implemented");
    }
    
    /**
     * Gets the named classes that are the direct or indirect domains of this
     * property with respect to the imports closure of the root ontology. The
     * classes are returned as a {@link NodeSet}
     * .
     * 
     * @param pe
     *            The property expression whose domains are to be retrieved.
     * @param direct
     *            Specifies if the direct domains should be retrieved (
     *            <code>true</code>), or if all domains should be retrieved (
     *            <code>false</code>).
     * 
     * @return Let
     *         <code>N = getEquivalentClasses(ObjectSomeValuesFrom(pe owl:Thing))</code>
     *         .
     *         <p>
     *         If <code>direct</code> is <code>true</code>: then if
     *         <code>N</code> is not empty then the return value is
     *         <code>N</code>, else the return value is the result of
     *         <code>getSuperClasses(ObjectSomeValuesFrom(pe owl:Thing), true)</code>.
     *         <p>
     *         If <code>direct</code> is <code>false</code>: then the result of
     *         <code>getSuperClasses(ObjectSomeValuesFrom(pe owl:Thing), false)</code>
     *         together with <code>N</code> if <code>N</code> is non-empty.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getObjectPropertyDomains(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyDomains not implemented");
    }
    
    /**
     * Gets the named classes that are the direct or indirect ranges of this
     * property with respect to the imports closure of the root ontology. The
     * classes are returned as a {@link NodeSet}
     * .
     * 
     * @param pe
     *            The property expression whose ranges are to be retrieved.
     * @param direct
     *            Specifies if the direct ranges should be retrieved (
     *            <code>true</code>), or if all ranges should be retrieved (
     *            <code>false</code>).
     * 
     * @return Let
     *         <code>N = getEquivalentClasses(ObjectSomeValuesFrom(ObjectInverseOf(pe) owl:Thing))</code>
     *         .
     *         <p>
     *         If <code>direct</code> is <code>true</code>: then if
     *         <code>N</code> is not empty then the return value is
     *         <code>N</code>, else the return value is the result of
     *         <code>getSuperClasses(ObjectSomeValuesFrom(ObjectInverseOf(pe) owl:Thing), true)</code>.
     *         <p>
     *         If <code>direct</code> is <code>false</code>: then the result of
     *         <code>getSuperClasses(ObjectSomeValuesFrom(ObjectInverseOf(pe) owl:Thing), false)</code>
     *         together with <code>N</code> if <code>N</code> is non-empty.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getObjectPropertyRanges(
            OWLObjectPropertyExpression pe, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyRanges not implemented");
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the top node (containing
     * <code>owl:topDataProperty</code>) in the data property hierarchy.
     * 
     * @return A <code>Node</code>, containing <code>owl:topDataProperty</code>,
     *         that is the top node in the data property hierarchy. This
     *         <code>Node</code> is essentially equal to the <code>Node</code>
     *         returned by calling
     *         {@link #getEquivalentDataProperties(org.semanticweb.owlapi.model.OWLDataProperty)}
     *         with a parameter of <code>owl:topDataProperty</code>.
     */
    @Override
    public Node<OWLDataProperty> getTopDataPropertyNode() {
        throw new ReasonerInternalException(
                "getTopDataPropertyNode not implemented");
    }
    
    /**
     * Gets the <code>Node</code> corresponding to the bottom node (containing
     * <code>owl:bottomDataProperty</code>) in the data property hierarchy.
     * 
     * @return A <code>Node</code>, containing
     *         <code>owl:bottomDataProperty</code>, that is the bottom node in
     *         the data property hierarchy. This <code>Node</code> is
     *         essentially equal to the <code>Node</code> that will be returned
     *         by calling
     *         {@link #getEquivalentDataProperties(org.semanticweb.owlapi.model.OWLDataProperty)}
     *         with a parameter of <code>owl:bottomDataProperty</code>.
     */
    @Override
    public Node<OWLDataProperty> getBottomDataPropertyNode() {
        throw new ReasonerInternalException(
                "getBottomDataPropertyNode not implemented");
    }
    
    /**
     * Gets the set of named data properties that are the strict (potentially
     * direct) subproperties of the specified data property expression with
     * respect to the imports closure of the root ontology. Note that the
     * properties are returned as a
     * {@link NodeSet}.
     * 
     * @param pe
     *            The data property whose strict (direct) subproperties are to
     *            be retrieved.
     * @param direct
     *            Specifies if the direct subproperties should be retrived (
     *            <code>true</code>) or if the all subproperties (descendants)
     *            should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> such that
     *         for each property <code>P</code> in the <code>NodeSet</code> the
     *         set of reasoner axioms entails
     *         <code>DirectSubDataPropertyOf(P, pe)</code>. </p> If direct is
     *         <code>false</code>, a <code>NodeSet</code> such that for each
     *         property <code>P</code> in the <code>NodeSet</code> the set of
     *         reasoner axioms entails
     *         <code>StrictSubDataPropertyOf(P, pe)</code>. </p> If
     *         <code>pe</code> is equivalent to
     *         <code>owl:bottomDataProperty</code> then the empty
     *         <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the data property is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLDataProperty> getSubDataProperties(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getSubDataProperties not implemented");
    }
    
    /**
     * Gets the set of named data properties that are the strict (potentially
     * direct) super properties of the specified data property with respect to
     * the imports closure of the root ontology. Note that the properties are
     * returned as a {@link NodeSet}.
     * 
     * @param pe
     *            The data property whose strict (direct) super properties are
     *            to be retrieved.
     * @param direct
     *            Specifies if the direct super properties should be retrived (
     *            <code>true</code>) or if the all super properties (ancestors)
     *            should be retrieved (<code>false</code>).
     * @return If direct is <code>true</code>, a <code>NodeSet</code> such that
     *         for each property <code>P</code> in the <code>NodeSet</code> the
     *         set of reasoner axioms entails
     *         <code>DirectSubDataPropertyOf(pe, P)</code>. </p> If direct is
     *         <code>false</code>, a <code>NodeSet</code> such that for each
     *         property <code>P</code> in the <code>NodeSet</code> the set of
     *         reasoner axioms entails
     *         <code>StrictSubDataPropertyOf(pe, P)</code>. </p> If
     *         <code>pe</code> is equivalent to <code>owl:topDataProperty</code>
     *         then the empty <code>NodeSet</code> will be returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the data property is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLDataProperty> getSuperDataProperties(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getSuperDataProperties not implemented");
    }
    
    /**
     * Gets the set of named data properties that are equivalent to the
     * specified data property expression with respect to the imports closure of
     * the root ontology. The properties are returned as a
     * {@link Node}.
     * 
     * @param pe
     *            The data property expression whose equivalent properties are
     *            to be retrieved.
     * @return A node containing the named data properties such that for each
     *         named data property <code>P</code> in the node, the set of
     *         reasoner axioms entails
     *         <code>EquivalentDataProperties(pe P)</code>. </p> If
     *         <code>pe</code> is a named data property then <code>pe</code>
     *         will be contained in the node. </p> If <code>pe</code> is
     *         unsatisfiable with respect to the set of reasoner axioms then the
     *         node representing and containing
     *         <code>owl:bottomDataProperty</code>, i.e. the bottom node, will
     *         be returned. </p> If <code>ce</code> is equivalent to
     *         <code>owl:topDataProperty</code> with respect to the set of
     *         reasoner axioms then the node representing and containing
     *         <code>owl:topDataProperty</code>, i.e. the top node, will be
     *         returned </p>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the data property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public Node<OWLDataProperty> getEquivalentDataProperties(OWLDataProperty pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getEquivalentDataProperties not implemented");
    }
    
    /**
     * Gets the data properties that are disjoint with the specified data
     * property expression <code>pe</code>. The data properties are returned as
     * a {@link NodeSet}.
     * 
     * @param pe
     *            The data property expression whose disjoint data properties
     *            are to be retrieved.
     * @return The return value is a <code>NodeSet</code> such that for each
     *         data property <code>P</code> in the <code>NodeSet</code> the set
     *         of reasoner axioms entails
     *         <code>EquivalentDataProperties(P, DataPropertyComplementOf(pe))</code>
     *         or
     *         <code>StrictSubDataPropertyOf(P, DataPropertyComplementOf(pe))</code>
     *         .
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if <code>data propertyExpression</code> is not within the
     *             profile that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of <code>pe</code> is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLDataProperty> getDisjointDataProperties(
            OWLDataPropertyExpression pe) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDisjointDataProperties not implemented");
    }
    
    /**
     * Gets the named classes that are the direct or indirect domains of this
     * property with respect to the imports closure of the root ontology. The
     * classes are returned as a {@link NodeSet}
     * .
     * 
     * @param pe
     *            The property expression whose domains are to be retrieved.
     * @param direct
     *            Specifies if the direct domains should be retrieved (
     *            <code>true</code>), or if all domains should be retrieved (
     *            <code>false</code>).
     * 
     * @return Let
     *         <code>N = getEquivalentClasses(DataSomeValuesFrom(pe rdfs:Literal))</code>
     *         .
     *         <p>
     *         If <code>direct</code> is <code>true</code>: then if
     *         <code>N</code> is not empty then the return value is
     *         <code>N</code>, else the return value is the result of
     *         <code>getSuperClasses(DataSomeValuesFrom(pe rdfs:Literal), true)</code>.
     *         <p>
     *         If <code>direct</code> is <code>false</code>: then the result of
     *         <code>getSuperClasses(DataSomeValuesFrom(pe rdfs:Literal), false)</code>
     *         together with <code>N</code> if <code>N</code> is non-empty.
     *         <p>
     *         (Note, <code>rdfs:Literal</code> is the top datatype).
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the object property expression is not
     *             contained within the signature of the imports closure of the
     *             root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getDataPropertyDomains(OWLDataProperty pe,
            boolean direct) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDataPropertyDomains not implemented");
    }
    
    /**
     * Gets the named classes which are (potentially direct) types of the
     * specified named individual. The classes are returned as a
     * {@link NodeSet}.
     * 
     * @param ind
     *            The individual whose types are to be retrieved.
     * @param direct
     *            Specifies if the direct types should be retrieved (
     *            <code>true</code>), or if all types should be retrieved (
     *            <code>false</code>).
     * @return If <code>direct</code> is <code>true</code>, a
     *         <code>NodeSet</code> containing named classes such that for each
     *         named class <code>C</code> in the node set, the set of reasoner
     *         axioms entails <code>DirectClassAssertion(C, ind)</code>. </p> If
     *         <code>direct</code> is <code>false</code>, a <code>NodeSet</code>
     *         containing named classes such that for each named class
     *         <code>C</code> in the node set, the set of reasoner axioms
     *         entails <code>ClassAssertion(C, ind)</code>. </p>
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the individual is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLClass> getTypes(OWLNamedIndividual ind, boolean direct)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException("getTypes not implemented");
    }
    
    /**
     * Gets the individuals which are instances of the specified class
     * expression. The individuals are returned a a
     * {@link NodeSet}.
     * 
     * @param ce
     *            The class expression whose instances are to be retrieved.
     * @param direct
     *            Specifies if the direct instances should be retrieved (
     *            <code>true</code>), or if all instances should be retrieved (
     *            <code>false</code>).
     * @return If <code>direct</code> is <code>true</code>, a
     *         <code>NodeSet</code> containing named individuals such that for
     *         each named individual <code>j</code> in the node set, the set of
     *         reasoner axioms entails <code>DirectClassAssertion(ce, j)</code>.
     *         </p> If <code>direct</code> is <code>false</code>, a
     *         <code>NodeSet</code> containing named individuals such that for
     *         each named individual <code>j</code> in the node set, the set of
     *         reasoner axioms entails <code>ClassAssertion(ce, j)</code>. </p>
     *         </p> If ce is unsatisfiable with respect to the set of reasoner
     *         axioms then the empty <code>NodeSet</code> is returned.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws ClassExpressionNotInProfileException
     *             if the class expression <code>ce</code> is not in the profile
     *             that is supported by this reasoner.
     * @throws FreshEntitiesException
     *             if the signature of the class expression is not contained
     *             within the signature of the imports closure of the root
     *             ontology and the undeclared entity policy of this reasoner is
     *             set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @see {@link org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy}
     */
    @Override
    public NodeSet<OWLNamedIndividual> getInstances(OWLClassExpression ce,
            boolean direct) throws InconsistentOntologyException,
            ClassExpressionNotInProfileException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        return new OWLNamedIndividualNodeSet();
    }
    
    /**
     * Gets the object property values for the specified individual and object
     * property expression. The individuals are returned as a
     * {@link NodeSet}.
     * 
     * @param ind
     *            The individual that is the subject of the object property
     *            values
     * @param pe
     *            The object property expression whose values are to be
     *            retrieved for the specified individual
     * @return A <code>NodeSet</code> containing named individuals such that for
     *         each individual <code>j</code> in the node set, the set of
     *         reasoner axioms entails
     *         <code>ObjectPropertyAssertion(pe ind j)</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the individual and property expression is
     *             not contained within the signature of the imports closure of
     *             the root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @see {@link org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy}
     */
    @Override
    public NodeSet<OWLNamedIndividual> getObjectPropertyValues(
            OWLNamedIndividual ind, OWLObjectPropertyExpression pe)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getObjectPropertyValues not implemented");
    }
    
    /**
     * Gets the data property values for the specified individual and data
     * property expression. The values are a set of literals. Note that the
     * results are not guaranteed to be complete for this method. The reasoner
     * may also return canonical literals or they may be in a form that bears a
     * resemblance to the syntax of the literals in the root ontology imports
     * closure.
     * 
     * @param ind
     *            The individual that is the subject of the data property values
     * @param pe
     *            The data property expression whose values are to be retrieved
     *            for the specified individual
     * @return A set of <code>OWLLiteral</code>s containing literals such that
     *         for each literal <code>l</code> in the set, the set of reasoner
     *         axioms entails <code>DataPropertyAssertion(pe ind l)</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the individual and property expression is
     *             not contained within the signature of the imports closure of
     *             the root ontology and the undeclared entity policy of this
     *             reasoner is set to {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     * @see {@link org.semanticweb.owlapi.reasoner.IndividualNodeSetPolicy}
     */
    @Override
    public Set<OWLLiteral> getDataPropertyValues(OWLNamedIndividual ind,
            OWLDataProperty pe) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        return Collections.emptySet();
    }
    
    /**
     * Gets the individuals that are the same as the specified individual.
     * 
     * @param ind
     *            The individual whose same individuals are to be retrieved.
     * @return A node containing individuals such that for each individual
     *         <code>j</code> in the node, the root ontology imports closure
     *         entails <code>SameIndividual(j, ind)</code>. Note that the node
     *         will contain <code>j</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the individual is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public Node<OWLNamedIndividual> getSameIndividuals(OWLNamedIndividual ind)
            throws InconsistentOntologyException, FreshEntitiesException,
            ReasonerInterruptedException, TimeOutException {
        throw new ReasonerInternalException(
                "getSameIndividuals not implemented");
    }
    
    /**
     * Gets the individuals which are entailed to be different from the
     * specified individual. The individuals are returned as a
     * {@link NodeSet}.
     * 
     * @param ind
     *            The individual whose different individuals are to be returned.
     * @return A <code>NodeSet</code> containing <code>OWLNamedIndividual</code>
     *         s such that for each individual <code>i</code> in the
     *         <code>NodeSet</code> the set of reasoner axioms entails
     *         <code>DifferentIndividuals(ind, i)</code>.
     * 
     * @throws InconsistentOntologyException
     *             if the imports closure of the root ontology is inconsistent
     * @throws FreshEntitiesException
     *             if the signature of the individual is not contained within
     *             the signature of the imports closure of the root ontology and
     *             the undeclared entity policy of this reasoner is set to
     *             {@link FreshEntityPolicy#DISALLOW}.
     * @throws ReasonerInterruptedException
     *             if the reasoning process was interrupted for any particular
     *             reason (for example if reasoning was cancelled by a client
     *             process)
     * @throws TimeOutException
     *             if the reasoner timed out during a basic reasoning operation.
     *             See {@link #getTimeOut()}.
     */
    @Override
    public NodeSet<OWLNamedIndividual> getDifferentIndividuals(
            OWLNamedIndividual ind) throws InconsistentOntologyException,
            FreshEntitiesException, ReasonerInterruptedException,
            TimeOutException {
        throw new ReasonerInternalException(
                "getDifferentIndividuals not implemented");
    }
    
    /**
     * Gets the time out (in milliseconds) for the most basic reasoning
     * operations. That is the maximum time for a satisfiability test,
     * subsumption test etc. The time out should be set at reasoner creation
     * time. During satisfiability (subsumption) checking the reasoner will
     * check to see if the time it has spent doing the single check is longer
     * than the value returned by this method. If this is the case, the reasoner
     * will throw a {@link org.semanticweb.owlapi.reasoner.TimeOutException} in
     * the thread that is executing the reasoning process. </p> Note that
     * clients that want a higher level timeout, at the level of classification
     * for example, should start their own timers and request that the reasoner
     * interrupts the current process using the {@link #interrupt()} method.
     * 
     * @return The time out in milliseconds for basic reasoner operation. By
     *         default this is the value of {@link Long#MAX_VALUE}.
     */
    @Override
    public long getTimeOut() {
        return (config != null) ? config.getTimeOut() : 0;
    }
    
    /**
     * Gets the Fresh Entity Policy in use by this reasoner. The policy is set
     * at reasoner creation time.
     * 
     * @return The policy.
     */
    @Override
    public FreshEntityPolicy getFreshEntityPolicy() {
        return (config != null) ? config.getFreshEntityPolicy()
                : FreshEntityPolicy.DISALLOW;
    }
    
    /**
     * Gets the IndividualNodeSetPolicy in use by this reasoner. The policy is
     * set at reasoner creation time.
     * 
     * @return The policy.
     */
    @Override
    public IndividualNodeSetPolicy getIndividualNodeSetPolicy() {
        return (config != null) ? config.getIndividualNodeSetPolicy()
                : IndividualNodeSetPolicy.BY_NAME;
    }
    
    /**
     * Disposes of this reasoner. This frees up any resources used by the
     * reasoner and detaches the reasoner as an
     * {@link org.semanticweb.owlapi.model.OWLOntologyChangeListener} from the
     * {@link org.semanticweb.owlapi.model.OWLOntologyManager} that manages the
     * ontologies contained within the reasoner.
     */
    @Override
    public void dispose() {
        owlOntology.getOWLOntologyManager().removeOntologyChangeListener(
                ontologyChangeListener);
        rawChanges.clear();
        reasoner = new SnorocketReasoner<>();
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
            System.out.println("Classifying");
            c.precomputeInferences(InferenceType.CLASS_HIERARCHY);
            System.out.println("Took " + (System.currentTimeMillis() - start)
                    + "ms");
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
    }

}
