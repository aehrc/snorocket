/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.importer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import javax.xml.bind.DatatypeConverter;

import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLClassExpressionVisitor;
import org.semanticweb.owlapi.model.OWLDataAllValuesFrom;
import org.semanticweb.owlapi.model.OWLDataExactCardinality;
import org.semanticweb.owlapi.model.OWLDataHasValue;
import org.semanticweb.owlapi.model.OWLDataMaxCardinality;
import org.semanticweb.owlapi.model.OWLDataMinCardinality;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLDataPropertyRangeAxiom;
import org.semanticweb.owlapi.model.OWLDataRange;
import org.semanticweb.owlapi.model.OWLDataSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLDatatype;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLDisjointClassesAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLEquivalentObjectPropertiesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectAllValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectComplementOf;
import org.semanticweb.owlapi.model.OWLObjectExactCardinality;
import org.semanticweb.owlapi.model.OWLObjectHasSelf;
import org.semanticweb.owlapi.model.OWLObjectHasValue;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectMaxCardinality;
import org.semanticweb.owlapi.model.OWLObjectMinCardinality;
import org.semanticweb.owlapi.model.OWLObjectOneOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLObjectUnionOf;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLReflexiveObjectPropertyAxiom;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;
import org.semanticweb.owlapi.model.OWLTransitiveObjectPropertyAxiom;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;
import org.semanticweb.owlapi.vocab.OWL2Datatype;

import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.axioms.RI;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.AbstractLiteral;
import au.csiro.snorocket.core.model.BooleanLiteral;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Datatype;
import au.csiro.snorocket.core.model.DateLiteral;
import au.csiro.snorocket.core.model.DoubleLiteral;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.model.FloatLiteral;
import au.csiro.snorocket.core.model.IntegerLiteral;
import au.csiro.snorocket.core.model.LongLiteral;
import au.csiro.snorocket.core.model.StringLiteral;

/**
 * Imports axioms in OWL format into the internal representation used by
 * Snorocket.
 * 
 * @author Alejandro Metke
 * 
 */
public class OWLImporter {

    private final IFactory factory;
    private final Set<OWLDataPropertyRangeAxiom> dprAxioms = new HashSet<OWLDataPropertyRangeAxiom>();
    private final Map<OWL2Datatype, Set<OWL2Datatype>> types = new HashMap<>();
    private final List<String> problems = new ArrayList<String>();

