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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
//import java.util.logging.Logger;

import org.semanticweb.owlapi.reasoner.NullReasonerProgressMonitor;
import org.semanticweb.owlapi.reasoner.ReasonerProgressMonitor;

import au.csiro.snorocket.core.util.DenseConceptMap;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.SparseConceptHashSet;
import au.csiro.snorocket.core.util.SparseConceptMap;

/**
 * Builds the taxonomy based on the result of the classification process.
 * 
 * @author Alejandro Metke
 *
 */
public class PostProcessedData {

    //private static final Logger LOGGER = Snorocket.getLogger();
    
    // Map of concepts to the node in the resulting taxonomy
 	private IConceptMap<ClassNode> conceptNodeIndex;
    
    public PostProcessedData() {
    	
    }
    
    /**
     * Indicates if cn2 is a parent of cn.
     * 
     * @param cn
     * @param cn2
     * @return
     */
    private boolean isParent(ClassNode cn, ClassNode cn2) {
    	Set<ClassNode> allParents = new HashSet<>();
    	Queue<ClassNode> queue = new LinkedList<>();
    	queue.addAll(cn.getParents());
    	while(!queue.isEmpty()) {
    		ClassNode tcn = queue.poll();
    		allParents.add(tcn);
    		Set<ClassNode> parents = tcn.getParents();
    		if(parents != null) queue.addAll(tcn.getParents());
    	}
    	if(allParents.contains(cn2))
    		return true;
    	else
    		return false;
    }
    
    /**
     * Removes indirect parents based on current taxonomy.
     * 
     * @param set
     */
    private void filterIndirect(IConceptSet set) {
    	Set<Integer> toRemove = new HashSet<>();
    	
    	int[] ids = set.toArray();
    	for(int i = 0; i < ids.length; i++) {
    		if(toRemove.contains(ids[i])) continue;
    		for(int j = i+1; j < ids.length; j++) {
    			if(toRemove.contains(ids[j])) continue;
    			ClassNode cn1 = conceptNodeIndex.get(ids[i]);
    			ClassNode cn2 = conceptNodeIndex.get(ids[j]);
    			// if cn2 is parent of cn1 then we remove cn2
    			if(isParent(cn1,  cn2)) {
    				toRemove.add(ids[j]);
    			} else if(isParent(cn2, cn1)) {
    				toRemove.add(ids[i]);
    			}
    		}
    	}
    	
    	for(Integer i : toRemove) {
    		set.remove(i.intValue());
    	}
    }
    
