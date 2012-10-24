package au.csiro.snorocket.core;

import java.util.Set;

import org.semanticweb.owlapi.reasoner.OWLReasoner;

import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.AbstractAxiom;

/**
 * This interface represents the functionality of a reasoner. It uses the 
 * internal ontology model. In order to use an OWL model refer to the 
 * {@link OWLReasoner} inteface and the {@link SnorocketOWLReasoner} class.
 * 
 * @author Alejandro Metke
 *
 */
public interface IReasoner {

    /**
     * Performs a full classification. Any state in the reasoner is replaced
     * with the results of this classification process.
     * 
     * @param axioms The axioms in the base ontology.
     */
    public abstract void classify(Set<AbstractAxiom> axioms);

    /**
     * Performs an incremental classification.
     * 
     * @param axioms The incremental axioms.
     */
    public abstract void classifyIncremental(Set<AbstractAxiom> axioms);

    /**
     * Returns the resulting {@link Taxonomy} after classification (or null if
     * the ontology has not been classified yet).
     * 
     * @return The taxonomy.
     */
    public abstract Taxonomy getTaxonomy();

}