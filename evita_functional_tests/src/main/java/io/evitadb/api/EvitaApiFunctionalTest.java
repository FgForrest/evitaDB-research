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

package io.evitadb.api;

import io.evitadb.api.configuration.CatalogConfiguration;
import io.evitadb.api.data.*;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.mutation.attribute.ApplyDeltaAttributeMutation;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.io.EvitaRequestBase;
import io.evitadb.api.io.EvitaResponseBase;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static io.evitadb.api.query.Query.query;
import static io.evitadb.api.query.QueryConstraints.*;
import static io.evitadb.test.TestConstants.FUNCTIONAL_TEST;
import static io.evitadb.test.TestConstants.TEST_CATALOG;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Base work with Evita API integration test:
 * <p>
 * - catalog creation
 * - catalog updating
 * - collection creation
 * - new entity insertion
 * - existing entity updating
 * - getting entity by id
 * - schema creation
 * - verification of the inserted entities by schema
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@DisplayName("Evita base API")
@Tag(FUNCTIONAL_TEST)
class EvitaApiFunctionalTest<REQUEST extends EvitaRequestBase, CONFIGURATION extends CatalogConfiguration, COLLECTION extends EntityCollectionBase<REQUEST>, CATALOG extends CatalogBase<REQUEST, CONFIGURATION, COLLECTION>, TRANSACTION extends TransactionBase, SESSION extends EvitaSessionBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION>, EVITA extends EvitaBase<REQUEST, CONFIGURATION, COLLECTION, CATALOG, TRANSACTION, SESSION>> {
	public static final String BRAND = "brand";
	public static final String PRODUCT = "product";
	public static final String CATEGORY = "category";
	public static final String PRICE_LIST = "priceList";
	public static final Locale LOCALE_CZECH = new Locale("cs");
	public static final String LOGO = "https://www.siemens.com/logo.png";
	public static final String SIEMENS_CODE = "siemens";
	public static final String SIEMENS_TITLE = "Siemens";
	public static final Currency EUR = Currency.getInstance("EUR");
	public static final Currency CZK = Currency.getInstance("CZK");

