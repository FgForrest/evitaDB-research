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

package io.evitadb.storage.exception;

import lombok.EqualsAndHashCode;

/**
 *  Indicates problems with querying from ES.
 *
 *  @author Stɇvɇn Kamenik (kamenik.stepan@gmail.cz) (c) 2021
 **/

/**
 * No extra information provided, if you see this, code is my very best work,
 * so each method is self-explanatory and description would be useless.
 * If this code is not masterpiece, there wasn't time to write proper code
 * and not even documentation, so so sorry.
 *
 * @author Štěpán Kameník (kamenik@fg.cz), FG Forrest a.s. (c) 2022
 **/
@EqualsAndHashCode(callSuper = true)
public class EvitaFacetComputationCheckedException extends EvitaEsException {
    public EvitaFacetComputationCheckedException(String message) {
        super(message);
    }
}
