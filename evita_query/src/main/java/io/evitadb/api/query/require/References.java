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

package io.evitadb.api.query.require;

import io.evitadb.api.query.RequireConstraint;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * This `references` requirement changes default behaviour of the query engine returning only entity primary keys in the result.
 * When this requirement is used result contains [entity bodies](entity_model.md) along with references with to entites
 * or external objects specified in one or more arguments of this requirement.
 *
 * This requirement implicitly triggers {@link EntityBody} requirement because references cannot be returned without entity.
 *
 * Example:
 *
 * ```
 * references(CATEGORY, 'stocks')
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class References extends AbstractRequireConstraintLeaf implements EntityContentRequire {
	private static final long serialVersionUID = 3374240925555151814L;

	public References(Serializable... referencedEntityType) {
		super(referencedEntityType);
	}

	/**
	 * Returns names of entity types or external entities which references should be loaded along with entity.
	 */
	@Nonnull
	public Serializable[] getReferencedEntityType() {
		return Arrays.stream(getArguments())
			.map(Serializable.class::cast)
			.toArray(Serializable[]::new);
	}

	/**
	 * Returns TRUE if all available associated data were requested to load.
	 */
	public boolean isAllRequested() {
		return ArrayUtils.isEmpty(getArguments());
	}

	@Override
	public boolean isEqualToOrWider(EntityContentRequire require) {
		return require instanceof References;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends EntityContentRequire> T combineWith(T anotherRequirement) {
		Assert.isTrue(anotherRequirement instanceof References, "Only References requirement can be combined with this one!");
		if (isAllRequested()) {
			return (T) this;
		} else if (((References) anotherRequirement).isAllRequested()) {
			return anotherRequirement;
		} else {
			return (T) new References(
				Stream.concat(
						Arrays.stream(getArguments()).map(Serializable.class::cast),
						Arrays.stream(anotherRequirement.getArguments()).map(Serializable.class::cast)
					)
					.distinct()
					.toArray(Serializable[]::new)
			);
		}
	}

	@Override
	public boolean isApplicable() {
		return true;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new References(newArguments);
	}
}
