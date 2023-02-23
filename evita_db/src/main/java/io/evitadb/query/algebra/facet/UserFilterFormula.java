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

package io.evitadb.query.algebra.facet;

import io.evitadb.api.io.extraResult.FacetSummary;
import io.evitadb.index.bitmap.BaseBitmap;
import io.evitadb.index.bitmap.Bitmap;
import io.evitadb.index.bitmap.EmptyBitmap;
import io.evitadb.index.bitmap.RoaringBitmapBackedBitmap;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.AndFormula;
import net.openhft.hashing.LongHashFunction;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import java.util.Arrays;
import java.util.Iterator;

/**
 * This formula has almost identical implementation as {@link AndFormula} but it accepts only set of
 * {@link Formula} as a children and allows containing even single child (on the contrary to the {@link AndFormula}).
 * The formula envelopes "user-defined" part of the formula tree that might be omitted for some computations and that's
 * the main purpose of this formula definition. Particularly the formula and its internals are omitted during initial
 * phase of the {@link FacetSummary} computation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class UserFilterFormula extends AbstractFormula {
	private static final long CLASS_ID = 6890499931556487481L;

	public UserFilterFormula(Formula... innerFormulas) {
		super(innerFormulas);
	}

	@Nonnull
	@Override
	public Formula getCloneWithInnerFormulas(@Nonnull Formula... innerFormulas) {
		return new UserFilterFormula(innerFormulas);
	}

	@Override
	public long getOperationCost() {
		return 15;
	}

	@Nonnull
	@Override
	protected Bitmap computeInternal() {
		final RoaringBitmap[] bitmaps = getRoaringBitmaps();
		if (bitmaps.length == 0 || Arrays.stream(bitmaps).anyMatch(RoaringBitmap::isEmpty)) {
			return EmptyBitmap.INSTANCE;
		} else if (bitmaps.length == 1) {
			return new BaseBitmap(bitmaps[0]);
		} else {
			final Iterator<RoaringBitmap> spliterator = Arrays.stream(bitmaps).iterator();
			return new BaseBitmap(
				RoaringBitmap.and(
					spliterator,
					Arrays.stream(bitmaps).mapToLong(RoaringBitmap::first).min().orElse(0L),
					Arrays.stream(bitmaps).mapToLong(RoaringBitmap::last).max().orElse(0L) + 1
				)
			);
		}
	}

	@Override
	public int getEstimatedCardinality() {
		return Arrays.stream(this.innerFormulas).mapToInt(Formula::getEstimatedCardinality).min().orElse(0);
	}

	@Override
	public String toString() {
		return "USER FILTER";
	}

	@Override
	protected long includeAdditionalHash(@Nonnull LongHashFunction hashFunction) {
		return CLASS_ID;
	}

	@Override
	protected long getClassId() {
		return CLASS_ID;
	}

	/*
		PRIVATE METHODS
	 */

	private RoaringBitmap[] getRoaringBitmaps() {
		return Arrays.stream(getInnerFormulas())
			.map(Formula::compute)
			.map(RoaringBitmapBackedBitmap::getRoaringBitmap)
			.toArray(RoaringBitmap[]::new);
	}
}