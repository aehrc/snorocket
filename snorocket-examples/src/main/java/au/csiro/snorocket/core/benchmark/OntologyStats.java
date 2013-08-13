/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions.
 */
package au.csiro.snorocket.core.benchmark;

import java.io.File;
import java.util.Iterator;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import au.csiro.ontology.Ontology;
import au.csiro.ontology.classification.IReasoner;
import au.csiro.ontology.importer.owl.OWLImporter;
import au.csiro.ontology.util.NullProgressMonitor;
import au.csiro.snorocket.core.SnorocketReasoner;

/**
 * @author Alejandro Metke
 *
 */
public class OntologyStats {

    /**
     * 
     */
    public OntologyStats() {
        
    }
    
    public static void main(String[] args) {
        OWLOntologyManager man = OWLManager.createOWLOntologyManager();
        
        // loading the root ontology
        OWLOntology root = null;
        try {
            root = man.loadOntologyFromOntologyDocument(new File(
                    "C:\\dev\\ontologies\\AMT\\amt3v.owl\\amt3v.owl"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return;
        }
        
        int numClasses = root.getClassesInSignature().size();
        int numProps = root.getObjectPropertiesInSignature().size();
        int numData = root.getDataPropertiesInSignature().size();
        
        int numEqClasses = root.getAxiomCount(AxiomType.EQUIVALENT_CLASSES);
        int numSubClasses = root.getAxiomCount(AxiomType.SUBCLASS_OF);
        int numEqRoles = root.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES);
        int numSubRoles = root.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY);
        int numPropChains = root.getAxiomCount(AxiomType.SUB_PROPERTY_CHAIN_OF);
        int numRefRoles = root.getAxiomCount(AxiomType.REFLEXIVE_OBJECT_PROPERTY);
        int numDatatypes = root.getAxiomCount(AxiomType.DATATYPE_DEFINITION);
        
        System.out.println("numClasses: "+numClasses);
        System.out.println("numProps: "+numProps);
        System.out.println("numData: "+numData);
        System.out.println("numEqClasses: "+numEqClasses);
        System.out.println("numSubClasses: "+numSubClasses);
        System.out.println("numEqRoles: "+numEqRoles);
        System.out.println("numSubRoles: "+numSubRoles);
        System.out.println("numPropChains: "+numPropChains);
        System.out.println("numRefRoles: "+numRefRoles);
        System.out.println("numDatatypes: "+numDatatypes);
        
        OWLImporter oi = new OWLImporter(root);
        
        Iterator<Ontology> it = oi.getOntologyVersions(new NullProgressMonitor()); 
        while(it.hasNext()) {
            Ontology ont = it.next();
            IReasoner r = new SnorocketReasoner();
            r.loadAxioms(ont);
            r = r.classify();
            Ontology co = r.getClassifiedOntology();
            int numConcepts = co.getNodeMap().keySet().size();
            System.out.println("numConcepts: "+numConcepts);
        }
        
    }

}
