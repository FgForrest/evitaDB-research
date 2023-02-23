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

package io.evitadb.api.dataType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.ByteBufferInput;
import com.esotericsoftware.kryo.io.ByteBufferOutput;
import io.evitadb.api.data.DataObjectConverter;
import io.evitadb.api.data.ReflectionCachingBehaviour;
import io.evitadb.api.dataType.trie.TrieSerializer;
import io.evitadb.api.serialization.KryoFactory;
import io.evitadb.api.serialization.common.WritableClassResolver;
import io.evitadb.api.utils.ReflectionLookup;
import lombok.Data;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies contract of {@link ComplexDataObjectSerializer} and {@link TrieSerializer}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class ComplexDataObjectSerializerTest {
	private final ReflectionLookup reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);

	@Test
	void shouldSerializeAndDeserializeHeader() {
		final Map<String, Integer> integerMap = new HashMap<>();
		integerMap.put("A", 89);
		integerMap.put("B", 93);
		final SomeDto dtoToStore = new SomeDto(
			1,
			Arrays.asList("abc", "def"),
			integerMap,
			new Long[] {7L, 9L, 10L}
		);

		final ByteArrayOutputStream baos = new ByteArrayOutputStream(2048);
		final WritableClassResolver classResolver = new WritableClassResolver(1000);
		final Kryo kryo = KryoFactory.createKryo(classResolver);
		final Serializable serializableForm = DataObjectConverter.getSerializableForm(dtoToStore);
		try (final ByteBufferOutput output = new ByteBufferOutput(baos)) {
			kryo.writeObject(output, serializableForm);
		}

		final byte[] serializedSchema = baos.toByteArray();
		assertNotNull(serializedSchema);
		assertTrue(serializedSchema.length > 0);

		final ComplexDataObject deserializedForm;
		try (final ByteBufferInput input = new ByteBufferInput(serializedSchema)) {
			deserializedForm = kryo.readObject(input, ComplexDataObject.class);
		}
		final SomeDto deserializedDto = DataObjectConverter.getOriginalForm(deserializedForm, SomeDto.class, reflectionLookup);

		assertEquals(dtoToStore, deserializedDto);
	}

	@Data
	public static class SomeDto implements Serializable {
		private static final long serialVersionUID = -2733408526337234240L;
		private final int id;
		private final List<String> name;
		private final Map<String, Integer> counts;
		private final Long[] numbers;
	}

}