    /**
     * 
     * @param factory
     */
    public OWLImporter(IFactory factory) {
        this.factory = factory;

        Set<OWL2Datatype> set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_BYTE);
        types.put(OWL2Datatype.XSD_BYTE, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_SHORT);
        set.addAll(types.get(OWL2Datatype.XSD_BYTE));
        types.put(OWL2Datatype.XSD_SHORT, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_INT);
        set.addAll(types.get(OWL2Datatype.XSD_SHORT));
        types.put(OWL2Datatype.XSD_INT, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_LONG);
        set.addAll(types.get(OWL2Datatype.XSD_INT));
        types.put(OWL2Datatype.XSD_LONG, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_UNSIGNED_BYTE);
        types.put(OWL2Datatype.XSD_UNSIGNED_BYTE, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_UNSIGNED_SHORT);
        set.addAll(types.get(OWL2Datatype.XSD_UNSIGNED_BYTE));
        types.put(OWL2Datatype.XSD_UNSIGNED_SHORT, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_UNSIGNED_INT);
        set.addAll(types.get(OWL2Datatype.XSD_UNSIGNED_SHORT));
        types.put(OWL2Datatype.XSD_UNSIGNED_INT, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_UNSIGNED_LONG);
        set.addAll(types.get(OWL2Datatype.XSD_UNSIGNED_INT));
        types.put(OWL2Datatype.XSD_UNSIGNED_LONG, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_POSITIVE_INTEGER);
        types.put(OWL2Datatype.XSD_POSITIVE_INTEGER, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NON_NEGATIVE_INTEGER);
        set.addAll(types.get(OWL2Datatype.XSD_POSITIVE_INTEGER));
        set.addAll(types.get(OWL2Datatype.XSD_UNSIGNED_LONG));
        types.put(OWL2Datatype.XSD_NON_NEGATIVE_INTEGER, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NEGATIVE_INTEGER);
        types.put(OWL2Datatype.XSD_NEGATIVE_INTEGER, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NON_POSITIVE_INTEGER);
        set.addAll(types.get(OWL2Datatype.XSD_NEGATIVE_INTEGER));
        types.put(OWL2Datatype.XSD_NON_POSITIVE_INTEGER, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_INTEGER);
        set.addAll(types.get(OWL2Datatype.XSD_NON_POSITIVE_INTEGER));
        set.addAll(types.get(OWL2Datatype.XSD_NON_NEGATIVE_INTEGER));
        set.addAll(types.get(OWL2Datatype.XSD_LONG));
        types.put(OWL2Datatype.XSD_INTEGER, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_DECIMAL);
        set.addAll(types.get(OWL2Datatype.XSD_INTEGER));
        types.put(OWL2Datatype.XSD_DECIMAL, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_DOUBLE);
        types.put(OWL2Datatype.XSD_DOUBLE, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_FLOAT);
        types.put(OWL2Datatype.XSD_FLOAT, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NCNAME);
        types.put(OWL2Datatype.XSD_NCNAME, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NAME);
        set.addAll(types.get(OWL2Datatype.XSD_NCNAME));
        types.put(OWL2Datatype.XSD_NAME, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NMTOKEN);
        types.put(OWL2Datatype.XSD_NMTOKEN, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_LANGUAGE);
        types.put(OWL2Datatype.XSD_LANGUAGE, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_TOKEN);
        set.addAll(types.get(OWL2Datatype.XSD_LANGUAGE));
        set.addAll(types.get(OWL2Datatype.XSD_NMTOKEN));
        set.addAll(types.get(OWL2Datatype.XSD_NAME));
        types.put(OWL2Datatype.XSD_TOKEN, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_NORMALIZED_STRING);
        set.addAll(types.get(OWL2Datatype.XSD_TOKEN));
        types.put(OWL2Datatype.XSD_NORMALIZED_STRING, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_STRING);
        set.addAll(types.get(OWL2Datatype.XSD_NORMALIZED_STRING));
        types.put(OWL2Datatype.XSD_STRING, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_BOOLEAN);
        types.put(OWL2Datatype.XSD_BOOLEAN, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_HEX_BINARY);
        types.put(OWL2Datatype.XSD_HEX_BINARY, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_BASE_64_BINARY);
        types.put(OWL2Datatype.XSD_BASE_64_BINARY, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_ANY_URI);
        types.put(OWL2Datatype.XSD_ANY_URI, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_DATE_TIME);
        types.put(OWL2Datatype.XSD_DATE_TIME, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.XSD_DATE_TIME_STAMP);
        types.put(OWL2Datatype.XSD_DATE_TIME_STAMP, set);

