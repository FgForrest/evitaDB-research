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

package io.evitadb.query.sort;

import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.OrderConstraint;
import io.evitadb.api.query.order.*;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.CollectionUtils;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.sort.attribute.translator.AscendingTranslator;
import io.evitadb.query.sort.attribute.translator.DescendingTranslator;
import io.evitadb.query.sort.attribute.translator.ReferenceAttributeTranslator;
import io.evitadb.query.sort.price.translator.PriceAscendingTranslator;
import io.evitadb.query.sort.price.translator.PriceDescendingTranslator;
import io.evitadb.query.sort.translator.OrderByTranslator;
import io.evitadb.query.sort.translator.OrderingConstraintTranslator;
import io.evitadb.query.sort.translator.RandomTranslator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;
import static java.util.Optional.ofNullable;

/**
 * This {@link ConstraintVisitor} translates tree of {@link OrderConstraint} to a composition of {@link io.evitadb.query.sort.Sorter}
 * Visitor represents the "planning" phase for the ordering resolution. The planning should be as light-weight as
 * possible.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class OrderByVisitor implements ConstraintVisitor<OrderConstraint> {
	private static final Map<Class<? extends OrderConstraint>, OrderingConstraintTranslator<? extends OrderConstraint>> TRANSLATORS;

	/* initialize list of all FilterableConstraint handlers once for a lifetime */
	static {
		TRANSLATORS = CollectionUtils.createHashMap(8);
		TRANSLATORS.put(OrderBy.class, new OrderByTranslator());
		TRANSLATORS.put(Ascending.class, new AscendingTranslator());
		TRANSLATORS.put(Descending.class, new DescendingTranslator());
		TRANSLATORS.put(ReferenceAttribute.class, new ReferenceAttributeTranslator());
		TRANSLATORS.put(Random.class, new RandomTranslator());
		TRANSLATORS.put(PriceAscending.class, new PriceAscendingTranslator());
		TRANSLATORS.put(PriceDescending.class, new PriceDescendingTranslator());
	}

	/**
	 * Reference to the query context that allows to access entity bodies, indexes, original request and much more.
	 */
	@Delegate private final QueryContext queryContext;
	/**
	 * Contains filtering formula tree that was used to produce results so that computed sub-results can be used for
	 * sorting.
	 */
	@Getter private final Formula filtering;
	/**
	 * Contemporary stack for auxiliary data resolved for each level of the query.
	 */
	private final Deque<ProcessingScope> scope = new LinkedList<>();
	/**
	 * Contains the created sorter from the ordering constraint source tree.
	 */
	private Sorter sorter;

	public OrderByVisitor(@Nonnull QueryContext queryContext, @Nonnull Formula filtering) {
		this.queryContext = queryContext;
		this.filtering = filtering;
		scope.push(
			new ProcessingScope(
				this.queryContext.getEntityIndex(new EntityIndexKey(EntityIndexType.GLOBAL)),
				attributeName -> this.queryContext.getSchema().getAttribute(attributeName)
			)
		);
	}

	/**
	 * Returns the created sorter from the ordering constraint source tree or default {@link NoSorter} instance.
	 */
	@Nonnull
	public Sorter getSorter() {
		return ofNullable(sorter).orElse(NoSorter.INSTANCE);
	}

	/**
	 * Returns last computed sorter. Method is targeted for internal usage by translators and is not expected to be
	 * called from anywhere else.
	 */
	@Nullable
	public Sorter getLastUsedSorter() {
		return sorter;
	}

	/**
	 * Sets different {@link EntityIndex} to be used in scope of lambda.
	 */
	public final <T> T executeInContext(@Nonnull EntityIndexKey entityIndexKey, @Nonnull Function<String, AttributeSchema> attributeSchemaAccessor, @Nonnull Supplier<T> lambda) {
		try {
			this.scope.push(new ProcessingScope(this.queryContext.getEntityIndex(entityIndexKey), attributeSchemaAccessor));
			return lambda.get();
		} finally {
			this.scope.pop();
		}
	}

	/**
	 * Returns attribute definition from current scope.
	 */
	@Nonnull
	public AttributeSchema getAttributeSchema(String attributeName) {
		if (scope.isEmpty()) {
			throw new IllegalStateException("Scope should never be empty");
		} else {
			final ProcessingScope processingScope = scope.peek();
			final AttributeSchema attributeDefinition = processingScope.getAttribute(attributeName);
			notNull(attributeDefinition, "Attribute `" + attributeName + "` is not known for entity `" + getSchema().getName() + "`!");
			isTrue(attributeDefinition.isSortable(), "Attribute `" + attributeName + "` is not marked as sortable in entity `" + getSchema().getName() + "`!");
			return attributeDefinition;
		}
	}

	/**
	 * Returns index which is best suited for supplying {@link io.evitadb.index.attribute.SortIndex}.
	 */
	public EntityIndex getIndexForSort() {
		final ProcessingScope theScope = this.scope.peek();
		isTrue(theScope != null, () -> new IllegalStateException("Scope is unexpectedly empty!"));
		return theScope.getEntityIndex();
	}

	@Override
	public void visit(@Nonnull OrderConstraint constraint) {
		@SuppressWarnings("unchecked") final OrderingConstraintTranslator<OrderConstraint> translator =
			(OrderingConstraintTranslator<OrderConstraint>) TRANSLATORS.get(constraint.getClass());
		isTrue(
			translator != null,
			() -> new IllegalStateException("No translator found for constraint `" + constraint.getClass() + "`!")
		);

		// if constraint is a container constraint
		if (constraint instanceof ConstraintContainer) {
			@SuppressWarnings("unchecked") final ConstraintContainer<OrderConstraint> container = (ConstraintContainer<OrderConstraint>) constraint;
			// process children constraints
			if (!(translator instanceof SelfTraversingTranslator)) {
				for (OrderConstraint subConstraint : container) {
					subConstraint.accept(this);
				}
			}
			// process the container constraint itself
			sorter = translator.apply(constraint, this);
		} else if (constraint instanceof ConstraintLeaf) {
			// process the leaf constraint
			sorter = translator.apply(constraint, this);
		} else {
			// sanity check only
			throw new IllegalStateException("Should never happen");
		}
	}

	/**
	 * Processing scope contains contextual information that could be overridden in {@link OrderingConstraintTranslator}
	 * implementations to exchange indexes that are being used, suppressing certain constraint evaluation or accessing
	 * attribute schema information.
	 */
	@RequiredArgsConstructor
	private static class ProcessingScope {
		/**
		 * Contains index, that should be used for accessing {@link io.evitadb.index.attribute.SortIndex}.
		 */
		private final EntityIndex entityIndex;
		/**
		 * Function provides access to the attribute schema in {@link EntitySchema}
		 */
		private final Function<String, AttributeSchema> attributeSchemaAccessor;

		/**
		 * Returns entity index that should be used for retrieving {@link io.evitadb.index.attribute.SortIndex}.
		 */
		public EntityIndex getEntityIndex() {
			return entityIndex;
		}

		/**
		 * Returns attribute schema for attribute of passed name.
		 */
		public AttributeSchema getAttribute(String attributeName) {
			return attributeSchemaAccessor.apply(attributeName);
		}

	}

}