    /**
     * Computes and incremental DAG based on the subsumptions for concepts in
     * the new axioms.
     * 
     * @param factory The factory.
     * @param subsumptions The subsumptions for the new or modified concepts 
     * only.
     * @param monitor
     */
    public void computeDagIncremental(final IFactory factory, 
    		final IConceptMap<IConceptSet> newConceptSubs,
    		final IConceptMap<IConceptSet> affectedConceptSubs,
    		ReasonerProgressMonitor monitor) {
    	// Keep only the subsumptions that involve real atomic concepts
    	IConceptMap<IConceptSet> cis = new SparseConceptMap<IConceptSet>(
				newConceptSubs.size());
		
		for(IntIterator itr = newConceptSubs.keyIterator(); itr.hasNext(); ) {
			final int X = itr.next();
            if(!factory.isVirtualConcept(X)) {
            	IConceptSet set = new SparseConceptHashSet();
            	cis.put(X, set);
				for(IntIterator it = newConceptSubs.get(X).iterator(); 
						it.hasNext() ; ) {
					int next = it.next();
					if(!factory.isVirtualConcept(next)) {
						set.add(next);
					}
				}
            }
		}
		
		for(IntIterator itr = affectedConceptSubs.keyIterator(); 
				itr.hasNext(); ) {
			final int X = itr.next();
            if(!factory.isVirtualConcept(X)) {
            	IConceptSet set = new SparseConceptHashSet();
            	cis.put(X, set);
				for(IntIterator it = affectedConceptSubs.get(X).iterator(); 
						it.hasNext() ; ) {
					int next = it.next();
					if(!factory.isVirtualConcept(next)) {
						set.add(next);
					}
				}
            }
		}
		
		// Build equivalent and direct concept sets
    	IConceptMap<IConceptSet> equiv = new SparseConceptMap<IConceptSet>(
    			factory.getTotalConcepts());
        IConceptMap<IConceptSet> direc = new SparseConceptMap<IConceptSet>(
    			factory.getTotalConcepts());
        
		for(IntIterator itr = cis.keyIterator(); itr.hasNext(); ) {
			final int a = itr.next();
            
            for(IntIterator itr2 = cis.get(a).iterator(); itr2.hasNext(); ) {
            	int c = itr2.next();
            	IConceptSet cs = cis.get(c);
            	
            	if(c == IFactory.BOTTOM_CONCEPT) {
            		addToSet(equiv, a, c);
            	} else if(cs != null && cs.contains(a)) {
            		addToSet(equiv, a, c);
            	} else {
            		boolean isDirect = true;
            		IConceptSet d = direc.get(a);
					if(d != null) {
						IConceptSet toRemove = new SparseConceptHashSet();
						for(IntIterator itr3 = d.iterator(); itr3.hasNext(); ) {
							int b = itr3.next();
							IConceptSet bs = cis.get(b);
							if(bs != null && bs.contains(c)) {
								isDirect = false;
								break;
							}
							if(cs != null && cs.contains(b)) {
								toRemove.add(b);
							}
						}
						d.removeAll(toRemove);
					}
					if(isDirect) {
						addToSet(direc, a, c);
					}
            	}
            }
		}
		
		Set<ClassNode> cns = new HashSet<>();
		
		// Introduce one taxonomy node for each distinct class of equivalent
		// concepts
		for(IntIterator it = equiv.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			IConceptSet equivs = equiv.get(key);
			// Check if any of the equivalent classes is already part of an
			// equivalent node
			ClassNode n = null;
			for(IntIterator it2 = equivs.iterator(); it2.hasNext(); ) {
				int e = it2.next();
				if(conceptNodeIndex.containsKey(e)) {
					n = conceptNodeIndex.get(e);
					break;
				}
			}
			
			if(n == null) {
				n = new ClassNode();
			}
			n.getEquivalentConcepts().add(key);
			n.getEquivalentConcepts().addAll(equivs);
			for(IntIterator it2 = equivs.iterator(); it2.hasNext(); ) {
				int e = it2.next();
				conceptNodeIndex.put(e, n);
			}
			cns.add(n);
		}
		
		// Remove indirect parents from direct parents set using current
		// taxonomy
		for(IntIterator it = direc.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			filterIndirect(direc.get(key));
		}
		
		// TODO: run and try this
		
		ClassNode top = conceptNodeIndex.get(Factory.TOP_CONCEPT);
		ClassNode bottom = conceptNodeIndex.get(Factory.BOTTOM_CONCEPT);
		
		// Connect the nodes according to the direct super-concept relationships
		Set<ClassNode> processed = new HashSet<>();
		for(ClassNode node : cns) {
			if(processed.contains(node) || node == top || node == bottom) 
				continue;
			processed.add(node);
			for(IntIterator it2 = node.getEquivalentConcepts().iterator(); 
					it2.hasNext(); ) {
				int c = it2.next();
				// Get direct superconcepts
				IConceptSet dc = direc.get(c);
				if(dc != null) {
					for(IntIterator it3 = dc.iterator(); it3.hasNext(); ) {
						int d = it3.next();
						ClassNode parent = (ClassNode)conceptNodeIndex.get(d);
						if(parent != null) {
							node.getParents().add(parent);
							parent.getChildren().add(node);
						}
					}
				}
			}
		}
		
		// Add bottom
		for(ClassNode node : cns) {
			if(node.getEquivalentConcepts().contains(Factory.BOTTOM_CONCEPT)) 
				continue;
			if(node.getChildren().isEmpty()) {
				bottom.getParents().add(node);
				node.getChildren().add(bottom);
			}
		}
		
		// Add top
		for(ClassNode node : cns) {
			if(node.getParents().isEmpty()) {
				node.getParents().add(top);
				top.getChildren().add(node);
			}
		}
		
		equiv = null;
		direc = null;
    }
    
