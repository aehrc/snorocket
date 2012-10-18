/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core.util;

import java.io.File;
import java.util.List;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAnonymousIndividual;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDeclarationAxiom;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLObjectIntersectionOf;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLObjectSomeValuesFrom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLSubClassOfAxiom;
import org.semanticweb.owlapi.model.OWLSubObjectPropertyOfAxiom;
import org.semanticweb.owlapi.model.OWLSubPropertyChainOfAxiom;

/**
 * @author Alejandro Metke
 * 
 */
public class DebugUtils {

    public static String formatAxiom(OWLAxiom a, OWLOntology ont) {
        if (a instanceof OWLSubPropertyChainOfAxiom) {
            // TODO: implement
            return a.toString();
        } else if (a instanceof OWLSubObjectPropertyOfAxiom) {
            // TODO: implement
            return a.toString();
        } else if (a instanceof OWLSubClassOfAxiom) {
            OWLSubClassOfAxiom sc = (OWLSubClassOfAxiom) a;
            return formatClassExpression(sc.getSubClass(), ont) + " \u2286 "
                    + formatClassExpression(sc.getSuperClass(), ont);
        } else if (a instanceof OWLEquivalentClassesAxiom) {
            OWLEquivalentClassesAxiom ec = (OWLEquivalentClassesAxiom) a;
            List<OWLClassExpression> ces = ec.getClassExpressionsAsList();
            StringBuilder sb = new StringBuilder();
            sb.append(formatClassExpression(ces.get(0), ont));
            for (int i = 1; i < ces.size(); i++) {
                sb.append(" = ");
                sb.append(formatClassExpression(ces.get(i), ont));
            }
            return sb.toString();
        } else if (a instanceof OWLDeclarationAxiom) {
            OWLDeclarationAxiom da = (OWLDeclarationAxiom) a;
            OWLEntity ent = da.getEntity();
            String name = (ent.isOWLClass()) ? getLabel(ent.asOWLClass(), ont)
                    : ent.toString();
            return "init(" + name + ")";
        } else {
            return a.toString();
        }
    }

    public static String formatClassExpression(OWLClassExpression ce,
            OWLOntology ont) {
        if (ce instanceof OWLClass) {
            return getLabel(ce.asOWLClass(), ont);
        } else if (ce instanceof OWLObjectIntersectionOf) {
            OWLObjectIntersectionOf oi = (OWLObjectIntersectionOf) ce;
            List<OWLClassExpression> ops = oi.getOperandsAsList();
            StringBuilder sb = new StringBuilder();
            sb.append(formatClassExpression(ops.get(0), ont));
            for (int i = 1; i < ops.size(); i++) {
                sb.append(" \u2229 ");
                sb.append(formatClassExpression(ops.get(i), ont));
            }
            return sb.toString();
        } else if (ce instanceof OWLObjectSomeValuesFrom) {
            OWLObjectSomeValuesFrom osv = (OWLObjectSomeValuesFrom) ce;
            OWLObjectPropertyExpression ope = osv.getProperty();
            String role = (!ope.isAnonymous()) ? getLabel(
                    ope.asOWLObjectProperty(), ont) : ope.toString();
            return "\u2203" + role + ".("
                    + formatClassExpression(osv.getFiller(), ont) + ")";
        } else {
            return ce.toString();
        }
    }

    public static String getLabel(OWLEntity e, OWLOntology ont) {
        for (OWLAnnotation an : e.getAnnotations(ont)) {
            if (an.getProperty().isLabel()) {
                OWLAnnotationValue val = an.getValue();

                if (val instanceof IRI) {
                    return ((IRI) val).toString();
                } else if (val instanceof OWLLiteral) {
                    OWLLiteral lit = (OWLLiteral) val;
                    return lit.getLiteral();
                } else if (val instanceof OWLAnonymousIndividual) {
                    OWLAnonymousIndividual ind = (OWLAnonymousIndividual) val;
                    return ind.toStringID();
                } else {
                    throw new RuntimeException("Unexpected class "
                            + val.getClass());
                }
            }
        }
        return e.toStringID();
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String[] vals = new String[] { "http://www.ihtsdo.org/SCT_424226004" };

        try {
            File stated = new File(
                    "src/test/resources/snomed_20110731_stated.owl");
            IRI iriStated = IRI.create(stated.getAbsoluteFile());
            OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
            OWLOntology ont = manager.loadOntology(iriStated);
            for (String val : vals) {
                OWLClass cl = manager.getOWLDataFactory().getOWLClass(
                        IRI.create(val));
                System.out.println(val + " -> " + DebugUtils.getLabel(cl, ont)
                        + " -> " + ont.getReferencingAxioms(cl).size());
                /*
                 * OWLEntity cl = manager.getOWLDataFactory().getOWLEntity(
                 * EntityType.OBJECT_PROPERTY, IRI.create(val));
                 * System.out.println(val+" -> "+
                 * ont.getReferencingAxioms(cl).size());
                 */

                for (OWLAxiom a : ont.getReferencingAxioms(cl)) {
                    AxiomType<?> type = a.getAxiomType();
                    if (type == AxiomType.SUB_OBJECT_PROPERTY)
                        System.out.println("  " + a);
                }

                System.out.println("--------------------------------");

                for (OWLAxiom a : ont.getReferencingAxioms(cl)) {
                    // AxiomType<?> type = a.getAxiomType();
                    // if(type == AxiomType.SUB_OBJECT_PROPERTY)
                    System.out.println("  " + DebugUtils.formatAxiom(a, ont));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
