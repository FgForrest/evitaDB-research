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

package io.evitadb.query.filter.translator.attribute.alternative;

import io.evitadb.api.EntityCollection;
import io.evitadb.api.EvitaSession;
import io.evitadb.api.data.Droppable;
import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.EntityEditor.EntityBuilder;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityStorageContainerAccessor;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.io.EvitaRequest;
import io.evitadb.api.io.predicate.*;
import io.evitadb.api.query.Query;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.cache.NoCacheSupervisor;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.filter.translator.attribute.*;
import io.evitadb.query.response.QueryTelemetry;
import io.evitadb.query.response.QueryTelemetry.QueryPhase;
import io.evitadb.test.Entities;
import io.evitadb.test.generator.DataGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.api.query.QueryConstraints.*;
import static java.util.Optional.ofNullable;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies behaviour of {@link AttributeBitmapFilter}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class AttributeBitmapFilterTest {
	private static final int SEED = 40;
	public static final String NUMBER_RANGE = "numberRange";

	private final AtomicReference<EntitySchema> entitySchema = new AtomicReference<>();
	private Map<Integer, Entity> entities;

	@BeforeEach
	void setUp() {
		final DataGenerator dataGenerator = new DataGenerator();
		entities = dataGenerator.generateEntities(
				dataGenerator.getSampleProductSchema(
					newValue -> {
						this.entitySchema.set(newValue);
						return newValue;
					},
					schemaBuilder -> {
						schemaBuilder
							.withoutGeneratedPrimaryKey()
							.withAttribute(NUMBER_RANGE, NumberRange.class);
					}
				),
				(serializable, faker) -> null,
				SEED
			)
			.limit(50)
			.collect(
				Collectors.toMap(
					EntityContract::getPrimaryKey,
					EntityBuilder::toInstance
				)
			);
	}

	@Test
	void shouldFilterByNumberBetween() {
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			DataGenerator.ATTRIBUTE_PRIORITY,
			BetweenTranslator.getComparablePredicate(25000L, 30000L)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				between(DataGenerator.ATTRIBUTE_PRIORITY, 25000, 30000)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final Long priority = entities.get(ePK).getAttribute(DataGenerator.ATTRIBUTE_PRIORITY);
			assertNotNull(priority);
			assertTrue(priority >= 25000L && priority <= 30000L);
		}
	}

	@Test
	void shouldFilterByNumberRangeOverlap() {
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			NUMBER_RANGE,
			BetweenTranslator.getNumberRangePredicate(40L, 50L)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				between(NUMBER_RANGE, 40, 50)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final NumberRange range = entities.get(ePK).getAttribute(NUMBER_RANGE);
			assertNotNull(range);
 			assertTrue(range.overlaps(NumberRange.between(40L, 50L)));
		}
	}

	@Test
	void shouldFilterByDateTimeRangeOverlap() {
		final ZonedDateTime from = ZonedDateTime.of(2007, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final ZonedDateTime to = ZonedDateTime.of(2008, 12, 31, 23, 59, 59, 0, ZoneId.systemDefault());
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			DataGenerator.ATTRIBUTE_VALIDITY,
			BetweenTranslator.getDateTimePredicate(from, to)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				between(DataGenerator.ATTRIBUTE_VALIDITY, from, to)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final DateTimeRange range = entities.get(ePK).getAttribute(DataGenerator.ATTRIBUTE_VALIDITY);
			assertNotNull(range);
			assertTrue(range.overlaps(DateTimeRange.between(from, to)));
		}
	}

	@Test
	void shouldFilterByNumberRangeWithin() {
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			NUMBER_RANGE,
			InRangeTranslator.getNumberRangePredicate(45L)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				inRange(NUMBER_RANGE, 45)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final NumberRange range = entities.get(ePK).getAttribute(NUMBER_RANGE);
			assertNotNull(range);
			assertTrue(range.isWithin(45L));
		}
	}

	@Test
	void shouldFilterByDateTimeRangeWithin() {
		final ZonedDateTime theMoment = ZonedDateTime.of(2007, 6, 1, 0, 0, 0, 0, ZoneId.systemDefault());
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			DataGenerator.ATTRIBUTE_VALIDITY,
			InRangeTranslator.getDateTimeRangePredicate(theMoment)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				inRange(DataGenerator.ATTRIBUTE_VALIDITY, theMoment)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final DateTimeRange range = entities.get(ePK).getAttribute(DataGenerator.ATTRIBUTE_VALIDITY);
			assertNotNull(range);
			assertTrue(range.isWithin(theMoment));
		}
	}

	@Test
	void shouldFilterByStringContains() {
		final String attributeName = DataGenerator.ATTRIBUTE_CODE;
		final String textToSearch = "Hat";
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			ContainsTranslator.getPredicate(textToSearch)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				contains(attributeName, textToSearch)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final String attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.contains(textToSearch));
		}
	}

	@Test
	void shouldFilterByStringEndsWith() {
		final String attributeName = DataGenerator.ATTRIBUTE_CODE;
		final String textToSearch = "1";
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			EndsWithTranslator.getPredicate(textToSearch)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				endsWith(attributeName, textToSearch)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final String attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.endsWith(textToSearch));
		}
	}

	@Test
	void shouldFilterByStringStartsWith() {
		final String attributeName = DataGenerator.ATTRIBUTE_CODE;
		final String textToSearch = "Practical";
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			StartsWithTranslator.getPredicate(textToSearch)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				startsWith(attributeName, textToSearch)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final String attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.startsWith(textToSearch));
		}
	}

	@Test
	void shouldFilterByNumberGreaterThanEquals() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final BigDecimal theNumber = new BigDecimal("200");
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			GreaterThanEqualsTranslator.getPredicate(theNumber)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				greaterThanEquals(attributeName, theNumber)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.compareTo(theNumber) >= 0);
		}
	}

	@Test
	void shouldFilterByNumberGreaterThan() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final BigDecimal theNumber = new BigDecimal("200");
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			GreaterThanTranslator.getPredicate(theNumber)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				greaterThan(attributeName, theNumber)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.compareTo(theNumber) >  0);
		}
	}

	@Test
	void shouldFilterByNumberLesserThan() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final BigDecimal theNumber = new BigDecimal("200");
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			LessThanTranslator.getPredicate(theNumber)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				lessThan(attributeName, theNumber)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.compareTo(theNumber) <  0);
		}
	}

	@Test
	void shouldFilterByNumberLesserThanEquals() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final BigDecimal theNumber = new BigDecimal("200");
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(
			attributeName,
			LessThanEqualsTranslator.getPredicate(theNumber)
		);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				lessThanEquals(attributeName, theNumber)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
			assertTrue(attribute.compareTo(theNumber) <=  0);
		}
	}

	@Test
	void shouldFilterByNumberIsNull() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(attributeName, Objects::isNull);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				isNull(attributeName)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNull(attribute);
		}
	}

	@Test
	void shouldFilterByNumberIsNotNull() {
		final String attributeName = DataGenerator.ATTRIBUTE_QUANTITY;
		final AttributeBitmapFilter filter = new AttributeBitmapFilter(attributeName, Objects::nonNull);
		final QueryContext queryContext = getQueryContext(Query.query(
			entities(Entities.PRODUCT),
			filterBy(
				isNotNull(attributeName)
			)
		));

		final Bitmap result = filter.filter(
			queryContext,
			queryContext.fetchEntities(entities.keySet().stream().mapToInt(it -> it).toArray(), filter.getRequirements())
		);

		assertFalse(result.isEmpty());
		for (int ePK : result.getArray()) {
			final BigDecimal attribute = entities.get(ePK).getAttribute(attributeName);
			assertNotNull(attribute);
		}
	}

	@Nonnull
	private QueryContext getQueryContext(@Nonnull Query query) {
		final EntityCollection entityCollection = Mockito.mock(EntityCollection.class);
		final QueryContext queryContext = new QueryContext(
			entityCollection,
			Mockito.mock(EntityStorageContainerAccessor.class),
			Mockito.mock(EvitaSession.class),
			new EvitaRequest(
				query,
				ZonedDateTime.now()
			),
			new QueryTelemetry(QueryPhase.EXECUTION),
			serializable -> null,
			Collections.emptyMap(),
			NoCacheSupervisor.INSTANCE
		);

		Mockito.when(entityCollection.getSchema()).thenReturn(new EntitySchema(Entities.PRODUCT));
		Mockito.when(entityCollection.getEntity(ArgumentMatchers.anyInt(), ArgumentMatchers.any(EvitaRequest.class)))
			.thenAnswer(invocationOnMock -> {
				final Entity entity = this.entities.get(invocationOnMock.getArgument(0, Integer.class));
				final EvitaRequest evitaRequest = invocationOnMock.getArgument(1, EvitaRequest.class);
				return ofNullable(entity)
					.filter(Droppable::exists)
					.map(it -> Entity.decorate(
						entity,
						entity.getSchema(),
						new HierarchicalContractSerializablePredicate(),
						new AttributeValueSerializablePredicate(evitaRequest),
						new AssociatedDataValueSerializablePredicate(evitaRequest),
						new ReferenceContractSerializablePredicate(evitaRequest),
						new PriceContractSerializablePredicate(evitaRequest)
					))
					.orElse(null);
			});
		return queryContext;
	}

}