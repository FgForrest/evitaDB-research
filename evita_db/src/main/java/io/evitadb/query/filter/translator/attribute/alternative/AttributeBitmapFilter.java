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

import io.evitadb.api.data.SealedEntity;
import io.evitadb.api.query.require.Attributes;
import io.evitadb.api.query.require.EntityContentRequire;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.query.algebra.deferred.EntityToBitmapFilter;
import io.evitadb.query.context.QueryContext;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Predicate;

/**
 * Implementation of {@link EntityToBitmapFilter} that verifies that the entity has the appropriate attribute value
 * matching the {@link #filter} predicate.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@RequiredArgsConstructor
public class AttributeBitmapFilter implements EntityToBitmapFilter {
	private static final EntityContentRequire[] ENTITY_CONTENT_REQUIRES = {
		new Attributes()
	};

	/**
	 * Contains name of the attribute value that has to be retrieved and passed to the filter predicate.
	 */
	private final String attributeName;
	/**
	 * Contains the predicate that must be fulfilled in order attribute value is accepted by the filter.
	 */
	private final Predicate<Object> filter;

	@Nonnull
	@Override
	public EntityContentRequire[] getRequirements() {
		return ENTITY_CONTENT_REQUIRES;
	}

	@Nonnull
	@Override
	public Bitmap filter(@Nonnull QueryContext queryContext, @Nonnull List<SealedEntity> entities) {
		final BaseBitmap result = new BaseBitmap();
		// iterate over all entities
		for (SealedEntity entity : entities) {
			// and filter by predicate
			if (filter.test(entity.getAttribute(attributeName))) {
				result.add(entity.getPrimaryKey());
			}
		}
		return result;
	}

}
