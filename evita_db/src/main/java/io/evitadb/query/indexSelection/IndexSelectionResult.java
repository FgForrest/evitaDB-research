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

package io.evitadb.query.indexSelection;

import io.evitadb.api.query.filter.IndexUsingConstraint;
import io.evitadb.index.EntityIndex;
import lombok.Data;

import java.util.List;

/**
 * This simple DTO object encapsulates set of {@link TargetIndexes} and information whether additional filtering
 * constraint that implements {@link IndexUsingConstraint} is used in the input query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Data
public class IndexSelectionResult {
	/**
	 * Collection contains all alternative {@link TargetIndexes} sets that might already contain precalculated information
	 * related to {@link EntityIndex} that can be used to partially resolve input filter although the target index set
	 * is not used to resolve entire query filter.
	 */
	private final List<TargetIndexes> targetIndexes;
	/**
	 * Field is set to TRUE when it's already known that filtering constraint contains constraint that uses data from
	 * the index - i.e. constraint implementing {@link IndexUsingConstraint}. This situation allows
	 * certain translators to entirely skip themselves because the constraint will be implicitly evaluated by the other
	 * constraints using already limited subset from the entity index.
	 */
	private final boolean targetIndexQueriedByOtherConstraints;

	/**
	 * Returns true if this DTO contains NO reference to the target indexes.
	 */
	public boolean isEmpty() {
		return targetIndexes.stream().anyMatch(TargetIndexes::isEmpty);
	}

}
