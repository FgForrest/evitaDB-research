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

package io.evitadb.api.dataType;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.time.ZonedDateTime;

/**
 * This {@link Serializer} implementation reads/writes {@link DateTimeRange} from/to binary format.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DateTimeRangeSerializer extends Serializer<DateTimeRange> {

	@Override
	public void write(Kryo kryo, Output output, DateTimeRange dateTimeRange) {
		final ZonedDateTime from = dateTimeRange.getPreciseFrom();
		if (from == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			ZonedDateTimeSerializer.write(output, from);
		}
		final ZonedDateTime to = dateTimeRange.getPreciseTo();
		if (to == null) {
			output.writeBoolean(false);
		} else {
			output.writeBoolean(true);
			ZonedDateTimeSerializer.write(output, to);
		}
	}

	@Override
	public DateTimeRange read(Kryo kryo, Input input, Class<? extends DateTimeRange> type) {
		final ZonedDateTime from = input.readBoolean() ? ZonedDateTimeSerializer.read(input) : null;
		final ZonedDateTime to = input.readBoolean() ? ZonedDateTimeSerializer.read(input) : null;
		return DateTimeRange.between(from, to);
	}

}
