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

package io.evitadb.api.serialization;

import com.esotericsoftware.kryo.ClassResolver;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.serializers.DefaultArraySerializers.*;
import com.esotericsoftware.kryo.serializers.DefaultSerializers.*;
import com.esotericsoftware.kryo.serializers.TimeSerializers.LocalDateSerializer;
import com.esotericsoftware.kryo.serializers.TimeSerializers.LocalDateTimeSerializer;
import com.esotericsoftware.kryo.serializers.TimeSerializers.LocalTimeSerializer;
import com.esotericsoftware.kryo.serializers.TimeSerializers.ZonedDateTimeSerializer;
import io.evitadb.api.CatalogState;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataKey;
import io.evitadb.api.data.AssociatedDataContract.AssociatedDataValue;
import io.evitadb.api.data.AttributesContract.AttributeKey;
import io.evitadb.api.data.AttributesContract.AttributeValue;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.key.AssociatedDataKeySerializer;
import io.evitadb.api.data.key.AttributeKeySerializer;
import io.evitadb.api.data.key.CompressiblePriceKey;
import io.evitadb.api.data.key.CompressiblePriceKeySerializer;
import io.evitadb.api.data.structure.AssociatedData;
import io.evitadb.api.data.structure.AssociatedDataSerializer;
import io.evitadb.api.data.structure.Attributes;
import io.evitadb.api.data.structure.AttributesSerializer;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.data.structure.*;
import io.evitadb.api.dataType.*;
import io.evitadb.api.dataType.ComplexDataObject.EmptyValue;
import io.evitadb.api.dataType.ComplexDataObject.NullValue;
import io.evitadb.api.dataType.ComplexDataObjectSerializer.EmptyValueSerializer;
import io.evitadb.api.dataType.ComplexDataObjectSerializer.NullValueSerializer;
import io.evitadb.api.dataType.trie.Trie;
import io.evitadb.api.dataType.trie.TrieNode;
import io.evitadb.api.dataType.trie.TrieNodeSerializer;
import io.evitadb.api.dataType.trie.TrieSerializer;
import io.evitadb.api.query.EntitiesSerializer;
import io.evitadb.api.query.Query;
import io.evitadb.api.query.QuerySerializer;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.query.head.Entities;
import io.evitadb.api.query.order.Random;
import io.evitadb.api.query.order.*;
import io.evitadb.api.query.orderBy.*;
import io.evitadb.api.query.require.PricesSerializer;
import io.evitadb.api.query.require.*;
import io.evitadb.api.schema.*;
import io.evitadb.api.serialization.common.EnumSetSerializer;
import io.evitadb.api.serialization.common.*;
import io.evitadb.api.serialization.utils.DefaultKryoSerializationHelper;
import io.evitadb.api.serialization.utils.KryoSerializationHelper;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.RequiredArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * KryoFactory class encapsulates {@link Kryo} instantiation and configuration for particular tasks. Kryo library was
 * chosen as a serialization engine because it represents currently the most performant way of (de)serialization of
 * Java projects.
 *
 * See:
 * - https://aaltodoc.aalto.fi/bitstream/handle/123456789/39869/master_Hagberg_Henri_2019.pdf?sequence=1&isAllowed=y
 * - https://www.javacodegeeks.com/2013/09/optimizing-java-serialization-java-vs-xml-vs-json-vs-kryo-vs-pof.html
 * - https://www.slideshare.net/Strannik_2013/serialization-and-performance-in-java
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class KryoFactory {
	/**
	 * This is the maximal number of classes registered by this class via {@link Kryo#register(Class, int)}. Ids above
	 * this number are allowed to be used by clients.
	 *
	 * This number is also start number for shared client classes stored on shared {@link MutableCatalogHeader}.
	 */
	public static final int CLASSES_RESERVED_FOR_INTERNAL_USE = 100000;
	/**
	 * This is the start number for client classes specific for entity serialization. Different entity types might
	 * assign same numbers to the different classes because they use different {@link MutableCatalogEntityHeader} and
	 * will never conflict with each other.
	 */
	public static final int CLASSES_RESERVED_FOR_ENTITY_USE = 200000;

	private KryoFactory() {
	}

	/**
	 * Method creates default Kryo instance ({@link #createKryo(ClassResolver)} and created instance hands to passed consumer
	 * implementation. This method should play well with {@link SchemaKryoConfigurer} and {@link EntityKryoConfigurer}
	 * consumers defined by this class.
	 */
	public static Kryo createKryo(ClassResolver classResolver, Consumer<Kryo> andThen) {
		final Kryo kryo = createKryo(classResolver);
		andThen.accept(kryo);
		return kryo;
	}

	/**
	 * Method creates default Kryo instance ({@link #createKryo(ClassResolver)}) and created instance hands to passed consumer
	 * implementation. This method should play well with {@link SchemaKryoConfigurer} and {@link EntityKryoConfigurer}
	 * consumers defined by this class.
	 */
	public static Kryo createKryo(ClassResolver classResolver, KryoSerializationHelper kryoSerializationHelper, Consumer<Kryo> andThen) {
		final Kryo kryo = initializeKryo(new Kryo(classResolver, null), kryoSerializationHelper);
		andThen.accept(kryo);
		return kryo;
	}

	/**
	 * Method creates default Kryo instance with all default serializers registered. This instance of the Kryo
	 * should be able to (de)serialize all {@link EvitaDataTypes#getSupportedDataTypes()} data types.
	 */
	public static Kryo createKryo(ClassResolver classResolver) {
		return initializeKryo(new Kryo(classResolver, null), new DefaultKryoSerializationHelper());
	}

	/**
	 * Method creates default Kryo instance with all default serializers registered. This instance of the Kryo
	 * should be able to (de)serialize all {@link EvitaDataTypes#getSupportedDataTypes()} data types.
	 */
	public static <T extends Kryo> T initializeKryo(T kryoInstance, KryoSerializationHelper kryoSerializationHelper) {
		kryoInstance.setRegistrationRequired(true);
		kryoInstance.setDefaultSerializer(DefaultSerializer.class);
		kryoInstance.setReferences(false);
		kryoInstance.register(Class.class, new ClassSerializer(), 99);
		kryoInstance.register(Serializable.class, new SerializableSerializer(), 100);
		kryoInstance.register(Serializable[].class, new SerializableArraySerializer(kryoSerializationHelper), 101);
		kryoInstance.register(String.class, new StringSerializer(), 102);
		kryoInstance.register(String[].class, new StringArraySerializer(), 103);
		kryoInstance.register(Byte.class, new ByteSerializer(), 104);
		kryoInstance.register(byte[].class, new ByteArraySerializer(), 105);
		kryoInstance.register(Byte[].class, new GenericArraySerializer<>(Byte.class), 106);
		kryoInstance.register(Short.class, new ShortSerializer(), 107);
		kryoInstance.register(short[].class, new ShortArraySerializer(), 108);
		kryoInstance.register(Short[].class, new GenericArraySerializer<>(Short.class), 109);
		kryoInstance.register(Integer.class, new IntSerializer(), 110);
		kryoInstance.register(int[].class, new IntArraySerializer(), 111);
		kryoInstance.register(Integer[].class, new GenericArraySerializer<>(Integer.class), 112);
		kryoInstance.register(Long.class, new LongSerializer(), 113);
		kryoInstance.register(long[].class, new LongArraySerializer(), 114);
		kryoInstance.register(Long[].class, new GenericArraySerializer<>(Long.class), 115);
		kryoInstance.register(Boolean.class, new BooleanSerializer(), 116);
		kryoInstance.register(boolean[].class, new BooleanArraySerializer(), 117);
		kryoInstance.register(Boolean[].class, new GenericArraySerializer<>(Boolean.class), 118);
		kryoInstance.register(Character.class, new CharSerializer(), 119);
		kryoInstance.register(char[].class, new CharArraySerializer(), 120);
		kryoInstance.register(Character[].class, new GenericArraySerializer<>(Character.class), 121);
		kryoInstance.register(BigDecimal.class, new BigDecimalSerializer(), 122);
		kryoInstance.register(BigDecimal[].class, new GenericArraySerializer<>(BigDecimal.class), 123);
		kryoInstance.register(ZonedDateTime.class, new ZonedDateTimeSerializer(), 124);
		kryoInstance.register(ZonedDateTime[].class, new GenericArraySerializer<>(ZonedDateTime.class), 125);
		kryoInstance.register(LocalDateTime.class, new LocalDateTimeSerializer(), 126);
		kryoInstance.register(LocalDateTime[].class, new GenericArraySerializer<>(LocalDateTime.class), 127);
		kryoInstance.register(LocalDate.class, new LocalDateSerializer(), 128);
		kryoInstance.register(LocalDate[].class, new GenericArraySerializer<>(LocalDate.class), 129);
		kryoInstance.register(LocalTime.class, new LocalTimeSerializer(), 130);
		kryoInstance.register(LocalTime[].class, new GenericArraySerializer<>(LocalTime.class), 131);
		kryoInstance.register(DateTimeRange.class, new DateTimeRangeSerializer(), 132);
		kryoInstance.register(DateTimeRange[].class, new GenericArraySerializer<>(DateTimeRange.class), 133);
		kryoInstance.register(NumberRange.class, new NumberRangeSerializer(kryoSerializationHelper), 134);
		kryoInstance.register(NumberRange[].class, new GenericArraySerializer<>(NumberRange.class), 135);
		kryoInstance.register(Locale.class, new LocaleSerializer(), 136);
		kryoInstance.register(Locale[].class, new GenericArraySerializer<>(Locale.class), 137);
		kryoInstance.register(EnumSet.class, new EnumSetSerializer(), 138);
		kryoInstance.register(Currency.class, new CurrencySerializer(), 139);
		kryoInstance.register(Currency[].class, new GenericArraySerializer<>(Currency.class), 140);
		kryoInstance.register(Multiple.class, new MultipleSerializer(kryoSerializationHelper), 141);
		kryoInstance.register(Multiple[].class, new GenericArraySerializer<>(Multiple.class), 142);
		kryoInstance.register(ComplexDataObject.class, new ComplexDataObjectSerializer(kryoSerializationHelper), 143);
		kryoInstance.register(ComplexDataObject[].class, new GenericArraySerializer<>(ComplexDataObject.class), 144);
		kryoInstance.register(Set.class, new SetSerializer<>(kryoSerializationHelper, count -> new HashSet<>((int) Math.ceil(count / .75f), .75f)), 145);
		kryoInstance.register(HashSet.class, new SetSerializer<>(kryoSerializationHelper, count -> new HashSet<>((int) Math.ceil(count / .75f), .75f)), 146);
		kryoInstance.register(LinkedHashSet.class, new SetSerializer<>(kryoSerializationHelper, count -> new LinkedHashSet<>((int) Math.ceil(count / .75f), .75f)), 147);
		kryoInstance.register(Map.class, new MapSerializer<>(kryoSerializationHelper, count -> new HashMap<>((int) Math.ceil(count / .75f), .75f)), 148);
		kryoInstance.register(HashMap.class, new MapSerializer<>(kryoSerializationHelper, count -> new HashMap<>((int) Math.ceil(count / .75f), .75f)), 149);
		kryoInstance.register(LinkedHashMap.class, new MapSerializer<>(kryoSerializationHelper, count -> new LinkedHashMap<>((int) Math.ceil(count / .75f), .75f)), 150);
		kryoInstance.register(NullValue.class, new NullValueSerializer(), 151);
		kryoInstance.register(EmptyValue.class, new EmptyValueSerializer(), 152);
		kryoInstance.register(Trie.class, new TrieSerializer<>(kryoSerializationHelper), 153);
		kryoInstance.register(TrieNode.class, new TrieNodeSerializer<>(kryoSerializationHelper), 154);
		kryoInstance.register(ZonedDateTime.class, new ZonedDateTimeSerializer(), 155);
		return kryoInstance;
	}

	/**
	 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
	 * required to (de)serialize {@link MutableCatalogHeader}.
	 */
	public static class CatalogSerializationHeaderKryoConfigurer implements Consumer<Kryo> {
		public static final CatalogSerializationHeaderKryoConfigurer INSTANCE = new CatalogSerializationHeaderKryoConfigurer();
		private final KryoSerializationHelper kryoSerializationHelper;

		private CatalogSerializationHeaderKryoConfigurer() {
			this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
		}

		public CatalogSerializationHeaderKryoConfigurer(KryoSerializationHelper kryoSerializationHelper) {
			this.kryoSerializationHelper = kryoSerializationHelper;
		}

		@Override
		public void accept(Kryo kryo) {
			kryo.register(AttributeKey.class, new SerialVersionBasedSerializer<>(new AttributeKeySerializer(), AttributeKey.class), 300);
			kryo.register(AssociatedDataKey.class, new SerialVersionBasedSerializer<>(new AssociatedDataKeySerializer(), AssociatedDataKey.class), 301);
			kryo.register(CompressiblePriceKey.class, new SerialVersionBasedSerializer<>(new CompressiblePriceKeySerializer(kryoSerializationHelper), CompressiblePriceKey.class), 302);
			kryo.register(CatalogState.class, new EnumNameSerializer<>(kryoSerializationHelper), 303);
		}

	}

	/**
	 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
	 * required to (de)serialize {@link EntitySchema}.
	 */
	public static class SchemaKryoConfigurer implements Consumer<Kryo> {
		public static final SchemaKryoConfigurer INSTANCE = new SchemaKryoConfigurer();
		private final KryoSerializationHelper kryoSerializationHelper;

		private SchemaKryoConfigurer() {
			this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
		}

		public SchemaKryoConfigurer(KryoSerializationHelper kryoSerializationHelper) {
			this.kryoSerializationHelper = kryoSerializationHelper;
		}

		@Override
		public void accept(Kryo kryo) {
			kryo.register(EntitySchema.class, new SerialVersionBasedSerializer<>(new EntitySchemaSerializer(kryoSerializationHelper), EntitySchema.class), 200);
			kryo.register(AttributeSchema.class, new SerialVersionBasedSerializer<>(new AttributeSchemaSerializer(kryoSerializationHelper), AttributeSchema.class), 201);
			kryo.register(AssociatedDataSchema.class, new SerialVersionBasedSerializer<>(new AssociatedDataSchemaSerializer(kryoSerializationHelper), AssociatedDataSchema.class), 202);
			kryo.register(ReferenceSchema.class, new SerialVersionBasedSerializer<>(new ReferenceSchemaSerializer(kryoSerializationHelper), ReferenceSchema.class), 203);
			kryo.register(EvolutionMode.class, new EnumNameSerializer<>(kryoSerializationHelper), 204);
		}

	}

	/**
	 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
	 * required to (de)serialize {@link Entity}.
	 */
	@RequiredArgsConstructor
	public static class EntityKryoConfigurer implements Consumer<Kryo> {
		private final Supplier<EntitySchema> schemaSupplier;
		private final ReflectionLookup reflectionLookup;
		private final KeyCompressor keyCompressor;
		private final KryoSerializationHelper kryoSerializationHelper;

		public EntityKryoConfigurer(Supplier<EntitySchema> schemaSupplier, ReflectionLookup reflectionLookup, KeyCompressor keyCompressor) {
			this.schemaSupplier = schemaSupplier;
			this.reflectionLookup = reflectionLookup;
			this.keyCompressor = keyCompressor;
			this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
		}

		@Override
		public void accept(Kryo kryo) {
			kryo.register(PriceInnerRecordHandling.class, new EnumNameSerializer<PriceInnerRecordHandling>(kryoSerializationHelper), 400);
			kryo.register(Entity.class, new SerialVersionBasedSerializer<>(new EntitySerializer(schemaSupplier, reflectionLookup), Entity.class), 401);
			kryo.register(Attributes.class, new SerialVersionBasedSerializer<>(new AttributesSerializer(), Attributes.class), 402);
			kryo.register(AttributeValue.class, new SerialVersionBasedSerializer<>(new AttributeValueSerializer(kryoSerializationHelper, keyCompressor), AttributeValue.class), 403);
			kryo.register(AssociatedData.class, new SerialVersionBasedSerializer<>(new AssociatedDataSerializer(reflectionLookup), AssociatedData.class), 404);
			kryo.register(AssociatedDataValue.class, new SerialVersionBasedSerializer<>(new AssociatedDataValueSerializer(kryoSerializationHelper, keyCompressor), AssociatedDataValue.class), 405);
			kryo.register(Reference.class, new SerialVersionBasedSerializer<>(new ReferenceSerializer(kryoSerializationHelper), Reference.class), 406);
			kryo.register(Prices.class, new SerialVersionBasedSerializer<>(new io.evitadb.api.data.structure.PricesSerializer(), Prices.class), 407);
			kryo.register(Price.class, new SerialVersionBasedSerializer<>(new PriceSerializer(keyCompressor), Price.class), 408);
			kryo.register(HierarchicalPlacement.class, new SerialVersionBasedSerializer<>(new HierarchicalPlacementSerializer(), HierarchicalPlacement.class), 409);
			kryo.register(EntityReference.class, new SerialVersionBasedSerializer<>(new EntityReferenceSerializer(kryoSerializationHelper), EntityReference.class), 410);
		}

	}

	/**
	 * This {@link Consumer} implementation takes default Kryo instance and registers additional serializers that are
	 * required to (de)serialize {@link io.evitadb.api.query.Query}.
	 */
	public static class QuerySerializationKryoConfigurer implements Consumer<Kryo> {
		public static final QuerySerializationKryoConfigurer INSTANCE = new QuerySerializationKryoConfigurer();
		private final KryoSerializationHelper kryoSerializationHelper;

		private QuerySerializationKryoConfigurer() {
			this.kryoSerializationHelper = new DefaultKryoSerializationHelper();
		}

		public QuerySerializationKryoConfigurer(KryoSerializationHelper kryoSerializationHelper) {
			this.kryoSerializationHelper = kryoSerializationHelper;
		}

		@Override
		public void accept(Kryo kryo) {
			kryo.register(Query.class, new QuerySerializer(), 990);

			kryo.register(PriceFetchMode.class, new EnumSerializer(PriceFetchMode.class), 996);
			kryo.register(QueryPriceMode.class, new EnumSerializer(QueryPriceMode.class), 997);
			kryo.register(FacetStatisticsDepth.class, new EnumSerializer(FacetStatisticsDepth.class), 998);
			kryo.register(Entities.class, new EntitiesSerializer(kryoSerializationHelper), 999);

			kryo.register(And.class, new AndSerializer(), 1000);
			kryo.register(Between.class, new BetweenSerializer<>(kryoSerializationHelper), 1001);
			kryo.register(Contains.class, new ContainsSerializer(), 1002);
			kryo.register(DirectRelation.class, new DirectRelationSerializer(), 1003);
			kryo.register(EndsWith.class, new EndsWithSerializer(), 1004);
			kryo.register(Equals.class, new EqualsSerializer<>(kryoSerializationHelper), 1005);
			kryo.register(Excluding.class, new ExcludingSerializer(), 1006);
			kryo.register(ExcludingRoot.class, new ExcludingRootSerializer(), 1007);
			kryo.register(Facet.class, new FacetSerializer(kryoSerializationHelper), 1008);
			kryo.register(FilterBy.class, new FilterBySerializer(), 1009);
			kryo.register(GreaterThan.class, new GreaterThanSerializer<>(kryoSerializationHelper), 1010);
			kryo.register(GreaterThanEquals.class, new GreaterThanEqualsSerializer<>(kryoSerializationHelper), 1011);
			kryo.register(InRange.class, new InRangeSerializer(kryoSerializationHelper), 1012);
			kryo.register(InSet.class, new InSetSerializer<>(kryoSerializationHelper), 1013);
			kryo.register(IsFalse.class, new IsFalseSerializer(), 1014);
			kryo.register(IsNotNull.class, new IsNotNullSerializer(), 1015);
			kryo.register(IsNull.class, new IsNullSerializer(), 1016);
			kryo.register(IsTrue.class, new IsTrueSerializer(), 1017);
			kryo.register(Language.class, new LanguageSerializer(), 1018);
			kryo.register(LessThan.class, new LessThanSerializer<>(kryoSerializationHelper), 1019);
			kryo.register(LessThanEquals.class, new LessThanEqualsSerializer<>(kryoSerializationHelper), 1020);
			kryo.register(Not.class, new NotSerializer(), 1021);
			kryo.register(Or.class, new OrSerializer(), 1022);
			kryo.register(PriceBetween.class, new PriceBetweenSerializer(), 1023);
			kryo.register(PriceInCurrency.class, new PriceInCurrencySerializer(), 1024);
			kryo.register(PriceInPriceLists.class, new PriceInPriceListsSerializer(kryoSerializationHelper), 1025);
			kryo.register(PriceValidIn.class, new PriceValidInSerializer(), 1026);
			kryo.register(PrimaryKey.class, new PrimaryKeySerializer(), 1027);
			kryo.register(ReferenceHavingAttribute.class, new ReferenceHavingAttributeSerializer(kryoSerializationHelper), 1028);
			kryo.register(StartsWith.class, new StartsWithSerializer(), 1029);
			kryo.register(UserFilter.class, new UserFilterSerializer(), 1030);
			kryo.register(WithinHierarchy.class, new WithinHierarchySerializer(kryoSerializationHelper), 1031);
			kryo.register(WithinRootHierarchy.class, new WithinRootHierarchySerializer(kryoSerializationHelper), 1032);

			kryo.register(Ascending.class, new AscendingSerializer(), 1500);
			kryo.register(Descending.class, new DescendingSerializer(), 1501);
			kryo.register(OrderBy.class, new OrderBySerializer(), 1502);
			kryo.register(PriceAscending.class, new PriceAscendingSerializer(), 1503);
			kryo.register(PriceDescending.class, new PriceDescendingSerializer(), 1504);
			kryo.register(Random.class, new RandomSerializer(), 1505);
			kryo.register(ReferenceAttribute.class, new ReferenceAttributeSerializer(kryoSerializationHelper), 1506);

			kryo.register(io.evitadb.api.query.require.AssociatedData.class, new io.evitadb.api.query.require.AssociatedDataSerializer(), 2000);
			kryo.register(AttributeHistogram.class, new AttributeHistogramSerializer(), 2001);
			kryo.register(DataInLanguage.class, new DataInLanguageSerializer(), 2002);
			kryo.register(EntityBody.class, new EntityBodySerializer(), 2003);
			kryo.register(FacetGroupsConjunction.class, new FacetGroupsConjunctionSerializer(kryoSerializationHelper), 2004);
			kryo.register(FacetGroupsDisjunction.class, new FacetGroupsDisjunctionSerializer(kryoSerializationHelper), 2005);
			kryo.register(FacetGroupsNegation.class, new FacetGroupsNegationSerializer(kryoSerializationHelper), 2006);
			kryo.register(FacetSummary.class, new FacetSummarySerializer(), 2007);
			kryo.register(HierarchyStatistics.class, new HierarchyStatisticsSerializer(kryoSerializationHelper), 2008);
			kryo.register(Page.class, new PageSerializer(), 2009);
			kryo.register(Parents.class, new ParentsSerializer(), 2010);
			kryo.register(ParentsOfType.class, new ParentsOfTypeSerializer(kryoSerializationHelper), 2011);
			kryo.register(PriceHistogram.class, new PriceHistogramSerializer(), 2012);
			kryo.register(Prices.class, new PricesSerializer(), 2013);
			kryo.register(References.class, new ReferencesSerializer(kryoSerializationHelper), 2014);
			kryo.register(Require.class, new RequireSerializer(), 2015);
			kryo.register(Strip.class, new StripSerializer(), 2016);
			kryo.register(UseOfPrice.class, new UseOfPriceSerializer(), 2017);
			kryo.register(io.evitadb.api.query.require.Attributes.class, new io.evitadb.api.query.require.AttributesSerializer(), 2018);
		}

	}

}
