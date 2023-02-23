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

package io.evitadb.api.schema;

import java.io.Serializable;
import java.util.function.BooleanSupplier;

/**
 * Internal {@link AssociatedDataSchema} builder used solely from within {@link EntitySchemaBuilder}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class AssociatedDataSchemaBuilder {
	/**
	 * Name of the associated data.
	 */
	private final String name;
	/**
	 * May be set by the client - makes associated data localized.
	 */
	private boolean localized;
	/**
	 * Type of the associated data.
	 */
	private final Class<? extends Serializable> ofType;

	public AssociatedDataSchemaBuilder(AssociatedDataSchema existingSchema) {
		this.name = existingSchema.getName();
		this.localized = existingSchema.isLocalized();
		this.ofType = existingSchema.getType();
	}

	AssociatedDataSchemaBuilder(String name, Class<? extends Serializable> ofType) {
		this.name = name;
		this.localized = false;
		this.ofType = ofType;
	}

	/**
	 * Localized associated data has to be ALWAYS used in connection with specific {@link java.util.Locale}. In other
	 * words - it cannot be stored unless associated locale is also provided.
	 *
	 * @return builder to continue with configuration
	 */
	public AssociatedDataSchemaBuilder localized() {
		this.localized = true;
		return this;
	}

	/**
	 * Localized associated data has to be ALWAYS used in connection with specific {@link java.util.Locale}. In other
	 * words - it cannot be stored unless associated locale is also provided.
	 *
	 * @param decider returns true when attribute should be localized
	 * @return builder to continue with configuration
	 */
	public AssociatedDataSchemaBuilder localized(BooleanSupplier decider) {
		this.localized = decider.getAsBoolean();
		return this;
	}

	/**
	 * Builds new instance of immutable {@link AssociatedDataSchema} filled with updated configuration.
	 * @return
	 */
	public AssociatedDataSchema build() {
		return new AssociatedDataSchema(
				name, ofType, localized
		);
	}
}
