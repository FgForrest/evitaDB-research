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

package io.evitadb.storage;

import io.evitadb.api.SqlEvita;
import io.evitadb.api.SqlEvitaSession;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.schema.AttributeSchemaBuilder;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.schema.ReferenceSchemaBuilder;
import io.evitadb.test.SqlStorageTestSupport;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.jdbc.PgArray;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

import static io.evitadb.test.SqlStorageTestSupport.CATALOG_TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link EntityCollectionWriter}
 *
 * @author Tomáš Pozler, 2021
 * @author Jiří Bönsch, 2021
 */
class EntityCollectionWriterTest {

	private static final int FULL_ENTITY_PK = 1;

	private static JdbcTemplate jdbc;
	private SqlEvita evita;

	private EntityCollectionReader reader; // as helper only
	private EntityCollectionWriter writer;
	private EntityCollectionContext collectionCtx;

	@BeforeAll
	static void init() {
		jdbc = new JdbcTemplate(SqlStorageTestSupport.createStorageDatasource(SqlStorageTestSupport.getDbNameForCatalog(CATALOG_TEST_CATALOG)));
	}

	@BeforeEach
	void setUp() {
		// delete all db data
		SqlStorageTestSupport.deleteStoredData(CATALOG_TEST_CATALOG);

		// create evita instance (to create db structure, should not be used in tests) and configure test catalog
		evita = new SqlEvita(SqlEvitaTestSupport.createTestCatalogConfiguration());
		evita.updateCatalog(CATALOG_TEST_CATALOG, this::defineSchema);

		collectionCtx = SqlEvitaTestSupport.createTestEntityCollectionContext();
		reader = new EntityCollectionReader(collectionCtx);
		collectionCtx.getEntitySchema().set(reader.findSchema().get());
		writer = new EntityCollectionWriter(collectionCtx);
	}

	@AfterEach
	void tearDown() {
		evita.close();
	}

	@Test
	void shouldStoreEmptyEntityInBulkMode() {
		writer.storeEntity(createEmptyEntity(1));

		// entity should be only in buffers
		assertInsertBufferCounts(1, 0, 0, 0);
		assertTableCounts(0, 0, 0, 0);

		// verify correctness of stored data
		writer.flush();
		assertEmptyEntity(1);
	}

	@Test
	@SneakyThrows
	void shouldStoreEmptyEntityInTransactionalMode() {
		switchToAliveState();
		writer.storeEntity(createEmptyEntity(1));

		// entity should be only in db
		assertInsertBufferCounts(0, 0, 0, 0);
		assertEmptyEntity(1);
	}

	@Test
	void shouldStoreFullEntityInBulkMode() {
		writer.storeEntity(createFullEntity());

		// entity should be only in buffers
		assertInsertBufferCounts(1, 2, 5, 2);
		assertTableCounts(0, 0, 0, 0);

		// verify correctness of stored data
		writer.flush();
		assertFullEntity();
	}

	@Test
	void shouldStoreFullEntityInTransactionalMode() {
		switchToAliveState();
		writer.storeEntity(createFullEntity());

		// entity should be only in db
		assertInsertBufferCounts(0, 0, 0, 0);
		assertFullEntity();
	}

	@Test
	void shouldUpdateFullEntityInTransactionalMode() {
		switchToAliveState();

		writer.storeEntity(createFullEntity());
		assertFullEntity();

		// update entity
		writer.storeEntity(updateFullEntity());

		// verify storage
		assertInsertBufferCounts(0, 0, 0, 0);
		assertUpdatedFullEntity();
	}

	@Test
	void shouldFlushInsertBuffersInBulkMode() {
		for (int i = 1; i <= 100; i++) {
			writer.storeEntity(createEmptyEntity(i));
		}
		assertInsertBufferCounts(100, 0, 0, 0);

		writer.flush();
		assertInsertBufferCounts(0, 0, 0, 0);
		assertTableCounts(100, 0, 0, 0);
	}

	@Test
	void shouldStoreSchema() {
		switchToAliveState();

		final EntitySchema updatedSchema = new EntitySchemaBuilder(reader.findSchema().get(), schema -> schema)
				.withAttribute("available", Boolean.class)
				.applyChanges();
		writer.storeSchema(updatedSchema);
		assertEquals(updatedSchema, reader.findSchema().get());
	}

