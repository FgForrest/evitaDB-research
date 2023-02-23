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

package io.evitadb.api.io.extraResult;

import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.io.EvitaResponseExtraResult;
import io.evitadb.api.query.filter.UserFilter;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This DTO allows returning summary of all facets that match query filter excluding those inside {@link UserFilter}.
 * DTO contains information about facet groups and individual facets in them as well as appropriate statistics for them.
 *
 * Instance of this class is returned in {@link io.evitadb.api.io.EvitaResponseBase#getAdditionalResults(Class)} when
 * {@link io.evitadb.api.query.require.FacetSummary} require constraint is used in the query.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@ThreadSafe
public class FacetSummary implements EvitaResponseExtraResult {
	private static final long serialVersionUID = -5622027322997919409L;
	/**
	 * Contains statistics of facets aggregated into facet groups ({@link Reference#getGroup()}).
	 */
	@Getter(value = AccessLevel.NONE)
	@Setter(value = AccessLevel.NONE)
	@Nonnull
	private final Map<GroupReference, FacetGroupStatistics> facetGroupStatistics;

	public FacetSummary(@Nonnull Collection<FacetGroupStatistics> facetGroupStatistics) {
		this.facetGroupStatistics = facetGroupStatistics.stream().collect(
			Collectors.toMap(
				it -> new GroupReference(it.getFacetType(), it.getGroupId()),
				Function.identity(),
				(facetGroupStatistics1, facetGroupStatistics2) -> {
					throw new IllegalArgumentException("Statistics are expected to be unique!");
				}
			)
		);
	}

	/**
	 * Returns statistics for facet group with passed referenced type.
	 */
	@Nullable
	public FacetGroupStatistics getFacetGroupStatistics(Serializable referencedEntityType) {
		return facetGroupStatistics.get(new GroupReference(referencedEntityType, null));
	}

	/**
	 * Returns statistics for facet group with passed referenced type and primary key of the group.
	 */
	@Nullable
	public FacetGroupStatistics getFacetGroupStatistics(Serializable referencedEntityType, int groupId) {
		return facetGroupStatistics.get(new GroupReference(referencedEntityType, groupId));
	}

	/**
	 * Returns collection of all facet statistics aggregated by their group.
	 */
	@Nonnull
	public Collection<FacetGroupStatistics> getFacetGroupStatistics() {
		return Collections.unmodifiableCollection(facetGroupStatistics.values());
	}

	@Override
	public int hashCode() {
		return Objects.hash(facetGroupStatistics);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		FacetSummary that = (FacetSummary) o;
		return facetGroupStatistics.equals(that.facetGroupStatistics);
	}

	@Override
	public String toString() {
		return "Facet summary:\n" +
			facetGroupStatistics
				.entrySet()
				.stream()
				.sorted(Entry.comparingByKey())
				.map(group ->
					"\t" + group.getKey() + ":\n" +
						group.getValue()
							.getFacetStatistics()
							.stream()
							.sorted()
							.map(facet -> "\t\t[" + (facet.isRequested() ? "X" : " ") + "] " + facet.getFacetId() +
								" (" + facet.getCount() + ")" +
								Optional.ofNullable(facet.getImpact()).map(RequestImpact::toString).map(it -> " " + it).orElse(""))
							.collect(Collectors.joining("\n"))
				)
				.collect(Collectors.joining("\n"));
	}

	/**
	 * This DTO contains information about the impact of adding respective facet into the filtering constraint. This
	 * would lead to expanding or shrinking the result response in certain way, that is described in this DTO.
	 * This implementation contains only the bare difference and the match count.
	 */
	@Data
	public static class RequestImpact implements Serializable {
		private static final long serialVersionUID = 8332603848272953977L;
		/**
		 * Projected number of entities that are added or removed from result if the query is altered by adding this
		 * facet to filtering constraint in comparison to current result.
		 */
		private final int difference;
		/**
		 * Projected number of filtered entities if the query is altered by adding this facet to filtering constraint.
		 */
		private final int matchCount;

		/**
		 * Returns either positive or negative number when the result expands or shrinks.
		 */
		public int getDifference() {
			return difference;
		}

		/**
		 * Selection has sense - TRUE if there is at least one entity still present in the result if the query is
		 * altered by adding this facet to filtering constraint.
		 */
		public boolean hasSense() {
			return matchCount > 0;
		}

		@Override
		public int hashCode() {
			return Objects.hash(getDifference(), getMatchCount());
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (!(o instanceof RequestImpact)) return false;
			RequestImpact that = (RequestImpact) o;
			return getDifference() == that.getDifference() && getMatchCount() == that.getMatchCount();
		}

		@Override
		public String toString() {
			if (difference > 0) {
				return "+" + difference;
			} else if (difference < 0) {
				return String.valueOf(difference);
			} else {
				return "0";
			}
		}

	}

	/**
	 * This DTO contains information about single facet statistics of the entities that are present in the response.
	 */
	@Data
	public static class FacetStatistics implements Comparable<FacetStatistics>, Serializable {
		private static final long serialVersionUID = -575288624429566680L;
		/**
		 * Contains id of the facet. This id relates to {@link Reference#getReferencedEntity()} primary key.
		 */
		private final int facetId;
		/**
		 * Contains TRUE if the facet was part of the query filtering constraints.
		 */
		private final boolean requested;
		/**
		 * Contains number of distinct entities in the response that possess of this reference.
		 */
		private final int count;
		/**
		 * This field is not null only when this facet is not requested - {@link #isRequested()} is FALSE.
		 * Contains projected impact on the current response if this facet is also requested in filtering constraints.
		 */
		@Nullable private final RequestImpact impact;

		public FacetStatistics(int facetId, boolean requested, int count, @Nullable RequestImpact impact) {
			this.facetId = facetId;
			this.requested = requested;
			this.count = count;
			this.impact = impact;
		}

		@Override
		public int compareTo(FacetStatistics o) {
			return Integer.compare(facetId, o.facetId);
		}

		@Override
		public int hashCode() {
			return Objects.hash(facetId, requested, count, impact);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FacetStatistics that = (FacetStatistics) o;
			return facetId == that.facetId &&
				requested == that.requested &&
				count == that.count &&
				Objects.equals(impact, that.impact);
		}
	}

	/**
	 * This DTO contains information about single facet group and statistics of the facets that relates to it.
	 */
	@Data
	public static class FacetGroupStatistics implements Serializable {
		private static final long serialVersionUID = 6527695818988488638L;
		/**
		 * Contains type of the facet. This type relates to {@link Reference#getReferencedEntity()} type.
		 */
		@Nonnull
		private final Serializable facetType;
		/**
		 * Contains id of the facet group. This id relates to {@link Reference#getGroup()} primary key.
		 */
		@Nullable
		private final Integer groupId;
		/**
		 * Contains statistics of individual facets.
		 */
		@Getter(value = AccessLevel.NONE)
		@Setter(value = AccessLevel.NONE)
		@Nonnull
		private final Map<Integer, FacetStatistics> facetStatistics;

		public FacetGroupStatistics(@Nonnull Serializable facetType, @Nullable Integer groupId, @Nonnull Map<Integer, FacetStatistics> facetStatistics) {
			this.facetType = facetType;
			this.groupId = groupId;
			this.facetStatistics = facetStatistics;
		}

		public FacetGroupStatistics(@Nonnull Serializable facetType, @Nullable Integer groupId, @Nonnull Collection<FacetStatistics> facetStatistics) {
			this.facetType = facetType;
			this.groupId = groupId;
			this.facetStatistics = facetStatistics
				.stream()
				.collect(
					Collectors.toMap(
						FacetStatistics::getFacetId,
						Function.identity(),
						(facetStatistics1, facetStatistics2) -> {
							throw new IllegalArgumentException("Statistics are expected to be unique!");
						}
					)
				);
		}

		/**
		 * Returns statistics for facet with passed primary key.
		 */
		@Nullable
		public FacetStatistics getFacetStatistics(int facetId) {
			return facetStatistics.get(facetId);
		}

		/**
		 * Returns collection of all facet statistics in this group.
		 */
		@Nonnull
		public Collection<FacetStatistics> getFacetStatistics() {
			return Collections.unmodifiableCollection(facetStatistics.values());
		}

		@Override
		public int hashCode() {
			return Objects.hash(facetType, groupId, facetStatistics);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			FacetGroupStatistics that = (FacetGroupStatistics) o;
			return facetType.equals(that.facetType) && Objects.equals(groupId, that.groupId) && facetStatistics.equals(that.facetStatistics);
		}
	}

	/**
	 * Internal data structure for referencing nullable groups.
	 */
	@Data
	private static class GroupReference implements Comparable<GroupReference> {
		@Nonnull private final Serializable entityType;
		@Nullable private final Integer groupId;

		@Override
		public int compareTo(GroupReference o) {
			final Serializable thisReferencedEntity = entityType;
			final Serializable thatReferencedEntity = o.entityType;
			final int primaryComparison;
			if (thisReferencedEntity.getClass().equals(o.entityType.getClass()) && this.entityType instanceof Comparable) {
				//noinspection unchecked, rawtypes
				primaryComparison = ((Comparable) thisReferencedEntity).compareTo(thatReferencedEntity);
			} else {
				primaryComparison = thisReferencedEntity.toString().compareTo(thatReferencedEntity.toString());
			}

			if (primaryComparison == 0) {
				if (groupId != null && o.groupId != null) {
					return Integer.compare(groupId, o.groupId);
				} else if (groupId == null) {
					return -1;
				} else {
					return 1;
				}
			} else {
				return primaryComparison;
			}
		}

		@Override
		public String toString() {
			return entityType + (groupId == null ? "" : " " + groupId);
		}

	}

}
