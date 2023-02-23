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

package io.evitadb.api;

import io.evitadb.api.exception.RollbackException;
import io.evitadb.index.transactionalMemory.TransactionalLayerConsumer;
import io.evitadb.index.transactionalMemory.TransactionalLayerMaintainer;
import io.evitadb.index.transactionalMemory.TransactionalMemory;
import io.evitadb.storage.model.storageParts.EntityCollectionUpdateInstruction;
import io.evitadb.storage.model.storageParts.PersistedStoragePartKey;
import io.evitadb.storage.model.storageParts.StoragePart;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.NotThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/**
 * Transaction represents internal object that allows to track all information necessary to keep writes in isolation
 * and finally commits them to the shared data storage or throw them away in case of rollback.
 * <p>
 * Transaction is created by creating instance of this class (i.e. constructor), transaction is closed by calling
 * {@link #close()} method and it either commits or rollbacks the changes (when {@link #setRollbackOnly()} is called.
 * <p>
 * This concept is pretty much known from relational databases.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@NotThreadSafe
@Log4j2
public class Transaction extends TransactionBase {
	private static final ReentrantLock LOCK = new ReentrantLock(true);
	public static final String ERROR_MESSAGE_TIMEOUT = "Failed to commit transaction within timeout!";
	/**
	 * Contains unique transactional id that gets incremented with each transaction opened in the catalog. Latest
	 * committed transaction id gets printed into the {@link io.evitadb.storage.model.CatalogHeader} and is restored
	 * when catalog is loaded. Transaction ids are sequential and transaction with higher id is guaranteed to be
	 * committed later than the transaction with lower id.
	 *
	 * TOBEDONE JNO - this should be changed - there should be another id that is connected with the commit phase and
	 * should be assigned when transaction is accepted and ordered for the commit
	 */
	@Getter private final long id;
	/**
	 * List of {@link StoragePart} items that got modified in transaction and needs to be persisted.
	 */
	private final Map<Serializable, List<EntityCollectionUpdateInstruction>> updateInstructions = new HashMap<>();

	public Transaction(@Nonnull Catalog currentCatalog, @Nonnull Consumer<Catalog> updatedCatalogCallback, @Nonnull Runnable beforeClose) {
		this.id = currentCatalog.getNextTransactionId();
		TransactionalMemory.open();
		TransactionalMemory.addTransactionCommitHandler(transactionalLayer -> {
			final List<? extends TransactionalLayerConsumer> layerConsumers = transactionalLayer.getLayerConsumers();
			if (!layerConsumers.isEmpty()) {
				try {
					if (LOCK.tryLock(5, TimeUnit.SECONDS)) {
						onCommit(currentCatalog, updatedCatalogCallback, transactionalLayer);
					} else {
						log.error(ERROR_MESSAGE_TIMEOUT);
						throw new RollbackException(ERROR_MESSAGE_TIMEOUT);
					}
				} catch (InterruptedException e) {
					log.error(ERROR_MESSAGE_TIMEOUT);
					Thread.currentThread().interrupt();
					throw new RollbackException(ERROR_MESSAGE_TIMEOUT);
				}
			}
			beforeClose.run();
		});
	}

	/**
	 * Registers an object that got modified in this transaction and needs persisting into the memory tables.
	 */
	public void registerForPersistence(@Nonnull Serializable entityType, @Nonnull Collection<StoragePart> storageParts) {
		final List<EntityCollectionUpdateInstruction> theUpdateInstructions = this.updateInstructions.computeIfAbsent(entityType, et -> new LinkedList<>());
		for (StoragePart storagePart : storageParts) {
			theUpdateInstructions.add(new EntityCollectionUpdateInstruction(storagePart));
		}
	}

	/**
	 * Registers an object that got removed in this transaction and needs removal from the memory tables.
	 */
	public void registerForRemoval(@Nonnull Serializable entityType, @Nonnull Collection<PersistedStoragePartKey> removedPartKeys) {
		final List<EntityCollectionUpdateInstruction> theUpdateInstructions = this.updateInstructions.computeIfAbsent(entityType, et -> new LinkedList<>());
		for (PersistedStoragePartKey removedPartKey : removedPartKeys) {
			theUpdateInstructions.add(new EntityCollectionUpdateInstruction(removedPartKey));
		}
	}

	@Override
	public void close() {
		super.close();

		if (isRollbackOnly()) {
			TransactionalMemory.rollback();
		} else {
			TransactionalMemory.commit();
		}
	}

	/**
	 * Method is executed when commit is executed.
	 */
	private void onCommit(@Nonnull Catalog currentCatalog, @Nonnull Consumer<Catalog> updatedCatalogCallback, @Nonnull TransactionalLayerMaintainer transactionalLayer) {
		try {
			// init new catalog with the same collections as previous one
			final Map<Serializable, EntityCollection> entityIndexCopy = transactionalLayer.getStateCopyWithCommittedChanges(
				currentCatalog.entityCollections, this
			);
			final Catalog newCatalog = new Catalog(
				currentCatalog.getConfiguration(),
				currentCatalog.getCatalogState(),
				currentCatalog.ioService,
				currentCatalog.cacheSupervisor,
				currentCatalog.observableOutputKeeper,
				currentCatalog.readWriteSessionCount,
				currentCatalog.txPkSequence,
				id,
				entityIndexCopy
			);
			// we need to switch references working with catalog (inter index relations) to new catalog
			// the collections are not yet used anywhere - we're still safe here
			entityIndexCopy.values().forEach(it -> it.updateReferenceToCatalog(newCatalog));
			// now let's flush the catalog on the disk
			newCatalog.flush(id, updateInstructions);
			// and replace reference to catalog in an atomic way
			updatedCatalogCallback.accept(newCatalog);
		} catch (Throwable throwable) {
			throw new RollbackException("Unexpected exception while committing!", throwable);
		} finally {
			LOCK.unlock();
		}
	}

}