    public void computeDag(final IFactory factory, 
    		final IConceptMap<IConceptSet> subsumptions, 
    		ReasonerProgressMonitor monitor) {
    	if(monitor == null) monitor = new NullReasonerProgressMonitor();
    	conceptNodeIndex = new DenseConceptMap<>(factory.getTotalConcepts());
		
		// Keep only the subsumptions that involve real atomic concepts
		IConceptMap<IConceptSet> cis = new SparseConceptMap<IConceptSet>(
				factory.getTotalConcepts());
		
		for(IntIterator itr = subsumptions.keyIterator(); itr.hasNext(); ) {
			final int X = itr.next();
            if(!factory.isVirtualConcept(X)) {
            	IConceptSet set = new SparseConceptHashSet();
            	cis.put(X, set);
				for(IntIterator it = subsumptions.get(X).iterator(); 
						it.hasNext() ; ) {
					int next = it.next();
					if(!factory.isVirtualConcept(next)) {
						set.add(next);
					}
				}
            }
		}
		
		int totalWork = cis.size();
		int workDone = 0;

    	IConceptMap<IConceptSet> equiv = new SparseConceptMap<IConceptSet>(
    			factory.getTotalConcepts());
        IConceptMap<IConceptSet> direc = new SparseConceptMap<IConceptSet>(
    			factory.getTotalConcepts());
		
		// Build equivalent and direct concept sets
		for(IntIterator itr = cis.keyIterator(); itr.hasNext(); ) {
			final int a = itr.next();
            
            for(IntIterator itr2 = cis.get(a).iterator(); itr2.hasNext(); ) {
            	int c = itr2.next();
            	IConceptSet cs = cis.get(c);
            	
            	if(c == IFactory.BOTTOM_CONCEPT) {
            		addToSet(equiv, a, c);
            	} else if(cs != null && cs.contains(a)) {
            		addToSet(equiv, a, c);
            	} else {
            		boolean isDirect = true;
            		IConceptSet d = direc.get(a);
					if(d != null) {
						IConceptSet toRemove = new SparseConceptHashSet();
						for(IntIterator itr3 = d.iterator(); itr3.hasNext(); ) {
							int b = itr3.next();
							IConceptSet bs = cis.get(b);
							if(bs != null && bs.contains(c)) {
								isDirect = false;
								break;
							}
							if(cs != null && cs.contains(b)) {
								toRemove.add(b);
							}
						}
						d.removeAll(toRemove);
					}
					if(isDirect) {
						addToSet(direc, a, c);
					}
            	}
            	
            	workDone++;
				monitor.reasonerTaskProgressChanged(workDone, totalWork);
            }
		}
		
		int bottomConcept = Factory.BOTTOM_CONCEPT;
		if(!equiv.containsKey(bottomConcept)) {
			addToSet(equiv, bottomConcept, bottomConcept);
		}
		
		int topConcept = Factory.TOP_CONCEPT;
		if(!equiv.containsKey(topConcept)) {
			addToSet(equiv, topConcept, topConcept);
		}
		
		monitor.reasonerTaskStopped();
		monitor.reasonerTaskStarted("Building taxonomy");
		
		totalWork = (conceptNodeIndex.size() * 3) + equiv.size();
		workDone = 0;
		
		// Introduce one taxonomy node for each distinct class of equivalent
		// concepts
		ClassNode top = null;
		ClassNode bottom = null;
		
		for(IntIterator it = equiv.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			IConceptSet equivs = equiv.get(key);
			// Check if any of the equivalent classes is already part of an
			// equivalent node
			ClassNode n = null;
			for(IntIterator it2 = equivs.iterator(); it2.hasNext(); ) {
				int e = it2.next();
				if(conceptNodeIndex.containsKey(e)) {
					n = conceptNodeIndex.get(e);
					break;
				}
			}
			
			if(n == null) {
				n = new ClassNode();
			}
			n.getEquivalentConcepts().add(key);
			n.getEquivalentConcepts().addAll(equivs);
			for(IntIterator it2 = equivs.iterator(); it2.hasNext(); ) {
				int e = it2.next();
				if(e == Factory.TOP_CONCEPT) top = n;
				if(e == Factory.BOTTOM_CONCEPT) bottom = n;
				conceptNodeIndex.put(e, n);
			}
			
			totalWork++;
			monitor.reasonerTaskProgressChanged(workDone, totalWork);
		}
		
		// Connect the nodes according to the direct super-concept relationships
		Set<ClassNode> processed = new HashSet<>();
		for(IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			ClassNode node = (ClassNode)conceptNodeIndex.get(key);
			if(processed.contains(node) || node == top || node == bottom) 
				continue;
			processed.add(node);
			for(IntIterator it2 = node.getEquivalentConcepts().iterator(); 
					it2.hasNext(); ) {
				int c = it2.next();
				// Get direct superconcepts
				IConceptSet dc = direc.get(c);
				if(dc != null) {
					for(IntIterator it3 = dc.iterator(); it3.hasNext(); ) {
						int d = it3.next();
						ClassNode parent = (ClassNode)conceptNodeIndex.get(d);
						if(parent != null) {
							node.getParents().add(parent);
							parent.getChildren().add(node);
						}
					}
				}
			}
			totalWork++;
			monitor.reasonerTaskProgressChanged(workDone, totalWork);
		}
		processed = null;
		
		// Add bottom
		if(bottom == null) {
			bottom = new ClassNode();
			bottom.getEquivalentConcepts().add(bottomConcept);
			conceptNodeIndex.put(bottomConcept, bottom);
		}
		
		for(IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			if(key == Factory.BOTTOM_CONCEPT) continue;
			ClassNode node = (ClassNode)conceptNodeIndex.get(key);
			if(node.getEquivalentConcepts().contains(Factory.BOTTOM_CONCEPT)) 
				continue;
			if(node.getChildren().isEmpty()) {
				bottom.getParents().add(node);
				node.getChildren().add(bottom);
			}
			totalWork++;
			monitor.reasonerTaskProgressChanged(workDone, totalWork);
		}
		
		// Add top
		if(top == null) {
			top = new ClassNode();
			top.getEquivalentConcepts().add(topConcept);
			conceptNodeIndex.put(topConcept, top);
		}
		
		for(IntIterator it = conceptNodeIndex.keyIterator(); it.hasNext(); ) {
			int key = it.next();
			
			if(key == Factory.BOTTOM_CONCEPT || key == Factory.TOP_CONCEPT)
				continue;
			
			ClassNode node = (ClassNode)conceptNodeIndex.get(key);
			if(node.getParents().isEmpty()) {
				node.getParents().add(top);
				top.getChildren().add(node);
			}
			totalWork++;
			monitor.reasonerTaskProgressChanged(workDone, totalWork);
		}
		
		equiv = null;
		direc = null;
		monitor.reasonerTaskStopped();
    }
    
