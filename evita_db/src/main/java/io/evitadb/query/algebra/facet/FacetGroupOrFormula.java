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
import io.evitadb.api.query.filter.Facet;
import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.bitmap.*;
import io.evitadb.query.algebra.AbstractFormula;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.base.OrFormula;
import lombok.Getter;
import net.openhft.hashing.LongHashFunction;
import org.roaringbitmap.RoaringBitmap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * This formula has almost identical implementation as {@link OrFormula} but it accepts only set of
 * {@link Formula} as a children and allows containing even single child (on the contrary to the {@link OrFormula}).
 * The formula envelopes "facet filtering" part of the formula so that it could be easily located during
 * {@link FacetSummary} computation.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FacetGroupOrFormula extends AbstractFormula implements FacetGroupFormula {
	private static final long CLASS_ID = 2720865649065325701L;

	/**
	 * Contains {@link Facet#getType()} of the facet that is targeted by this formula.
	 */
	@Getter private final Serializable facetType;
	/**
	 * Contains requested facet group id that is shared among all {@link #facetIds} of this formula.
	 */
	@Getter private final Integer facetGroupId;
	/**
	 * Contains array of requested facet ids from {@link Facet#getFacetIds()} filtering constraint.
	 */
	@Getter private final int[] facetIds;
	/**
	 * Contains array of bitmaps that represents the entity primary keys that match {@link #facetIds}.
	 */
	private final Bitmap[] bitmaps;

	public FacetGroupOrFormula(@Nonnull Serializable facetType, @Nullable Integer facetGroupId, @Nonnull int[] facetIds, @Nonnull Bitmap... bitmaps) {
		super();
		this.facetType = facetType;
		this.facetGroupId = facetGroupId;
		this.facetIds = facetIds;
		this.bitmaps = bitmaps;
	}

	@Nonnull
	@Override
	public Formula getCloneWithInnerFormulas(@Nonnull Formula... innerFormulas) {
		Assert.isTrue(innerFormulas.length == 0, "This constraint doesn't allow inner formulas!");
		return this;
	}

	@Override
	public long getOperationCost() {
		return 11;
	}

	@Override
	public FacetGroupFormula getCloneWithFacet(int facetId, @Nonnull Bitmap... entityIds) {
		Assert.isTrue(!ArrayUtils.contains(this.facetIds, facetId), "Formula already contains facet id `" + facetId + "`");
		return new FacetGroupOrFormula(
			facetType,
			facetGroupId,
			ArrayUtils.insertIntIntoArrayOnIndex(facetId, facetIds, facetIds.length),
			ArrayUtils.mergeArrays(bitmaps, entityIds)
		);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder("FACET " + facetType + " OR (" + ofNullable(facetGroupId).map(Object::toString).orElse("-") + " - " + Arrays.toString(facetIds) + "): ");
		for (int i = 0; i < bitmaps.length; i++) {
			final Bitmap bitmap = bitmaps[i];
			sb.append(" ↦ ").append(bitmap.toString());
			if (i + 1 < facetIds.length) {
				sb.append(", ");
			}
		}
		return sb.toString();
	}

	@Nonnull
	@Override
	protected Bitmap computeInternal() {
		if (this.bitmaps.length == 0) {
			return EmptyBitmap.INSTANCE;
		} else if (this.bitmaps.length == 1) {
			return bitmaps[0];
		} else {
			return new BaseBitmap(
				RoaringBitmap.or(
					Arrays.stream(bitmaps)
						.map(RoaringBitmapBackedBitmap::getRoaringBitmap)
						.toArray(RoaringBitmap[]::new)
				)
			);
		}
	}

	@Override
	protected long getEstimatedBaseCost() {
		return ofNullable(this.bitmaps)
			.map(it -> Arrays.stream(it).mapToLong(Bitmap::size).sum())
			.orElseGet(super::getEstimatedBaseCost);
	}

	@Override
	public int getEstimatedCardinality() {
		if (bitmaps == null) {
			return Arrays.stream(this.innerFormulas).mapToInt(Formula::getEstimatedCardinality).sum();
		} else {
			return Arrays.stream(this.bitmaps).mapToInt(Bitmap::size).sum();
		}
	}

	@Override
	protected long includeAdditionalHash(@Nonnull LongHashFunction hashFunction) {
		return hashFunction.hashLongs(
			Stream.of(
					LongStream.of(hashFunction.hashChars(facetType.toString())),
					facetGroupId == null ? LongStream.empty() : LongStream.of(facetGroupId),
					IntStream.of(facetIds).mapToLong(it -> it).sorted(),
					Arrays.stream(bitmaps).mapToLong(it -> ((TransactionalBitmap) it).getId()).sorted()
				)
				.flatMapToLong(it -> it)
				.toArray()
		);
	}

	@Override
	protected long getClassId() {
		return CLASS_ID;
	}

	@Override
	protected long getCostInternal() {
		return ofNullable(this.bitmaps)
			.map(it -> Arrays.stream(it).mapToLong(Bitmap::size).sum())
			.orElseGet(super::getCostInternal);
	}
}