	@Test
	void shouldGenerateNextPrimaryKey() {
		assertEquals(1, writer.generateNextPrimaryKey());
		assertEquals(2, writer.generateNextPrimaryKey());
	}

	@Test
	void shouldCountBufferEntitiesInBulkMode() {
		assertEquals(0, writer.countBufferEntities());

		writer.storeEntity(createEmptyEntity(1));
		assertEquals(1, writer.countBufferEntities());

		writer.storeEntity(createEmptyEntity(2));
		assertEquals(2, writer.countBufferEntities());
	}

	@Test
	void shouldStoreSerializationHeader() {
		final String headerQuery = "select serializationHeader from t_collection where name = ?";

		switchToAliveState();
		writer.storeEntity(createEmptyEntity(1));

		byte[] header = jdbc.queryForObject(headerQuery, byte[].class, collectionCtx.getEntityType().toString());
		assertNull(header);

		writer.storeSerializationHeader(new byte[] { 1, 2 });
		header = jdbc.queryForObject(headerQuery, byte[].class, collectionCtx.getEntityType().toString());
		assertArrayEquals(new byte[] { 1, 2 }, header);
	}

	@Test
	void shouldGoLive() {
		final String indexCountQuery = "select count(*) from pg_indexes where indexname like 'ix_%'";

		final int indexCountBefore = jdbc.queryForObject(indexCountQuery, Integer.class);
		writer.goLive(false);
		final Integer indexCountAfter = jdbc.queryForObject(indexCountQuery, Integer.class);

		assertTrue(indexCountAfter > indexCountBefore);
	}

	@Test
	void shouldRestoreAliveState() {
		final String indexCountQuery = "select count(*) from pg_indexes where indexname like 'ix_%'";

		switchToAliveState();

		final int indexCountBefore = jdbc.queryForObject(indexCountQuery, Integer.class);
		writer.goLive(true);
		final int indexCountAfter = jdbc.queryForObject(indexCountQuery, Integer.class);

		assertEquals(indexCountBefore, indexCountAfter);
	}


	private void switchToAliveState() {
		collectionCtx.getTransactionMode().set(true);
		writer.goLive(false);
	}

	private Entity createEmptyEntity(int primaryKey) {
		return new InitialEntityBuilder(collectionCtx.getEntityType(), primaryKey).toInstance();
	}

	private Entity createFullEntity() {
		final InitialEntityBuilder builder = new InitialEntityBuilder("product", FULL_ENTITY_PK);

		builder.setReference("product",
			FULL_ENTITY_PK + 1,
			referenceBuilder -> {
				referenceBuilder.setAttribute("brand", Locale.FRENCH, "lorem");
				referenceBuilder.setAttribute("weight", 2);
				referenceBuilder.setGroup("testGroup", 3);
			}
		);
		builder.setReference("category", 1);

		builder.setAttribute("value", new BigDecimal("3.14"));
		builder.setAttribute("values", new NumberRange[] { NumberRange.between(1L, 2L), NumberRange.between(10L, 15L) });
		builder.setAttribute("name", Locale.GERMAN, "test");

		builder.setPrice(1, "test", Currency.getInstance("EUR"), 1, new BigDecimal(100), new BigDecimal(15), new BigDecimal(115), true);
		builder.setPrice(2, "test", Currency.getInstance("CZK"), 21, new BigDecimal(1000), new BigDecimal(21), new BigDecimal(1210), DateTimeRange
			.between(LocalDateTime.of(2021, 5, 3, 18, 22), LocalDateTime.of(2021, 5, 3, 19, 22), ZoneId.of("Europe/Prague")), false);
		builder.setPrice(3, "basic", Currency.getInstance("CZK"), 1, new BigDecimal(100), new BigDecimal(21), new BigDecimal(121), true);

		return builder.toInstance();
	}

