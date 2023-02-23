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

package io.evitadb.query.response;

import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.StringUtils;
import lombok.Data;

import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This DTO contains detailed information about query processing time and its decomposition to single operations.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2019
 */
@Data
public class QueryTelemetry implements EvitaResponseExtraResult {
	private static final long serialVersionUID = 4135633155110416711L;

	/**
	 * Phase of the query processing.
	 */
	private final QueryPhase operation;
	/**
	 * Date and time of the start of this step in nanoseconds.
	 */
	private final long start;
	/**
	 * Internal steps of this telemetry step (operation decomposition).
	 */
	private final List<QueryTelemetry> steps = new LinkedList<>();
	/**
	 * Arguments of the processing phase.
	 */
	private Serializable[] arguments;
	/**
	 * Duration in nanoseconds.
	 */
	private long spentTime;

	public QueryTelemetry(QueryPhase operation, Serializable... arguments) {
		this.operation = operation;
		this.arguments = arguments;
		this.start = System.nanoTime();
	}

	/**
	 * Finalizes current step of the query telemetry and stores the time spent.
	 */
	public QueryTelemetry finish(Serializable... arguments) {
		this.spentTime += (System.nanoTime() - start);
		Assert.isTrue(ArrayUtils.isEmpty(this.arguments), "Arguments have been aready set!");
		this.arguments = arguments;
		return this;
	}

	/**
	 * Adds internal step of query processing in current phase.
	 */
	public QueryTelemetry addStep(QueryPhase operation, Serializable... arguments) {
		final QueryTelemetry step = new QueryTelemetry(operation, arguments);
		this.steps.add(step);
		return step;
	}

	/**
	 * Finalizes current step of the query telemetry and stores the time spent.
	 */
	public QueryTelemetry finish() {
		this.spentTime += (System.nanoTime() - start);
		return this;
	}

	@Override
	public String toString() {
		return toString(0);
	}

	public String toString(int indent) {
		final StringBuilder sb = new StringBuilder(" ".repeat(indent));
		sb.append(operation);
		if (arguments.length > 0) {
			sb.append("(")
				.append(Arrays.stream(arguments).map(Object::toString).collect(Collectors.joining(", ")))
				.append(") ");
		}
		sb.append(": ").append(StringUtils.formatNano(spentTime)).append("\n");
		if (!steps.isEmpty()) {
			for (QueryTelemetry step : steps) {
				sb.append(step.toString(indent + 5));
			}
		}
		return sb.toString();
	}

	/**
	 * Enum contains all query execution phases, that leads from request to response.
	 */
	public enum QueryPhase {

		/**
		 * Entire query execution time.
		 */
		OVERALL,
		/**
		 * Entire planning phase of the query execution.
		 */
		PLANNING,
		/**
		 * Determining which indexes should be used.
		 */
		PLANNING_INDEX_USAGE,
		/**
		 * Creating formula for filtering entities.
		 */
		PLANNING_FILTER,
		/**
		 * Creating formula for filtering entities.
		 */
		PLANNING_FILTER_ALTERNATIVE,
		/**
		 * Creating formula for sorting result entities.
		 */
		PLANNING_SORT,
		/**
		 * Creating factories for requested extra results.
		 */
		PLANNING_EXTRA_RESULT_FABRICATION,
		/**
		 * Entire query execution phase.
		 */
		EXECUTION,
		/**
		 * Computing entities that should be returned in output (filtering).
		 */
		EXECUTION_FILTER,
		/**
		 * Sorting output entities and slicing requested page.
		 */
		EXECUTION_SORT_AND_SLICE,
		/**
		 * Fabricating requested extra results.
		 */
		EXTRA_RESULTS_FABRICATION,
		/**
		 * Fabricating requested single extra result.
		 */
		EXTRA_RESULT_ITEM_FABRICATION,
		/**
		 * Fetching rich data from the storage based on computed entity primary keys.
		 */
		FETCHING

	}
}
