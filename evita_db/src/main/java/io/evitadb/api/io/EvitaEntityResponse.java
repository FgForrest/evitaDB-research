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

package io.evitadb.api.io;

import io.evitadb.api.data.EntityContract;
import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.dataType.DataChunk;
import io.evitadb.api.query.Query;

import javax.annotation.Nonnull;

/**
 * This class passes simple references to the found entities - i.e. full {@link EntityContract}. There wouldn't be probably
 * necessary any additional extension for this class, but if so, implementors may add new properties to this class here.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EvitaEntityResponse extends EvitaResponseBase<SealedEntity> {

	public EvitaEntityResponse(@Nonnull Query sourceQuery, @Nonnull DataChunk<SealedEntity> recordPage) {
		super(sourceQuery, recordPage);
	}

	public EvitaEntityResponse(@Nonnull Query sourceQuery, @Nonnull DataChunk<SealedEntity> recordPage, EvitaResponseExtraResult... additionalResults) {
		super(sourceQuery, recordPage, additionalResults);
	}

}
