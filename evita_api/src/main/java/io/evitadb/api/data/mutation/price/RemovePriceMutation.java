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
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This mutation allows to remove existing {@link Price} of the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RemovePriceMutation extends PriceMutation {
	private static final long serialVersionUID = -1049985270997762455L;

	public RemovePriceMutation(PriceKey priceKey) {
		super(priceKey);
	}

	@Override
	public @Nonnull Class<PriceContract> affects() {
		return PriceContract.class;
	}

	@Nonnull
	@Override
	public PriceContract mutateLocal(@Nullable PriceContract value) {
		Assert.isTrue(
				value != null && value.exists(),
				() -> new InvalidMutationException("Cannot remove price that doesn't exist!")
		);
		return new Price(
			value.getVersion() + 1,
			value.getPriceKey(),
			value.getInnerRecordId(),
			value.getPriceWithoutVat(),
			value.getVat(),
			value.getPriceWithVat(),
			value.getValidity(),
			value.isSellable(),
			true
		);
	}

	@Override
	public long getPriority() {
		return PRIORITY_REMOVAL;
	}
}