	private Entity updateFullEntity() {
		return createFullEntity().open()
			// add new
			.setAttribute("visible", true)
			// update existing
			.setAttribute("value", new BigDecimal("6.1415"))
			// remove existing
			.removeAttribute("values")
			// add new
			.setPrice(4, "new", Currency.getInstance("EUR"), new BigDecimal("499"), new BigDecimal("21"), new BigDecimal("603.79"), DateTimeRange
				.between(LocalDateTime.of(2021, 5, 22, 18, 22), LocalDateTime.of(2021, 5, 23, 19, 22), ZoneId.of("Europe/Prague")), true)
			// update existing
			.setPrice(1, "test", Currency.getInstance("EUR"), 1, new BigDecimal("10"), new BigDecimal("15"), new BigDecimal("11.5"), true)
			// remove existing
			.removePrice(3, "basic", Currency.getInstance("CZK"))
			// update existing
			.setReference(
				"product", FULL_ENTITY_PK + 1,
				thatIs -> {
					thatIs.setGroup("testGroup", 20);
					thatIs.setAttribute("priority", 20);
					thatIs.removeAttribute("weight");
				})
			// remove existing
			.removeReference(
				"category", 1
			)
			.toInstance();
	}

	private void defineSchema(SqlEvitaSession session) {
		session.defineSchema("product")
			.withAttribute("value", BigDecimal.class, it -> it.sortable().indexDecimalPlaces(2))
			.withAttribute("values", NumberRange[].class, AttributeSchemaBuilder::unique)
			.withAttribute("name", String.class, a -> a.filterable().localized())
			.withAttribute("visible", Boolean.class)
			.withReferenceTo(
				"product",
				r -> r
					.indexed()
					.withAttribute("brand", String.class, a -> a.filterable().localized())
					.withAttribute("weight", Integer.class, AttributeSchemaBuilder::sortable)
					.withAttribute("priority", Integer.class, AttributeSchemaBuilder::filterable)
					.withGroupType("testGroup")
			)
			.withReferenceTo("category", ReferenceSchemaBuilder::faceted)
			.withReferenceTo("parameter")
			.withPrice()
			.applyChanges();
	}


	private void assertTableCounts(int entityCount, int referenceCount, int attributeCount, int priceCount) {
		assertEquals(entityCount, jdbc.queryForObject("select count(*) from t_entity", Integer.class));
		assertEquals(referenceCount, jdbc.queryForObject("select count(*) from c_1.t_referenceIndex", Integer.class));
		assertEquals(attributeCount, jdbc.queryForObject("select count(*) from c_1.t_attributeIndex", Integer.class));
		assertEquals(priceCount, jdbc.queryForObject("select count(*) from c_1.t_priceIndex", Integer.class));
	}

	private void assertInsertBufferCounts(int entityCount, int referenceCount, int attributeCount, int priceCount) {
		assertEquals(entityCount, writer.entityBodyInsertBuffer.size());
		assertEquals(referenceCount, writer.referenceInsertBuffer.size());
		assertEquals(attributeCount, writer.attributeInsertBuffer.size());
		assertEquals(priceCount, writer.priceInsertBuffer.size());
	}

	private void assertReference(Map<String, Object> reference, long entityId, int version, boolean faceted, int primarykey, String type, Integer groupPrimaryKey, String groupType) {
		assertNotNull(reference.get("reference_id"));
		assertEquals(entityId, reference.get("entity_id"));
		assertEquals(version, reference.get("version"));
		assertEquals(faceted, reference.get("faceted"));
		assertEquals(primarykey, reference.get("entityPrimaryKey"));
		assertEquals(type, reference.get("entityType"));
		assertEquals("java.lang.String", reference.get("entityTypeDataType"));
		assertEquals(groupPrimaryKey, reference.get("groupPrimaryKey"));
		assertEquals(groupType, reference.get("groupType"));
		if (groupType != null) {
			assertEquals("java.lang.String", reference.get("groupTypeDataType"));
		} else {
			assertNull(reference.get("groupTypeDataType"));
		}
	}

