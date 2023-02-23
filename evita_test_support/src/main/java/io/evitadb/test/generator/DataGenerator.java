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

package io.evitadb.test.generator;

import com.github.javafaker.Commerce;
import com.github.javafaker.Faker;
import io.evitadb.api.EvitaSessionBase;
import io.evitadb.api.data.*;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.structure.InitialEntityBuilder;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.schema.*;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.test.Entities;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.PMPTT;
import one.edee.oss.pmptt.dao.memory.MemoryStorage;
import one.edee.oss.pmptt.exception.MaxLevelExceeded;
import one.edee.oss.pmptt.exception.SectionExhausted;
import one.edee.oss.pmptt.model.Hierarchy;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Helper class used to quickly setup test data. Allows to generate pseudo random data based on {@link com.github.javafaker.Faker}
 * library.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@SuppressWarnings("ALL")
public class DataGenerator {
	public static final Locale CZECH_LOCALE = new Locale("cs", "CZ");
	public static final String ATTRIBUTE_NAME = "name";
	public static final String ATTRIBUTE_CODE = "code";
	public static final String ATTRIBUTE_URL = "url";
	public static final String ATTRIBUTE_EAN = "ean";
	public static final String ATTRIBUTE_PRIORITY = "priority";
	public static final String ATTRIBUTE_VALIDITY = "validity";
	public static final String ATTRIBUTE_QUANTITY = "quantity";
	public static final String ATTRIBUTE_ALIAS = "alias";
	public static final String ASSOCIATED_DATA_REFERENCED_FILES = "referencedFiles";
	public static final String ASSOCIATED_DATA_LABELS = "labels";
	public static final Currency CURRENCY_CZK = Currency.getInstance("CZK");
	public static final Currency CURRENCY_EUR = Currency.getInstance("EUR");
	public static final Currency CURRENCY_USD = Currency.getInstance("USD");
	public static final Currency CURRENCY_GBP = Currency.getInstance("GBP");
	public static final String PRICE_LIST_BASIC = "basic";
	public static final String PRICE_LIST_REFERENCE = "reference";
	public static final String PRICE_LIST_SELLOUT = "sellout";
	public static final String PRICE_LIST_VIP = "vip";
	public static final String PRICE_LIST_B2B = "b2b";
	public static final String PRICE_LIST_INTRODUCTION = "introduction";
	public static final String[] PRICE_LIST_NAMES = new String[]{PRICE_LIST_BASIC, PRICE_LIST_REFERENCE, PRICE_LIST_SELLOUT, PRICE_LIST_VIP, PRICE_LIST_B2B, PRICE_LIST_INTRODUCTION};
	public static final Currency[] CURRENCIES = new Currency[]{
		CURRENCY_CZK, CURRENCY_EUR, CURRENCY_USD, CURRENCY_GBP
	};
	public static final Predicate<String> TRUE_PREDICATE = s -> true;
	private static final ReflectionLookup REFLECTION_LOOKUP = new ReflectionLookup(ReflectionCachingBehaviour.CACHE);
	private static final DateTimeRange[] DATE_TIME_RANGES = new DateTimeRange[]{
		DateTimeRange.between(LocalDateTime.MIN, LocalDateTime.MAX, ZoneId.systemDefault()),
		DateTimeRange.between(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.of(2012, 12, 31, 0, 0), ZoneId.systemDefault()),
		DateTimeRange.between(LocalDateTime.of(2012, 1, 1, 0, 0), LocalDateTime.of(2014, 12, 31, 0, 0), ZoneId.systemDefault()),
		DateTimeRange.between(LocalDateTime.of(2014, 1, 1, 0, 0), LocalDateTime.of(2016, 12, 31, 0, 0), ZoneId.systemDefault()),
		DateTimeRange.between(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.of(2014, 12, 31, 0, 0), ZoneId.systemDefault()),
		DateTimeRange.between(LocalDateTime.of(2010, 1, 1, 0, 0), LocalDateTime.of(2016, 12, 31, 0, 0), ZoneId.systemDefault()),
	};
	private static final BigDecimal VAT_RATE = new BigDecimal("21");
	private static final BigDecimal VAT_MULTIPLICATOR = VAT_RATE.setScale(2, RoundingMode.UNNECESSARY).divide(new BigDecimal("100.00"), RoundingMode.HALF_UP).add(BigDecimal.ONE);
	/**
	 * Holds information about number of unique values.
	 */
	final Map<Serializable, Map<Object, Integer>> uniqueSequencer = new HashMap<>();
	final Map<Serializable, SortableAttributesChecker> sortableAttributesChecker = new HashMap<>();
	final Map<Integer, Integer> parameterIndex = new HashMap<>();
	/**
	 * Holds function that is used for generating price inner record handling strategy.
	 */
	private final Function<Faker, PriceInnerRecordHandling> priceInnerRecordHandlingGenerator;
	/**
	 * Holds information about created hierarchies for generated / modified entities indexed by their type.
	 */
	private final PMPTT hierarchies = new PMPTT(new MemoryStorage());

	public DataGenerator() {
		this.priceInnerRecordHandlingGenerator = faker -> PriceInnerRecordHandling.NONE;
	}

	public DataGenerator(Function<Faker, PriceInnerRecordHandling> priceInnerRecordHandlingGenerator) {
		this.priceInnerRecordHandlingGenerator = priceInnerRecordHandlingGenerator;
	}

	/**
	 * Clears internal data structures.
	 */
	public void clear() {
		this.uniqueSequencer.clear();
		this.sortableAttributesChecker.clear();
		this.parameterIndex.clear();
		this.hierarchies
			.getExistingHierarchyCodes()
			.forEach(this.hierarchies::removeHierarchy);
	}

