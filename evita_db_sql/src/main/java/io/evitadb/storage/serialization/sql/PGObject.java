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

package io.evitadb.storage.serialization.sql;

import org.postgresql.util.PGobject;

import javax.annotation.Nullable;
import java.sql.SQLException;

/**
 * Extension of {@link PGobject} which adds easier creation methods.
 *
 * @author Lukáš Hornych 2021
 */
public class PGObject extends PGobject {

    private static final long serialVersionUID = 931397990836757587L;

    public PGObject(String type, String value) {
        super();

        setType(type);
        try {
            setValue(value);
        } catch (SQLException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj instanceof PGobject) {
            final Object otherType = ((PGobject) obj).getType();
            final Object otherValue = ((PGobject) obj).getValue();

            if (!otherType.equals(getType())) {
                return false;
            }
            if (otherValue == null) {
                return getValue() == null;
            }
            return otherValue.equals(getValue());
        }
        return false;
    }


    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (getValue() == null ? 0 : getValue().hashCode());
        hash = 31 * hash + (getType() == null ? 0 : getType().hashCode());
        return hash;
    }
}