	@SneakyThrows
	private void assertAttribute(Map<String, Object> attribute, long entityId, String referenceType, String name, String locale, int version, boolean sortable, boolean uniq, String[] stringValues, Long[] intValues, String intRangeValues) {
		assertEquals(entityId, attribute.get("entity_id"));
		if (referenceType != null) {
			assertNotNull(attribute.get("reference_id"));
		} else {
			assertNull(attribute.get("reference_id"));
		}
		assertEquals(referenceType, attribute.get("reference_entityType"));
		assertEquals(name, attribute.get("name"));
		assertEquals(locale, attribute.get("locale"));
		assertEquals(version, attribute.get("version"));
		assertEquals(sortable, attribute.get("sortable"));
		assertEquals(uniq, attribute.get("uniq"));
		if (stringValues != null) {
			assertArrayEquals(stringValues, (String[]) ((PgArray) attribute.get("stringValues")).getArray());
		} else {
			assertNull(attribute.get("stringValues"));
		}
		if (intValues != null) {
			assertArrayEquals(intValues, (Long[]) ((PgArray) attribute.get("intValues")).getArray());
		} else {
			assertNull(attribute.get("intValues"));
		}
		if (intRangeValues != null) {
			assertEquals(intRangeValues, attribute.get("intRangeValues").toString());
		} else {
			assertNull(attribute.get("intRangeValues"));
		}
	}

	private void assertPrice(Map<String, Object> price, long entityId, int primaryKey, int version, BigDecimal priceWithoutVat, BigDecimal priceWithVat, Integer innerRecordId, PriceInnerRecordHandling innerRecordHandling, Currency currency, String priceList, String validity) {
		assertEquals(entityId, price.get("entity_id"));
		assertEquals(primaryKey, price.get("primaryKey"));
		assertEquals(version, price.get("version"));
		assertEquals(priceWithoutVat, price.get("priceWithoutVat"));
		assertEquals(priceWithVat, price.get("priceWithVat"));
		assertEquals(innerRecordId, price.get("innerRecordId"));
		assertEquals(innerRecordHandling.toString(), price.get("innerRecordHandling"));
		assertEquals(currency.getCurrencyCode(), price.get("currency"));
		assertEquals(priceList, price.get("priceList"));
		assertEquals("java.lang.String", price.get("priceListDataType"));
		assertEquals(validity, Optional.ofNullable(price.get("validity")).map(Object::toString).orElse(null));
	}

	@SneakyThrows
	private void assertEntity(int primaryKey, String type, int version, boolean dropped, String[] locales) {
		final Map<String, Object> entity = jdbc.queryForMap("select * from t_entity where primaryKey = ?", primaryKey);

		assertEquals(primaryKey, entity.get("primaryKey"));
		assertEquals(type, entity.get("type"));
		assertEquals(version, entity.get("version"));
		assertEquals(dropped, entity.get("dropped"));
		assertArrayEquals(locales, (String[]) ((PgArray) entity.get("locales")).getArray());
		assertNull(entity.get("parentPrimaryKey"));
		assertNull(entity.get("leftBound"));
		assertNull(entity.get("rightBound"));
		assertNull(entity.get("level"));
		assertNull(entity.get("numberOfChildren"));
		assertNull(entity.get("orderAmongSiblings"));
		assertNull(entity.get("hierarchyBucket"));
	}

	@SneakyThrows
	private void assertEmptyEntity(int primaryKey) {
		assertTableCounts(1, 0, 0, 0);
		assertEntity(primaryKey, "product", 1, false, new String[0]);
	}