	@DisplayName("Create entity with primary key already provided")
	@Test
	void shouldCreateEntityWithExistingPrimaryKey(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// create new entity
				final EntityBuilder newBrand = createBrand(session, 1);
				// store it to the catalog
				session.upsertEntity(newBrand);
			}
		);

		// get entity by primary key and verify its contents
		assertBrand(evita, 1, Locale.ENGLISH, SIEMENS_CODE, SIEMENS_TITLE, LOGO, 1);
	}

	@DisplayName("Don't allow creating entity with same primary key twice")
	@Test
	void shouldFailToCreateEntityWithExistingPrimaryKeyTwice(EVITA evita) {
		shouldCreateEntityWithExistingPrimaryKey(evita);
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// create new entity
				final EntityBuilder newBrand = createBrand(session, 1);
				// store it to the catalog
				assertThrows(InvalidMutationException.class, () -> session.upsertEntity(newBrand));
			}
		);
	}

	@DisplayName("Create entity without PK and verify PK has been assigned")
	@Test
	void shouldAutomaticallyGeneratePrimaryKey(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// primary keys should be automatically generated in monotonic fashion
				assertEquals(1, session.upsertEntity(createBrand(session, null)).getPrimaryKey());
				assertEquals(2, session.upsertEntity(createBrand(session, null)).getPrimaryKey());
				assertEquals(3, session.upsertEntity(createBrand(session, null)).getPrimaryKey());
				final EntitySchema brandSchema = session.getEntitySchema(BRAND);
				assertNotNull(brandSchema);
				assertTrue(brandSchema.isWithGeneratedPrimaryKey());
			}
		);
	}

	@DisplayName("Verify code has no problems assigning new PK in concurrent environment")
	@Test
	void shouldAutomaticallyGeneratePrimaryKeyInParallel(EVITA evita) throws InterruptedException {

		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.upsertEntity(createBrand(session, null));
			}
		);

		final int numberOfThreads = 10;
		final int iterations = 100;
		final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
		final CountDownLatch latch = new CountDownLatch(numberOfThreads);
		final Set<Integer> primaryKeys = new ConcurrentSkipListSet<>();

		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(() -> {
				for (int j = 0; j < iterations; j++) {
					evita.updateCatalog(
						TEST_CATALOG,
						session -> {
							// primary keys should be automatically generated in monotonic fashion
							primaryKeys.add(session.upsertEntity(createBrand(session, null)).getPrimaryKey());
						}
					);
				}
				latch.countDown();
			});
		}
		assertTrue(latch.await(45, TimeUnit.SECONDS), "Timeouted!");

		assertEquals(primaryKeys.size(), numberOfThreads * iterations);
		for (int i = 1; i <= numberOfThreads * iterations; i++) {
			assertTrue(primaryKeys.contains(i + 1), "Primary key missing: " + (i + 1));
		}
	}

	@DisplayName("Create entity outside Evita engine and insert it")
	@Test
	void shouldCreateDetachedEntity(EVITA evita) {
		final EntityBuilder detachedBuilder = new InitialEntityBuilder(BRAND, 1)
			.setAttribute("code", "siemens")
			.setAttribute("name", Locale.ENGLISH, SIEMENS_TITLE)
			.setAttribute("logo", LOGO)
			.setAttribute("productCount", 1);

		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// store it to the catalog
				session.upsertEntity(detachedBuilder);
			}
		);

		// get entity by primary key and verify its contents
		assertBrand(evita, 1, Locale.ENGLISH, "siemens", SIEMENS_TITLE, LOGO, 1);
	}

	@DisplayName("Entity created outside Evita with incompatible schema should be rejected")
	@Test
	void shouldRefuseUpsertOfSchemaIncompatibleDetachedEntity(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.defineSchema(PRODUCT)
					.verifySchemaStrictly()
					.withAttribute("code", String.class, whichIs -> whichIs.unique())
					.withAttribute("url", String.class, whichIs -> whichIs.unique().localized())
					/* finally apply schema changes */
					.applyChanges();
			}
		);

		final EntityBuilder detachedBuilder = new InitialEntityBuilder(PRODUCT, 1)
			.setAttribute("code", "siemens")
			.setAttribute("name", Locale.ENGLISH, SIEMENS_TITLE)
			.setAttribute("logo", LOGO)
			.setAttribute("productCount", 1);

		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// store it to the catalog
				assertThrows(InvalidMutationException.class, () -> session.upsertEntity(detachedBuilder));
			}
		);
	}

	@DisplayName("All resources should be safely terminated in try block")
	@Test
	void shouldCreateEntityAndUseAutoCloseableForSessionTermination(EVITA evita) {
		// first prepare data in R/W session
		evita.updateCatalog(TEST_CATALOG, session -> {
			//create new entity
			final EntityBuilder newBrand = createBrand(session, 1);
			// store it to the catalog
			session.upsertEntity(newBrand);
		});
		// now test the non-lambda approach to query the session
		try (final SESSION session = evita.createReadOnlySession(TEST_CATALOG)) {
			// get entity by primary key and verify its contents
			assertBrandInSession(session, 1, Locale.ENGLISH, "siemens", SIEMENS_TITLE, LOGO, 1);
		}

		// get entity by primary key and verify its contents
		assertBrand(evita, 1, Locale.ENGLISH, "siemens", SIEMENS_TITLE, LOGO, 1);
	}

	@DisplayName("Existing entity can be altered and updated back to Evita DB")
	@Test
	void shouldUpdateExistingEntity(EVITA evita) {
		// create new entity
		shouldCreateEntityWithExistingPrimaryKey(evita);
		// open new session
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// select existing entity
				final SealedEntity siemensBrand = getBrandById(session, 1);
				// modify the entity contents
				final EntityBuilder updatedBrand = siemensBrand.open()
					.setAttribute("name", LOCALE_CZECH, "Siemens Česko")
					.setAttribute("logo", "https://www.siemens.cz/logo.png");
				// store entity back to the database
				session.upsertEntity(updatedBrand);
				// get entity by primary key and verify its contents
				assertBrandInSession(session, 1, LOCALE_CZECH, "siemens", "Siemens Česko", "https://www.siemens.cz/logo.png", 1);
			}
		);
		// verify data was changed
		assertBrand(evita, 1, LOCALE_CZECH, "siemens", "Siemens Česko", "https://www.siemens.cz/logo.png", 1);
	}

	@DisplayName("Entity with all possible data is created")
	@Test
	SealedEntity shouldCreateFullFeaturedEntity(EVITA evita) {
		final AtomicReference<EntityContract> product = new AtomicReference<>();
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// create referenced entities
				final EntityReferenceContract brand = session.upsertEntity(createBrand(session, 1));
				final EntityReferenceContract category = session.upsertEntity(createCategory(session, 1));
				// create full featured entity
				final EntityReferenceContract productRef = session.upsertEntity(createProduct(session, 1, brand, category));
				product.set(
					session.getEntity(productRef.getType(), productRef.getPrimaryKey(), fullEntity())
				);
			}
		);

		return evita.queryCatalog(TEST_CATALOG, session -> {
			// get entity by primary key and verify its contents
			final SealedEntity loadedProduct = getProductById(session, 1);
			assertNotNull(loadedProduct);
			assertFalse(product.get().differsFrom(loadedProduct));
			return loadedProduct;
		});
	}

	@DisplayName("Entity with all possible data is altered")
	@Test
	void shouldUpdateFullFeaturedEntity(EVITA evita) {
		final SealedEntity createdEntity = shouldCreateFullFeaturedEntity(evita);

		final AtomicReference<EntityContract> product = new AtomicReference<>();
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				final EntityReferenceContract categoryTwo = session.upsertEntity(createCategory(session, 2));
				// alter full featured entity
				final EntityReferenceContract productRef = session.upsertEntity(updateProduct(createdEntity, categoryTwo));
				product.set(
					session.getEntity(productRef.getType(), productRef.getPrimaryKey(), fullEntity())
				);
			}
		);

		evita.queryCatalog(TEST_CATALOG, session -> {
			// get entity by primary key and verify its contents
			final EntityContract loadedProduct = getProductById(session, 1);
			assertNotNull(loadedProduct);
			assertFalse(product.get().differsFrom(loadedProduct));
			assertEquals(Boolean.TRUE, loadedProduct.getAttribute("aquaStop"));
			assertEquals(Byte.valueOf((byte) 60), loadedProduct.getAttribute("width"));
			assertNull(loadedProduct.getAttributeValue("waterConsumption"));
			assertEquals(new LocalizedLabels().withLabel("name", "iQ700完全內置式洗碗機60厘米XXL"), loadedProduct.getAssociatedData("localizedLabels", Locale.CHINA, LocalizedLabels.class));
			assertEquals(new LocalizedLabels().withLabel("name", "iQ700 Plně vestavná myčka nádobí 60 cm XXL (ve slevě)"), loadedProduct.getAssociatedData("localizedLabels", LOCALE_CZECH, LocalizedLabels.class));
			assertNull(loadedProduct.getAssociatedData("localizedLabels", Locale.ENGLISH));
			assertEquals(new BigDecimal("499"), Objects.requireNonNull(loadedProduct.getPrice(3, "action", EUR)).getPriceWithoutVat());
			assertEquals(new BigDecimal("899"), Objects.requireNonNull(loadedProduct.getPrice(2, "reference", EUR)).getPriceWithoutVat());
			assertNull(loadedProduct.getPrice(1, "reference", CZK));
			final Collection<ReferenceContract> references = loadedProduct.getReferences(CATEGORY);
			assertEquals(2, references.size());
			for (ReferenceContract reference : references) {
				if (reference.getReferencedEntity().getPrimaryKey() == 1) {
					final GroupEntityReference group = reference.getGroup();
					assertNotNull(group);
					assertEquals("CATEGORY_GROUP", group.getType());
					assertEquals(Integer.valueOf(45), group.getPrimaryKey());
					assertEquals(Long.valueOf(20L), reference.getAttribute("priority"));
					assertNull(reference.getAttribute("visibleToCustomerTypes"));
				} else if (reference.getReferencedEntity().getPrimaryKey() == 2) {
					// ok
				} else {
					fail("Unexpected referenced entity " + reference.getReferencedEntity());
				}
			}
			assertTrue(loadedProduct.getReferences(BRAND).isEmpty());
			return null;
		});
	}

	@DisplayName("Entity can be altered by individual mutations avoiding builder entirely")
	@Test
	void shouldUpdateExistingEntityByHandPickedMutations(EVITA evita) {
		// create new entity
		shouldCreateEntityWithExistingPrimaryKey(evita);
		// open new session
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				// select existing entity
				final SealedEntity siemensBrand = getBrandById(session, 1);
				// modify contents by delta mutation
				session.upsertEntity(
					siemensBrand.withMutations(
						new ApplyDeltaAttributeMutation("productCount", 5)
					)
				);
				// get entity by primary key and verify its contents
				assertBrandInSession(session, 1, Locale.ENGLISH, "siemens", SIEMENS_TITLE, LOGO, 6);
			}
		);

		assertBrand(evita, 1, Locale.ENGLISH, "siemens", SIEMENS_TITLE, LOGO, 6);
	}

	@DisplayName("Example schema definition can be inserted into Evita DB")
	@Test
	void shouldDefineStrictSchema(EVITA evita) {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				//noinspection Convert2MethodRef
				session.defineSchema(PRODUCT)
					/* all is strictly verified but associated data and references can be added on the fly */
					.verifySchemaButAllow(EvolutionMode.ADDING_ASSOCIATED_DATA, EvolutionMode.ADDING_REFERENCES)
					/* product are not organized in the tree */
					.withoutHierarchy()
					/* prices are referencing another entity stored in Evita */
					.withPrice()
					/* en + cs localized attributes and associated data are allowed only */
					.withLocale(Locale.ENGLISH, new Locale("cs", "CZ"))
					/* here we define list of attributes with indexes for search / sort */
					.withAttribute("code", String.class, whichIs -> whichIs.unique())
					.withAttribute("url", String.class, whichIs -> whichIs.unique().localized())
					.withAttribute("oldEntityUrls", String[].class, whichIs -> whichIs.filterable().localized())
					.withAttribute("name", String.class, whichIs -> whichIs.filterable().sortable())
					.withAttribute("ean", String.class, whichIs -> whichIs.filterable())
					.withAttribute("priority", Long.class, whichIs -> whichIs.sortable())
					.withAttribute("validity", DateTimeRange.class, whichIs -> whichIs.filterable())
					.withAttribute("quantity", BigDecimal.class, whichIs -> whichIs.filterable().indexDecimalPlaces(2))
					.withAttribute("alias", Boolean.class, whichIs -> whichIs.filterable())
					/* here we define set of associated data, that can be stored along with entity */
					//.withAssociatedData("referencedFiles", ReferencedFileSet.class)
					//.withAssociatedData("labels", Labels.class, whichIs -> whichIs.localized())
					/* here we define references that relate to another entities stored in Evita */
					.withReferenceToEntity(
						CATEGORY,
						whichIs ->
							/* we can specify special attributes on relation */
							whichIs.withAttribute("categoryPriority", Long.class, thatIs -> thatIs.sortable())
					)
					/* for faceted references we can compute "counts" */
					.withReferenceToEntity(BRAND, whichIs -> whichIs.faceted())
					/* references may be also represented be entities unknown to Evita */
					.withReferenceTo("stock", whichIs -> whichIs.faceted())
					/* finally apply schema changes */
					.applyChanges();
			}
		);
	}

	@DisplayName("Evita DB tracks open sessions so that they can be closed on Evita DB close")
	@Test
	void shouldTrackAndFreeOpenSessions(EVITA evita) throws InterruptedException {
		evita.updateCatalog(
			TEST_CATALOG,
			session -> {
				session.upsertEntity(createBrand(session, null));
			}
		);

		final int numberOfThreads = 10;
		final int iterations = 100;
		final ExecutorService service = Executors.newFixedThreadPool(numberOfThreads);
		final CountDownLatch latch = new CountDownLatch(numberOfThreads);
		final AtomicInteger peek = new AtomicInteger();

		for (int i = 0; i < numberOfThreads; i++) {
			service.execute(() -> {
				for (int j = 0; j < iterations; j++) {
					evita.updateCatalog(
						TEST_CATALOG,
						session -> {
							// this is kind of unsafe, but it should work
							final int activeSessions = evita.activeSessions.size();
							if (peek.get() < activeSessions) {
								peek.set(activeSessions);
							}
							session.upsertEntity(createBrand(session, null));
						}
					);
				}
				latch.countDown();
			});
		}

		assertTrue(latch.await(45, TimeUnit.SECONDS), "Timeouted!");
		assertTrue(peek.get() > 6, "There should be multiple session in parallel!");
		assertEquals(0, evita.activeSessions.size(), "There should be no active session now!");
	}

	/*
		HELPER METHODS
	 */

	private EntityBuilder createBrand(SESSION session, Integer primaryKey) {
		final EntityBuilder newBrand = session.createNewEntity(BRAND, primaryKey);
		newBrand.setAttribute("code", "siemens");
		newBrand.setAttribute("name", Locale.ENGLISH, SIEMENS_TITLE);
		newBrand.setAttribute("logo", LOGO);
		newBrand.setAttribute("productCount", 1);
		return newBrand;
	}

	private EntityBuilder createCategory(SESSION session, Integer primaryKey) {
		final EntityBuilder newCategory = session.createNewEntity(CATEGORY, primaryKey);
		newCategory.setAttribute("code", "builtin-dishwashers");
		newCategory.setAttribute("name", LOCALE_CZECH, "Vestavné myčky nádobí");
		newCategory.setAttribute("name", Locale.ENGLISH, "Built-in dishwashers");
		newCategory.setAttribute("active", createDateRange(2020, 2021));
		newCategory.setAttribute("priority", 456L);
		newCategory.setAttribute("visible", true);
		newCategory.setHierarchicalPlacement(1);
		return newCategory;
	}

	private EntityBuilder createProduct(SESSION session, int primaryKey, EntityReferenceContract brand, EntityReferenceContract category) {
		final EntityBuilder newProduct = session.createNewEntity(PRODUCT, primaryKey);
		newProduct.setAttribute("code", "SX87Y800BE");
		newProduct.setAttribute("name", LOCALE_CZECH, "iQ700 Plně vestavná myčka nádobí 60 cm XXL");
		newProduct.setAttribute("name", Locale.ENGLISH, "iQ700 Fully built-in dishwasher 60 cm XXL");
		newProduct.setAttribute("temperatures", NumberRange.between(45, 70));
		newProduct.setAttribute("visible", true);
		newProduct.setAttribute("type", new ProductType[]{ProductType.HOME, ProductType.INDUSTRIAL});
		newProduct.setAttribute("noise", 'B');
		newProduct.setAttribute("width", (byte) 59);
		newProduct.setAttribute("height", (short) 86);
		newProduct.setAttribute("waterConsumption", new BigDecimal("9.5"));
		newProduct.setAttribute("energyConsumption", 64L);
		newProduct.setAttribute("ageSort", new Multiple(2018, 5));
		newProduct.setAttribute("producedTime", LocalTime.of(17, 45, 11));
		newProduct.setAttribute("producedDate", LocalDate.of(2021, 2, 1));
		newProduct.setAttribute("qaPassed", LocalDateTime.of(2021, 2, 2, 0, 15, 45));
		newProduct.setAttribute("arrivedAtCZ", ZonedDateTime.of(2021, 2, 2, 0, 15, 45, 0, ZoneId.of("Europe/Prague")));

		newProduct.setReference(
			BRAND, Objects.requireNonNull(brand.getPrimaryKey()),
			with -> {
				with.setGroup("BRAND_GROUP", 89);
				with.setAttribute("distributor", "Siemens GmBH");
			}
		);
		newProduct.setReference(
			CATEGORY, Objects.requireNonNull(category.getPrimaryKey()),
			whichIs -> {
				whichIs.setAttribute("priority", 15L);
				whichIs.setAttribute("visibleToCustomerTypes", new Integer[]{1, 8, 9});
			}
		);

		newProduct.setPriceInnerRecordHandling(PriceInnerRecordHandling.FIRST_OCCURRENCE);
		newProduct.setPrice(1, "basic", CZK, new BigDecimal("15000"), new BigDecimal("21"), new BigDecimal("18150"), true);
		newProduct.setPrice(2, "basic", EUR, new BigDecimal("555.5"), new BigDecimal("21"), new BigDecimal("672.16"), true);
		newProduct.setPrice(1, "reference", CZK, new BigDecimal("20000"), new BigDecimal("21"), new BigDecimal("24200"), false);
		newProduct.setPrice(2, "reference", EUR, new BigDecimal("799"), new BigDecimal("21"), new BigDecimal("966.79"), false);

		newProduct.setAssociatedData("localizedLabels", LOCALE_CZECH, new LocalizedLabels().withLabel("name", "iQ700 Plně vestavná myčka nádobí 60 cm XXL"));
		newProduct.setAssociatedData("localizedLabels", Locale.ENGLISH, new LocalizedLabels().withLabel("name", "iQ700 Fully built-in dishwasher 60 cm XXL"));
		newProduct.setAssociatedData("stockQuantity", new StockQuantity().withQuantity(1, new BigDecimal("124")).withQuantity(2, new BigDecimal("783")));

		return newProduct;
	}

	private EntityBuilder updateProduct(SealedEntity createdEntity, EntityReferenceContract newCategory) {
		return createdEntity.open()
			// add new
			.setAttribute("aquaStop", true)
			// update existing
			.setAttribute("width", (byte) 60)
			// remove existing
			.removeAttribute("waterConsumption")
			// add new
			.setAssociatedData("localizedLabels", Locale.CHINA, new LocalizedLabels().withLabel("name", "iQ700完全內置式洗碗機60厘米XXL"))
			// update existing
			.setAssociatedData("localizedLabels", LOCALE_CZECH, new LocalizedLabels().withLabel("name", "iQ700 Plně vestavná myčka nádobí 60 cm XXL (ve slevě)"))
			// remove existing
			.removeAssociatedData("localizedLabels", Locale.ENGLISH)
			// add new
			.setPrice(3, "action", EUR, new BigDecimal("499"), new BigDecimal("21"), new BigDecimal("603.79"), true)
			// update existing
			.setPrice(2, "reference", EUR, new BigDecimal("899"), new BigDecimal("21"), new BigDecimal("1087.79"), false)
			// remove existing
			.removePrice(1, "reference", CZK)
			// add new
			.setReference(newCategory.getType(), newCategory.getPrimaryKey())
			// update existing
			.setReference(
				CATEGORY, 1,
				thatIs -> {
					thatIs.setGroup("CATEGORY_GROUP", 45);
					thatIs.setAttribute("priority", 20L);
					thatIs.removeAttribute("visibleToCustomerTypes");
				})
			// remove existing
			.removeReference(
				BRAND, 1
			);
	}

	@Nonnull
	private DateTimeRange createDateRange(int yearFrom, int yearTo) {
		return DateTimeRange.between(
			LocalDateTime.of(yearFrom, 1, 1, 0, 0, 0),
			LocalDateTime.of(yearTo, 1, 1, 0, 0, 0),
			ZoneId.systemDefault()
		);
	}

	private SealedEntity getBrandById(SESSION session, int primaryKey) {
		final EvitaResponseBase<SealedEntity> response = session.query(
			query(
				entities(BRAND),
				filterBy(primaryKey(primaryKey)),
				require(entityBody(), attributes(), dataInLanguage())
			),
			SealedEntity.class
		);
		return response.getRecordData().get(0);
	}

	private SealedEntity getProductById(SESSION session, int primaryKey) {
		final EvitaResponseBase<SealedEntity> response = session.query(
			query(
				entities(PRODUCT),
				filterBy(primaryKey(primaryKey)),
				require(fullEntity())
			),
			SealedEntity.class
		);
		return response.getRecordData().get(0);
	}

	private void assertBrand(EVITA evita, int primaryKey, Locale locale, String code, String name, String logo, int productCount) {
		evita.queryCatalog(TEST_CATALOG, session -> {
			assertBrandInSession(session, primaryKey, locale, code, name, logo, productCount);
			return null;
		});
	}

	private void assertBrandInSession(SESSION session, int primaryKey, Locale locale, String code, String name, String logo, int productCount) {
		final EvitaResponseBase<SealedEntity> response = session.query(
			query(
				entities(BRAND),
				filterBy(
					and(
						primaryKey(primaryKey),
						language(locale)
					)
				),
				require(
					entityBody(), attributes()
				)
			),
			SealedEntity.class
		);
		assertEquals(1, response.getRecordData().size());
		final SealedEntity brand = response.getRecordData().get(0);

		assertEquals(code, brand.getAttribute("code", locale));
		assertEquals(name, brand.getAttribute("name", locale));
		assertEquals(logo, brand.getAttribute("logo", locale));
		assertEquals(productCount, (Integer) brand.getAttribute("productCount", locale));
	}

	private enum ProductType {

		INDUSTRIAL, HOME

	}

	@RequiredArgsConstructor
	@EqualsAndHashCode
	@ToString
	public static class LocalizedLabels implements Serializable {
		private static final long serialVersionUID = -608008423373881676L;
		@Getter private final Map<String, String> labels;

		public LocalizedLabels() {
			this.labels = new HashMap<>();
		}

		public void addLabel(String code, String label) {
			this.labels.put(code, label);
		}

		public String getLabel(String code) {
			return this.labels.get(code);
		}

		public LocalizedLabels withLabel(String code, String label) {
			addLabel(code, label);
			return this;
		}
	}

	@RequiredArgsConstructor
	@EqualsAndHashCode
	@ToString
	public static class StockQuantity implements Serializable {
		private static final long serialVersionUID = 3168900957425721663L;
		@Getter private final Map<Integer, BigDecimal> stockQuantity;

		public StockQuantity() {
			this.stockQuantity = new HashMap<>();
		}

		public void setStockQuantity(int stockId, BigDecimal quantity) {
			this.stockQuantity.put(stockId, quantity);
		}

		public BigDecimal getStockQuantity(int stockId) {
			return this.stockQuantity.get(stockId);
		}

		public StockQuantity withQuantity(int stockId, BigDecimal quantity) {
			setStockQuantity(stockId, quantity);
			return this;
		}
	}

}
