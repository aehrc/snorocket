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

package au.csiro.snorocket.snapi;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import au.csiro.snorocket.core.Factory;
import au.csiro.snorocket.core.IFactory;
import au.csiro.snorocket.core.NormalisedOntology;
import au.csiro.snorocket.core.ParseException;
import au.csiro.snorocket.core.PostProcessedData;
import au.csiro.snorocket.core.R;
//import au.csiro.snorocket.core.NormalisedOntology.Classification;
import au.csiro.snorocket.core.axioms.GCI;
import au.csiro.snorocket.core.axioms.Inclusion;
import au.csiro.snorocket.core.axioms.RI;
import au.csiro.snorocket.core.model.AbstractConcept;
import au.csiro.snorocket.core.model.Concept;
import au.csiro.snorocket.core.model.Conjunction;
import au.csiro.snorocket.core.model.Existential;
import au.csiro.snorocket.core.util.IConceptMap;
import au.csiro.snorocket.core.util.IConceptSet;
import au.csiro.snorocket.core.util.IntIterator;
import au.csiro.snorocket.core.util.RoleSet;
import au.csiro.snorocket.core.util.SparseConceptSet;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IAttribute;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IAttributeGroup;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IAttributeName;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IConcept;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IExpression;
import au.csiro.snorocket.snapi.SnomedExpressionParser.IRefinement;

public class Snorocket implements I_Snorocket {

	// Increment 3rd place for upwards/backwards compatible change
	// Increment 2nd place for upwards compatible change
	// Increment 1st place for incompatible change
	private static final String FILE_VERSION = "1.0.0";

	private static final int INCLUSIVE = 0;
	private static final int EXCLUSIVE = 1;

	private static final Logger LOGGER = au.csiro.snorocket.core.Snorocket
			.getLogger();

	private String isaId;

	//private Classification classification = null;
	private PostProcessedData postProcessedData = new PostProcessedData();

	final private List<Row> rowList = new ArrayList<Row>();

	transient private int isa;
	
	transient public String rootConceptStr = null;

	transient private IConceptSet ungroupedRoles = new SparseConceptSet();

	/**
	* The Concept ids for the roots of the role hierarchy.
	* roleRoots[INCLUSIVE] and roleRoots[EXCLUSIVE]
	*/
	transient protected Set[] roleRoots = {new HashSet<Integer>(), new HashSet<Integer>()};

	transient private Collection<RI> roleCompositions = new ArrayList<RI>();

	//transient final private Classification baseClassification;

	transient final protected IFactory factory;

	transient final private int ROLE_GROUP;

	transient private IConceptSet fullyDefined = new SparseConceptSet();

	transient private int nestedRoleGroupCount;
	
	public Snorocket() {
        LOGGER.info("::: Snorocket()");
		//baseClassification = null;
		factory = new Factory();
		ROLE_GROUP = factory.getRole("roleGroup");

		// :DEBUG:BEGIN:!!!: open UUID dump files
		if (isDebugDumping()) {
		    try {
		        bwDebugCons = new BufferedWriter(new FileWriter("RocketInputCons_compare.txt"));
		        bwDebugRels = new BufferedWriter(new FileWriter("RocketInputRels_compare.txt"));
		        LOGGER.info("::: opened RocketInput*_compare.txt files");
		    } catch (IOException e1) {
		        e1.printStackTrace();
		    }
		    // :DEBUG:END: 
		}
	}

	// called by createExtension
	/*
	protected Snorocket(final Classification classification, String isaId) {
        LOGGER.info("::: Snorocket(final Classification classification, String isaId)");
		if (null == classification) {
			throw new IllegalArgumentException("classification can not be null");
		}

		baseClassification = classification;
		factory = baseClassification.getExtensionFactory();
		ROLE_GROUP = factory.getRole("roleGroup");
		setIsa(isaId);
	}
	*/

	/**
	 * Pre-load from stored state
	 * 
	 * @param snomedVersion
	 * @throws RuntimeException
	 *             if version is not available
	 */
	/*
	public Snorocket(InputStream state) {
        LOGGER.info("::: Snorocket(InputStream state)");
        
		try {
			baseClassification = null;
			final BufferedReader reader = new BufferedReader(
					new InputStreamReader(state));

			classification = NormalisedOntology.loadClassification(reader);
			factory = classification.getExtensionFactory();
			ROLE_GROUP = factory.getRole("roleGroup");

			final String isaConcept = reader.readLine();
			setIsa(isaConcept);

			String line = reader.readLine();
			if (null != line) {
				
				if (!line.contains(".")) {	  // no '.'s means not a version string
					// Handle deprecated file format in an unreleased version.
					// TODO consider removing code for this rare case at end of 2009
					final int roleRootCount = Integer.parseInt(line);
					for (int i = 0; i < roleRootCount; i++) {
						String roleRoot = reader.readLine();
						addRoleRoot(roleRoot, false);
					}
				} else {
					// check compatible version
					if (!FILE_VERSION.equals(line)) {
						// TODO - choose a better exception to throw
						throw new Error("Malformed SNOMED Resource: Unsupported file format version, found " + line + ", expected " + FILE_VERSION + " or compatible.");
					}

					{   // ungrouped roles
						line = reader.readLine();
						final int count = Integer.parseInt(line);
						for (int i = 0; i < count; i++) {
							String role = reader.readLine();
							ungroupedRoles.add(Integer.parseInt(role));
						}
					}

					{   // role roots INCLUSIVE
						line = reader.readLine();
						final int count = Integer.parseInt(line);
						for (int i = 0; i < count; i++) {
							line = reader.readLine();
							roleRoots[INCLUSIVE].add(Integer.parseInt(line));
						}
					}
					{   // role roots EXCLUSIVE
						line = reader.readLine();
						final int count = Integer.parseInt(line);
						for (int i = 0; i < count; i++) {
							line = reader.readLine();
							roleRoots[EXCLUSIVE].add(Integer.parseInt(line));
						}
					}

				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new Error("Malformed SNOMED Resource: "
					+ e.getLocalizedMessage(), e);
		} catch (ParseException e) {
			e.printStackTrace();
			throw new Error("Malformed SNOMED Resource: "
					+ e.getLocalizedMessage(), e);
		}
	}
	*/

