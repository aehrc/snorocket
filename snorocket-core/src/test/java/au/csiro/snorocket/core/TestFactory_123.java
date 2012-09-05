/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;

import au.csiro.snorocket.core.axioms.Inclusion_123;
import au.csiro.snorocket.core.importer.RF1Importer_123;

/**
 * @author Alejandro Metke
 *
 */
public class TestFactory_123 {
	
	final static String TEST_DIR = "src/test/resources/";
	
	/**
	 * Tests the Factory_123 without preallocating the concepts and roles 
	 * arrays.
	 */
	@Test
	public void testGetConceptIdx() {
		String concepts = TEST_DIR+"sct1_Concepts_Core_INT_20110731.txt";
		String relations = TEST_DIR+
				"res1_StatedRelationships_Core_INT_20110731.txt";
		System.out.println("Classifying ontology from "+concepts);
		int[] conceptArray = new int[3];
		int[] roleArray = new int[3];
		IFactory_123 factory = new Factory_123(conceptArray, 2, roleArray, 0);
        RF1Importer_123 imp = new RF1Importer_123(factory, concepts, relations);
        NullReasonerProgressMonitor mon = new NullReasonerProgressMonitor();
        @SuppressWarnings("unused")
		List<Inclusion_123> axioms = imp.transform(mon);
        assertTrue(factory.getConceptIdx("SCT_10001005") >= 0);
	}

}