	/**
	 * Generates requested number of Evita entities fully setup with data according to passed schema definition.
	 * Initialization occurs randomly, but respects passed seed so that when called multiple times with same configuration
	 * in passed arguments the result is same.
	 *
	 * @param referencedEntityResolver for accessing random referenced entities
	 * @param seed                     makes generation pseudorandom
	 */
	public Stream<EntityBuilder> generateEntities(@Nonnull EntitySchema schema, @Nonnull BiFunction<Serializable, Faker, Integer> referencedEntityResolver, long seed) {
		final Map<Object, Integer> uniqueSequencer = this.uniqueSequencer.computeIfAbsent(
			schema.getName(),
			serializable -> new HashMap<>()
		);
		final SortableAttributesChecker sortableAttributesHolder = this.sortableAttributesChecker.computeIfAbsent(
			schema.getName(),
			serializable -> new SortableAttributesChecker()
		);
		final Map<Locale, Faker> localeFaker = new HashMap<>();
		final Function<Locale, Faker> localizedFakerFetcher = locale -> localeFaker.computeIfAbsent(locale, theLocale -> new Faker(new Random(seed)));
		final Faker genericFaker = new Faker(new Random(seed));
		final Set<Locale> allLocales = schema.getLocales();
		final Set<Currency> allCurrencies = new LinkedHashSet<>(Arrays.asList(CURRENCIES));
		final Set<String> allPriceLists = new LinkedHashSet<>(Arrays.asList(PRICE_LIST_NAMES));
		final Hierarchy hierarchy = getHierarchyIfNeeded(hierarchies, schema);

		return Stream.generate(() -> {
			// create new entity of desired type
			final EntityBuilder detachedBuilder = new InitialEntityBuilder(
				schema,
				// generate unique primary key (only when required by schema)
				schema.isWithGeneratedPrimaryKey() ? null : uniqueSequencer.merge(new SchemaKey(schema.getName()), 1, Integer::sum)
			);

			generateRandomHierarchy(schema, referencedEntityResolver, hierarchy, genericFaker, detachedBuilder);

			final List<Locale> usedLocales = pickRandomFromSet(genericFaker, allLocales);

			generateRandomAttributes(schema.getName(), schema.getAttributes().values(), uniqueSequencer, sortableAttributesHolder, TRUE_PREDICATE, localizedFakerFetcher, genericFaker, detachedBuilder, usedLocales);
			generateRandomAssociatedData(schema, genericFaker, detachedBuilder, usedLocales);

			generateRandomPrices(schema, uniqueSequencer, genericFaker, allCurrencies, allPriceLists, detachedBuilder, priceInnerRecordHandlingGenerator);
			generateRandomReferences(schema, referencedEntityResolver, uniqueSequencer, parameterIndex, sortableAttributesHolder, localizedFakerFetcher, genericFaker, detachedBuilder, usedLocales);

			return detachedBuilder;
		});
	}

	/**
	 * Creates function that randomly modifies contents of the existing entity and returns modified builder.
	 */
	@Nonnull
	public ModificationFunction createModificationFunction(@Nonnull BiFunction<Serializable, Faker, Integer> referencedEntityResolver, Random random) {
		final Map<Locale, Faker> localeFaker = new HashMap<>();
		final Function<Locale, Faker> localizedFakerFetcher = locale -> localeFaker.computeIfAbsent(locale, theLocale -> new Faker(random));
		final Faker genericFaker = new Faker(random);
		final HashSet<Currency> allCurrencies = new LinkedHashSet<>(Arrays.asList(CURRENCIES));
		final Set<String> allPriceLists = new LinkedHashSet<>(Arrays.asList(PRICE_LIST_NAMES));

		return new ModificationFunction(
			genericFaker, hierarchies, uniqueSequencer, sortableAttributesChecker, allCurrencies, allPriceLists,
			priceInnerRecordHandlingGenerator, referencedEntityResolver, localizedFakerFetcher, parameterIndex
		);
	}

