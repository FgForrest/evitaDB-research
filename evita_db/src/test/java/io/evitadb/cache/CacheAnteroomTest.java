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

package io.evitadb.cache;

import io.evitadb.api.EvitaSession;
import io.evitadb.api.configuration.EvitaConfiguration;
import io.evitadb.cache.payload.FlattenedFormula;
import io.evitadb.query.algebra.CacheableFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.AndFormula;
import io.evitadb.scheduling.Scheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static io.evitadb.cache.CacheEden.COOL_ENOUGH;
import static io.evitadb.cache.FormulaCacheVisitorTest.toConstantFormula;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * This test verifies behaviour of {@link FormulaCacheVisitor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
class CacheAnteroomTest {
	public static final String SOME_ENTITY = "SomeEntity";
	private static final int MAX_RECORD_COUNT = 10;
	private static final Random RANDOM = new Random(52);
	private CacheAnteroom cacheAnteroom;
	private CacheEden cacheEden;
	private CacheableFormula[] inputFormulas;

	public static int[] generateRandomNumbers(int recsToGenerate, int maxRecordId) {
		return IntStream.generate(() -> RANDOM.nextInt(maxRecordId) + 1)
			.distinct()
			.limit(recsToGenerate)
			.sorted()
			.toArray();
	}

	@BeforeEach
	void setUp() {
		cacheEden = new CacheEden(1_000_000, 1, 100L);
		this.cacheAnteroom = new CacheAnteroom(
			MAX_RECORD_COUNT, 100L,
			cacheEden,
			new Scheduler(new EvitaConfiguration()) {
				@Override
				public void execute(@Nonnull Runnable runnable) {
					runnable.run();
				}
			}
		);
		this.inputFormulas = new CacheableFormula[MAX_RECORD_COUNT + 2];
		for (int i = 0; i < MAX_RECORD_COUNT + 2; i++) {
			this.inputFormulas[i] = new AndFormula(
				toConstantFormula(generateRandomNumbers(10, 20)),
				toConstantFormula(generateRandomNumbers(10, 20)),
				toConstantFormula(generateRandomNumbers(10, 20))
			);
		}
	}

	@Test
	void shouldPropagateHotSpotsToCache() {
		final EvitaSession evitaSession = Mockito.mock(EvitaSession.class);

		final Map<Integer, Integer> cacheHits = new HashMap<>();
		for (int i = 0; i < 1000; i++) {
			final int formulaIndex = RANDOM.nextInt(inputFormulas.length);
			final CacheableFormula inputFormula = inputFormulas[formulaIndex];
			final Formula theFormula = FormulaCacheVisitor.analyse(evitaSession, SOME_ENTITY, inputFormula, cacheAnteroom);
			assertEquals(inputFormula.compute(), theFormula.compute());
			if (theFormula instanceof FlattenedFormula) {
				cacheHits.merge(formulaIndex, 1, Integer::sum);
			}
		}

		cacheAnteroom.evaluateAssociatesSynchronously();
		assertEquals(12, cacheEden.getCacheRecordCount());
		final long firstFullCacheSize = cacheEden.getByteSizeUsedByCache();
		assertTrue(firstFullCacheSize > 2200);
		assertTrue(cacheHits.size() > 0);
		cacheHits.clear();

		for (int j = 0; j < COOL_ENOUGH; j++) {
			for (int i = 0; i < 1000; i++) {
				final int formulaIndex = RANDOM.nextInt(inputFormulas.length / 2);
				final CacheableFormula inputFormula = inputFormulas[formulaIndex];
				final Formula theFormula = FormulaCacheVisitor.analyse(evitaSession, SOME_ENTITY, inputFormula, cacheAnteroom);
				assertEquals(inputFormula.compute(), theFormula.compute());
				if (theFormula instanceof FlattenedFormula) {
					cacheHits.merge(formulaIndex, 1, Integer::sum);
				}
			}

			cacheAnteroom.evaluateAssociatesSynchronously();

			assertEquals(12, cacheEden.getCacheRecordCount());
			assertTrue(cacheHits.size() > 0);
			cacheHits.clear();
		}

		cacheAnteroom.evaluateAssociatesSynchronously();
		assertEquals(6, cacheEden.getCacheRecordCount());
		assertTrue(cacheEden.getByteSizeUsedByCache() > 1200);
		assertTrue(cacheEden.getByteSizeUsedByCache() < firstFullCacheSize);
	}
}