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

package io.evitadb.api.exception;

import io.evitadb.api.CatalogState;

/**
 * Exception is used when there is an attempt to create second or additional session in phase
 * of {@link CatalogState#WARMING_UP}.
 * <p>
 * In this phase there is only single session allowed to fill the database. Multiple sessiona are allowed only when
 * the state is changed to {@link CatalogState#ALIVE}.
 *
 * @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/
public class ConcurrentInitializationException extends RuntimeException {
	private static final long serialVersionUID = -9062588323022507459L;

	public ConcurrentInitializationException(String message) {
		super(message);
	}

}
