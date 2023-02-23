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

package one.edee.oss.pmptt.dao;

import io.evitadb.storage.CatalogContext;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.Hierarchy;
import one.edee.oss.pmptt.model.HierarchyItem;
import one.edee.oss.pmptt.model.HierarchyItemWithHistory;
import one.edee.oss.pmptt.model.SectionWithBucket;
import one.edee.oss.pmptt.spi.HierarchyChangeListener;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.util.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link HierarchyStorage} to run against PostgreSQL specifically for {@link io.evitadb.api.SqlEvita}.
 *
 * It maps {@link HierarchyItem}s to hierarchical {@link io.evitadb.api.data.structure.Entity}s with
 * {@code code} to {@code primaryKey} mapping (therefore codes have to be integers) and
 * {@link Hierarchy}s to {@link io.evitadb.api.EntityCollectionBase}s with {@code hierarchyCode} to {@code entityType}
 * mapping.
 *
 * Also, it automatically updates and deletes item references in entities where are stored bounds of
 * referenced hierarchy entities, because this is core feature used in sql evita.
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
public class SqlStorage implements HierarchyStorage {

	/**
	 * Selects whole hierarchy item
	 */
	private static final String ITEM_SELECT =
			"type, " +
			"primaryKey, " +
			"level, " +
			"leftBound, " +
			"rightBound, " +
			"numberOfChildren, " +
			"orderAmongSiblings, " +
			"hierarchyBucket as bucket";

	private final CatalogContext catalogContext;
	private final List<HierarchyChangeListener> changeListeners = new LinkedList<>();

	@Override
	public void registerChangeListener(HierarchyChangeListener listener) {
		this.changeListeners.add(listener);
	}

	@Override
	public void createHierarchy(Hierarchy hierarchy) {
		// Storing of new hierarchy is handled together with schema storing as hierarchy is represented by collection, so
		// hierarchy data are stored with schema.
	}

	@Override
	public Hierarchy getHierarchy(String serializedEntityType) {
		try {
			return catalogContext.getNpJdbcTemplate()
				.queryForObject(
					"select entityType, hierarchyLevels, hierarchySectionSize " +
							"from t_schema " +
							"where entityType = :entityType and withHierarchy = true",
					Collections.singletonMap("entityType", serializedEntityType),
					new HierarchyRowMapper(this)
				);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	/**
	 * Actual returns serialized entity types of hierarchical collections
	 */
	@Override
	public Collection<String> getExistingHierarchyCodes() {
		return catalogContext.getNpJdbcTemplate()
			.queryForList(
				"select entityType from t_schema where withHierarchy = true ",
				Collections.emptyMap()
			)
				.stream()
				.map(it -> (String) it.get("entityType"))
				.collect(Collectors.toList());
	}

	@Override
	public boolean removeHierarchy(String serializedEntityType) {
		// Removing of hierarchy currently means removing of whole collection which is handled by methods on collection.
		throw new IllegalStateException("Hierarchy for collection cannot be deleted separately. Use catalog methods to delete whole collections.");
	}

	@Override
	public void createItem(HierarchyItem newItem, HierarchyItem parent) {
		// Storing of new item is handled by entity collection repository as item is represented by entity, so item data
		// are stored with entity.

		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemCreated(newItem);
		}
	}

	@Override
	public void updateItem(HierarchyItem updatedItem) {
		final Map<String, Object> params = new HashMap<>();
		params.put("primaryKey", Integer.parseInt(updatedItem.getCode()));
		params.put("type", updatedItem.getHierarchyCode());
		params.put("leftBound", updatedItem.getLeftBound());
		params.put("rightBound", updatedItem.getRightBound());
		params.put("level", updatedItem.getLevel());
		params.put("numberOfChildren", updatedItem.getNumberOfChildren());
		params.put("order", updatedItem.getOrder());
		params.put("bucket", updatedItem.getBucket());

		final int affectedRows = catalogContext.getNpJdbcTemplate()
			.update(
				"update t_entity " +
					"set leftBound = :leftBound, " +
					"	 rightBound = :rightBound, " +
					"	 level = :level, " +
					"	 numberOfChildren = :numberOfChildren, " +
					"    orderAmongSiblings = :order, " +
					"    hierarchyBucket = :bucket " +
					"where primaryKey = :primaryKey and type = :type",
				params
			);
		Assert.isTrue(affectedRows == 1, "Removed unexpected count of rows: " + affectedRows + "!");

		Assert.isTrue(updatedItem instanceof HierarchyItemWithHistory, "Only item with history is supported.");
		final HierarchyItemWithHistory hiwh = (HierarchyItemWithHistory) updatedItem;
		updatedItemReferences(hiwh);
		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemUpdated(hiwh.getDelegate(), hiwh.getOriginal());
		}
	}

