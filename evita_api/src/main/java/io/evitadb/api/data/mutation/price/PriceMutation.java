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
import io.evitadb.api.data.mutation.LocalMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.Price;
import io.evitadb.api.data.structure.Price.PriceKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

/**
 * Base mutation class for mutations that work with {@link Price} of the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@RequiredArgsConstructor
public abstract class PriceMutation implements LocalMutation<PriceContract, PriceKey> {
	private static final long serialVersionUID = 2424285135744614172L;
	@Getter @Nonnull protected final PriceKey priceKey;

	@Override
	public PriceKey getComparableKey() {
		return priceKey;
	}
}
