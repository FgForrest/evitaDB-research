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

package io.evitadb.api.data.mutation.price;

import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Prices;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * This mutation allows to set / remove {@link PriceInnerRecordHandling} behaviour of the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PriceInnerRecordHandlingMutation implements SchemaEvolvingLocalMutation<Prices, PriceInnerRecordHandling> {
	private static final long serialVersionUID = -2047915704875849615L;
	@Getter private final PriceInnerRecordHandling priceInnerRecordHandling;

	public PriceInnerRecordHandlingMutation(PriceInnerRecordHandling priceInnerRecordHandling) {
		this.priceInnerRecordHandling = priceInnerRecordHandling;
	}

	@Override
	public @Nonnull
	Class<Prices> affects() {
		return Prices.class;
	}

	@Nonnull
	@Override
	public Prices mutateLocal(@Nullable Prices value) {
		if (value == null) {
			return new Prices(priceInnerRecordHandling);
		} else {
			return new Prices(
				value.getVersion() + 1,
				value.getPrices(),
				priceInnerRecordHandling
			);
		}
	}

	@Override
	public long getPriority() {
		// we need to run before the prices are upserted
		return UpsertPriceMutation.PRICE_UPSERT_PRIORITY + 1;
	}

	@Override
	public PriceInnerRecordHandling getComparableKey() {
		return priceInnerRecordHandling;
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return Price.class;
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		if (!schema.isWithPrice() && priceInnerRecordHandling != PriceInnerRecordHandling.NONE) {
			if (schema.allows(EvolutionMode.ADDING_PRICES)) {
				return schema.open(schemaUpdater).withPrice().applyChanges();
			} else {
				throw new InvalidMutationException(
					"Entity " + schema.getName() + " doesn't support prices, " +
						"you need to change the schema definition for it first."
				);
			}
		} else {
			return schema;
		}
	}
}