	public void setIsa(String id) {
		if (null == id) {
			throw new IllegalArgumentException("Isa id cannot be null");
		}
		addConcept(id, false); // ensure it exists - clients can still make it
		// fully-defined with a later call
		isaId = id;
		isa = factory.getRole(id);
	}

	public void addRoleNeverGrouped(String id) {
		ungroupedRoles.add(factory.getRole(id));
	}

	public void addRoleRoot(String id, boolean inclusive) {
		if (inclusive) {
			roleRoots[INCLUSIVE].add(factory.getConcept(id));
		} else {
			roleRoots[EXCLUSIVE].add(factory.getConcept(id));
		}
	}

	public void addConcept(String conceptId, boolean fullyDefined) {
		addConcept(conceptId, fullyDefined, false);
	}

	private void addConcept(String conceptId, boolean fullyDefined,
			boolean isVirtual) {
		final int concept = factory.getConcept(conceptId);
		if (isVirtual) {
			factory.setVirtualConcept(concept, true);
		}
		if (fullyDefined) {
			this.fullyDefined.add(concept);
		}
        // :DEBUG:!!!: write added concept to debug dump file
        if (bwDebugCons != null)
            try {
                bwDebugCons.write(getSnomedUuid(conceptId) + "\t"
                        + fullyDefined + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
	}

	public void addConcept(final String conceptId, boolean fullyDefined,
			String expression) {
		addConcept(conceptId, fullyDefined);
		final SnomedExpressionParser parser = new SnomedExpressionParser(
				expression);

		parser.parseExpression(new ExpressionMatcher(conceptId));
	}

	public void addRelationship(String conceptId1, String relId,
			String conceptId2, int group) {
		int ok = 0;
		if (!checkConceptExists("Concept1", conceptId1)) {
			ok += 1;
		}
		if (!checkRoleExists("Relationship", relId)) {
			ok += 2;
		}
		if (!checkConceptExists("Concept2", conceptId2)) {
			ok += 4;
		}

		if (ok > 0) {
			String message = String
					.format(
							"Warning, relationship: %s %s %s %d includes undefined concepts/role: ",
							conceptId1, relId, conceptId2, group);
			if ((ok & 1) > 0)
				message = message + "'" + conceptId1 + "' ";
			if ((ok & 2) > 0)
				message = message + "'" + relId + "' ";
			if ((ok & 4) > 0)
				message = message + "'" + conceptId2 + "'";
			LOGGER.info(message);
			// return;
		}
		final int c1 = factory.getConcept(conceptId1);
		final int rel = factory.getRole(relId);
		final int c2 = factory.getConcept(conceptId2);

		// TODO make this check more efficient (cache the subsumptions)
		/*if (null != baseClassification
				&& baseClassification.getSubsumptions().containsKey(c1)) {
			throw new IllegalArgumentException(
					"Cannot add new relationships for concepts defined in base ontology: "
							+ conceptId1);
		}*/

		rowList.add(new Row(c1, rel, c2, group));
		
		// :DEBUG:!!!: write added relationship to debug dump file
		if (bwDebugRels != null)
            try {
                bwDebugRels.write(getSnomedUuid(conceptId1) + "\t" + getSnomedUuid(relId) + "\t"
                        + getSnomedUuid(conceptId2) + "\r\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
	}

	public void addRoleComposition(String[] lhsIds, String rhsId) {
		final int[] lhs = new int[lhsIds.length];
		final int rhs = factory.getRole(rhsId);
		
		for (int i = 0; i < lhsIds.length; i++) {
			lhs[i] = factory.getRole(lhsIds[i]);
		}
		
		roleCompositions.add(new RI(lhs, rhs));
	}

	private boolean checkConceptExists(final String name, String conceptId) {
		if (!factory.conceptExists(conceptId)) {
			// LOGGER.warning(name + ": " + conceptId + " was not defined.");
			return false;
		}
		return true;
	}

	private boolean checkRoleExists(final String name, String conceptId) {
		// FIXME? no API to define roles
		// if (!factory.roleExists(conceptId)) {
		if (!factory.conceptExists(conceptId)) {
			// LOGGER.warning(name + ": " + conceptId + " was not defined.");
			return false;
		}
		return true;
	}

	public void classify() {
	    // :DEBUG:!!!: close uuid dump files
	    try {
	        if (bwDebugCons != null)
	            bwDebugCons.close();
	        if (bwDebugRels != null)
	            bwDebugRels.close();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }

		long start = System.currentTimeMillis();
		final NormalisedOntology ontology = populateOntology();
        LOGGER.info("populate time: " + (System.currentTimeMillis() - start)
				/ 1000.0 + "s");
		start = System.currentTimeMillis();
		//classification = ontology.getClassification();
		LOGGER.info("classify time: " + (System.currentTimeMillis() - start)
				/ 1000.0 + "s");
	}

	// ----------------------------------------------------------------

	private class Populater {

		public final Set<Inclusion> ontology = new HashSet<Inclusion>();

		private int currentId = Integer.MIN_VALUE;
		private int currentGroup = 0;
		private int lhs = -1;
		private List<AbstractConcept> rhs = null;
		private List<Existential> rhsGroup = null;

		/**
		 * 
		 * @param totalConcepts
		 * @param rootConcepts
		 */
		public Populater(final int totalConcepts, final int[] rootConcepts) {
			final Row[] rowArray = rowList.toArray(new Row[rowList.size()]);
			Arrays.sort(rowArray);

			for (Row row : rowArray) {
				processConceptRow(row);
				}
			
			if (null != rhs) {
				addGroup(rhs, rhsGroup);
				store(ontology, lhs, rhs);
			}

			ontology.addAll(roleCompositions);
			
            // :!!!:zzz:
//            LOGGER.info("::: roleCompositions.size()" + 
//            		" --> SIZE= " + roleCompositions.size());
            // :!!!:zzz:
//            LOGGER.info("::: ontology.addAll(roleCompositions)... ontology" + 
//            		" --> SIZE= " + ontology.size());

            // We have to loop here in case processRoleRow(...) identifies extra concepts
			// to treat as roles and thus changes the outcome of the roleExists tests.
			//
			int extraRoleCount;
			do {
				int roleMax = factory.getTotalRoles();
				
				for (Row row: rowArray) {
					final String concept1 = factory.lookupConceptId(row.concept1);
					final String concept2 = factory.lookupConceptId(row.concept2);

					if (factory.roleExists(concept1) || factory.roleExists(concept2)) {
						processRoleRow(row);
					}
				}

				extraRoleCount = factory.getTotalRoles() - roleMax;
                // :!!!:zzz:
//                LOGGER.info("::: extraRoleCount" + 
//                		" --> == " + extraRoleCount);
			} while (extraRoleCount > 0);

            // :!!!:zzz:
//            LOGGER.info("::: (Row row : rowArray)... ontology" + 
//            		" --> SIZE= " + ontology.size());
		}

		private void processRoleRow(final Row row) {
			if (isa == row.role) {
				if (roleRoots[INCLUSIVE].contains(row.concept1) || roleRoots[EXCLUSIVE].contains(row.concept1)) {
					// The child is a roleRoot so the inheritance from its parent
					// is not a subRole relationship
					return;
				}
				if (roleRoots[EXCLUSIVE].contains(row.concept2)) {
					// The parent is a roleRoot but not a role so the inheritance to its child
					// is not a subRole relationship
					return;
				}
				// EKM
				LOGGER.info("Role inclusion: "
						+ factory.lookupConceptId(row.concept1) + " "
						+ factory.lookupConceptId(row.concept2));
				int[] lhs = { factory.getRole(factory
						.lookupConceptId(row.concept1)) };
				int rhs = factory
						.getRole(factory.lookupConceptId(row.concept2));
				ontology.add(new RI(lhs, rhs));
			} else {
				throw new AssertionError(
						"only valid relationship for roles is 'is a', not "
								+ row.role + " (role is " + row.concept1 + ")");
			}
		}

		private void processConceptRow(final Row row) {
			if (row.concept1 < currentId) {
				throw new AssertionError("concept1 mis-sorted; expected >= "
						+ currentId + ", got " + row.concept1);
			}

			if (row.group != 0 && ungroupedRoles.contains(row.role)) {
				throw new AssertionError("Role " + factory.lookupRoleId(row.role) + " is marked as never grouped, but occurs in a grouped relationship: " + row.toString(factory));
			}

			// check for beginning of a concept definition
			if (row.concept1 != currentId) {
				// check that there was a previous concept whose definition
				// needs storing
				if (null != rhs) {
					// store definition
					addGroup(rhs, rhsGroup);
					store(ontology, lhs, rhs);
				}
				currentId = row.concept1;
				currentGroup = row.group;
				lhs = row.concept1;
				rhs = new ArrayList<AbstractConcept>();
				rhsGroup = new ArrayList<Existential>();
			} else if (row.group < currentGroup) {
				throw new AssertionError("group mis-sorted; expected >= "
						+ currentGroup + ", got " + row.group);
			}

			final Concept c2 = new Concept(row.concept2);

			if (isa == row.role) {
				assert row.group == 0;

				rhs.add(c2);
			} else {
				if ((row.group == 0 || row.group > currentGroup)
						&& rhsGroup.size() > 0) {
					addGroup(rhs, rhsGroup);
					rhsGroup = new ArrayList<Existential>();
				}
				currentGroup = row.group;

				rhsGroup.add(new Existential(row.role, c2));
			}
		}

		private void addGroup(final List<AbstractConcept> rhs,
				final List<Existential> rhsGroup) {
			if (rhsGroup.size() > 0) {
				final AbstractConcept groupConcept = getConcept(rhsGroup);

				if (groupConcept instanceof Existential &&
					ungroupedRoles.contains(((Existential) groupConcept).getRole())) {
					
					rhs.add(groupConcept);
				} else {
					rhs.add(new Existential(ROLE_GROUP, groupConcept));
				}
			}
		}

		private AbstractConcept getConcept(final List<? extends AbstractConcept> rhs) {
			if (rhs.size() > 1) {
				return new Conjunction(rhs);
			} else {
				return rhs.get(0);
			}
		}

		private void store(final Set<Inclusion> ontology, final int lhs,
				final List<AbstractConcept> rhsList) {
			final AbstractConcept rhs = getConcept(rhsList);
			final GCI gci = new GCI(lhs, rhs);

			// if (isDebugging()) {
			// StringBuilder sb = new StringBuilder();
			// sb.append(factory.lookupConceptId(lhs)).append(" [ ");
			// p(sb, rhs);
			// LOGGER.info(sb.toString());
			// }

			// System.err.print(factory.lookupConceptId(lhs) + "\t[ ");
			// p(rhs);
			// System.err.println();

			ontology.add(gci);

			if (fullyDefined.contains(lhs)) {
				ontology.add(new GCI(rhs, lhs));
			}
		}

	}

	private NormalisedOntology populateOntology() {
		final Set<Inclusion> ontology = getInclusions();

        // :!!!:zzz:
//        LOGGER.info("::: Set<Inclusion> ontology = getInclusions(); --> SIZE= " + ontology.size());
		
		/*if (null != baseClassification) {
			//return baseClassification.getExtensionOntology(factory, ontology);
			return new NormalisedOntology(factory, ontology);
		} else {
			return new NormalisedOntology(factory, ontology);
		}*/
		return null;
	}

	protected Set<Inclusion> getInclusions() {
		if (null == isaId) {
			throw new AssertionError(
					"No ISA id has been specified with setIsa(String id).");
		}

		// If not expressly set, then assume SnomedID
		if (rootConceptStr == null)
			rootConceptStr = "138875005";  
		
		final int[] rootConcepts = { factory.getConcept(rootConceptStr) };

		return new Populater(factory.getTotalConcepts(), rootConcepts).ontology;
	}

	// private void p(final StringBuilder sb, final AbstractConcept ac) {
	// if (ac instanceof Concept) {
	// sb.append(factory.lookupConceptId(((Concept) ac).hashCode()));
	// } else if (ac instanceof Existential) {
	// final Existential e = (Existential) ac;
	// sb.append(factory.lookupRoleId(e.getRole())).append(".");
	// p(sb, e.getConcept());
	// } else {
	// final Conjunction c = (Conjunction) ac;
	// sb.append("(");
	// AbstractConcept[] conjuncts = c.getConcepts();
	// for (int i = 0; i < conjuncts.length; i++) {
	// p(sb, conjuncts[i]);
	// if ((i + 1) < conjuncts.length) {
	// sb.append(" + ");
	// }
	// }
	// sb.append(")");
	// }
	// }

	private final class ExpressionMatcher implements IExpression {

		final String rootConcept;

		private ExpressionMatcher(String conceptId) {
			assert null != conceptId;

			rootConcept = conceptId;
		}

		public IConcept conceptMatcher() {
			return new IConcept() {
				public void matchConcept(final String conceptId,
						final String term) {
					addRelationship(rootConcept, isaId, conceptId, 0);
				}
			};
		}

		public IRefinement refinementMatcher() {
			return new RefinementMatcher();
		}

		public void matchExpression() {
		}

		private final class RefinementMatcher implements IRefinement {
			int currentGroup = 0;

			public IAttribute attributeMatcher() {
				return new AttributeMatcher(0);
			}

			public IAttributeGroup attributeGroupMatcher() {
				return new IAttributeGroup() {
					public IAttribute attributeMatcher() {
						return new AttributeMatcher(++currentGroup);
					}
				};
			}

			private final class AttributeMatcher implements IAttribute {
				private int group = 0;

				private String relationship;

				public AttributeMatcher(int group) {
					this.group = group;
				}

				public IAttributeName attributeNameMatcher() {
					return new IAttributeName() {
						public void matchAttribute(String attributeId,
								String term) {
							relationship = attributeId;
						}
					};
				}

				public IConcept conceptMatcher() {
					return new IConcept() {
						public void matchConcept(final String conceptId,
								final String term) {
							addRelationship(rootConcept, relationship,
									conceptId, group);
						}
					};
				}

				public IExpression expressionMatcher() {
					final String newConcept = UUID.randomUUID().toString();
					addConcept(newConcept, true, true);
					addRelationship(rootConcept, relationship, newConcept,
							group);
					return new ExpressionMatcher(newConcept);
				}

			}

		}

	}

	final protected static class Row implements Comparable<Row> {

		final int concept1;
		final int role;
		final int concept2;
		final int group;

		// Row(final String concept1, final String role, final String concept2,
		// final int group) {
		// this.concept1 = Concept.getInstance(concept1).hashCode();
		// this.role = factory.getRole(role).hashCode();
		// this.concept2 = Concept.getInstance(concept2).hashCode();
		// this.group = group;
		// }

		Row(int concept1, int role, int concept2, int group) {
			this.concept1 = concept1;
			this.role = role;
			this.concept2 = concept2;
			this.group = group;
		}

		@Override
		public int hashCode() {
			return (concept1 ^ role ^ concept2 ^ group);
		}

		@Override
		public boolean equals(Object o) {
			Row other = (Row) o;
			return concept1 == other.concept1 && concept2 == other.concept2
					&& role == other.role && group == other.group;
		}

		public int compareTo(Row other) {
			return concept1 == other.concept1 ? (group == other.group ? (role == other.role ? (compareTo(
					concept2, other.concept2))
					: compareTo(role, other.role))
					: compareTo(group, other.group))
					: compareTo(concept1, other.concept1);
		}

		private static int compareTo(int lhs, int rhs) {
			return lhs < rhs ? -1 : (lhs > rhs ? 1 : 0);
		}

		public String toString(IFactory factory) {
			return factory.lookupConceptId(concept1) + ",\t" + factory.lookupRoleId(role) + ",\t" + factory.lookupConceptId(concept2) + ",\t" + group;
		}

		@Override
		public String toString() {
			return concept1 + ",\t" + role + ",\t" + concept2 + ",\t" + group;
		}

	}

	/**
	 * Only need to record role and value since concept is constant and always
	 * known in context.
	 * 
	 * @author law223
	 */
	final private static class Rel implements Comparable<Rel> {
		final int role;
		final int concept2;

		Rel(final int role, final int concept2) {
			this.role = role;
			this.concept2 = concept2;
		}

		@Override
		public int hashCode() {
			return (role ^ concept2);
		}

		@Override
		public boolean equals(Object o) {
			Rel other = (Rel) o;
			return concept2 == other.concept2 && role == other.role;
		}

		public int compareTo(Rel other) {
			return role == other.role ? (compareTo(concept2, other.concept2))
					: compareTo(role, other.role);
		}

		private static int compareTo(int lhs, int rhs) {
			return lhs < rhs ? -1 : (lhs > rhs ? 1 : 0);
		}

		@Override
		public String toString() {
			return role + ",\t" + concept2;
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * <p>
	 * <em><strong>Note</strong>, results are currently undefined for incremental classification.</em>
	 * 
	 * @see {@link I_Snorocket#getEquivalents(au.csiro.snorocket.snapi.I_Snorocket.I_EquivalentCallback)}
	 */
	public void getEquivalents(I_EquivalentCallback callback) {
		PostProcessedData ppd = getPostProcessedData();
		for (final IntIterator keyItr = ppd.getConceptIterator(); 
				keyItr.hasNext();) {
			final int key = keyItr.next();

			if (skip(key)) {
				continue;
			}
			
			final IConceptSet equivalentsConceptSet = 
					ppd.getEquivalents(key).getEquivalentConcepts();

			if (equivalentsConceptSet.size() > 0) {
				final Collection<String> equivalentConcepts = 
						new ArrayList<String>();
				equivalentConcepts.add(factory.lookupConceptId(key));

				for (final IntIterator valItr = equivalentsConceptSet
						.iterator(); valItr.hasNext();) {
					final int val = valItr.next();

					if (skip(val)) {
						continue;
					}

					equivalentConcepts.add(factory.lookupConceptId(val));
				}

				if (equivalentConcepts.size() > 1) {
					callback.equivalent(equivalentConcepts);
				}
			}
		}
	}

	public void getRelationships(I_Callback callback) {
		returnRelationships(callback, false);
	}

	public void getDistributionFormRelationships(final I_Callback callback) {
		returnRelationships(callback, true);
	}

	private void returnRelationships(final I_Callback callback,
			final boolean filterRedundant) {
		/*if (null == classification) {
			throw new IllegalStateException("Ontology has not been classified.");
		}*/
		nestedRoleGroupCount = 0;

		/*final IConceptMap<IConceptSet> subsumptions = classification
				.getSubsumptions();
		final R rels = classification.getRelationships();
		final IConceptMap<IConceptSet> filteredSubsumptions = filterRedundant ? getPostProcessedData()
				.getParents(factory)
				: subsumptions;

		final int limit = factory.getTotalConcepts();
		for (int concept = 0; concept < limit; concept++) {

			if (skip(concept)) {
				continue;
			}

			returnIsaRelationships(callback, filteredSubsumptions, concept);

			if (!factory.isBaseConcept(concept)) {
				returnOtherRelationships(callback, classification, rels,
						concept, filterRedundant);

				// handle ROLE_GROUP special case
				final IConceptSet roleValues = rels
						.lookupB(concept, ROLE_GROUP);
				final IConceptSet candidateValues = getLeaves(roleValues);
				returnGroupedRelationships(callback, classification, rels,
						concept, candidateValues, filterRedundant);
			}

		}

		if (nestedRoleGroupCount > 0) {
			LOGGER
					.warning("SNOMED CT should not contain nested role groups, but detected "
							+ nestedRoleGroupCount);
		}*/
	}

	private PostProcessedData getPostProcessedData() {
		/*
		if (null == classification) {
			throw new IllegalStateException("Ontology has not been classified.");
		}

		final IConceptMap<IConceptSet> subsumptions = classification
				.getSubsumptions();

		if (!postProcessedData.hasData()) {
			if(null == baseClassification) {
				postProcessedData.computeDag(factory, subsumptions, null);
			} else {
				postProcessedData.computeDeltaDag(factory, 
						baseClassification.getSubsumptions(), 
						subsumptions, 
						null);
			}
		}*/

		return postProcessedData;
	}

	/**
	 * Invokes callback for all isa relationships for concept identified by key
	 * that were not part of the original stated form. That is, only returns
	 * new, inferred subsumptions.
	 * 
	 * @param callback
	 * @param dag
	 * @param concept
	 */
	private void returnIsaRelationships(final I_Callback callback,
			final IConceptMap<IConceptSet> dag, final int concept) {
		final String conceptId = factory.lookupConceptId(concept);

		final IConceptSet conceptSet = dag.get(concept);
		if (null == conceptSet) {
			return;
		}

		for (final IntIterator itr = conceptSet.iterator(); itr.hasNext();) {
			final int parent = itr.next();
			// if (isDebugging()) {
			// System.err.println("ISA: " + conceptId + " [ " +
			// factory.lookupConceptId(parent)); // FIXME delete
			// }

			if (concept == parent || skip(parent)) {
				continue;
			}

			final String parentId = factory.lookupConceptId(parent);

			callback.addRelationship(conceptId, isaId, parentId, 0);
		}
	}

	/**
	 * Invokes callback for all non-isa relationships for concept.
	 * 
	 * @param callback
	 * @param classification
	 * @param rels
	 * @param concept
	 * @param filterRedundant
	 */
	/*
	private void returnOtherRelationships(final I_Callback callback,
			final Classification classification, final R rels,
			final int concept, final boolean filterRedundant) {

		final RVGroup rvGroup = computeRoleValues(classification, rels,
				concept, filterRedundant);

		// return role values
		final String conceptId = factory.lookupConceptId(concept);

		rvGroup.map(new RVCallback() {
			public void map(int role, int value) {
				final String roleId = factory.lookupRoleId(role);
				if (!skip(value)) {
					final String valueId = factory.lookupConceptId(value);
					callback.addRelationship(conceptId, roleId, valueId, 0);
				}
			}
		});
		// for (int role = 0; role < roleValuesMap.length; role++) {
		// final String roleId = factory.lookupRoleId(role);
		// final IConceptSet values = roleValuesMap[role];
		// returnUngroupedRelationships(callback, concept, conceptId, role,
		// roleId, values);
		// }
	}
	*/

	private interface RVCallback {
		void map(int role, int value);
	}
	
	
	private class RVGroup {
		//final private Classification classification;

		final IConceptSet[] roleValuesMap = new IConceptSet[factory
				.getTotalRoles()];
		Collection<Rel> _rels = null;

		/*RVGroup(final Classification classification) {
			this.classification = classification;
		}*/

		void add(int rel, int val) {
			if (null == roleValuesMap[rel]) {
				roleValuesMap[rel] = IConceptSet.FACTORY.createConceptSet();
			}
			roleValuesMap[rel].add(val);
		}

		void map(RVCallback cb) {
			for (int role = 0; role < roleValuesMap.length; role++) {
				final IConceptSet values = roleValuesMap[role];
				if (null != values) {
					for (final IntIterator itr = values.iterator(); itr
							.hasNext();) {
						cb.map(role, itr.next());
					}
				}
			}
		}
		
		/*
		boolean containsAll(final RVGroup other) {
			for (final Rel otherRel : other.getRels()) {
				boolean contained = false;
				for (final Rel ourRel : getRels()) {
					if (contains(ourRel, otherRel)) {
						contained = true;
						break;
					}
				}
				if (!contained) {
					return false;
				}
			}

			return true;
		}
		*/
		
		/*
		void filterRedundant() {
			for (int role = 0; role < roleValuesMap.length; role++) {
				final IConceptSet values = roleValuesMap[role];
				if (null != values) {
					final RoleSet parentRoles = classification
							.getRoleClosure(role);
					for (int parentRole = parentRoles.first(); parentRole >= 0; parentRole = parentRoles
							.next(parentRole + 1)) {

						if (role == parentRole) {
							continue;
						}

						if (null != roleValuesMap[parentRole]) {

							int beforeSize = roleValuesMap[parentRole].size();
							roleValuesMap[parentRole].removeAll(values);
						}
					}
				}
			}
		}
		*/
		/*
		private boolean contains(final Rel lhs, final Rel rhs) {
			IConceptSet set;
			return (lhs.role == rhs.role || classification.getRoleClosure(
					lhs.role).contains(rhs.role))
					&& (lhs.concept2 == rhs.concept2 || (null != (set = classification
							.getSubsumptions().get(lhs.concept2)) && set
							.contains(rhs.concept2)));
		}
		*/

		private Collection<Rel> getRels() {
			if (null == _rels) {
				_rels = new ArrayList<Rel>();
				map(new RVCallback() {
					public void map(int role, int value) {
						_rels.add(new Rel(role, value));
					}
				});
			}
			return _rels;
		}

		int size() {
			return getRels().size();
		}
	}

	/**
	 * Returns a map of roles to values, possible filtering redundant values.
	 * 
	 * @param classification
	 * @param rels
	 * @param concept
	 * @param filterRedundant
	 * @return
	 */
	/*
	private RVGroup computeRoleValues(final Classification classification,
			final R rels, final int concept, final boolean filterRedundant) {
		final int maxRole = factory.getTotalRoles();

		// map from role to concept
		final RVGroup rvGroup = new RVGroup(classification);
		// final IConceptSet[] roleValuesMap = new IConceptSet[maxRole];
		// roleValuesMap[ROLE_GROUP] = IConceptSet.EMPTY_SET;

		for (int role = 0; role < maxRole; role++) {
			if (ROLE_GROUP == role) {
				continue; // Handle this outside the loop
			}

			final IConceptSet candidateValues = getLeaves(rels.lookupB(concept,
					role));

			// final int numValues = candidateValues.size();
			// if (numValues > 0) {
			// roleValuesMap[role] =
			// IConceptSet.FACTORY.createConceptSet(numValues);
			// } else {
			// roleValuesMap[role] = IConceptSet.EMPTY_SET;
			// continue;
			// }

			for (final IntIterator valueItr = candidateValues.iterator(); valueItr
					.hasNext();) {
				final int value = valueItr.next();

				if (skip(value)) {
					continue;
				}

				// We now have concept [ role.value but want to avoid also
				// returning concept [ role2.value for some role [ role2
				// roleValuesMap[role].add(value);
				rvGroup.add(role, value);
			}
		}

		if (filterRedundant) {
			// Filter redundant role values
			//rvGroup.filterRedundant();
			// for (int role = 0; role < roleValuesMap.length; role++) {
			// final IConceptSet values = roleValuesMap[role];
			// final IConceptSet parentRoles = subsumptions.get(role);
			// for (final IntIterator itr = parentRoles.iterator();
			// itr.hasNext(); ) {
			// final int parentRole = itr.next();
			//
			// int beforeSize = roleValuesMap[parentRole].size();
			// roleValuesMap[parentRole].removeAll(values);
			// if (beforeSize > roleValuesMap[parentRole].size()) {
			// System.err.println("FILTER REDUNDANT"); // FIXME delete
			// }
			// }
			// }
		}

		return rvGroup;
	}
	*/

	/**
	 * Before invoking the callback we check that the (ungrouped) relationship
	 * was not part of the stated view.
	 * 
	 * @param callback
	 * @param conceptId
	 * @param rString
	 * @param roleValues
	 */
	private void returnUngroupedRelationships(I_Callback callback,
			final int concept, final String conceptId, final int role,
			final String roleId, final IConceptSet roleValues) {
		for (final IntIterator valueItr = roleValues.iterator(); valueItr
				.hasNext();) {
			final int value = valueItr.next();

			if (skip(value)) {
				continue;
			}

			final String valueId = factory.lookupConceptId(value);
			callback.addRelationship(conceptId, roleId, valueId, 0);
		}
	}

	/**
	 * For each group of inferred relationships, we check if an identical group
	 * was part of the stated view. If so, then we skip it, otherwise we add it
	 * to the collection of newRelGroups.
	 * 
	 * Finally, for each group of inferred relationships in newRelGroups we
	 * invoke the callback.
	 * 
	 * The new groupIds are allocated from 1 more than the number of re-used
	 * ids.
	 * 
	 * Need to watch out for following case:
	 * <code>X [ rg.(r1.Y + r2.Z) + rg.(r1.Y + r3.Z) where r3 [ r2</code> since
	 * this will manifest as
	 * <code>X [ rg.(r1.Y + r2.Z) + rg.(r1.Y + r2.Z + r3.Z) where r3 [ r2</code>
	 * Code does following: <code>
	 * newRelGroups = [{r1.Y, r2.Z}]
	 * {r1.Y,r2.Z}.containsAll({r1.Y,r2.Z,r3.Z})?
	 * FALSE -> {r1.Y,r2.Z,r3.Z}.containsAll({r1.Y,r2.Z})?
	 * TRUE -> red.add({r1.Y,r2.Z})
	 * newRelGroups.removeAll(red)  // newRel = []
	 * newRelGroups = [{r1.Y, r2.Z, r3.Z}]
	 * </code> Can't filter out r2.Z until this point
	 * 
	 * @param callback
	 *            The object to send the grouped relationships to
	 * @param rels
	 *            All computed relationships
	 * @param concept
	 *            The concept that is the subject of the relationships
	 * @param roleValues
	 *            The concepts corresponding to the grouped relationships
	 *            (should all be virtual?)
	 * @param filterRedundant
	 * @param filterStated
	 */
	/*
	private void returnGroupedRelationships(final I_Callback callback,
			final Classification classification, final R rels,
			final int concept, final IConceptSet roleValues,
			final boolean filterRedundant) {

		final Collection<RVGroup> newRVGroups = new ArrayList<RVGroup>();

		int groupCount = 0;

		final String conceptId = factory.lookupConceptId(concept);
		for (final IntIterator valueItr = roleValues.iterator(); valueItr
				.hasNext();) {
			final int groupedValue = valueItr.next();

			// invariant: valueConcept is a composite concept:
			assert factory.isVirtualConcept(groupedValue);
			if (isDebugging() && !factory.isVirtualConcept(groupedValue)) {
				throw new AssertionError(
						"Internal error: non-virtual grouped concepts found: "
								+ factory.lookupConceptId(groupedValue));
			}

			// check we don't have nested role groups
			final IConceptSet nestedCandidateValues = getLeaves(rels.lookupB(
					groupedValue, ROLE_GROUP));
			if (nestedCandidateValues.size() > 0) {
				nestedRoleGroupCount++;
				if (isDebugging()) {
					handleNestedRoleGroups(conceptId, nestedCandidateValues);
				}
			}

			final RVGroup rvGroup = computeRoleValues(classification, rels,
					groupedValue, filterRedundant);

			final Collection<RVGroup> redundant = new ArrayList<RVGroup>();

			boolean duplicate = false;

			for (final RVGroup rvg : newRVGroups) {
				//if (rvg.containsAll(rvGroup)) {
				//	duplicate = true;
				//	break;
				//} else if (rvGroup.containsAll(rvg)) {
				//	redundant.add(rvg);
				//}
			}

			newRVGroups.removeAll(redundant);

			if (!duplicate) {
				newRVGroups.add(rvGroup);
			}

			groupCount++;
		}

		// calculate the initial groupId for the newly inferred relationship
		// groups
		// (need to avoid re-using group ids)
		//
		// int groupId = groupCount; // - newRelGroups.size();

		for (RVGroup rvGroup : newRVGroups) {
			// If there's only one item in a group, don't "group" it.
			final int groupId = rvGroup.size() > 1 ? groupCount++ : 0;

			rvGroup.map(new RVCallback() {
				public void map(int role, int value) {
					callback.addRelationship(conceptId, factory
							.lookupRoleId(role),
							factory.lookupConceptId(value), groupId);
				}
			});
			// for (Rel rel: group) {
			// callback.addRelationship(conceptId,
			// factory.lookupRoleId(rel.role),
			// factory.lookupConceptId(rel.concept2), groupId);
			// }
		}
	}
	*/

	private void handleNestedRoleGroups(final String conceptId,
			final IConceptSet nestedCandidateValues) {
		StringBuilder detail = new StringBuilder(conceptId);
		detail.append(" [ ");
		for (final IntIterator nestedValueItr = nestedCandidateValues
				.iterator(); nestedValueItr.hasNext();) {
			final int nestedValue = nestedValueItr.next();

			detail.append("  <");
			detail.append(factory.lookupConceptId(nestedValue));
			detail.append(">");
		}

		// throw new
		// AssertionError("SNOMED should not contain nested role groups:" +
		// detail);
		LOGGER.warning("SNOMED should not contain nested role groups: "
				+ detail.toString());
	}

	/**
	 * Given a set of concepts, computes the subset such that no member of the
	 * subset is subsumed by another member.
	 * 
	 * result = {c | c in bs and not c' in b such that c' [ c}
	 * 
	 * @param s
	 *            subsumption relationships
	 * @param bs
	 *            set of subsumptions to filter
	 * @return
	 */
	private IConceptSet getLeaves(final IConceptSet bs) {
		/*
		final IConceptMap<IConceptSet> subsumptions = classification
				.getSubsumptions();
		final IConceptMap<IConceptSet> baseSubsumptions = null == baseClassification ? null
				: baseClassification.getSubsumptions();

		final IConceptSet leafBs = IConceptSet.FACTORY.createConceptSet(bs);

		for (final IntIterator bItr = bs.iterator(); bItr.hasNext();) {
			final int b = bItr.next();

			final IConceptSet ancestors = IConceptSet.FACTORY
					.createConceptSet(subsumptions.get(b));
			if (null != baseSubsumptions) {
				final IConceptSet set = baseSubsumptions.get(b);
				if (null != set) {
					ancestors.addAll(set);
				}
			}
			ancestors.remove(b);
			leafBs.removeAll(ancestors);
		}
		return leafBs;
		*/
		return null;
	}

	private boolean skip(int id) {
		return (id == Factory.TOP_CONCEPT || id == Factory.BOTTOM_CONCEPT || factory
				.isVirtualConcept(id));
	}
	
	
	public InputStream getStream() throws IOException {
		final PipedInputStream result = new PipedInputStream();
		final PipedOutputStream out = new PipedOutputStream(result);

		new Thread(new Runnable() {
			public void run() {
				final PrintWriter printWriter = new PrintWriter(out);
				try {
					//classification.printClassification(printWriter);

					printWriter.println(isaId);
					printWriter.println(FILE_VERSION);
					
					// ungrouped roles
					printWriter.println(ungroupedRoles.size());
					for (final IntIterator itr = ungroupedRoles.iterator(); itr.hasNext(); ) {
						final int role = itr.next();
						printWriter.println(role);
					}

					// role roots
					printWriter.println(roleRoots[INCLUSIVE].size());
					for (Integer role: (Set<Integer>) roleRoots[INCLUSIVE]) {
						printWriter.println(factory.lookupConceptId(role));
					}
					printWriter.println(roleRoots[EXCLUSIVE].size());
					for (Integer role: (Set<Integer>) roleRoots[EXCLUSIVE]) {
						printWriter.println(factory.lookupConceptId(role));
					}
					
				} catch (Throwable t) {
					t.printStackTrace();
				} finally {
					printWriter.close();
				}
			}
		}).start();

		return result;
	}

	public I_Snorocket createExtension() {
		//return createExtension(classification, isaId);
		return null;
	}
	
	/*
	protected I_Snorocket createExtension(Classification classification,
			String isaId) {
		//return new Snorocket(classification, isaId);
		return null;
	}
	*/

	static boolean isDebugging() {
		return au.csiro.snorocket.core.Snorocket.DEBUGGING;
	}
	
    static boolean isDebugDumping() {
        return au.csiro.snorocket.core.Snorocket.DEBUG_DUMP;
    }

    BufferedWriter bwDebugCons = null;  // :DEBUG:
	BufferedWriter bwDebugRels = null;  // :DEBUG:

	private static final String encoding = "8859_1";

	private UUID getSnomedUuid(String id) {
	    String name = "org.snomed." + id;
	    try {
	        return UUID.nameUUIDFromBytes(name.getBytes(encoding));
	    } catch (UnsupportedEncodingException e) {
	        // TODO Auto-generated catch block
	        e.printStackTrace();
	        return null;
	    }
	}


}
