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

package io.evitadb.index.bool;

import io.evitadb.api.Transaction;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalLayerProducer;
import io.evitadb.index.transactionalMemory.TransactionalObjectVersion;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;

import static io.evitadb.index.transactionalMemory.TransactionalMemory.getTransactionalMemoryLayer;

/**
 * This class envelopes simple primitive boolean and makes it transactional. This means, that the boolean can be updated
 * by multiple writers and also multiple readers can read from its original array without spotting the changes made
 * in transactional access. Each transaction is bound to the same thread and different threads doesn't see changes in
 * another threads.
 *
 * If no transaction is opened, changes are applied directly to the delegate array. In such case the class is not thread
 * safe for multiple writers!
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@ThreadSafe
public class TransactionalBoolean implements TransactionalLayerProducer<BooleanChanges, Boolean>, Serializable {
	private static final long serialVersionUID = 7796376128158582312L;
	@Getter private final long id = TransactionalObjectVersion.SEQUENCE.nextId();
	private boolean value;

	@Override
	public BooleanChanges createLayer() {
		return new BooleanChanges();
	}

	/**
	 * Sets the value to TRUE in a transactional safe way (if transaction is available).
	 */
	public void setToTrue() {
		final BooleanChanges layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			value = true;
		} else {
			layer.setToTrue();
		}
	}

	/**
	 * Sets the value to FALSE in a transactional safe way (if transaction is available).
	 */
	public boolean isTrue() {
		final BooleanChanges layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			return value;
		} else {
			return layer.isTrue();
		}
	}

	/**
	 * Method resets the local value to FALSE.
	 */
	public void reset() {
		final BooleanChanges layer = getTransactionalMemoryLayer(this);
		if (layer == null) {
			value = false;
		} else {
			layer.setToFalse();
		}
	}

	/*
		TransactionalLayerProducer implementation
	 */

	@Override
	public Boolean createCopyWithMergedTransactionalMemory(@Nullable BooleanChanges layer, @Nonnull TransactionalLayerMaintainer transactionalLayer, @Nullable Transaction transaction) {
		return layer == null ? value : layer.isTrue();
	}
}
