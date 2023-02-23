/*
 *                         _ _        ____  ____
 *               _____   _(_) |_ __ _|  _ \| __ )
 *              / _ \ \ / / | __/ _` | | | |  _ \
 *             |  __/\ V /| | || (_| | |_| | |_) |
 *              \___| \_/ |_|\__\__,_|____/|____/
 *
 *   Copyright (c) 2023
 *
 *   Licensed under the Business Source License, Version 1.1 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   https://github.com/FgForrest/evitaDB/blob/main/LICENSE
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package io.evitadb.index.facet;

import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.facet.FacetGroupFormula;
import io.evitadb.query.algebra.facet.FacetGroupOrFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.test.Entities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link FacetIndex} class.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FacetIndexTest {
	private final Function<Serializable, TriFunction<Integer, int[], Bitmap[], FacetGroupFormula>> fct =
		entityType -> (groupId, facetIds, bitmaps) -> new FacetGroupOrFormula(entityType, groupId, facetIds, bitmaps);
	private FacetIndex facetIndex;

	@BeforeEach
	void setUp() {
		facetIndex = new FacetIndex();
		facetIndex.addFacet(new EntityReference(Entities.BRAND, 1), new GroupEntityReference("anything", 1), 1);
		facetIndex.addFacet(new EntityReference(Entities.BRAND, 2), new GroupEntityReference("anything", 2), 2);
		facetIndex.addFacet(new EntityReference(Entities.BRAND, 1), new GroupEntityReference("anything", 1), 3);
		facetIndex.addFacet(new EntityReference(Entities.BRAND, 3), new GroupEntityReference("anything", 3), 4);
		facetIndex.addFacet(new EntityReference(Entities.STORE, 1), new GroupEntityReference("anything", 1), 5);
		facetIndex.addFacet(new EntityReference(Entities.PARAMETER, 1), null, 100);
		facetIndex.addFacet(new EntityReference(Entities.PARAMETER, 1), null, 101);
		facetIndex.addFacet(new EntityReference(Entities.PARAMETER, 2), null, 102);
	}

	@Test
	void shouldReturnFacetingEntityTypes() {
		final Set<Serializable> referencedEntities = facetIndex.getReferencedEntities();
		assertEquals(3, referencedEntities.size());
		assertTrue(referencedEntities.contains(Entities.BRAND));
		assertTrue(referencedEntities.contains(Entities.STORE));
		assertTrue(referencedEntities.contains(Entities.PARAMETER));
	}

	@Test
	void shouldReturnFacetingEntityIds() {
		final List<FacetGroupFormula> brandReferencingEntityIds = facetIndex.getFacetReferencingEntityIdsFormula(
			Entities.BRAND, fct.apply(Entities.BRAND), 1
		);

		assertEquals(1, brandReferencingEntityIds.size());
		assertArrayEquals(new int[]{1, 3}, brandReferencingEntityIds.get(0).compute().getArray());

		final List<FacetGroupFormula> storeReferencingEntityIds = facetIndex.getFacetReferencingEntityIdsFormula(
			Entities.STORE, fct.apply(Entities.STORE), 1
		);

		assertEquals(1, storeReferencingEntityIds.size());
		assertArrayEquals(new int[]{5}, storeReferencingEntityIds.get(0).compute().getArray());

		assertEquals(0, facetIndex.getFacetReferencingEntityIdsFormula(Entities.BRAND, fct.apply(Entities.BRAND), 8).size());
		assertEquals(0, facetIndex.getFacetReferencingEntityIdsFormula(Entities.STORE, fct.apply(Entities.STORE), 8).size());
		assertEquals(0, facetIndex.getFacetReferencingEntityIdsFormula(Entities.CATEGORY, fct.apply(Entities.CATEGORY), 1).size());
	}

	@Test
	void shouldMaintainCorrectGroups() {
		assertEquals(
			"BRAND:\n" +
				"\tGROUP 1:\n" +
				"\t\t1: [1, 3]\n" +
				"\tGROUP 2:\n" +
				"\t\t2: [2]\n" +
				"\tGROUP 3:\n" +
				"\t\t3: [4]\n" +
				"STORE:\n" +
				"\tGROUP 1:\n" +
				"\t\t1: [5]\n" +
				"PARAMETER:\n" +
				"\t[NO_GROUP]:\n" +
				"\t\t1: [100, 101]\n" +
				"\t\t2: [102]",
			facetIndex.toString()
		);
	}

	@Test
	void shouldInsertNewFacetingEntityId() {
		facetIndex.resetDirty();
		assertEquals(0, facetIndex.getModifiedStorageParts(1).size());

		facetIndex.addFacet(new EntityReference(Entities.BRAND, 2), new GroupEntityReference("anything", 2), 8);

		final List<FacetGroupFormula> brandReferencingEntityIds = facetIndex.getFacetReferencingEntityIdsFormula(
			Entities.BRAND, fct.apply(Entities.BRAND), 2
		);
		assertArrayEquals(new int[]{2, 8}, FormulaFactory.and(brandReferencingEntityIds.toArray(Formula[]::new)).compute().getArray());
		assertEquals(1, facetIndex.getModifiedStorageParts(1).size());
	}

	@Test
	void shouldRemoveExistingFacetingEntityId() {
		facetIndex.resetDirty();
		assertEquals(0, facetIndex.getModifiedStorageParts(1).size());

		facetIndex.removeFacet(new EntityReference(Entities.BRAND, 2), 2, 2);

		final List<FacetGroupFormula> brandReferencingEntityIds = facetIndex.getFacetReferencingEntityIdsFormula(
			Entities.BRAND, fct.apply(Entities.BRAND), 1
		);
		assertArrayEquals(new int[]{1, 3}, FormulaFactory.and(brandReferencingEntityIds.toArray(Formula[]::new)).compute().getArray());
		assertEquals(1, facetIndex.getModifiedStorageParts(1).size());
	}

	@Disabled("This infinite test performs random operations on hierarchy index and plain hierarchy structure and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random(254);
		final int maxEntityTypes = 3;
		final int maxNodes = 50;
		final AtomicReference<FacetIndex> transactionalRef = new AtomicReference<>(new FacetIndex());
		final AtomicReference<Map<EntityReference, int[][]>> nextBaseToCompare = new AtomicReference<>(new LinkedHashMap<>());
		final StringBuilder ops = new StringBuilder("final FacetIndex facetIndex = new FacetIndex();\n");

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalRef.get(),
				original -> {
					final int operationsInTransaction = rnd.nextInt(10);
					for (int i = 0; i < operationsInTransaction; i++) {
						final FacetIndex index = transactionalRef.get();
						final Map<EntityReference, int[][]> baseStructure = nextBaseToCompare.get();
						final Serializable[] entityTypes = baseStructure.keySet().stream().map(EntityReference::getType).distinct().toArray(Serializable[]::new);

						final int entityTypesLength = index.getReferencedEntities().size();
						final int totalCount = index.getSize();
						final int operation = rnd.nextInt(2);
						if (totalCount < maxNodes && (operation == 0 || totalCount < 10)) {
							final Serializable entityType = entityTypesLength < maxEntityTypes ?
								Entities.values()[rnd.nextInt(Entities.values().length)] :
								entityTypes[rnd.nextInt(entityTypes.length)];
							final int groupId = rnd.nextInt(10) + 1;
							// insert new item
							int newEntityId;
							int newReferencedId;
							boolean retry;
							do {
								newReferencedId = rnd.nextInt(maxNodes / 2);
								newEntityId = rnd.nextInt(maxNodes * 2);
								int finalNewEntityId = newEntityId;
								retry = ofNullable(baseStructure.get(new EntityReference(entityType, newReferencedId)))
									.map(it -> ArrayUtils.contains(it[0], finalNewEntityId))
									.orElse(false);
							} while (retry);

							ops.append("facetIndex.addFacet(new EntityReference(\"")
								.append(entityType).append("\", ")
								.append(newReferencedId).append("), ")
								.append(newEntityId)
								.append(");\n");

							try {
								final EntityReference referenceKey = new EntityReference(entityType, newReferencedId);
								index.addFacet(referenceKey, new GroupEntityReference("anything", groupId), newEntityId);
								baseStructure.merge(
									referenceKey,
									new int[][]{{newEntityId}, {groupId}},
									(oldOnes, newOnes) -> {
										final int addedEntityId = newOnes[0][0];
										final int addedGroupId = newOnes[1][0];
										final InsertionPosition insertionPosition = ArrayUtils.computeInsertPositionOfIntInOrderedArray(addedEntityId, oldOnes[0]);
										Assert.isTrue(!insertionPosition.isAlreadyPresent(), "Record should not be present!");
										final int[] newEntityIds = ArrayUtils.insertIntIntoArrayOnIndex(addedEntityId, oldOnes[0], insertionPosition.getPosition());
										final int[] newGroupIds = ArrayUtils.insertIntIntoArrayOnIndex(addedGroupId, oldOnes[1], insertionPosition.getPosition());
										return new int[][]{newEntityIds, newGroupIds};
									}
								);
							} catch (Exception ex) {
								fail(ex.getMessage() + "\n" + ops, ex);
							}
						} else {
							// remove existing item
							final EntityReference entityReference = new ArrayList<>(baseStructure.keySet()).get(rnd.nextInt(baseStructure.size()));
							final int[][] entityIds = baseStructure.get(entityReference);
							final int rndNo = rnd.nextInt(entityIds[0].length);
							final int entityIdToRemove = entityIds[0][rndNo];
							final int groupIdToRemove = entityIds[1][rndNo];

							ops.append("facetIndex.removeFacet(\"")
								.append(entityReference.getType()).append("\", ")
								.append(entityReference.getPrimaryKey()).append("), ")
								.append(entityIdToRemove)
								.append(");\n");

							try {
								index.removeFacet(entityReference, groupIdToRemove, entityIdToRemove);
								final int[] newEntityIds = ArrayUtils.removeIntFromArrayOnIndex(entityIds[0], rndNo);
								final int[] newGroupIds = ArrayUtils.removeIntFromArrayOnIndex(entityIds[1], rndNo);
								if (ArrayUtils.isEmpty(newEntityIds)) {
									baseStructure.remove(entityReference);
								} else {
									baseStructure.put(entityReference, new int[][]{newEntityIds, newGroupIds});
								}
							} catch (Exception ex) {
								fail(ex.getMessage() + "\n" + ops, ex);
							}
						}
					}
				},
				(original, committed) -> {
					final Map<EntityReference, int[][]> baseStructure = nextBaseToCompare.get();
					final String realToString = committed.toString();
					final String expectedToString = toString(baseStructure);
					assertEquals(
						expectedToString, realToString,
						"\nExpected: " + expectedToString + "\n" +
							"Actual:   " + committed + "\n\n" +
							ops
					);

					for (Entry<EntityReference, int[][]> entry : baseStructure.entrySet()) {
						final int[][] value = entry.getValue();
						for (int i = 0; i < value[1].length; i++) {
							int expectedFacetGroup = value[1][i];
							final EntityReference facetRef = entry.getKey();
							assertTrue(
								committed.isFacetInGroup(
									facetRef.getType(),
									expectedFacetGroup,
									facetRef.getPrimaryKey()
								),
								"Facet " + facetRef.getPrimaryKey() + " is not present in group " + expectedFacetGroup +
									" for facet entity type " + facetRef.getType() + "!"
							);
						}
					}

					transactionalRef.set(committed);

					ops.setLength(0);
					ops.append("final FacetIndex facetIndex = new FacetIndex();\n")
						.append(
							baseStructure
								.keySet()
								.stream()
								.sorted()
								.map(it -> {
										final StringBuilder innerSb = new StringBuilder();
										final int[][] entityIds = baseStructure.get(it);
										for (int i = 0; i < entityIds[0].length; i++) {
											int entityId = entityIds[0][i];
											int groupId = entityIds[1][i];
											innerSb.append("facetIndex.addFacet(new EntityReference(\"")
												.append(it.getType()).append("\",").append(it.getPrimaryKey()).append("), ")
												.append(groupId).append(", ").append(entityId).append(");\n");
										}
										return innerSb.toString();
									}
								)
								.collect(Collectors.joining())
						);
				});

			if (iteration++ % 100 == 0) {
				System.out.print(".");
				System.out.flush();
			}
			if (iteration % 5_000 == 0) {
				System.out.print("\n");
				System.out.flush();
			}
		} while (true);
	}

	private String toString(Map<EntityReference, int[][]> baseStructure) {
		final StringBuilder sb = new StringBuilder();
		final Map<Serializable, List<EntityReference>> references = baseStructure.keySet().stream().collect(Collectors.groupingBy(EntityReference::getType));
		references.keySet().stream().sorted().forEach(it -> {
			sb.append(it).append(":\n");
			final List<EntityReference> entityReferences = references.get(it);
			final Map<Integer, Map<Integer, int[]>> groupsFacetsIx = new TreeMap<>();
			for (EntityReference ref : entityReferences) {
				final int[][] data = baseStructure.get(ref);
				for (int i = 0; i < data[0].length; i++) {
					final int entityId = data[0][i];
					final Map<Integer, int[]> groupIndex = groupsFacetsIx.computeIfAbsent(data[1][i], gId -> new TreeMap<>());
					groupIndex.merge(
						ref.getPrimaryKey(),
						new int[]{entityId},
						(oldOnes, newOnes) -> ArrayUtils.insertIntIntoOrderedArray(newOnes[0], oldOnes)
					);
				}
			}

			groupsFacetsIx
				.forEach((key, value) -> {
					sb.append("\t").append("GROUP ").append(key).append(":\n");
					value.forEach((fct, eId) -> sb.append("\t\t").append(fct).append(": ")
						.append(Arrays.toString(eId)).append("\n"));

				});
		});
		if (sb.length() > 0) {
			while (sb.charAt(sb.length() - 1) == '\n') {
				sb.deleteCharAt(sb.length() - 1);
			}
		}
		return sb.toString();
	}

}