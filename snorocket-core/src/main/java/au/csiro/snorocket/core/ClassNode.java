/**
 * Copyright CSIRO Australian e-Health Research Centre (http://aehrc.com).
 * All rights reserved. Use is subject to license terms and conditions. 
 */
package au.csiro.snorocket.core;

import java.util.HashSet;
import java.util.Set;

import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.SparseConceptSet;

/**
 * Represents a node in the taxonomy generated after classifying an ontology.
 * 
 * @author Alejandro Metke
 *
 */
public class ClassNode {
	private final IConceptSet equivalentConcepts = new SparseConceptSet();
	private final Set<ClassNode> parents = new HashSet<>();
	private final Set<ClassNode> children = new HashSet<>();
	
	/**
	 * @return the equivalentConcepts
	 */
	public IConceptSet getEquivalentConcepts() {
		return equivalentConcepts;
	}
	
	/**
	 * @return the parents
	 */
	public Set<ClassNode> getParents() {
		return parents;
	}
	
	/**
	 * @return the children
	 */
	public Set<ClassNode> getChildren() {
		return children;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int size = equivalentConcepts.size();
		int i = 0;
		sb.append("{");
		for(IntIterator it = equivalentConcepts.iterator(); it.hasNext(); ) {
			sb.append(it.next());
			if(++i < size) sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}
	
}
