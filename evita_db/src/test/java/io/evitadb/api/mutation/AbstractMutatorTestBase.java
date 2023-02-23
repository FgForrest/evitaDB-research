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

package io.evitadb.api.mutation;

import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.GlobalEntityIndex;
import io.evitadb.test.generator.DataGenerator;

import java.util.function.UnaryOperator;

/**
 * This class contains shared variables and logic for mutation specific tests in this package.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
abstract class AbstractMutatorTestBase {
	protected final MockStorageContainerAccessor containerAccessor = new MockStorageContainerAccessor();
	protected final DataGenerator dataGenerator = new DataGenerator();
	protected final EntitySchema schema = dataGenerator.getSampleProductSchema(
		UnaryOperator.identity(),
		AbstractMutatorTestBase.this::alterProductSchema
	);
	protected final GlobalEntityIndex index = new GlobalEntityIndex(1, new EntityIndexKey(EntityIndexType.GLOBAL), () -> schema);
	protected final EntityIndexLocalMutationExecutor executor = new EntityIndexLocalMutationExecutor(
		containerAccessor, 1, new MockEntityIndexCreator(index), () -> schema,
		entityType -> entityType.equals(schema.getName()) ? schema : null
	);

	protected abstract void alterProductSchema(EntitySchemaBuilder schema);

}
