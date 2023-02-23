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
import io.evitadb.api.query.filter.Language;
import io.evitadb.api.utils.Assert;

import java.io.Serializable;

/**
 * This `attributes` requirement changes default behaviour of the query engine returning only entity primary keys in the result. When
 * this requirement is used result contains [entity bodies](entity_model.md) except `associated data` that could
 * become big. These type of data can be fetched either lazily or by specifying additional requirements in the query.
 *
 * This requirement implicitly triggers {@link EntityBody} requirement because attributes cannot be returned without entity.
 * [Localized interface](classes/localized_interface.md) attributes are returned according to {@link Language}
 * constraint.
 *
 * Example:
 *
 * ```
 * attributes()
 * ```
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class Attributes extends AbstractRequireConstraintLeaf implements EntityContentRequire {
	private static final long serialVersionUID = 869775256765143926L;

	private Attributes(Serializable... arguments) {
		super(arguments);
	}

	public Attributes() {
	}

	@Override
	public boolean isEqualToOrWider(EntityContentRequire require) {
		return require instanceof Attributes;
	}

	@Override
	public <T extends EntityContentRequire> T combineWith(T anotherRequirement) {
		Assert.isTrue(anotherRequirement instanceof Attributes, "Only Attributes requirement can be combined with this one!");
		return anotherRequirement;
	}

	@Override
	public RequireConstraint cloneWithArguments(Serializable[] newArguments) {
		return new Attributes(newArguments);
	}
}
