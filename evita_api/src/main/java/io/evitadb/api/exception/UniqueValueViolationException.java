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

import lombok.Getter;

import java.io.Serializable;

/**
 * Exception is thrown when there is attempt to index entity with conflicting attribute which violates unique constraint.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2020
 */
public class UniqueValueViolationException extends IllegalArgumentException {
	private static final long serialVersionUID = -3516490780028476047L;
	@Getter private final String attributeName;
	@Getter private final Serializable value;
	@Getter private final int existingRecordId;
	@Getter private final int newRecordId;

	public UniqueValueViolationException(String attributeName, Serializable value, int existingRecordId, int newRecordId) {
		super("Can't change existing unique attribute for " + attributeName + " key: " + value + " (existing: " + existingRecordId + ", new: " + newRecordId + ")!");
		this.attributeName = attributeName;
		this.value = value;
		this.existingRecordId = existingRecordId;
		this.newRecordId = newRecordId;
	}
}