	@SneakyThrows
	private void assertFullEntity() {
		assertTableCounts(1, 2, 5, 2);
		assertEntity(FULL_ENTITY_PK, "product", 1, false, new String[] { "de   " });

		final List<Map<String, Object>> references = jdbc.queryForList(
				"select * from c_1.t_referenceIndex where entity_id = ? order by entityType",
				FULL_ENTITY_PK
		);
		assertEquals(2, references.size());
		assertReference(references.get(0), 1L, 1, true, 1, "category", null, null);
		assertReference(references.get(1), 1L, 1, false, 2, "product", 3, "testGroup");

		final List<Map<String, Object>> referenceAttributes = jdbc.queryForList(
				"select * " +
					"from c_1.t_attributeIndex " +
					"where entity_id = ? and reference_id is not null " +
					"order by name",
				FULL_ENTITY_PK
		);
		assertEquals(2, referenceAttributes.size());
		assertAttribute(referenceAttributes.get(0), 1L, "product", "brand", Locale.FRENCH + "   ", 1, false, false, new String[] { "lorem" }, null, null);
		assertAttribute(referenceAttributes.get(1), 1L, "product", "weight", null, 1, true, false, null, new Long[] { 2L }, null);

		final List<Map<String, Object>> entityAttributes = jdbc.queryForList(
				"select * " +
						"from c_1.t_attributeIndex " +
						"where entity_id = ? and reference_id is null " +
						"order by name",
				FULL_ENTITY_PK
		);
		assertEquals(3, entityAttributes.size());
		assertAttribute(entityAttributes.get(0), 1, null, "name", Locale.GERMAN + "   ", 1, false, false, new String[] { "test" }, null, null);
		assertAttribute(entityAttributes.get(1), 1, null, "value", null, 1, true, false, null, new Long[] { 314L }, null);
		assertAttribute(entityAttributes.get(2), 1, null, "values", null, 1, false, true, null, null, "{\"[1,3)\",\"[10,16)\"}");

		final List<Map<String, Object>> prices = jdbc.queryForList(
				"select * from c_1.t_priceIndex where entity_id = ? order by primaryKey",
				FULL_ENTITY_PK
		);
		assertEquals(2, prices.size());
		assertPrice(prices.get(0), 1L, 1, 1, new BigDecimal("100.00"), new BigDecimal("115.00"), 1, PriceInnerRecordHandling.NONE, Currency.getInstance("EUR"), "test", null);
		assertPrice(prices.get(1), 1L, 3, 1, new BigDecimal("100.00"), new BigDecimal("121.00"), 1, PriceInnerRecordHandling.NONE, Currency.getInstance("CZK"), "basic", null);
	}

	@SneakyThrows
	private void assertUpdatedFullEntity() {
		assertTableCounts(1, 1, 4, 2);
		assertEntity(FULL_ENTITY_PK, "product", 2, false, new String[] { "de   " });

		final List<Map<String, Object>> references = jdbc.queryForList(
				"select * from c_1.t_referenceIndex where entity_id = ? order by entityType",
				FULL_ENTITY_PK
		);
		assertEquals(1, references.size());
		assertReference(references.get(0), 1L, 4, false, 2, "product", 20, "testGroup");

		final List<Map<String, Object>> referenceAttributes = jdbc.queryForList(
				"select * " +
						"from c_1.t_attributeIndex " +
						"where entity_id = ? and reference_id is not null " +
						"order by name",
				FULL_ENTITY_PK
		);
		assertEquals(2, referenceAttributes.size());
		assertAttribute(referenceAttributes.get(0), 1L, "product", "brand", Locale.FRENCH + "   ", 1, false, false, new String[] { "lorem" }, null, null);
		assertAttribute(referenceAttributes.get(1), 1L, "product", "priority", null, 1, false, false, null, new Long[] { 20L }, null);

		final List<Map<String, Object>> entityAttributes = jdbc.queryForList(
				"select * " +
						"from c_1.t_attributeIndex " +
						"where entity_id = ? and reference_id is null " +
						"order by name",
				FULL_ENTITY_PK
		);
		assertEquals(2, entityAttributes.size());
		assertAttribute(entityAttributes.get(0), 1, null, "name", Locale.GERMAN + "   ", 1, false, false, new String[] { "test" }, null, null);
		assertAttribute(entityAttributes.get(1), 1, null, "value", null, 2, true, false, null, new Long[] { 614L }, null);

		final List<Map<String, Object>> prices = jdbc.queryForList(
				"select * from c_1.t_priceIndex where entity_id = ? order by primaryKey",
				FULL_ENTITY_PK
		);
		assertEquals(2, prices.size());
		assertPrice(prices.get(0), 1L, 1, 2, new BigDecimal("10.00"), new BigDecimal("11.50"), 1, PriceInnerRecordHandling.NONE, Currency.getInstance("EUR"), "test", null);
		assertPrice(prices.get(1), 1L, 4, 1, new BigDecimal("499.00"), new BigDecimal("603.79"), null, PriceInnerRecordHandling.NONE, Currency.getInstance("EUR"), "new", "[1621700520,1621790521)");
	}
}
