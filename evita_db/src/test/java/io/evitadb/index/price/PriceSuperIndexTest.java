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

package io.evitadb.index.price;

import com.carrotsearch.hppc.IntIntHashMap;
import com.carrotsearch.hppc.IntIntMap;
import io.evitadb.api.data.PriceInnerRecordHandling;
import io.evitadb.api.data.structure.Price.PriceKey;
import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.index.price.model.PriceIndexKey;
import io.evitadb.index.price.model.priceRecord.PriceRecord;
import io.evitadb.index.price.model.priceRecord.PriceRecordContract;
import io.evitadb.index.price.model.priceRecord.PriceRecordInnerRecordSpecific;
import io.evitadb.index.range.RangeIndex;
import lombok.Data;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static io.evitadb.utils.AssertionUtils.assertStateAfterCommit;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This class verifies contract of {@link PriceSuperIndex}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class PriceSuperIndexTest {
	private final AtomicInteger priceIdSequence = new AtomicInteger(0);
	private static final Currency CURRENCY_CZK = Currency.getInstance("CZK");
	private static final String PRICE_LIST = "basic";
	private final PriceSuperIndex priceIndex = new PriceSuperIndex();
	private final IntIntMap priceToInternalId = new IntIntHashMap();

	private void addPrice(@Nonnull PriceListAndCurrencyPriceSuperIndex priceListIndex, int entityPrimaryKey, int priceId, @Nullable Integer innerRecordId, DateTimeRange validity, int priceWithoutVat, int priceWithVat) {
		final PriceRecordContract priceRecord = innerRecordId == null ?
			new PriceRecord(priceIdSequence.incrementAndGet(), priceId, entityPrimaryKey, priceWithVat, priceWithoutVat) :
			new PriceRecordInnerRecordSpecific(priceIdSequence.incrementAndGet(), priceId, entityPrimaryKey, innerRecordId, priceWithVat, priceWithoutVat);
		priceListIndex.addPrice(priceRecord, validity);
		priceToInternalId.put(priceRecord.getPriceId(), priceRecord.getInternalPriceId());
	}

	@Test
	void shouldAddStandardPrice() {
		priceIndex.addPrice(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, 20, null, 1000, 1210);
		priceIndex.addPrice(2, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, 21, null, 999, 2000);
		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(2, priceRecords.length);

		final PriceRecordInnerRecordSpecific price1 = new PriceRecordInnerRecordSpecific(1, 10, 1, 20,1210, 1000);
		final PriceRecordInnerRecordSpecific price2 = new PriceRecordInnerRecordSpecific(2, 11, 2, 21, 2000, 999);
		assertEquals(price1, priceRecords[0]);
		assertEquals(price2, priceRecords[1]);

		assertArrayEquals(new PriceRecordContract[] {price1}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(priceRecords[0].getEntityPrimaryKey()));

		assertArrayEquals(new int[]{1, 2}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{1, 2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{1, 2}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());
	}

	@Test
	void shouldAddStandardPriceWithValidity() {
		final DateTimeRange validity1 = DateTimeRange.between(ZonedDateTime.now().minusMinutes(10), ZonedDateTime.now().plusMinutes(10));
		final DateTimeRange validity2 = DateTimeRange.between(ZonedDateTime.now().plusHours(1).minusMinutes(10), ZonedDateTime.now().plusHours(1).plusMinutes(10));

		priceIndex.addPrice(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, null, validity1, 1000, 1210);
		priceIndex.addPrice(2, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, null, validity2, 999, 2000);
		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now().plusHours(1)).compute().getArray());
	}

	@Test
	void shouldAddFirstOccurrencePrice() {
		priceIndex.addPrice(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.FIRST_OCCURRENCE, 20, null, 1000, 1210);
		priceIndex.addPrice(1, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.FIRST_OCCURRENCE,  21, null, 999, 2000);
		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.FIRST_OCCURRENCE);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(2, priceRecords.length);

		final PriceRecordInnerRecordSpecific price1 = new PriceRecordInnerRecordSpecific(1, 10, 1, 20, 1210, 1000);
		final PriceRecordInnerRecordSpecific price2 = new PriceRecordInnerRecordSpecific(2, 11, 1, 21, 2000, 999);
		assertEquals(price1, priceRecords[0]);
		assertEquals(price2, priceRecords[1]);

		assertArrayEquals(new PriceRecordContract[] {price1, price2}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(1));

		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{1, 2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());
	}

	@Test
	void shouldAddSumPrice() {
		priceIndex.addPrice(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.SUM, 1, null, 1000, 1210);
		priceIndex.addPrice(1, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.SUM, 2, null, 999, 2000);
		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.SUM);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(2, priceRecords.length);

		final PriceRecordInnerRecordSpecific price1 = new PriceRecordInnerRecordSpecific(1, 10, 1, 20, 1210, 1000);
		final PriceRecordInnerRecordSpecific price2 = new PriceRecordInnerRecordSpecific(2, 11, 1, 21, 2000, 999);
		assertEquals(price1, priceRecords[0]);
		assertEquals(price2, priceRecords[1]);

		assertArrayEquals(new PriceRecordContract[] {price1, price2}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(1));

		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{1, 2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());
	}

	@Test
	void shouldRemoveStandardPrice() {
		shouldAddStandardPrice();

		priceIndex.priceRemove(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, 1, null, 1000, 1210);

		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(1, priceRecords.length);

		final PriceRecordInnerRecordSpecific price = new PriceRecordInnerRecordSpecific(2, 11, 2, 21, 2000, 999);
		assertEquals(price, priceRecords[0]);

		assertArrayEquals(new PriceRecordContract[] {price}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(priceRecords[0].getEntityPrimaryKey()));

		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());

		priceIndex.priceRemove(2, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.NONE, 2, null, 999, 2000);
		assertNull(priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE));
	}

	@Test
	void shouldRemoveFirstOccurrencePrice() {
		shouldAddFirstOccurrencePrice();

		priceIndex.priceRemove(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.FIRST_OCCURRENCE, 1, null, 1000, 1210);

		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.FIRST_OCCURRENCE);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(1, priceRecords.length);

		final PriceRecordInnerRecordSpecific price = new PriceRecordInnerRecordSpecific(2, 11, 2, 21, 2000, 999);
		assertEquals(price, priceRecords[0]);

		assertArrayEquals(new PriceRecordContract[] {price}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(priceRecords[0].getEntityPrimaryKey()));

		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());

		priceIndex.priceRemove(1, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.FIRST_OCCURRENCE, 2, null, 999, 2000);
		assertNull(priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.FIRST_OCCURRENCE));
	}

	@Test
	void shouldRemoveSumPrice() {
		shouldAddSumPrice();

		priceIndex.priceRemove(1, 1, new PriceKey(10, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.SUM, 1, null, 1000, 1210);

		final PriceListAndCurrencyPriceSuperIndex priceAndCurrencyIndex = priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.SUM);
		assertNotNull(priceAndCurrencyIndex);
		assertFalse(priceAndCurrencyIndex.isEmpty());

		final PriceRecordContract[] priceRecords = priceAndCurrencyIndex.getPriceRecords();
		assertEquals(1, priceRecords.length);

		final PriceRecordInnerRecordSpecific price = new PriceRecordInnerRecordSpecific(2, 11, 2, 21, 2000, 999);
		assertEquals(price, priceRecords[0]);

		assertArrayEquals(new PriceRecordContract[] {price}, priceAndCurrencyIndex.getLowestPriceRecordsForEntity(priceRecords[0].getEntityPrimaryKey()));

		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.getIndexedPriceEntityIds().getArray());
		assertArrayEquals(new int[]{2}, priceAndCurrencyIndex.getIndexedRecordIdsValidInFormula(ZonedDateTime.now()).compute().getArray());
		assertArrayEquals(new int[]{1}, priceAndCurrencyIndex.createPriceIndexFormulaWithAllRecords().compute().getArray());

		priceIndex.priceRemove(1, 2, new PriceKey(11, PRICE_LIST, CURRENCY_CZK), PriceInnerRecordHandling.SUM, 2, null, 999, 2000);
		assertNull(priceIndex.getPriceIndex(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.SUM));
	}

	@Test
	void shouldGenerationalTest1() {
		final PriceListAndCurrencyPriceSuperIndex priceIndex = new PriceListAndCurrencyPriceSuperIndex(new PriceIndexKey(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE));
		addPrice(priceIndex, 11, 4, 21, DateTimeRange.between(ZonedDateTime.now().minusMinutes(1132), ZonedDateTime.now().plusMinutes(1132)), 841, 1017);
		addPrice(priceIndex, 4, 7, 1, DateTimeRange.between(ZonedDateTime.now().minusMinutes(2142), ZonedDateTime.now().plusMinutes(2142)), 676, 817);
		addPrice(priceIndex, 11, 9, 10, DateTimeRange.between(ZonedDateTime.now().minusMinutes(2141), ZonedDateTime.now().plusMinutes(2141)), 683, 826);
		addPrice(priceIndex, 1, 19, 1, DateTimeRange.between(ZonedDateTime.now().minusMinutes(6801), ZonedDateTime.now().plusMinutes(6801)), 739, 894);
		addPrice(priceIndex, 8, 28, 13, DateTimeRange.between(ZonedDateTime.now().minusMinutes(2382), ZonedDateTime.now().plusMinutes(2382)), 216, 261);
		addPrice(priceIndex, 0, 32, 9, DateTimeRange.between(ZonedDateTime.now().minusMinutes(469), ZonedDateTime.now().plusMinutes(469)), 35, 42);
		addPrice(priceIndex, 11, 33, 6, DateTimeRange.between(ZonedDateTime.now().minusMinutes(4092), ZonedDateTime.now().plusMinutes(4092)), 963, 1165);
		addPrice(priceIndex, 6, 38, 0, DateTimeRange.between(ZonedDateTime.now().minusMinutes(9076), ZonedDateTime.now().plusMinutes(9076)), 633, 765);
		addPrice(priceIndex, 8, 43, 20, DateTimeRange.between(ZonedDateTime.now().minusMinutes(8841), ZonedDateTime.now().plusMinutes(8841)), 513, 620);
		addPrice(priceIndex, 10, 47, 8, DateTimeRange.between(ZonedDateTime.now().minusMinutes(4377), ZonedDateTime.now().plusMinutes(4377)), 642, 776);
		addPrice(priceIndex, 5, 50, 7, DateTimeRange.between(ZonedDateTime.now().minusMinutes(8792), ZonedDateTime.now().plusMinutes(8792)), 346, 418);
		addPrice(priceIndex, 8, 51, 7, DateTimeRange.between(ZonedDateTime.now().minusMinutes(1046), ZonedDateTime.now().plusMinutes(1046)), 833, 1007);
		addPrice(priceIndex, 0, 53, 5, DateTimeRange.between(ZonedDateTime.now().minusMinutes(5718), ZonedDateTime.now().plusMinutes(5718)), 35, 42);
		addPrice(priceIndex, 8, 55, 2, DateTimeRange.between(ZonedDateTime.now().minusMinutes(8279), ZonedDateTime.now().plusMinutes(8279)), 246, 297);
		addPrice(priceIndex, 4, 59, 6, DateTimeRange.between(ZonedDateTime.now().minusMinutes(138), ZonedDateTime.now().plusMinutes(138)), 988, 1195);
		addPrice(priceIndex, 2, 62, 5, DateTimeRange.between(ZonedDateTime.now().minusMinutes(1556), ZonedDateTime.now().plusMinutes(1556)), 641, 775);
		addPrice(priceIndex, 4, 65, 18, DateTimeRange.between(ZonedDateTime.now().minusMinutes(2616), ZonedDateTime.now().plusMinutes(2616)), 109, 131);
		addPrice(priceIndex, 2, 75, 9, DateTimeRange.between(ZonedDateTime.now().minusMinutes(8838), ZonedDateTime.now().plusMinutes(8838)), 802, 970);
		addPrice(priceIndex, 3, 77, 7, DateTimeRange.between(ZonedDateTime.now().minusMinutes(71), ZonedDateTime.now().plusMinutes(71)), 90, 108);

		assertStateAfterCommit(
			priceIndex,
			pi -> {
				priceIndex.removePrice(0, priceToInternalId.get(32), DateTimeRange.between(ZonedDateTime.now().minusMinutes(469), ZonedDateTime.now().plusMinutes(469)));
				priceIndex.removePrice(11, priceToInternalId.get(33), DateTimeRange.between(ZonedDateTime.now().minusMinutes(4092), ZonedDateTime.now().plusMinutes(4092)));
				priceIndex.removePrice(0, priceToInternalId.get(53), DateTimeRange.between(ZonedDateTime.now().minusMinutes(5718), ZonedDateTime.now().plusMinutes(5718)));
			},
			(original, committed) -> {

			}
		);

	}

	@Disabled("This infinite test performs random operations on trans. list and normal list and verifies consistency")
	@Test
	void generationalProofTest() {
		final Random rnd = new Random(42);
		final int maxPrices = 50;
		final PriceIndexKey key = new PriceIndexKey(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE);
		final PriceRecordWithValidity[] initialArray = new PriceRecordWithValidity[0];
		final AtomicReference<PriceListAndCurrencyPriceSuperIndex> transactionalArray = new AtomicReference<>(new PriceListAndCurrencyPriceSuperIndex(key));
		final AtomicReference<PriceRecordWithValidity[]> nextArrayToCompare = new AtomicReference<>(initialArray);
		final StringBuilder ops = new StringBuilder("final PriceListAndCurrencyPriceSuperIndex priceIndex = new PriceListAndCurrencyPriceSuperIndex(new PriceIndexKey(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE));\n");

		int iteration = 0;
		do {
			assertStateAfterCommit(
				transactionalArray.get(),
				original -> {
					final int operationsInTransaction = rnd.nextInt(10);
					final Set<Integer> addedInThisRound = new HashSet<>();
					final List<PriceRecordWithValidity> addedRecordInThisRound = new ArrayList<>();
					final Set<Integer> removedInThisRound = new HashSet<>();
					for (int i = 0; i < operationsInTransaction; i++) {
						final PriceListAndCurrencyPriceSuperIndex index = transactionalArray.get();
						final int length = index.getPriceRecords().length;
						if (length < maxPrices && rnd.nextBoolean() || length < 10) {
							// insert new item
							int newPriceId;
							do {
								newPriceId = rnd.nextInt(maxPrices * 2);
							} while (addedInThisRound.contains(newPriceId) || ArrayUtils.binarySearch(nextArrayToCompare.get(), newPriceId, (priceRecordWithValidity, pid) -> Integer.compare(priceRecordWithValidity.getPriceId(), pid)) >= 0);

							final int newEntityId = rnd.nextInt(maxPrices / 4);
							final int newInnerRecordId = rnd.nextInt(maxPrices / 2);
							final int randomPriceWithoutVat = rnd.nextInt(1000);
							final int randomPriceWithVat = (int) (randomPriceWithoutVat * 1.21);
							final int differenceInMinutes = rnd.nextInt(10_000);
							final ZonedDateTime from = ZonedDateTime.now().minusMinutes(differenceInMinutes);
							final DateTimeRange validity = DateTimeRange.between(from, from.plusMinutes(differenceInMinutes));

							final int internalPriceId = priceIdSequence.incrementAndGet();
							final PriceRecordWithValidity priceRecord = new PriceRecordWithValidity(
								internalPriceId, newPriceId, newEntityId, newInnerRecordId,
								randomPriceWithVat, randomPriceWithoutVat, differenceInMinutes, validity
							);

							ops.append("priceIndex.addPrice(")
								.append(newEntityId).append(",")
								.append(newPriceId).append(",")
								.append(newInnerRecordId).append(",")
								.append("DateTimeRange.between(ZonedDateTime.now().minusMinutes(").append(differenceInMinutes).append("), ZonedDateTime.now().plusMinutes(").append(differenceInMinutes).append(")),")
								.append(randomPriceWithoutVat).append(",")
								.append(randomPriceWithVat)
								.append(");\n");

							try {
								index.addPrice(
									new PriceRecordInnerRecordSpecific(internalPriceId, newPriceId, newEntityId, newInnerRecordId, randomPriceWithVat, randomPriceWithoutVat),
									validity
								);
								nextArrayToCompare.set(ArrayUtils.insertRecordIntoOrderedArray(priceRecord, nextArrayToCompare.get(), Comparator.comparingInt(PriceRecordWithValidity::getPriceId)));
								addedInThisRound.add(newPriceId);
								addedRecordInThisRound.add(priceRecord);
								removedInThisRound.remove(newPriceId);
							} catch (Exception ex) {
								fail(ex.getMessage() + "\n" + ops, ex);
							}

						} else {
							// remove existing item
							PriceRecordWithValidity recordToRemove;
							do {
								if (addedInThisRound.isEmpty() || rnd.nextInt(5) == 0) {
									recordToRemove = nextArrayToCompare.get()[rnd.nextInt(nextArrayToCompare.get().length)];
								} else {
									recordToRemove = addedRecordInThisRound.get(rnd.nextInt(addedRecordInThisRound.size()));
								}
							} while (removedInThisRound.contains(recordToRemove.getPriceId()));

							ops.append("priceIndex.removePrice(")
								.append(recordToRemove.getEntityPrimaryKey()).append(",")
								.append(recordToRemove.getPriceId()).append(",")
								.append("DateTimeRange.between(ZonedDateTime.now().minusMinutes(").append(recordToRemove.getDifferenceInMinutes()).append("), ZonedDateTime.now().plusMinutes(").append(recordToRemove.getDifferenceInMinutes()).append("))")
								.append(");\n");

							try {
								index.removePrice(
									recordToRemove.getEntityPrimaryKey(),
									recordToRemove.getInternalPriceId(),
									recordToRemove.getValidity()
								);
								nextArrayToCompare.set(ArrayUtils.removeRecordFromOrderedArray(recordToRemove, nextArrayToCompare.get()));
								if (addedInThisRound.remove(recordToRemove.getPriceId())) {
									addedRecordInThisRound.remove(recordToRemove);
								} else {
									removedInThisRound.add(recordToRemove.getPriceId());
								}
							} catch (Exception ex) {
								fail(ex.getMessage() + "\n" + ops, ex);
							}
						}
					}
				},
				(original, committed) -> {
					final PriceRecordWithValidity[] correctRecords = nextArrayToCompare.get();
					final PriceRecordContract[] priceRecords = buildPriceRecordsFrom(correctRecords);
					final RangeIndex validityIndex = buildValidityIndexFrom(correctRecords);
					assertArrayEquals(
						priceRecords, committed.getPriceRecords(),
						"\nExpected: " + Arrays.toString(priceRecords) + "\n" +
							"Actual:   " + Arrays.toString(committed.getPriceRecords()) + "\n\n" +
							ops
					);

					transactionalArray.set(new PriceListAndCurrencyPriceSuperIndex(key, validityIndex, priceRecords));

					ops.setLength(0);
					ops.append("final PriceListAndCurrencyPriceSuperIndex priceIndex = new PriceListAndCurrencyPriceSuperIndex(new PriceIndexKey(PRICE_LIST, CURRENCY_CZK, PriceInnerRecordHandling.NONE));\n")
						.append(Arrays.stream(correctRecords)
							.map(it ->
								"priceIndex.addPrice(" +
									it.getEntityPrimaryKey() + "," +
									it.getPriceId() + "," +
									it.getInnerRecordId() + "," +
									"DateTimeRange.between(ZonedDateTime.now().minusMinutes(" + it.getDifferenceInMinutes() + "), ZonedDateTime.now().plusMinutes(" + it.getDifferenceInMinutes() + "))," +
									it.getPriceWithoutVat() + "," +
									it.getPriceWithVat() +
									");"
							)
							.collect(Collectors.joining("\n")));
					ops.append("\nOps:\n");
				}
			);
			if (iteration++ % 100 == 0) {
				System.out.print(".");
				System.out.flush();
			}
			if (iteration % 5_000 == 0) {
				System.out.print("\n");
				System.out.flush();
			}
		} while (true);
	}

	private PriceRecordContract[] buildPriceRecordsFrom(PriceRecordWithValidity[] priceRecords) {
		final PriceRecordContract[] result = new PriceRecordContract[priceRecords.length];
		for (int i = 0; i < priceRecords.length; i++) {
			final PriceRecordWithValidity priceRecord = priceRecords[i];
			result[i] = new PriceRecordInnerRecordSpecific(
				priceRecord.getInternalPriceId(),
				priceRecord.getPriceId(),
				priceRecord.getEntityPrimaryKey(),
				priceRecord.getInnerRecordId(),
				priceRecord.getPriceWithVat(),
				priceRecord.getPriceWithoutVat()
			);
		}
		Arrays.sort(result, Comparator.comparingInt(PriceRecordContract::getInternalPriceId));
		return result;
	}

	private RangeIndex buildValidityIndexFrom(PriceRecordWithValidity[] priceRecords) {
		final RangeIndex result = new RangeIndex();
		for (PriceRecordWithValidity priceRecord : priceRecords) {
			result.addRecord(
				priceRecord.getValidity().getFrom(),
				priceRecord.getValidity().getTo(),
				priceRecord.getPriceId()
			);
		}
		return result;
	}

	@Data
	private static class PriceRecordWithValidity implements Comparable<PriceRecordWithValidity> {
		private final int internalPriceId;
		private final int priceId;
		private final int entityPrimaryKey;
		private final int innerRecordId;
		private final int priceWithVat;
		private final int priceWithoutVat;
		private final int differenceInMinutes;
		private final DateTimeRange validity;

		@Override
		public int compareTo(PriceRecordWithValidity o) {
			return Integer.compare(priceId, o.priceId);
		}

	}

}
