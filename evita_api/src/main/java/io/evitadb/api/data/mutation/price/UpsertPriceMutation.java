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

import io.evitadb.api.data.PriceContract;
import io.evitadb.api.data.mutation.SchemaEvolvingLocalMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EvolutionMode;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.function.UnaryOperator;

/**
 * This mutation allows to create / update {@link Price} of the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UpsertPriceMutation extends PriceMutation implements SchemaEvolvingLocalMutation<PriceContract, PriceKey> {
	public static final long PRICE_UPSERT_PRIORITY = PRIORITY_UPSERT;
	private static final long serialVersionUID = 6899193328262302023L;
	@Getter private final Integer innerRecordId;
	@Getter private final BigDecimal priceWithoutVat;
	@Getter private final BigDecimal vat;
	@Getter private final BigDecimal priceWithVat;
	@Getter private final DateTimeRange validity;
	@Getter private final boolean sellable;

	public UpsertPriceMutation(PriceKey priceKey, Integer innerRecordId, BigDecimal priceWithoutVat, BigDecimal vat, BigDecimal priceWithVat, DateTimeRange validity, boolean sellable) {
		super(priceKey);
		this.innerRecordId = innerRecordId;
		this.priceWithoutVat = priceWithoutVat;
		this.vat = vat;
		this.priceWithVat = priceWithVat;
		this.validity = validity;
		this.sellable = sellable;
	}

	public UpsertPriceMutation(PriceKey priceKey, PriceContract price) {
		super(priceKey);
		this.innerRecordId = price.getInnerRecordId();
		this.priceWithoutVat = price.getPriceWithoutVat();
		this.vat = price.getVat();
		this.priceWithVat = price.getPriceWithVat();
		this.validity = price.getValidity();
		this.sellable = price.isSellable();
	}

	@Override
	public @Nonnull
	Class<PriceContract> affects() {
		return PriceContract.class;
	}

	@Nonnull
	@Override
	public PriceContract mutateLocal(@Nullable PriceContract value) {
		if (value == null) {
			return new Price(
				priceKey,
				innerRecordId,
				priceWithoutVat,
				vat,
				priceWithVat,
				validity,
				sellable
			);
		} else {
			return new Price(
				value.getVersion() + 1,
				value.getPriceKey(),
				innerRecordId,
				priceWithoutVat,
				vat,
				priceWithVat,
				validity,
				sellable
			);
		}
	}

	@Override
	public long getPriority() {
		return PRICE_UPSERT_PRIORITY;
	}

	@Nonnull
	@Override
	public Serializable getSkipToken() {
		return Price.class;
	}

	@Override
	public @Nonnull
	EntitySchema verifyOrEvolveSchema(@Nonnull EntitySchema schema, @Nonnull UnaryOperator<EntitySchema> schemaUpdater) throws InvalidMutationException {
		if (!schema.isWithPrice()) {
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