	@Nonnull
	public EntitySchema getSamplePriceListSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSamplePriceListSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSamplePriceListSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return new EntitySchemaBuilder(
			new EntitySchema(Entities.PRICE_LIST),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* price lists are not organized in the tree */
			.withoutHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			.withAttribute(ATTRIBUTE_PRIORITY, Long.class, whichIs -> whichIs.sortable())
			.withAttribute(ATTRIBUTE_VALIDITY, DateTimeRange.class, whichIs -> whichIs.filterable())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleCategorySchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleCategorySchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleCategorySchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession, Consumer<EntitySchemaBuilder> schemaAlterLogic) {
		return getSampleCategorySchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		}, schemaAlterLogic);
	}

	@Nonnull
	public EntitySchema getSampleCategorySchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return getSampleCategorySchema(schemaUpdater, null);
	}

	@Nonnull
	public EntitySchema getSampleCategorySchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater, Consumer<EntitySchemaBuilder> schemaAlterLogic) {
		final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(
			new EntitySchema(Entities.CATEGORY),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* for sake of generating hierarchies we need to generate keys by ourselves */
			.withoutGeneratedPrimaryKey()
			/* categories are organized in the tree */
			.withHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			.withAttribute(ATTRIBUTE_URL, String.class, whichIs -> whichIs.unique().localized())
			.withAttribute(ATTRIBUTE_PRIORITY, Long.class, whichIs -> whichIs.sortable())
			.withAttribute(ATTRIBUTE_VALIDITY, DateTimeRange.class, whichIs -> whichIs.filterable());
		ofNullable(schemaAlterLogic).ifPresent(it -> it.accept(schemaBuilder));
		return schemaBuilder
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleBrandSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleBrandSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleBrandSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return new EntitySchemaBuilder(
			new EntitySchema(Entities.BRAND),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* brands are not organized in the tree */
			.withoutHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			.withAttribute(ATTRIBUTE_URL, String.class, whichIs -> whichIs.unique().localized())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleStoreSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleStoreSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleStoreSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return new EntitySchemaBuilder(
			new EntitySchema(Entities.STORE),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* stores are not organized in the tree */
			.withoutHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleParameterGroupSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleParameterGroupSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleParameterGroupSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return new EntitySchemaBuilder(
			new EntitySchema(Entities.PARAMETER_GROUP),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* stores are not organized in the tree */
			.withoutHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleParameterSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleParameterSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleParameterSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return new EntitySchemaBuilder(
			new EntitySchema(Entities.PARAMETER),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* stores are not organized in the tree */
			.withoutHierarchy()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Nonnull
	public EntitySchema getSampleProductSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession) {
		return getSampleProductSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		});
	}

	@Nonnull
	public EntitySchema getSampleProductSchema(@Nonnull EvitaSessionBase<?, ?, ?, ?, ?> evitaSession, Consumer<EntitySchemaBuilder> schemaAlterLogic) {
		return getSampleProductSchema(entitySchema -> {
			evitaSession.defineSchema(entitySchema);
			return evitaSession.getEntitySchema(entitySchema.getName());
		}, schemaAlterLogic);
	}

	@Nonnull
	public EntitySchema getSampleProductSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater) {
		return getSampleProductSchema(schemaUpdater, null);
	}

	@Nonnull
	public EntitySchema getSampleProductSchema(@Nonnull UnaryOperator<EntitySchema> schemaUpdater, Consumer<EntitySchemaBuilder> schemaAlterLogic) {
		final EntitySchemaBuilder schemaBuilder = new EntitySchemaBuilder(
			new EntitySchema(Entities.PRODUCT),
			schemaUpdater
		)
			/* all is strictly verified */
			.verifySchemaStrictly()
			/* let Evita generates the key */
			.withGeneratedPrimaryKey()
			/* product are not organized in the tree */
			.withoutHierarchy()
			/* prices are referencing another entity stored in Evita */
			.withPrice()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, CZECH_LOCALE)
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute(ATTRIBUTE_NAME, String.class, whichIs -> whichIs.filterable().localized().sortable())
			.withAttribute(ATTRIBUTE_CODE, String.class, whichIs -> whichIs.unique())
			.withAttribute(ATTRIBUTE_URL, String.class, whichIs -> whichIs.unique().localized())
			.withAttribute(ATTRIBUTE_EAN, String.class, whichIs -> whichIs.filterable())
			.withAttribute(ATTRIBUTE_PRIORITY, Long.class, whichIs -> whichIs.sortable())
			.withAttribute(ATTRIBUTE_VALIDITY, DateTimeRange.class, whichIs -> whichIs.filterable())
			.withAttribute(ATTRIBUTE_QUANTITY, BigDecimal.class, whichIs -> whichIs.filterable().indexDecimalPlaces(2))
			.withAttribute(ATTRIBUTE_ALIAS, Boolean.class, whichIs -> whichIs.filterable())
			/* here we define set of associated data, that can be stored along with entity */
			.withAssociatedData(ASSOCIATED_DATA_REFERENCED_FILES, ReferencedFileSet.class)
			.withAssociatedData(ASSOCIATED_DATA_LABELS, Labels.class, whichIs -> whichIs.localized())
			/* here we define facets that relate to another entities stored in Evita */
			.withReferenceToEntity(
				Entities.CATEGORY,
				whichIs ->
					/* we can specify special attributes on relation */
					whichIs.indexed()
						.withAttribute("categoryPriority", Long.class, thatIs -> thatIs.sortable())
			)
			/* for indexed facets we can compute "counts" */
			.withReferenceToEntity(Entities.BRAND, whichIs -> whichIs.faceted())
			/* facets may be also represented be entities unknown to Evita */
			.withReferenceTo(Entities.STORE, whichIs -> whichIs.faceted());

		/* apply custom logic if passed */
		ofNullable(schemaAlterLogic)
			.ifPresent(it -> it.accept(schemaBuilder));

		return schemaBuilder
			/* finally, apply schema changes */
			.applyChanges();
	}

	/**
	 * Returns hierarchy connected with passed entity type.
	 */
	@Nonnull
	public Hierarchy getHierarchy(@Nonnull Serializable entityType) {
		return hierarchies.getOrCreateHierarchy(entityType.toString(), (short) 5, (short) 10);
	}

	/**
	 * Returns hierarchy connected with passed entity type.
	 */
	@Nonnull
	public static Hierarchy getHierarchy(@Nonnull PMPTT hierarchies, @Nonnull Serializable entityType) {
		return hierarchies.getOrCreateHierarchy(entityType.toString(), (short) 5, (short) 10);
	}

	/**
	 * Returns index that maps parameters to their groups.
	 */
	public Map<Integer, Integer> getParameterIndex() {
		return parameterIndex;
	}

	@Nonnull
	private static <T> T pickRandomOneFromSet(Faker genericFaker, Set<T> set) {
		if (set.isEmpty()) {
			throw new IllegalStateException("Set is empty!");
		}
		final Iterator<T> it = set.iterator();
		T itemToUse = it.next();
		for (int i = 0; i < genericFaker.random().nextInt(set.size()); i++) {
			itemToUse = it.next();
		}
		return itemToUse;
	}

	@Nonnull
	private static <T> List<T> pickRandomFromSet(Faker genericFaker, Set<T> set) {
		if (set.isEmpty()) {
			return Collections.emptyList();
		}
		final Integer itemsCount = genericFaker.random().nextInt(1, set.size());
		final List<T> usedItems = new ArrayList<>(itemsCount);
		while (usedItems.size() < itemsCount) {
			final Iterator<T> it = set.iterator();
			T itemToUse = null;
			for (int i = 0; i <= genericFaker.random().nextInt(set.size()); i++) {
				itemToUse = it.next();
			}
			if (!usedItems.contains(itemToUse)) {
				usedItems.add(itemToUse);
			}
		}
		return usedItems;
	}

	private static void generateRandomHierarchy(@Nonnull EntitySchema schema, @Nonnull BiFunction<Serializable, Faker, Integer> referencedEntityResolver, @Nullable Hierarchy hierarchy, @Nonnull Faker genericFaker, @Nonnull EntityBuilder detachedBuilder) {
		if (hierarchy != null) {
			try {
				// when there are very few root items, force to create some by making next other one as root
				final Integer parentKey = hierarchy.getRootItems().size() < 5 && genericFaker.random().nextBoolean() ?
					null : referencedEntityResolver.apply(schema.getName(), genericFaker);
				if (parentKey == null) {
					hierarchy.createRootItem(Objects.requireNonNull(detachedBuilder.getPrimaryKey()).toString());
					detachedBuilder.setHierarchicalPlacement(
						// we can't easily randomize the position
						hierarchy.getRootItems().size()
					);
				} else {
					hierarchy.createItem(Objects.requireNonNull(detachedBuilder.getPrimaryKey()).toString(), parentKey.toString());
					detachedBuilder.setHierarchicalPlacement(
						parentKey,
						// we can't easily randomize the position
						hierarchy.getChildItems(parentKey.toString()).size()
					);
				}
			} catch (MaxLevelExceeded | SectionExhausted ignores) {
				// just repeat again
				generateRandomHierarchy(schema, referencedEntityResolver, hierarchy, genericFaker, detachedBuilder);
			}
		}
	}

	private static void generateRandomAttributes(Serializable entityType, Collection<AttributeSchema> attributeSchema, Map<Object, Integer> uniqueSequencer, SortableAttributesChecker sortableAttributesHolder, Predicate<String> attributeFilter, Function<Locale, Faker> localeFaker, Faker genericFaker, AttributesEditor<?> attributesEditor, List<Locale> usedLocales) {
		for (AttributeSchema attribute : attributeSchema) {
			final Class<? extends Serializable> type = attribute.getType();
			final String attributeName = attribute.getName();
			if (!attributeFilter.test(attributeName)) {
				continue;
			}
			if (!attribute.isUnique() && genericFaker.random().nextInt(10) == 0) {
				// randomly skip attributes
				continue;
			}
			if (attribute.isLocalized()) {
				for (Locale usedLocale : usedLocales) {
					generateAndSetAttribute(
						uniqueSequencer, sortableAttributesHolder, attributesEditor, attribute, type, entityType, attributeName,
						localeFaker.apply(usedLocale),
						value -> attributesEditor.setAttribute(attributeName, usedLocale, value)
					);
				}
			} else {
				generateAndSetAttribute(
					uniqueSequencer, sortableAttributesHolder, attributesEditor, attribute, type, entityType, attributeName, genericFaker,
					value -> attributesEditor.setAttribute(attributeName, value)
				);
			}
		}
	}

	private static void generateRandomAssociatedData(@Nonnull EntitySchema schema, Faker genericFaker, EntityBuilder detachedBuilder, List<Locale> usedLocales) {
		for (AssociatedDataSchema associatedData : schema.getAssociatedData().values()) {
			final String associatedDataName = associatedData.getName();
			if (genericFaker.random().nextInt(5) == 0) {
				// randomly skip associated data
				continue;
			}
			if (associatedData.isLocalized()) {
				for (Locale usedLocale : usedLocales) {
					generateAndSetAssociatedData(
						associatedData,
						genericFaker,
						value -> detachedBuilder.setAssociatedData(associatedDataName, usedLocale, value)
					);
				}
			} else {
				generateAndSetAssociatedData(
					associatedData,
					genericFaker,
					value -> detachedBuilder.setAssociatedData(associatedDataName, value)
				);
			}
		}
	}

	private static void generateRandomPrices(@Nonnull EntitySchema schema, Map<Object, Integer> uniqueSequencer, Faker genericFaker, Set<Currency> allCurrencies, Set<String> allPriceLists, EntityBuilder detachedBuilder, Function<Faker, PriceInnerRecordHandling> priceInnerRecordHandlingGenerator) {
		if (schema.isWithPrice()) {
			detachedBuilder.setPriceInnerRecordHandling(priceInnerRecordHandlingGenerator.apply(genericFaker));

			if (detachedBuilder.getPriceInnerRecordHandling() == PriceInnerRecordHandling.NONE) {
				generateRandomPrices(schema, null, uniqueSequencer, genericFaker, allCurrencies, allPriceLists, detachedBuilder);
			} else {
				final Integer numberOfInnerRecords = genericFaker.random().nextInt(2, 15);
				final Set<Integer> alreadyAssignedInnerIds = new HashSet<>();
				for (int i = 0; i < numberOfInnerRecords; i++) {
					int innerRecordId;
					do {
						innerRecordId = genericFaker.random().nextInt(1, numberOfInnerRecords + 1);
					} while (alreadyAssignedInnerIds.contains(innerRecordId));

					alreadyAssignedInnerIds.add(innerRecordId);
					generateRandomPrices(schema, innerRecordId, uniqueSequencer, genericFaker, allCurrencies, allPriceLists, detachedBuilder);
				}
			}
		}
	}

	private static void generateRandomPrices(@Nonnull EntitySchema schema, Integer innerRecordId, Map<Object, Integer> uniqueSequencer, Faker genericFaker, Set<Currency> allCurrencies, Set<String> allPriceLists, EntityBuilder detachedBuilder) {
		final List<Currency> usedCurrencies = pickRandomFromSet(genericFaker, allCurrencies);
		Iterator<Currency> currencyToUse = null;
		final Integer priceCount = genericFaker.random().nextInt(1, allPriceLists.size());
		final LinkedHashSet<String> priceListsToUse = new LinkedHashSet<>(allPriceLists);
		for (int i = 0; i < priceCount; i++) {
			if (currencyToUse == null || !currencyToUse.hasNext()) {
				currencyToUse = usedCurrencies.iterator();
			}
			final String priceList = pickRandomOneFromSet(genericFaker, priceListsToUse);
			// avoid generating multiple prices for the same price list
			priceListsToUse.remove(priceList);
			final BigDecimal basePrice = new BigDecimal(genericFaker.commerce().price());
			final DateTimeRange validity = genericFaker.bool().bool() ? DATE_TIME_RANGES[genericFaker.random().nextInt(DATE_TIME_RANGES.length)] : null;
			final boolean randomSellableFlag = genericFaker.random().nextInt(8) == 0;
			final boolean sellable;
			if (PRICE_LIST_REFERENCE.equals(priceList)) {
				sellable = false;
			} else if (PRICE_LIST_BASIC.equals(priceList)) {
				sellable = true;
			} else {
				sellable = randomSellableFlag;
			}
			final Integer priceId = uniqueSequencer.merge(new PriceKey(schema.getName()), 1, Integer::sum);
			final Currency currency = currencyToUse.next();
			final BigDecimal basePriceWithVat = basePrice.multiply(VAT_MULTIPLICATOR).setScale(2, RoundingMode.HALF_UP);

			detachedBuilder.setPrice(
				priceId,
				priceList,
				currency,
				innerRecordId,
				basePrice,
				VAT_RATE,
				basePriceWithVat,
				validity,
				sellable
			);
		}
	}

	private static void generateRandomReferences(@Nonnull EntitySchema schema, @Nonnull BiFunction<Serializable, Faker, Integer> referencedEntityResolver, Map<Object, Integer> uniqueSequencer, Map<Integer, Integer> parameterGroupIndex, SortableAttributesChecker sortableAttributesHolder, Function<Locale, Faker> localeFaker, Faker genericFaker, EntityBuilder detachedBuilder, List<Locale> usedLocales) {
		final Set<Serializable> referencableEntityTypes = schema.getReferences()
			.values()
			.stream()
			.map(ReferenceSchema::getEntityType)
			.collect(Collectors.toCollection(LinkedHashSet::new));

		final List<Serializable> referencedTypes = pickRandomFromSet(genericFaker, referencableEntityTypes);
		for (Serializable referencedType : referencedTypes) {
			final int count;
			if (Entities.CATEGORY.equals(referencedType)) {
				count = genericFaker.random().nextInt(4);
			} else if (Entities.STORE.equals(referencedType)) {
				count = genericFaker.random().nextInt(8);
			} else if (Entities.PARAMETER.equals(referencedType)) {
				count = genericFaker.random().nextInt(16);
			} else {
				count = 1;
			}
			final Predicate<String> attributePredicate = new SingleSortableAttributePredicate(
				schema.getReference(referencedType), detachedBuilder
			);
			for (int i = 0; i < count; i++) {
				final Integer referencedEntity = referencedEntityResolver.apply(referencedType, genericFaker);
				if (referencedEntity != null) {
					detachedBuilder.setReference(
						referencedType,
						Objects.requireNonNull(referencedEntity),
						thatIs -> {
							if (Entities.PARAMETER.equals(referencedType)) {
								thatIs.setGroup(
									Entities.PARAMETER_GROUP,
									parameterGroupIndex.computeIfAbsent(
										referencedEntity,
										parameterId -> referencedEntityResolver.apply(Entities.PARAMETER_GROUP, genericFaker)
									)
								);
							}
							sortableAttributesHolder.executeWithPredicate(
								Set::isEmpty,
								() -> generateRandomAttributes(
									schema.getName(), schema.getReference(referencedType).getAttributes().values(),
									uniqueSequencer,
									sortableAttributesHolder,
									attributePredicate,
									localeFaker, genericFaker, thatIs, usedLocales
								)
							);
						}
					);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private static <T extends Serializable & Comparable<?>> void generateAndSetAttribute(Map<Object, Integer> uniqueSequencer, SortableAttributesChecker sortableAttributesChecker, AttributesEditor<?> attributesBuilder, AttributeSchema attribute, Class<? extends Serializable> type, Serializable entityType, String attributeName, Faker fakerToUse, Consumer<T> generatedValueWriter) {
		Object value;
		int sanityCheck = 0;
		do {
			if (String.class.equals(type)) {
				value = generateRandomString(uniqueSequencer, attributesBuilder, attribute, entityType, attributeName, fakerToUse);
			} else if (Boolean.class.equals(type)) {
				value = fakerToUse.bool().bool();
			} else if (Integer.class.equals(type)) {
				value = generateRandomInteger(uniqueSequencer, attribute, attributeName, fakerToUse);
			} else if (Long.class.equals(type)) {
				value = generateRandomLong(uniqueSequencer, attribute, attributeName, fakerToUse);
			} else if (BigDecimal.class.equals(type)) {
				value = generateRandomBigDecimal(fakerToUse, attribute.getIndexedDecimalPlaces());
			} else if (type.isArray() && BigDecimal.class.equals(type.getComponentType())) {
				final BigDecimal[] randomArray = new BigDecimal[fakerToUse.random().nextInt(8)];
				for (int i = 0; i < randomArray.length; i++) {
					randomArray[i] = generateRandomBigDecimal(fakerToUse, attribute.getIndexedDecimalPlaces());
				}
				value = randomArray;
			} else if (ZonedDateTime.class.equals(type)) {
				value = generateRandomZonedDateTime(fakerToUse);
			} else if (LocalDateTime.class.equals(type)) {
				value = generateRandomLocalDateTime(fakerToUse);
			} else if (LocalDate.class.equals(type)) {
				value = generateRandomLocalDate(fakerToUse);
			} else if (LocalTime.class.equals(type)) {
				value = generateRandomLocalTime(fakerToUse);
			} else if (DateTimeRange.class.equals(type)) {
				value = generateRandomDateTimeRange(fakerToUse);
			} else if (NumberRange.class.equals(type)) {
				value = generateRandomNumberRange(fakerToUse);
			} else if (type.isArray() && NumberRange.class.equals(type.getComponentType())) {
				final NumberRange[] randomArray = new NumberRange[fakerToUse.random().nextInt(8)];
				for (int i = 0; i < randomArray.length; i++) {
					randomArray[i] = generateRandomNumberRange(fakerToUse);
				}
				value = randomArray;
			} else if (Multiple.class.equals(type)) {
				value = new Multiple(fakerToUse.random().nextInt(10000), fakerToUse.random().nextInt(10000));
			} else if (type.isEnum()) {
				final Object[] values = type.getEnumConstants();
				value = values[fakerToUse.random().nextInt(values.length)];
			} else {
				throw new IllegalArgumentException("Unsupported auto-generated value type: " + type);
			}
			if (attribute.isSortable()) {
				value = sortableAttributesChecker.getUniqueAttribute(attributeName, value);
			}
		} while (value == null && sanityCheck++ < 1000);

		if (attribute.isSortable() && value == null) {
			throw new IllegalStateException("Cannot generate unique " + attributeName + " even in 1000 iterations!");
		}

		generatedValueWriter.accept((T) value);
	}

	private static <T extends Serializable & Comparable<?>> T generateRandomDateTimeRange(Faker fakerToUse) {
		final T value;
		value = (T) (DATE_TIME_RANGES[fakerToUse.random().nextInt(DATE_TIME_RANGES.length)]);
		return value;
	}

	private static <T extends Serializable & Comparable<?>> T generateRandomNumberRange(Faker fakerToUse) {
		final T value;
		final int from = fakerToUse.number().numberBetween(1, 100);
		final int to = fakerToUse.number().numberBetween(from, from + 100);
		value = (T) (NumberRange.between(from, to));
		return value;
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomLong(Map<Object, Integer> uniqueSequencer, AttributeSchema attribute, String attributeName, Faker fakerToUse) {
		final T value;
		if (attribute.isUnique()) {
			value = (T) uniqueSequencer.merge(new AttributeKey(attributeName), 1, Integer::sum);
		} else {
			value = (T) (Long) fakerToUse.number().numberBetween(1L, 100000L);
		}
		return value;
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomInteger(Map<Object, Integer> uniqueSequencer, AttributeSchema attribute, String attributeName, Faker fakerToUse) {
		final T value;
		if (attribute.isUnique()) {
			value = (T) uniqueSequencer.merge(new AttributeKey(attributeName), 1, Integer::sum);
		} else {
			value = (T) (Integer) fakerToUse.number().numberBetween(1, 2000);
		}
		return value;
	}

	private static <T extends Serializable & Comparable<?>> T generateRandomString(Map<Object, Integer> uniqueSequencer, AttributesEditor<?> attributesBuilder, AttributeSchema attribute, Serializable entityType, String attributeName, Faker fakerToUse) {
		final T value;
		final String suffix = attribute.isUnique() ? " " + uniqueSequencer.merge(new AttributeKey(attributeName), 1, Integer::sum) : "";
		final Optional<String> assignedName = attributesBuilder.getAttributeValues()
			.stream()
			.filter(it -> ATTRIBUTE_NAME.equals(it.getKey().getAttributeName()))
			.map(AttributeValue::getValue)
			.map(Objects::toString)
			.findFirst();
		if (Objects.equals(attributeName, ATTRIBUTE_CODE)) {
			if (assignedName.isPresent()) {
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(assignedName.get() + suffix, '-', "-/");
			} else if (Objects.equals(Entities.BRAND, entityType)) {
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(fakerToUse.company().name() + suffix, '-', "-/");
			} else if (Objects.equals(Entities.CATEGORY, entityType)) {
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(fakerToUse.commerce().department() + suffix, '-', "-/");
			} else if (Objects.equals(Entities.PRODUCT, entityType)) {
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(fakerToUse.commerce().productName() + suffix, '-', "-/");
			} else if (Objects.equals(Entities.PARAMETER_GROUP, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(commerce.promotionCode() + " " + commerce.material() + suffix, '-', "-/");
			} else if (Objects.equals(Entities.PARAMETER, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(commerce.promotionCode() + " " + commerce.material() + " " + commerce.color() + suffix, '-', "-/");
			} else {
				value = (T) io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(fakerToUse.beer().name() + suffix, '-', "-/");
			}
		} else if (Objects.equals(attributeName, ATTRIBUTE_NAME)) {
			if (Objects.equals(Entities.BRAND, entityType)) {
				value = (T) (fakerToUse.company().name() + suffix);
			} else if (Objects.equals(Entities.CATEGORY, entityType)) {
				value = (T) (fakerToUse.commerce().department() + suffix);
			} else if (Objects.equals(Entities.PRODUCT, entityType)) {
				value = (T) (fakerToUse.commerce().productName() + suffix);
			} else if (Objects.equals(Entities.PRICE_LIST, entityType)) {
				value = (T) (PRICE_LIST_NAMES[fakerToUse.random().nextInt(PRICE_LIST_NAMES.length)]);
			} else if (Objects.equals(Entities.PARAMETER_GROUP, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) (commerce.promotionCode() + " " + commerce.material() + suffix);
			} else if (Objects.equals(Entities.PARAMETER, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) (commerce.promotionCode() + " " + commerce.material() + " " + commerce.color() + suffix);
			} else {
				value = (T) (fakerToUse.beer().name() + suffix);
			}
		} else if (Objects.equals(attributeName, ATTRIBUTE_URL)) {
			if (assignedName.isPresent()) {
				value = (T) url(fakerToUse, assignedName.get() + suffix);
			} else if (Objects.equals(Entities.BRAND, entityType)) {
				value = (T) (fakerToUse.company().url() + io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(suffix, '-', "-/"));
			} else if (Objects.equals(Entities.CATEGORY, entityType)) {
				value = (T) url(fakerToUse, fakerToUse.commerce().department() + suffix);
			} else if (Objects.equals(Entities.PRODUCT, entityType)) {
				value = (T) url(fakerToUse, fakerToUse.commerce().productName() + suffix);
			} else if (Objects.equals(Entities.PARAMETER_GROUP, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) url(fakerToUse, commerce.promotionCode() + " " + commerce.material() + suffix);
			} else if (Objects.equals(Entities.PARAMETER, entityType)) {
				final Commerce commerce = fakerToUse.commerce();
				value = (T) url(fakerToUse, commerce.promotionCode() + " " + commerce.material() + " " + commerce.color() + suffix);
			} else {
				value = (T) url(fakerToUse, fakerToUse.beer().name() + suffix);
			}
		} else {
			value = (T) fakerToUse.beer().name();
		}
		return value;
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomBigDecimal(Faker fakerToUse, int indexedDecimalPlaces) {
		Assert.isTrue(indexedDecimalPlaces >= 0, () -> new IllegalArgumentException("Indexed decimal places must be positive or zero!"));
		final BigDecimal value;
		final long decimalNumber = fakerToUse.number().numberBetween(
			50 * Math.round(Math.pow(10, indexedDecimalPlaces)),
			1000 * Math.round(Math.pow(10, indexedDecimalPlaces))
		);
		value = BigDecimal.valueOf(decimalNumber);
		//noinspection unchecked
		return (T) (indexedDecimalPlaces > 0 ? value.setScale(indexedDecimalPlaces, RoundingMode.UNNECESSARY).divide(BigDecimal.valueOf(Math.pow(10, indexedDecimalPlaces)).setScale(0), RoundingMode.UNNECESSARY) : value);
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomZonedDateTime(Faker fakerToUse) {
		return (T) ZonedDateTime.of(
			fakerToUse.number().numberBetween(2000, 2020),
			fakerToUse.number().numberBetween(1, 12),
			fakerToUse.number().numberBetween(1, 28),
			fakerToUse.number().numberBetween(0, 23),
			fakerToUse.number().numberBetween(0, 59),
			fakerToUse.number().numberBetween(0, 59),
			0,
			ZoneId.systemDefault()
		);
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomLocalDateTime(Faker fakerToUse) {
		return (T) LocalDateTime.of(
			fakerToUse.number().numberBetween(2000, 2020),
			fakerToUse.number().numberBetween(1, 12),
			fakerToUse.number().numberBetween(1, 28),
			fakerToUse.number().numberBetween(0, 23),
			fakerToUse.number().numberBetween(0, 59),
			fakerToUse.number().numberBetween(0, 59),
			0
		);
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomLocalDate(Faker fakerToUse) {
		return (T) LocalDate.of(
			fakerToUse.number().numberBetween(2000, 2020),
			fakerToUse.number().numberBetween(1, 12),
			fakerToUse.number().numberBetween(1, 28)
		);
	}

	@Nonnull
	private static <T extends Serializable & Comparable<?>> T generateRandomLocalTime(Faker fakerToUse) {
		return (T) LocalTime.of(
			fakerToUse.number().numberBetween(0, 23),
			fakerToUse.number().numberBetween(0, 59),
			fakerToUse.number().numberBetween(0, 59),
			0
		);
	}

	@SuppressWarnings("unchecked")
	private static <T extends Serializable> void generateAndSetAssociatedData(AssociatedDataSchema associatedData, Faker genericFaker, Consumer<T> generatedValueWriter) {
		if (associatedData.getType().isArray()) {
			if (Integer.class.equals(associatedData.getType().getComponentType())) {
				final Integer[] newValue = new Integer[genericFaker.random().nextInt(8)];
				for (int i = 0; i < newValue.length; i++) {
					newValue[i] = genericFaker.random().nextInt(10000);
				}
				generatedValueWriter.accept((T) newValue);
			}
		} else {
			final Constructor<? extends Serializable> defaultConstructor = REFLECTION_LOOKUP.findDefaultConstructor(associatedData.getType());
			try {
				generatedValueWriter.accept(
					(T) defaultConstructor.newInstance()
				);
			} catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
				throw new IllegalStateException("Test associated data class " + defaultConstructor.toGenericString() + " threw exception!", e);
			}
		}
	}

	private static String url(@Nonnull Faker faker, @Nonnull String name) {
		return "https://www.evita." + faker.resolve("internet.domain_suffix") + "/" + io.evitadb.api.utils.StringUtils.removeDiacriticsAndAllNonStandardCharactersExcept(name, '-', "-/");
	}

	@Nullable
	private static Hierarchy getHierarchyIfNeeded(@Nonnull PMPTT hierarchies, @Nonnull EntitySchema schema) {
		return schema.isWithHierarchy() ? getHierarchy(hierarchies, schema.getName()) : null;
	}

	@Data
	public static class ReferencedFileSet implements Serializable {
		private static final long serialVersionUID = -1355676966187183143L;
		private String someField = "someValue";

	}

	@Data
	public static class Labels implements Serializable {
		private static final long serialVersionUID = 1121150156843379388L;
		private String someField = "someValue";

	}

	@Data
	private static class SchemaKey {
		private final Serializable type;
	}

	@Data
	private static class AttributeKey {
		private final String name;
	}

	@Data
	private static class PriceKey {
		private final Serializable entityType;
	}

	private static class SortableAttributesChecker {
		private final Map<String, Map<Object, Integer>> sortableAttributes = new HashMap<>();
		private Predicate<Set<Object>> canAddAttribute;

		public Object getUniqueAttribute(String attributeName, Object value) {
			final Map<Object, Integer> uniqueValueMap = sortableAttributes.computeIfAbsent(attributeName, an -> new HashMap<>());
			final Integer count = uniqueValueMap.get(value);
			if (count != null) {
				if (value instanceof String) {
					final Integer newCount = count + 1;
					uniqueValueMap.put(value, newCount);
					return value + "_" + newCount;
				} else {
					return null;
				}
			} else {
				uniqueValueMap.put(value, 1);
			}
			return value;
		}

		public void executeWithPredicate(Predicate<Set<Object>> canAddAttribute, Runnable runnable) {
			Assert.isTrue(this.canAddAttribute == null, "Cannot nest predicated!");
			try {
				this.canAddAttribute = canAddAttribute;
				runnable.run();
			} finally {
				this.canAddAttribute = null;
			}
		}
	}

	@RequiredArgsConstructor
	private static class SingleSortableAttributePredicate implements Predicate<String> {
		private final ReferenceSchema reference;
		private final Set<String> alreadyGenerated = new HashSet<>();

		public SingleSortableAttributePredicate(ReferenceSchema reference, EntityBuilder entityBuilder) {
			this.reference = reference;
			entityBuilder
				.getReferences(reference.getEntityType())
				.forEach(it ->
					reference.getAttributes()
						.values()
						.stream()
						.filter(AttributeSchema::isSortable)
						.forEach(attr -> alreadyGenerated.add(attr.getName()))
				);
		}

		@Override
		public boolean test(String attributeName) {
			final AttributeSchema attributeSchema = reference.getAttribute(attributeName);
			if (attributeSchema.isSortable()) {
				if (alreadyGenerated.contains(attributeName)) {
					return false;
				} else {
					alreadyGenerated.add(attributeName);
				}
			}
			return true;
		}

	}

	@RequiredArgsConstructor
	public static class ModificationFunction implements Function<SealedEntity, EntityBuilder> {
		private final Faker genericFaker;
		private final PMPTT hierarchies;
		private final Map<Serializable, Map<Object, Integer>> uniqueSequencer;
		private final Map<Serializable, SortableAttributesChecker> sortableAttributesChecker;
		private final Set<Currency> allCurrencies;
		private final Set<String> allPriceLists;
		private final Function<Faker, PriceInnerRecordHandling> priceInnerRecordHandlingGenerator;
		private final BiFunction<Serializable, Faker, Integer> referencedEntityResolver;
		private final Function<Locale, Faker> localizedFakerFetcher;
		private final Map<Integer, Integer> parameterIndex;

		@Override
		public EntityBuilder apply(SealedEntity existingEntity) {
			final EntityBuilder detachedBuilder = existingEntity.open();
			final EntitySchema schema = existingEntity.getSchema();
			final Set<Locale> allLocales = schema.getLocales();
			final Map<Object, Integer> uniqueSequencer = this.uniqueSequencer.computeIfAbsent(
				schema.getName(),
				serializable -> new HashMap<>()
			);
			final SortableAttributesChecker sortableAttributesHolder = this.sortableAttributesChecker.computeIfAbsent(
				schema.getName(),
				serializable -> new SortableAttributesChecker()
			);

			// randomly delete hierarchy placement
			if (detachedBuilder.getHierarchicalPlacement() != null && genericFaker.random().nextInt(3) == 0) {
				detachedBuilder.removeHierarchicalPlacement();
			}
			generateRandomHierarchy(schema, referencedEntityResolver, getHierarchyIfNeeded(hierarchies, schema), genericFaker, detachedBuilder);

			final List<Locale> usedLocales = pickRandomFromSet(genericFaker, allLocales);

			// randomly delete attributes
			final Set<AttributesContract.AttributeKey> existingAttributeKeys = new TreeSet<>(detachedBuilder.getAttributeKeys());
			for (AttributesContract.AttributeKey existingAttributeKey : existingAttributeKeys) {
				if (genericFaker.random().nextInt(4) == 0) {
					detachedBuilder.removeAttribute(existingAttributeKey.getAttributeName(), existingAttributeKey.getLocale());
				}
			}
			generateRandomAttributes(schema.getName(), schema.getAttributes().values(), uniqueSequencer, sortableAttributesHolder, TRUE_PREDICATE, localizedFakerFetcher, genericFaker, detachedBuilder, usedLocales);

			// randomly delete associated data
			final Set<AssociatedDataContract.AssociatedDataKey> existingAssociatedDataKeys = new TreeSet<>(detachedBuilder.getAssociatedDataKeys());
			for (AssociatedDataContract.AssociatedDataKey existingAssociatedDataKey : existingAssociatedDataKeys) {
				if (genericFaker.random().nextInt(4) == 0) {
					detachedBuilder.removeAssociatedData(existingAssociatedDataKey.getAssociatedDataName(), existingAssociatedDataKey.getLocale());
				}
			}
			generateRandomAssociatedData(schema, genericFaker, detachedBuilder, usedLocales);

			// randomly delete prices
			final List<Price.PriceKey> prices = detachedBuilder.getPrices().stream().map(PriceContract::getPriceKey).sorted().collect(Collectors.toList());
			for (Price.PriceKey price : prices) {
				if (genericFaker.random().nextInt(4) == 0) {
					detachedBuilder.removePrice(price.getPriceId(), price.getPriceList(), price.getCurrency());
				}
			}
			generateRandomPrices(schema, uniqueSequencer, genericFaker, allCurrencies, allPriceLists, detachedBuilder, priceInnerRecordHandlingGenerator);

			// randomly delete references
			final Collection<EntityReferenceContract> references = detachedBuilder.getReferences().stream().map(ReferenceContract::getReferencedEntity).sorted().collect(Collectors.toList());
			for (EntityReferenceContract reference : references) {
				if (genericFaker.random().nextInt(4) == 0) {
					detachedBuilder.removeReference(reference.getType(), reference.getPrimaryKey());
				}
			}
			generateRandomReferences(schema, referencedEntityResolver, uniqueSequencer, parameterIndex, sortableAttributesHolder, localizedFakerFetcher, genericFaker, detachedBuilder, usedLocales);

			return detachedBuilder;
		}
	}

}