    public void computeDeltaDag(final IFactory factory, 
    		final IConceptMap<IConceptSet> subsumptions, 
    		final IConceptMap<IConceptSet> baseSubsumptions,
    		ReasonerProgressMonitor monitor) {
    	// TODO: implement - used by SNAPI
    }
    
    public IConceptMap<IConceptSet> getParents(final IFactory factory) {
    	IConceptMap<IConceptSet> res = new DenseConceptMap<IConceptSet>(
    			factory.getTotalConcepts());
    	for(IntIterator it = getConceptIterator(); it.hasNext(); ) {
    		int c = it.next();
    		ClassNode eq = getEquivalents(c);
    		for(ClassNode pn : eq.getParents()) {
    			for(IntIterator it2 = pn.getEquivalentConcepts().iterator(); 
    					it2.hasNext(); ) {
    				addToSet(res, c, it2.next());
    			}
    		}
    	}
    	return res;
    }
    
    private void addToSet(IConceptMap<IConceptSet> map, int key, int val) {
    	IConceptSet set = map.get(key);
    	if(set == null) {
    		set = new SparseConceptHashSet();
    		map.put(key, set);
    	}
    	set.add(val);
    }

    public ClassNode getEquivalents(int concept) {
    	return conceptNodeIndex.get(concept);
    }
    
    public IntIterator getConceptIterator() {
    	return conceptNodeIndex.keyIterator();
    }
    
    public boolean hasData() {
    	return conceptNodeIndex != null;
    }
}
