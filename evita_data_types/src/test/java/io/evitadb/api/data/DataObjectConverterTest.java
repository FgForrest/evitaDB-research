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

package io.evitadb.api.data;

import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.NumberRange;
import io.evitadb.api.exception.IncompleteDeserializationException;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.Map.Entry;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies general POJO conversion logic to {@link io.evitadb.api.dataType.ComplexDataObject} that can be
 * handled by Evita DB.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class DataObjectConverterTest {
	private final ReflectionLookup reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);;

	@Test
	void shouldSerializeSimpleObject() {
		final DataObjectConverter<String> converter = new DataObjectConverter<>("ABC");
		final Serializable serializedForm = converter.getSerializableForm();

		assertEquals("ABC", serializedForm);
	}

	@Test
	void shouldSerializeAndDeserializeSimpleObjectArray() {
		final DataObjectConverter<String[]> converter = new DataObjectConverter<>(new String[] {"ABC", "DEF"});
		final Serializable serializedForm = converter.getSerializableForm();

		assertArrayEquals(new String[] {"ABC", "DEF"}, DataObjectConverter.getOriginalForm(serializedForm, String[].class, reflectionLookup));
	}

	@Test
	void shouldSerializeComplexObject() throws IOException {
		final TestComplexObject veryComplexObject = createVeryComplexObject();
		final DataObjectConverter<TestComplexObject> converter = new DataObjectConverter<>(veryComplexObject);
		final Serializable serializedForm = converter.getSerializableForm();

		assertTrue(serializedForm instanceof ComplexDataObject);
		assertEquals(ordered(readFromClasspath("testData/DataObjectConverterTest_complexObject.txt")), serializedForm.toString());
	}

	@Test
	void shouldDeserializeComplexObject() {
		final TestComplexObject veryComplexObject = createVeryComplexObject();
		final DataObjectConverter<TestComplexObject> serializer = new DataObjectConverter<>(veryComplexObject);
		final Serializable serializedForm = serializer.getSerializableForm();

		final DataObjectConverter<TestComplexObject> deserializer = new DataObjectConverter<>(TestComplexObject.class, reflectionLookup);
		final TestComplexObject deserializedObject = deserializer.getOriginalForm(serializedForm);
		assertEquals(createVeryComplexObject(), deserializedObject);
	}

	@Test
	void shouldSerializeAndDeserializeComplexObjectArray() {
		final TestComplexObject veryComplexObject = createVeryComplexObject();
		final DataObjectConverter<TestComplexObject[]> converter = new DataObjectConverter<>(new TestComplexObject[] {veryComplexObject, veryComplexObject});
		final Serializable serializedForm = converter.getSerializableForm();

		final DataObjectConverter<TestComplexObject[]> deserializer = new DataObjectConverter<>(TestComplexObject[].class, reflectionLookup);
		final TestComplexObject[] deserializedObject = deserializer.getOriginalForm(serializedForm);
		final TestComplexObject matrixObject = createVeryComplexObject();
		assertArrayEquals(new TestComplexObject[] {matrixObject, matrixObject}, deserializedObject);
	}

	@Test
	void shouldFailToSerializeIncompatibleObject() {
		assertThrows(
			IllegalArgumentException.class,
			() -> DataObjectConverter.getSerializableForm(new IncompatibleObject())
		);
	}

	@Test
	void shouldSerializeComplexImmutableObject() throws IOException {
		final TestComplexImmutableObject veryComplexObject = createVeryComplexImmutableObject();
		final DataObjectConverter<TestComplexImmutableObject> converter = new DataObjectConverter<>(veryComplexObject);
		final Serializable serializedForm = converter.getSerializableForm();

		assertTrue(serializedForm instanceof ComplexDataObject);
		assertEquals(ordered(readFromClasspath("testData/DataObjectConverterTest_complexObject.txt")), serializedForm.toString());
	}

	@Test
	void shouldDeserializeComplexImmutableObject() {
		final TestComplexImmutableObject veryComplexObject = createVeryComplexImmutableObject();
		final DataObjectConverter<TestComplexImmutableObject> serializer = new DataObjectConverter<>(veryComplexObject);
		final Serializable serializedForm = serializer.getSerializableForm();

		final DataObjectConverter<TestComplexImmutableObject> deserializer = new DataObjectConverter<>(TestComplexImmutableObject.class, reflectionLookup);
		final TestComplexImmutableObject deserializedObject = deserializer.getOriginalForm(serializedForm);
		assertEquals(createVeryComplexImmutableObject(), deserializedObject);
	}

	@Test
	void shouldDeserializeObjectWithRenamedField() throws MalformedURLException {
		final OriginalClass beforeRename = new OriginalClass("ABC", 1, new URL("https", "www.fg.cz", 80, "/index.html"));
		final Serializable serializedForm = new DataObjectConverter<>(beforeRename).getSerializableForm();

		final ClassAfterRename deserializedClass = new DataObjectConverter<>(ClassAfterRename.class, reflectionLookup).getOriginalForm(serializedForm);
		assertEquals("ABC", deserializedClass.getRenamedField());
		assertEquals(1, deserializedClass.getSomeNumber());
	}

	@Test
	void shouldDeserializeObjectWithRenamedFieldOnImmutableClass() throws MalformedURLException {
		final OriginalImmutableClass beforeRename = new OriginalImmutableClass("ABC", 1, new URL("https", "www.fg.cz", 80, "/index.html"));
		final Serializable serializedForm = new DataObjectConverter<>(beforeRename).getSerializableForm();

		final ImmutableClassAfterRename deserializedClass = new DataObjectConverter<>(ImmutableClassAfterRename.class, reflectionLookup).getOriginalForm(serializedForm);
		assertEquals("ABC", deserializedClass.getRenamedField());
		assertEquals(1, deserializedClass.getSomeNumber());
	}

	@Test
	void shouldFailToDeserializeWhenDataDoesNotFit() throws MalformedURLException {
		final OriginalClass beforeRename = new OriginalClass("ABC", 1, new URL("https", "www.fg.cz", 80, "/index.html"));
		final Serializable serializedForm = new DataObjectConverter<>(beforeRename).getSerializableForm();

		assertThrows(
				IncompleteDeserializationException.class,
				() -> new DataObjectConverter<>(ClassAfterNonDeclaredDiscard.class, reflectionLookup).getOriginalForm(serializedForm)
		);
	}

	@Test
	void shouldDeserializeObjectWithDiscardedField() throws MalformedURLException {
		final OriginalClass beforeRename = new OriginalClass("ABC", 1, new URL("https", "www.fg.cz", 80, "/index.html"));
		final Serializable serializedForm = new DataObjectConverter<>(beforeRename).getSerializableForm();

		final ClassAfterDiscard deserializedClass = new DataObjectConverter<>(ClassAfterDiscard.class, reflectionLookup).getOriginalForm(serializedForm);
		assertEquals(1, deserializedClass.getSomeNumber());
	}

	@Test
	void shouldDeserializeImmutableObjectWithDiscardedField() throws MalformedURLException {
		final OriginalClass beforeRename = new OriginalClass("ABC", 1, new URL("https", "www.fg.cz", 80, "/index.html"));
		final Serializable serializedForm = new DataObjectConverter<>(beforeRename).getSerializableForm();

		final ImmutableClassAfterDiscard deserializedClass = new DataObjectConverter<>(ImmutableClassAfterDiscard.class, reflectionLookup).getOriginalForm(serializedForm);
		assertEquals(1, deserializedClass.getSomeNumber());
	}

	@Test
	void shouldSerializeAndDeserializeObjectWithComplexMapKeys() {
		final TestComplexObject complexObject = createComplexObject(
			"ABC",
			new InnerContainer(
				Collections.singletonMap(
					"RTE",
					createComplexObject("DEF", null)
				),
				Collections.singletonMap(
					new ComplexKey("a", 5),
					createComplexObject("DEF", null)
				),
				null, null, null, null
			)
		);
		final Serializable serializableForm = new DataObjectConverter<>(complexObject).getSerializableForm();
		assertNotNull(serializableForm);
		final TestComplexObject deserializedForm = new DataObjectConverter<>(TestComplexObject.class, reflectionLookup).getOriginalForm(serializableForm);
		assertEquals(complexObject, deserializedForm);
	}

	private String readFromClasspath(String path) throws IOException {
		return IOUtils.toString(
				Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(path)),
				StandardCharsets.UTF_8
		);
	}

	private TestComplexObject createVeryComplexObject() {
		return createComplexObject(
				"ABC",
				new InnerContainer(
						Collections.singletonMap(
								"ZZZ",
								createComplexObject("DEF", null)
						),
						null,
						new LinkedHashSet<>(
								Arrays.asList(
										createComplexObject("RTE", null),
										createComplexObject("EGD", null)
								)
						),
						Arrays.asList(
								createComplexObject("RRR", null),
								createComplexObject("EEE", null)
						),
						new TestComplexObject[]{
								createComplexObject("TTT", null),
								createComplexObject("GGG", null)
						},
						createComplexObject("WWW", null)
				)
		);
	}

	private TestComplexImmutableObject createVeryComplexImmutableObject() {
		return createComplexImmutableObject(
			"ABC",
			new InnerImmutableContainer(
				Collections.singletonMap(
					"ZZZ",
					createComplexImmutableObject("DEF", null)
				),
				null,
				new LinkedHashSet<>(
					Arrays.asList(
						createComplexImmutableObject("RTE", null),
						createComplexImmutableObject("EGD", null)
					)
				),
				Arrays.asList(
					createComplexImmutableObject("RRR", null),
					createComplexImmutableObject("EEE", null)
				),
				new TestComplexImmutableObject[]{
					createComplexImmutableObject("TTT", null),
					createComplexImmutableObject("GGG", null)
				},
				createComplexImmutableObject("WWW", null)
			)
		);
	}

	private String ordered(String unorderedString) {
		final TreeMap<String, Serializable> orderedLines = new TreeMap<>();
		for (String line : unorderedString.split("\n")) {
			final String[] keyValue = line.split("=");
			orderedLines.put(keyValue[0], keyValue[1]);
		}
		final StringBuilder sb = new StringBuilder();
		for (Entry<String, Serializable> entry : orderedLines.entrySet()) {
			sb.append(entry.getKey()).append("=").append(entry.getValue()).append("\n");
		}
		return sb.toString();
	}

	private TestComplexObject createComplexObject(String id, InnerContainer innerContainer) {
		return new TestComplexObject(
				id,
				(byte)1, (byte)2,
				(short)3, (short)4,
				5, 6,
				7L, 7L,
				true, false,
				'A', 'B',
				new BigDecimal("123.12"),
				ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
				LocalDateTime.of(2021, 1, 1, 0, 0, 0, 0),
				LocalDate.of(2021, 1, 1),
				LocalTime.of(0, 0, 0, 0),
				DateTimeRange.since(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())),
				NumberRange.to(124),
				Locale.CANADA,
				SomeEnum.A,
				innerContainer
		);
	}

	private TestComplexImmutableObject createComplexImmutableObject(String id, InnerImmutableContainer innerContainer) {
		return new TestComplexImmutableObject(
			id,
			(byte)1, (byte)2,
			(short)3, (short)4,
			5, 6,
			7L, 7L,
			true, false,
			'A', 'B',
			new BigDecimal("123.12"),
			ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault()),
			LocalDateTime.of(2021, 1, 1, 0, 0, 0, 0),
			LocalDate.of(2021, 1, 1),
			LocalTime.of(0, 0, 0, 0),
			DateTimeRange.since(ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneId.systemDefault())),
			NumberRange.to(124),
			Locale.CANADA,
			SomeEnum.A,
			innerContainer
		);
	}

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public static class TestComplexObject implements Serializable {
		private static final long serialVersionUID = 7169622529995308080L;
		private String fString;
		private Byte fByte;
		private byte fByteP;
		private Short fShort;
		private short fShortP;
		private Integer fInteger;
		private int fIntegerP;
		private Long fLong;
		private long fLongP;
		private Boolean fBoolean;
		private boolean fBooleanP;
		private Character fChar;
		private char fCharP;
		private BigDecimal fBigDecimal;
		private ZonedDateTime fZonedDateTime;
		private LocalDateTime fLocalDateTime;
		private LocalDate fLocalDate;
		private LocalTime fLocalTime;
		private DateTimeRange fDateTimeRange;
		private NumberRange fNumberRange;
		private Locale fLocale;
		private SomeEnum fEnum;
		private InnerContainer innerContainer;
	}

	@Data
	public static class TestComplexImmutableObject implements Serializable {
		private static final long serialVersionUID = 7169622529995308080L;
		private final String fString;
		private final Byte fByte;
		private final byte fByteP;
		private final Short fShort;
		private final short fShortP;
		private final Integer fInteger;
		private final int fIntegerP;
		private final Long fLong;
		private final long fLongP;
		private final Boolean fBoolean;
		private final boolean fBooleanP;
		private final Character fChar;
		private final char fCharP;
		private final BigDecimal fBigDecimal;
		private final ZonedDateTime fZonedDateTime;
		private final LocalDateTime fLocalDateTime;
		private final LocalDate fLocalDate;
		private final LocalTime fLocalTime;
		private final DateTimeRange fDateTimeRange;
		private final NumberRange fNumberRange;
		private final Locale fLocale;
		private final SomeEnum fEnum;
		private final InnerImmutableContainer innerContainer;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class InnerContainer implements Serializable {
		private static final long serialVersionUID = -690137131054088501L;
		private Map<String, TestComplexObject> index;
		private Map<ComplexKey, TestComplexObject> indexWithComplexKey;
		private Set<TestComplexObject> set;
		private List<TestComplexObject> list;
		private TestComplexObject[] arr;
		private TestComplexObject pojo;
	}

	@Data
	public static class InnerImmutableContainer implements Serializable {
		private static final long serialVersionUID = -690137131054088501L;
		private final Map<String, TestComplexImmutableObject> index;
		private final Map<ComplexKey, TestComplexImmutableObject> indexWithComplexKey;
		private final Set<TestComplexImmutableObject> set;
		private final List<TestComplexImmutableObject> list;
		private final TestComplexImmutableObject[] arr;
		private final TestComplexImmutableObject pojo;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ComplexKey implements Serializable {
		private static final long serialVersionUID = 3532029096367942509L;
		private String string;
		private int number;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OriginalClass implements Serializable {
		private static final long serialVersionUID = -5885429548269444707L;
		private String field;
		private int someNumber;
		@NonSerializedData
		private URL url;
	}

	@Data
	public static class OriginalImmutableClass implements Serializable {
		private static final long serialVersionUID = -5885429548269444707L;
		private final String field;
		private final int someNumber;
		@NonSerializedData
		private final URL url;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class ClassAfterRename implements Serializable {
		private static final long serialVersionUID = -7012393429041887402L;
		@RenamedData("field")
		private String renamedField;
		private int someNumber;
	}

	@Data
	public static class ImmutableClassAfterRename implements Serializable {
		private static final long serialVersionUID = -7012393429041887402L;
		@RenamedData("field")
		private final String renamedField;
		private final int someNumber;
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	@DiscardedData("field")
	public static class ClassAfterDiscard implements Serializable {
		private static final long serialVersionUID = -7012393429041887402L;
		private int someNumber;
	}

	@Data
	@DiscardedData("field")
	public static class ImmutableClassAfterDiscard implements Serializable {
		private static final long serialVersionUID = -7012393429041887402L;
		private final int someNumber;
	}

	@Data
	@NoArgsConstructor
	public static class ClassAfterNonDeclaredDiscard implements Serializable {
		private static final long serialVersionUID = 7510539260764292731L;
	}

	@Data
	public static class IncompatibleObject implements Serializable {
		private static final long serialVersionUID = -6419803592905656920L;
		private final int constantValue = 1;
	}

	public enum SomeEnum {
		A, B
	}

}