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

package io.evitadb.api.configuration;

import lombok.Data;

/**
 * This class is simple DTO object holding general options of the Evita shared for all catalogs (or better - catalog
 * agnostic).
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@Data
public class EvitaConfiguration {
	/**
	 * Defines count of threads that are spun up in {@link io.evitadb.scheduling.Scheduler} for handling maintenance
	 * tasks. The more catalog in Evita DB there is, the higher count of thread count might be required.
	 */
	private final int backgroundThreadCount = 2;
	/**
	 * Defines a {@link Thread#getPriority()} for background threads. The number must be in interval 1-10. The threads
	 * with higher priority should be preferred over the ones with lesser priority.
	 */
	private final int backgroundThreadPriority = 5;
}
