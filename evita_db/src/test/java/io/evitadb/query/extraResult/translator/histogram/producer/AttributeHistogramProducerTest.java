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

package io.evitadb.query.extraResult.translator.histogram.producer;

import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.histogram.HistogramBucket;
import io.evitadb.query.algebra.base.ConstantFormula;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

/**
 * This test verifies {@link AttributeHistogramProducer} contract.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2022
 */
@SuppressWarnings({"rawtypes", "unchecked"})
class AttributeHistogramProducerTest {

	@Test
	void shouldReturnSimpleBuckets() {
		final HistogramBucket[] input = {
			new HistogramBucket<>(1, 1),
			new HistogramBucket<>(2, 2),
			new HistogramBucket<>(3, 3)
		};
		final HistogramBucket[] output = AttributeHistogramProducer.getCombinedAndFilteredBucketArray(
			new ConstantFormula(new BaseBitmap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
			new HistogramBucket[][]{
				input
			}
		);
		assertArrayEquals(input, output);
	}

	@Test
	void shouldReturnFilteredSimpleBuckets() {
		final HistogramBucket[] output = AttributeHistogramProducer.getCombinedAndFilteredBucketArray(
			new ConstantFormula(new BaseBitmap(2, 4, 6, 8, 10)),
			new HistogramBucket[][]{
				new HistogramBucket[]{
					new HistogramBucket<>(1, 1, 2, 3),
					new HistogramBucket<>(2, 4, 5, 6),
					new HistogramBucket<>(3, 7, 8, 9)
				}
			}
		);
		assertArrayEquals(
			new HistogramBucket[]{
				new HistogramBucket<>(1, 2),
				new HistogramBucket<>(2, 4, 6),
				new HistogramBucket<>(3, 8)
			},
			output
		);
	}

	@Test
	void shouldReturnCombinedBuckets() {
		final HistogramBucket[] output = AttributeHistogramProducer.getCombinedAndFilteredBucketArray(
			new ConstantFormula(new BaseBitmap(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)),
			new HistogramBucket[][]{
				new HistogramBucket[]{
					new HistogramBucket<>(1, 1, 3),
					new HistogramBucket<>(3, 8, 9)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(1, 1),
					new HistogramBucket<>(2, 6)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(1, 2),
					new HistogramBucket<>(2, 6),
					new HistogramBucket<>(3, 7)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(2, 4, 5)
				}
			}
		);
		assertArrayEquals(
			new HistogramBucket[]{
				new HistogramBucket<>(1, 1, 2, 3),
				new HistogramBucket<>(2, 4, 5, 6),
				new HistogramBucket<>(3, 7, 8, 9)
			},
			output
		);
	}

	@Test
	void shouldReturnFilteredAndCombinedBuckets() {
		final HistogramBucket[] output = AttributeHistogramProducer.getCombinedAndFilteredBucketArray(
			new ConstantFormula(new BaseBitmap(2, 4, 6, 8, 10)),
			new HistogramBucket[][]{
				new HistogramBucket[]{
					new HistogramBucket<>(1, 1, 3),
					new HistogramBucket<>(2, 6)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(1, 1),
					new HistogramBucket<>(3, 8, 9)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(1, 2),
					new HistogramBucket<>(2, 6),
					new HistogramBucket<>(3, 7)
				},
				new HistogramBucket[]{
					new HistogramBucket<>(2, 4, 5)
				}
			}
		);
		assertArrayEquals(
			new HistogramBucket[]{
				new HistogramBucket<>(1, 2),
				new HistogramBucket<>(2, 4, 6),
				new HistogramBucket<>(3, 8)
			},
			output
		);
	}

}