	@Override
	public void removeItem(HierarchyItem removedItem) {
		// Storing of new item is handled by entity collection repository as item is represented by entity, so item is
		// deleted when entity is deleted.

		removeItemReferences(removedItem);
		for (HierarchyChangeListener changeListener : changeListeners) {
			changeListener.itemRemoved(removedItem);
		}
	}

	@Override
	public HierarchyItem getItem(String entityType, String entityPrimaryKey) {
		try {
			final HashMap<String, Object> params = new HashMap<>();
			params.put("primaryKey", Integer.parseInt(entityPrimaryKey));
			params.put("type", entityType);
			return catalogContext.getNpJdbcTemplate()
				.queryForObject(
					"select " + ITEM_SELECT + " from t_entity " +
							"where primaryKey = :primaryKey and type = :type",
					params,
					new HierarchyItemRowMapper()
				);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	@Override
	public HierarchyItem getParentItem(HierarchyItem pivot) {
		try {
			final HashMap<String, Object> params = new HashMap<>();
			params.put("type", pivot.getHierarchyCode());
			params.put("level", (short) (pivot.getLevel() - 1));
			params.put("leftBound", pivot.getLeftBound());
			params.put("rightBound", pivot.getRightBound());
			return catalogContext.getNpJdbcTemplate()
				.queryForObject(
					"select " + ITEM_SELECT + " from t_entity " +
						"where type = :type " +
						"   and level = :level " +
						"   and leftBound <= :leftBound " +
						"	and rightBound >= :rightBound",
					params,
					new HierarchyItemRowMapper()
				);
		} catch (EmptyResultDataAccessException ex) {
			return null;
		}
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getParentsOfItem(HierarchyItem pivot) {
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :hierarchyCode " +
					"   and level < :level " +
					"   and leftBound <= :leftBound" +
					"	and rightBound >= :rightBound " +
					"order by level asc",
				new BeanPropertySqlParameterSource(pivot),
				new HierarchyItemRowMapper()
			);
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getRootItems(String entityType) {
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :type " +
					"  and level = 1 " +
					"order by orderAmongSiblings asc",
				Collections.singletonMap("type", entityType),
				new HierarchyItemRowMapper()
			);
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getChildItems(HierarchyItem parent) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("type", parent.getHierarchyCode());
		params.put("level", (short) (parent.getLevel() + 1));
		params.put("leftBound", parent.getLeftBound());
		params.put("rightBound", parent.getRightBound());
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :type " +
					"   and level = :level " +
					"   and leftBound >= :leftBound" +
					"	and rightBound <= :rightBound " +
					"order by orderAmongSiblings asc",
				params,
				new HierarchyItemRowMapper()
			);
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getAllChildrenItems(HierarchyItem parent) {
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :hierarchyCode " +
					"   and level > :level " +
					"   and leftBound >= :leftBound" +
					"	and rightBound <= :rightBound " +
					"order by level asc, orderAmongSiblings asc",
				new BeanPropertySqlParameterSource(parent),
				new HierarchyItemRowMapper()
			);
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getLeafItems(HierarchyItem parent) {
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :hierarchyCode " +
					"   and level > :level " +
					"   and leftBound >= :leftBound" +
					"	and rightBound <= :rightBound " +
					"   and numberOfChildren = 0 " +
					"order by level asc, orderAmongSiblings asc",
				new BeanPropertySqlParameterSource(parent),
				new HierarchyItemRowMapper()
			);
	}

	@Nonnull
	@Override
	public List<HierarchyItem> getLeafItems(String entityType) {
		return catalogContext.getNpJdbcTemplate()
			.query(
				"select " + ITEM_SELECT + " from t_entity " +
					"where type = :type " +
					"  and numberOfChildren = 0 " +
					"order by leftBound asc, rightBound asc",
				Collections.singletonMap("type", entityType),
				new HierarchyItemRowMapper()
			);
	}

	@Nullable
	@Override
	public SectionWithBucket getFirstEmptySection(String entityType, long sectionSize, short maxCount) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("type", entityType);
		params.put("sectionSize", sectionSize);

		final Short childrenCount = catalogContext.getNpJdbcTemplate().queryForObject(
			"select count(0) " +
					"from t_entity " +
					"where type = :type and level = 1",
			params,
			Short.class
		);

		if (childrenCount >= maxCount) {
			return null;
		} else if (childrenCount == 0) {
			return new SectionWithBucket(1L, sectionSize, (short) 1);
		} else {
			try {
				return catalogContext.getNpJdbcTemplate()
					.queryForObject(
						"select t1.leftBound - :sectionSize as leftBound, " +
							"	t1.leftBound - 1 as rightBound, " +
							"	t1.hierarchyBucket - 1 as hierarchyBucket " +
							"from t_entity t1 " +
							"left join t_entity t2 on t2.leftBound = t1.leftBound - :sectionSize " +
							"	and t2.type = t1.type " +
							"	and t2.level = 1 " +
							"where t1.type = :type " +
							"  	and t1.level = 1 " +
							"  	and t1.leftBound - :sectionSize > 0 " +
							"  	and t2.leftBound is null " +
							"order by t1.leftBound asc " +
							"limit 1",
						params,
						new SectionRowMapper()
					);
			} catch (EmptyResultDataAccessException ex) {
				return new SectionWithBucket(
					childrenCount * sectionSize + 1,
					(childrenCount + 1) * sectionSize,
					(short) (childrenCount + 1)
				);
			}
		}
	}

