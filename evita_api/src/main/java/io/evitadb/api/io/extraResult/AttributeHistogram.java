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

package io.evitadb.api.io.extraResult;

import io.evitadb.api.io.EvitaResponseExtraResult;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nullable;
import java.util.Map;

/**
 * Attribute histogram is just index for {@link Histogram} objects for multiple attributes.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 * @see Histogram
 */
@RequiredArgsConstructor
public class AttributeHistogram implements EvitaResponseExtraResult {
	private static final long serialVersionUID = -7146180484780093482L;
	private final Map<String, HistogramContract> histograms;

	/**
	 * Returns {@link Histogram} for the passed attribute when it was requested to be created by the query requirements.
	 */
	@Nullable
	public HistogramContract getHistogram(String attributeName) {
		return histograms.get(attributeName);
	}
}
