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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import au.csiro.ontology.IOntology;
import au.csiro.ontology.Node;
import au.csiro.ontology.Ontology;
import au.csiro.ontology.Taxonomy;
import au.csiro.ontology.axioms.IAxiom;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.model.Concept;
import au.csiro.ontology.model.IConcept;
import au.csiro.snorocket.core.concurrent.Context;

/**
 * This class represents an instance of the reasoner. It uses the internal
 * ontology model. If you need to use an OWL model refer to the 
 * {@link SnorocketOWLReasoner} class.
 * 
 * @author Alejandro Metke
 *
 */
@SuppressWarnings("deprecation")
final public class SnorocketReasoner<T extends Comparable<T>> implements IReasoner<T>, Serializable {
    
    /**
     * Serialisation version.
     */
    private static final long serialVersionUID = 1L;

    private final static Logger log = Logger.getLogger(SnorocketReasoner.class);
    
    public static final int BUFFER_SIZE = 10;
    
    private NormalisedOntology<T> no = null;
    private IFactory<T> factory = null;
    private boolean isClassified = false;
    
    /**
     * Loads a saved instance of a {@link SnorocketReasoner} from an input
     * stream.
     * 
     * @param in
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static SnorocketReasoner load(InputStream in) {
        SnorocketReasoner res; 
        try(ObjectInputStream ois = new ObjectInputStream(in); ) { 
            res = (SnorocketReasoner)ois.readObject();  
        } catch(Exception e) { 
            log.error("Problem loading reasoner." + e);
            throw new RuntimeException(e);
        }
        Concept.reconnectTopBottom(
                (IConcept)res.factory.lookupConceptId(CoreFactory.TOP_CONCEPT), 
                (IConcept)res.factory.lookupConceptId(CoreFactory.BOTTOM_CONCEPT));
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
        
    }

    @Override
    public IReasoner<T> classify(Set<IAxiom> axioms) {
        if(!isClassified) {
            factory = new CoreFactory<T>();
            no = new NormalisedOntology<T>(factory);
            no.loadAxioms(new HashSet<IAxiom>(axioms));
            no.classify();
            isClassified = true;
        } else {
            no.classifyIncremental(axioms);
        }
        return this;
    }
    
    @Override
    public IReasoner<T> classify(Iterator<IAxiom> axioms) {
        IReasoner<T> res = null;
        Set<IAxiom> axiomSet = new HashSet<>();
        while(axioms.hasNext()) {
            IAxiom axiom = axioms.next();
            if(axiom == null) continue;
            axiomSet.add(axiom);
            if(axiomSet.size() == BUFFER_SIZE) {
                res = classify(axiomSet);
                axiomSet.clear();
            }
        }
        
        if(!axiomSet.isEmpty()) {
            res = classify(axiomSet);
        }
        
        return res;
    }
    
    @Override
    public IReasoner<T> classify(IOntology<T> ont) {
        return classify(new HashSet<>(ont.getStatedAxioms()));
    }

    @Override
    public void prune() {
        // TODO: implement
        throw new UnsupportedOperationException();
    }

    @Override
    public IOntology<T> getClassifiedOntology() {
        // Check ontology is classified
        if(!isClassified) throw new RuntimeException(
                "Ontology is not classified!");
        
        log.info("Building taxonomy");
        no.buildTaxonomy();
        Map<T, Node<T>> taxonomy = no.getTaxonomy();
        Set<Node<T>> affectedNodes = no.getAffectedNodes();
        
        return new Ontology<T>(null, taxonomy, affectedNodes);
    }
    
    /**
     * @deprecated Use {@link SnorocketReasoner#getClassifiedOntology()} 
     * instead.
     */
    @Override
    public Taxonomy<T> getTaxonomy() {
        if(no == null)
            return null;
        
        Map<T, Node<T>> res = no.getTaxonomy();
        return new Taxonomy<T>(res);
    }

    @Override
    public void save(OutputStream out) {
        try (ObjectOutputStream oos = new ObjectOutputStream(out)){
            oos.writeObject(this); 
            oos.flush();
        } catch(Exception e) {
            log.error("Problem saving reasoner.", e);
            throw new RuntimeException(e);
        }
    }
    
}