	@Nullable
	@Override
	public SectionWithBucket getFirstEmptySection(String entityType, long sectionSize, short maxCount, HierarchyItem parent) {
		final HashMap<String, Object> params = new HashMap<>();
		params.put("type", entityType);
		params.put("sectionSize", sectionSize);
		params.put("level", parent.getLevel() + 1);
		params.put("parentCode", parent.getCode());
		params.put("leftBound", parent.getLeftBound());
		params.put("rightBound", parent.getRightBound());

		final Short childrenCount = catalogContext.getNpJdbcTemplate().queryForObject(
			"select count(0) " +
				"from t_entity " +
				"where type = :type " +
				"   and level = :level " +
				"   and leftBound >= :leftBound" +
				"	and rightBound <= :rightBound",
			params,
			Short.class
		);

		if (childrenCount >= maxCount) {
			return null;
		} else if (childrenCount == 0) {
			return new SectionWithBucket(parent.getLeftBound() + 1, parent.getLeftBound() + sectionSize, (short) 1);
		} else {
			try {
				return catalogContext.getNpJdbcTemplate()
					.queryForObject(
						"select t1.leftBound - :sectionSize as leftBound, " +
							"	t1.leftBound - 1 as rightBound, " +
							"	t1.hierarchyBucket - 1 as hierarchyBucket " +
							"from t_entity t1 " +
							"left join t_entity t2 on t2.leftBound = t1.leftBound - :sectionSize " +
							"	and t2.type = t1.type " +
							"	and t2.level = :level " +
							"where t1.type = :type " +
							"  and t1.level = :level " +
							"  and t1.leftBound - :sectionSize > :leftBound " +
							"  and t1.rightBound < :rightBound " +
							"  and t2.leftBound is null " +
							" order by t1.leftBound asc " +
							"limit 1",
						params,
						new SectionRowMapper()
					);
			} catch (EmptyResultDataAccessException ex) {
				return new SectionWithBucket(
					parent.getLeftBound() + childrenCount * sectionSize + 1,
					parent.getLeftBound() + (childrenCount + 1) * sectionSize,
					(short) (childrenCount + 1)
				);
			}
		}
	}


	/**
	 * Updates bounds in entity which has stored bounds of referenced hierarchy entity
	 *
	 * @param updatedItem updated hierarchy entity
	 */
	private void updatedItemReferences(HierarchyItemWithHistory updatedItem) {
		final Map<String, Object> params = new HashMap<>();
		params.put("referencedType", updatedItem.getHierarchyCode());
		params.put("referencedPrimaryKey", updatedItem.getCode());
		params.put("leftBound", updatedItem.getLeftBound());
		params.put("rightBound", updatedItem.getRightBound());
		params.put("level", updatedItem.getLevel());

		catalogContext.getNpJdbcTemplate().update(
				"update t_entityHierarchyPlacement " +
					"set leftBound = :leftBound, rightBound = :rightBound, level = :level " +
					"where type = :referencedType and primaryKey = :referencedPrimaryKey",
				params
		);
	}

	/**
	 * Removes bounds from entity which has stored bounds of referenced hierarchy entity
	 *
	 * @param removedItem removed hierarchy entity
	 */
	private void removeItemReferences(HierarchyItem removedItem) {
		final Map<String, Object> params = new HashMap<>();
		params.put("referencedType", removedItem.getHierarchyCode());
		params.put("referencedPrimaryKey", removedItem.getCode());

		catalogContext.getNpJdbcTemplate().update(
				"delete from t_entityHierarchyPlacement " +
					"where type = :referencedType and primaryKey = :referencedPrimaryKey",
				params
		);
	}
}
