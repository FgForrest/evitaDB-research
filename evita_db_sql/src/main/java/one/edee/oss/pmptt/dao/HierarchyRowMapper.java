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

package one.edee.oss.pmptt.dao;

import lombok.RequiredArgsConstructor;
import one.edee.oss.pmptt.model.Hierarchy;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Maps stored schema (collection) to hierarchy.
 *
 * @author Lukáš Hornych 2021
 */
@RequiredArgsConstructor
class HierarchyRowMapper implements RowMapper<Hierarchy> {
	private final HierarchyStorage hierarchyStorage;

	@Override
	public Hierarchy mapRow(ResultSet resultSet, int i) throws SQLException {
		final Hierarchy hierarchy = new Hierarchy(
				resultSet.getString("entityType"),
				((short) (resultSet.getShort("hierarchyLevels") - 1)),
				((short) (resultSet.getShort("hierarchySectionSize") - 1))
		);
		hierarchy.setStorage(hierarchyStorage);
		return hierarchy;
	}
}