        // TODO: check hierachies for these datatypes
        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.OWL_RATIONAL);
        types.put(OWL2Datatype.OWL_RATIONAL, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.OWL_REAL);
        types.put(OWL2Datatype.OWL_REAL, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.RDF_PLAIN_LITERAL);
        types.put(OWL2Datatype.RDF_PLAIN_LITERAL, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.RDFS_LITERAL);
        types.put(OWL2Datatype.RDFS_LITERAL, set);

        set = new HashSet<OWL2Datatype>();
        set.add(OWL2Datatype.RDF_XML_LITERAL);
        types.put(OWL2Datatype.RDF_XML_LITERAL, set);
    }

    private Inclusion transformOWLSubPropertyChainOfAxiom(
            OWLSubPropertyChainOfAxiom a) {
        List<OWLObjectPropertyExpression> sub = a.getPropertyChain();
        OWLObjectPropertyExpression sup = a.getSuperProperty();

        int size = sub.size();
        int[] lhss = new int[size];
        for (int i = 0; i < size; i++) {
            lhss[i] = factory.getRole(sub.get(i).asOWLObjectProperty()
                    .toStringID());
        }

        int rhs = factory.getRole(sup.asOWLObjectProperty().toStringID());

        if (lhss.length == 1) {
            return new RI(lhss[0], rhs);
        } else if (lhss.length == 2) {
            return new RI(lhss, rhs);
        } else {
            throw new RuntimeException(
                    "RoleChains longer than 2 not supported.");
        }
    }

    private Inclusion transformOWLSubObjectPropertyOfAxiom(
            OWLSubObjectPropertyOfAxiom a) {
        OWLObjectPropertyExpression sub = a.getSubProperty();
        OWLObjectPropertyExpression sup = a.getSuperProperty();

        int lhs = factory.getRole(sub.asOWLObjectProperty().toStringID());
        int rhs = factory.getRole(sup.asOWLObjectProperty().toStringID());

        return new RI(lhs, rhs);
    }

    private Inclusion transformOWLReflexiveObjectPropertyAxiom(
            OWLReflexiveObjectPropertyAxiom a) {
        OWLObjectPropertyExpression exp = a.getProperty();
        return new RI(new int[] {}, factory.getRole(exp.asOWLObjectProperty()
                .toStringID()));
    }

    private Inclusion transformOWLTransitiveObjectPropertyAxiom(
            OWLTransitiveObjectPropertyAxiom a) {
        OWLObjectPropertyExpression exp = a.getProperty();
        int r = factory.getRole(exp.asOWLObjectProperty().toStringID());
        return new RI(new int[] { r, r }, r);
    }

    private Inclusion transformOWLSubClassOfAxiom(OWLSubClassOfAxiom a) {
        OWLClassExpression sub = a.getSubClass();
        OWLClassExpression sup = a.getSuperClass();

        AbstractConcept subConcept = getConcept(sub);
        AbstractConcept superConcept = getConcept(sup);

        if (subConcept != null && superConcept != null) {
            return new GCI(subConcept, superConcept);
        } else {
            throw new RuntimeException("Unable to load axiom " + a);
        }
    }

    private List<Inclusion> transformOWLEquivalentClassesAxiom(
            OWLEquivalentClassesAxiom a) {
        List<Inclusion> axioms = new ArrayList<>();
        List<OWLClassExpression> exps = a.getClassExpressionsAsList();

        int size = exps.size();

        for (int i = 0; i < size - 1; i++) {
            OWLClassExpression e1 = exps.get(i);
            AbstractConcept concept1 = getConcept(e1);
            for (int j = i; j < size; j++) {
                OWLClassExpression e2 = exps.get(j);
                if (e1 == e2)
                    continue;
                AbstractConcept concept2 = getConcept(e2);
                axioms.add(new GCI(concept1, concept2));
                axioms.add(new GCI(concept2, concept1));
            }
        }
        return axioms;
    }

    private Inclusion transformOWLDisjointClassesAxiom(OWLDisjointClassesAxiom a) {
        List<OWLClassExpression> exps = a.getClassExpressionsAsList();
        List<AbstractConcept> concepts = new ArrayList<AbstractConcept>();
        for (OWLClassExpression exp : exps) {
            concepts.add(getConcept(exp));
        }

        AbstractConcept[] conjs = new AbstractConcept[concepts.size()];
        int i = 0;
        for (; i < concepts.size(); i++) {
            conjs[i] = concepts.get(i);
        }

        return new GCI(new Conjunction(conjs),
                factory.getConcept(IFactory.BOTTOM));
    }

    private List<Inclusion> transformOWLEquivalentObjectPropertiesAxiom(
            OWLEquivalentObjectPropertiesAxiom a) {
        List<Inclusion> axioms = new ArrayList<>();
        for (OWLSubObjectPropertyOfAxiom ax : a.asSubObjectPropertyOfAxioms()) {
            OWLObjectPropertyExpression sub = ax.getSubProperty();
            OWLObjectPropertyExpression sup = ax.getSuperProperty();

            axioms.add(new RI(factory.getRole(sub.asOWLObjectProperty()
                    .toStringID()), factory.getRole(sup.asOWLObjectProperty()
                    .toStringID())));
        }
        return axioms;
    }

    public Set<Inclusion> transform(List<OWLAxiom> axioms,
            ReasonerProgressMonitor monitor) {
        monitor.reasonerTaskStarted("Loading axioms");
        final Set<Inclusion> res = new HashSet<>();
        int totalAxioms = axioms.size();
        int workDone = 0;

        for (OWLAxiom axiom : axioms) {
            if (axiom instanceof OWLDeclarationAxiom)
                continue;

            if (axiom instanceof OWLSubPropertyChainOfAxiom) {
                OWLSubPropertyChainOfAxiom a = (OWLSubPropertyChainOfAxiom) axiom;
                res.add(transformOWLSubPropertyChainOfAxiom(a));
                monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
            } else if (axiom instanceof OWLSubObjectPropertyOfAxiom) {
                OWLSubObjectPropertyOfAxiom a = (OWLSubObjectPropertyOfAxiom) axiom;
                res.add(transformOWLSubObjectPropertyOfAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLReflexiveObjectPropertyAxiom) {
                OWLReflexiveObjectPropertyAxiom a = (OWLReflexiveObjectPropertyAxiom) axiom;
                res.add(transformOWLReflexiveObjectPropertyAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLTransitiveObjectPropertyAxiom) {
                OWLTransitiveObjectPropertyAxiom a = (OWLTransitiveObjectPropertyAxiom) axiom;
                res.add(transformOWLTransitiveObjectPropertyAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLSubClassOfAxiom) {
                OWLSubClassOfAxiom a = (OWLSubClassOfAxiom) axiom;
                res.add(transformOWLSubClassOfAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLEquivalentClassesAxiom) {
                OWLEquivalentClassesAxiom a = (OWLEquivalentClassesAxiom) axiom;
                res.addAll(transformOWLEquivalentClassesAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLDisjointClassesAxiom) {
                OWLDisjointClassesAxiom a = (OWLDisjointClassesAxiom) axiom;
                res.add(transformOWLDisjointClassesAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            } else if (axiom instanceof OWLEquivalentObjectPropertiesAxiom) {
                OWLEquivalentObjectPropertiesAxiom a = (OWLEquivalentObjectPropertiesAxiom) axiom;
                res.addAll(transformOWLEquivalentObjectPropertiesAxiom(a));
                monitor.reasonerTaskProgressChanged(++workDone, totalAxioms);
            }
        }

        // TODO: deal with other axioms types even if Snorocket does not
        // currently support them
        monitor.reasonerTaskStopped();
        return res;
    }

    public Set<Inclusion> transform(OWLOntology ont,
            ReasonerProgressMonitor monitor) {
        monitor.reasonerTaskStarted("Loading axioms");
        final Set<Inclusion> axioms = new HashSet<>();

        int totalAxioms = ont.getAxiomCount(AxiomType.DECLARATION, true)
                + ont.getAxiomCount(AxiomType.SUB_OBJECT_PROPERTY, true)
                + ont.getAxiomCount(AxiomType.REFLEXIVE_OBJECT_PROPERTY, true)
                + ont.getAxiomCount(AxiomType.TRANSITIVE_OBJECT_PROPERTY, true)
                + ont.getAxiomCount(AxiomType.SUB_PROPERTY_CHAIN_OF, true)
                + ont.getAxiomCount(AxiomType.SUBCLASS_OF, true)
                + ont.getAxiomCount(AxiomType.EQUIVALENT_CLASSES, true)
                + ont.getAxiomCount(AxiomType.DISJOINT_CLASSES, true)
                + ont.getAxiomCount(AxiomType.EQUIVALENT_OBJECT_PROPERTIES,
                        true)
                + ont.getAxiomCount(AxiomType.DATA_PROPERTY_RANGE, true);

        int workDone = 0;

        for (OWLDeclarationAxiom a : ont.getAxioms(AxiomType.DECLARATION, true)) {
            OWLEntity ent = a.getEntity();
            if (ent.isOWLClass()) {
                factory.getConcept(ent.asOWLClass().toStringID());
            } else if (ent.isOWLObjectProperty()) {
                factory.getRole(ent.asOWLObjectProperty().toStringID());
            } else if (ent.isOWLDataProperty()) {
                factory.getFeature(ent.asOWLDataProperty().toStringID());
            }
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLDataPropertyRangeAxiom a : ont.getAxioms(
                AxiomType.DATA_PROPERTY_RANGE, true)) {
            dprAxioms.add(a);
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLSubPropertyChainOfAxiom a : ont.getAxioms(
                AxiomType.SUB_PROPERTY_CHAIN_OF, true)) {
            axioms.add(transformOWLSubPropertyChainOfAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLSubObjectPropertyOfAxiom a : ont.getAxioms(
                AxiomType.SUB_OBJECT_PROPERTY, true)) {
            axioms.add(transformOWLSubObjectPropertyOfAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLReflexiveObjectPropertyAxiom a : ont.getAxioms(
                AxiomType.REFLEXIVE_OBJECT_PROPERTY, true)) {
            axioms.add(transformOWLReflexiveObjectPropertyAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLTransitiveObjectPropertyAxiom a : ont.getAxioms(
                AxiomType.TRANSITIVE_OBJECT_PROPERTY, true)) {
            axioms.add(transformOWLTransitiveObjectPropertyAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLSubClassOfAxiom a : ont.getAxioms(AxiomType.SUBCLASS_OF, true)) {
            axioms.add(transformOWLSubClassOfAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLEquivalentClassesAxiom a : ont.getAxioms(
                AxiomType.EQUIVALENT_CLASSES, true)) {
            axioms.addAll(transformOWLEquivalentClassesAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLDisjointClassesAxiom a : ont.getAxioms(
                AxiomType.DISJOINT_CLASSES, true)) {
            axioms.add(transformOWLDisjointClassesAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        for (OWLEquivalentObjectPropertiesAxiom a : ont.getAxioms(
                AxiomType.EQUIVALENT_OBJECT_PROPERTIES, true)) {
            axioms.addAll(transformOWLEquivalentObjectPropertiesAxiom(a));
            workDone++;
            monitor.reasonerTaskProgressChanged(workDone, totalAxioms);
        }

        // TODO: deal with other axioms types even if Snorocket does not
        // currently support them
        monitor.reasonerTaskStopped();
        return axioms;
    }

    /**
     * 
     * @param l
     * @return
     */
    private AbstractLiteral getLiteral(OWLLiteral l) {
        OWLDatatype dt = l.getDatatype();
        String literal = l.getLiteral();

        AbstractLiteral res = null;

        if (dt.isBoolean()) {
            res = new BooleanLiteral(Boolean.parseBoolean(literal));
        } else if (dt.isDouble()) {
            res = new DoubleLiteral(Double.parseDouble(literal));
        } else if (dt.isFloat()) {
            res = new FloatLiteral(Float.parseFloat(literal));
        } else if (dt.isInteger()) {
            res = new IntegerLiteral(Integer.parseInt(literal));
        } else if (dt.isRDFPlainLiteral()) {
            res = new StringLiteral(literal);
        } else {
            OWL2Datatype odt = dt.getBuiltInDatatype();
            switch (odt) {
            case XSD_LONG:
                res = new LongLiteral(Long.parseLong(literal));
                break;
            case XSD_DATE_TIME:
                res = new DateLiteral(DatatypeConverter.parseDateTime(literal));
                break;
            default:
                throw new IllegalArgumentException("Unsupported literal " + l);
            }
        }

        return res;
    }

    /**
     * Determines if the datatype specified in a property is compatible with an
     * actual datatype.
     * 
     * @param propertyType
     * @param actualType
     * @return
     */
    private boolean compatibleTypes(OWLDatatype propertyType,
            OWLDatatype actualType) {
        OWL2Datatype pt = propertyType.getBuiltInDatatype();
        OWL2Datatype at = actualType.getBuiltInDatatype();
        Set<OWL2Datatype> compatible = types.get(pt);
        boolean res = false;
        if (compatible != null && compatible.contains(at)) {
            res = true;
        }
        return res;
    }

    /**
     * 
     * @param desc
     * @return
     */
    private AbstractConcept getConcept(OWLClassExpression desc) {
        final Stack<AbstractConcept> stack = new Stack<AbstractConcept>();
        desc.accept(new OWLClassExpressionVisitor() {

            private void unimplemented(OWLClassExpression e) {
                System.err.println("not implemented: " + e);
            }

            private AbstractConcept pop() {
                return stack.pop();
            }

            private void push(AbstractConcept concept) {
                stack.push(concept);
            }

            public void visit(OWLDataMaxCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLDataExactCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLDataMinCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLDataHasValue e) {
                OWLDataPropertyExpression dpe = e.getProperty();
                // TODO: consider the case where dpe is anonymous
                OWLDataProperty dp = dpe.asOWLDataProperty();
                OWLLiteral l = e.getValue();
                OWLDatatype type = l.getDatatype();

                // Check for inconsistencies
                for (OWLDataPropertyRangeAxiom a : dprAxioms) {
                    OWLDataPropertyExpression pe = a.getProperty();
                    OWLDataRange r = a.getRange();
                    // TODO: check DataOneOf
                    // TODO: check OWLDataIntersectionOf
                    OWLDatatype otype = r.asOWLDatatype();

                    if (!pe.isAnonymous()) {
                        OWLDataProperty odp = pe.asOWLDataProperty();

                        if (dp.equals(odp)) {
                            boolean compatible = compatibleTypes(otype, type);
                            if (!compatible) {
                                // throw new InconsistentOntologyException();
                                problems.add("The literal value restriction "
                                        + e + " is inconsistent with the data "
                                        + "property range axiom " + a);
                            }
                        }
                    } else {
                        System.err.println("Found anonymous data property "
                                + "expression in data property range axiom: "
                                + pe);
                    }
                }

                int f = factory.getFeature(dp.toStringID());
                push(new Datatype(f, Datatype.OPERATOR_EQUALS, getLiteral(l)));
            }

            public void visit(OWLDataAllValuesFrom e) {
                unimplemented(e);
            }

            public void visit(OWLDataSomeValuesFrom e) {
                // TODO: also support this for concrete domains
                unimplemented(e);
            }

            public void visit(OWLObjectOneOf e) {
                // TODO: implement to support EL profile
                unimplemented(e);
            }

            public void visit(OWLObjectHasSelf e) {
                // TODO: implement to support EL profile
                unimplemented(e);
            }

            public void visit(OWLObjectMaxCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLObjectExactCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLObjectMinCardinality e) {
                unimplemented(e);
            }

            public void visit(OWLObjectHasValue e) {
                // TODO: implement to support EL profile
                unimplemented(e);
            }

            public void visit(OWLObjectAllValuesFrom e) {
                unimplemented(e);
            }

            public void visit(OWLObjectSomeValuesFrom e) {
                int r = factory.getRole(e.getProperty().asOWLObjectProperty()
                        .toStringID());
                e.getFiller().accept(this);
                push(new Existential(r, pop()));
            }

            public void visit(OWLObjectComplementOf e) {
                unimplemented(e);
            }

            public void visit(OWLObjectUnionOf e) {
                unimplemented(e);
            }

            public void visit(OWLObjectIntersectionOf e) {
                List<AbstractConcept> items = new ArrayList<AbstractConcept>();

                for (OWLClassExpression desc : e.getOperands()) {
                    desc.accept(this);
                    items.add(pop());
                }

                Conjunction conj = new Conjunction(items);
                push(conj);
            }

            public void visit(OWLClass e) {
                String id = e.toStringID();
                if ("<http://www.w3.org/2002/07/owl#Thing>".equals(id)
                        || "http://www.w3.org/2002/07/owl#Thing".equals(id))
                    id = IFactory.TOP;
                if ("<http://www.w3.org/2002/07/owl#Nothing>".equals(id)
                        || "http://www.w3.org/2002/07/owl#Nothing".equals(id))
                    id = IFactory.BOTTOM;

                push(new Concept(factory.getConcept(id)));
            }

        });

        if (stack.size() != 1) {
            throw new RuntimeException("Stack size should be 1 but is "
                    + stack.size());
        }

        return stack.pop();
    }

    /**
     * Clears all the state in the importer.
     */
    public void clear() {
        dprAxioms.clear();
        problems.clear();
    }

    /**
     * @return the problems
     */
    public List<String> getProblems() {
        return problems;
    }

}
