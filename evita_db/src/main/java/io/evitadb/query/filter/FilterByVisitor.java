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

package io.evitadb.query.filter;

import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;
import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.*;
import io.evitadb.api.schema.AttributeSchema;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.ReferenceSchema;
import io.evitadb.api.utils.Assert;
import io.evitadb.index.EntityIndex;
import io.evitadb.index.EntityIndexKey;
import io.evitadb.index.EntityIndexType;
import io.evitadb.index.attribute.FilterIndex;
import io.evitadb.index.attribute.UniqueIndex;
import io.evitadb.query.algebra.Formula;
import io.evitadb.query.algebra.FormulaPostProcessor;
import io.evitadb.query.algebra.base.EmptyFormula;
import io.evitadb.query.algebra.infra.SkipFormula;
import io.evitadb.query.algebra.utils.FormulaFactory;
import io.evitadb.query.common.translator.SelfTraversingTranslator;
import io.evitadb.query.context.QueryContext;
import io.evitadb.query.filter.translator.FilterByTranslator;
import io.evitadb.query.filter.translator.FilteringConstraintTranslator;
import io.evitadb.query.filter.translator.PrimaryKeyTranslator;
import io.evitadb.query.filter.translator.UserFilterTranslator;
import io.evitadb.query.filter.translator.attribute.*;
import io.evitadb.query.filter.translator.bool.AndTranslator;
import io.evitadb.query.filter.translator.bool.NotTranslator;
import io.evitadb.query.filter.translator.bool.OrTranslator;
import io.evitadb.query.filter.translator.facet.FacetTranslator;
import io.evitadb.query.filter.translator.hierarchy.WithinHierarchyTranslator;
import io.evitadb.query.filter.translator.hierarchy.WithinRootHierarchyTranslator;
import io.evitadb.query.filter.translator.price.PriceBetweenTranslator;
import io.evitadb.query.filter.translator.price.PriceInCurrencyTranslator;
import io.evitadb.query.filter.translator.price.PriceInPriceListsTranslator;
import io.evitadb.query.filter.translator.price.PriceValidInTranslator;
import io.evitadb.query.indexSelection.TargetIndexes;
import lombok.Getter;
import lombok.experimental.Delegate;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static io.evitadb.api.utils.Assert.isTrue;
import static io.evitadb.api.utils.Assert.notNull;
import static io.evitadb.api.utils.CollectionUtils.createHashMap;
import static java.util.Optional.ofNullable;

/**
 * This {@link ConstraintVisitor} translates tree of {@link FilterConstraint} to a tree of {@link Formula}.
 * Visitor represents the "planning" phase for the filtering resolution. The planning should be as light-weight as
 * possible.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class FilterByVisitor implements ConstraintVisitor<FilterConstraint> {
	private static final Formula[] EMPTY_INTEGER_FORMULA = new Formula[0];
	private static final Map<Class<? extends FilterConstraint>, FilteringConstraintTranslator<? extends FilterConstraint>> TRANSLATORS;
	private static final String ERROR_SCOPE_COULD_NEVER_BE_NULL = "Scope could never be null!";

	/* initialize list of all FilterableConstraint handlers once for a lifetime */
	static {
		TRANSLATORS = createHashMap(40);
		TRANSLATORS.put(FilterBy.class, new FilterByTranslator());
		TRANSLATORS.put(And.class, new AndTranslator());
		TRANSLATORS.put(Or.class, new OrTranslator());
		TRANSLATORS.put(Not.class, new NotTranslator());
		TRANSLATORS.put(PrimaryKey.class, new PrimaryKeyTranslator());
		TRANSLATORS.put(Equals.class, new EqualsTranslator());
		TRANSLATORS.put(LessThan.class, new LessThanTranslator());
		TRANSLATORS.put(LessThanEquals.class, new LessThanEqualsTranslator());
		TRANSLATORS.put(GreaterThan.class, new GreaterThanTranslator());
		TRANSLATORS.put(GreaterThanEquals.class, new GreaterThanEqualsTranslator());
		TRANSLATORS.put(Between.class, new BetweenTranslator());
		TRANSLATORS.put(InRange.class, new InRangeTranslator());
		TRANSLATORS.put(InSet.class, new InSetTranslator());
		TRANSLATORS.put(IsTrue.class, new IsTrueTranslator());
		TRANSLATORS.put(IsFalse.class, new IsFalseTranslator());
		TRANSLATORS.put(IsNull.class, new IsNullTranslator());
		TRANSLATORS.put(IsNotNull.class, new IsNotNullTranslator());
		TRANSLATORS.put(StartsWith.class, new StartsWithTranslator());
		TRANSLATORS.put(EndsWith.class, new EndsWithTranslator());
		TRANSLATORS.put(Contains.class, new ContainsTranslator());
		TRANSLATORS.put(Language.class, new LanguageTranslator());
		TRANSLATORS.put(ReferenceHavingAttribute.class, new ReferenceHavingAttributeTranslator());
		TRANSLATORS.put(PriceInCurrency.class, new PriceInCurrencyTranslator());
		TRANSLATORS.put(PriceValidIn.class, new PriceValidInTranslator());
		TRANSLATORS.put(PriceInPriceLists.class, new PriceInPriceListsTranslator());
		TRANSLATORS.put(PriceBetween.class, new PriceBetweenTranslator());
		TRANSLATORS.put(WithinHierarchy.class, new WithinHierarchyTranslator());
		TRANSLATORS.put(WithinRootHierarchy.class, new WithinRootHierarchyTranslator());
		TRANSLATORS.put(Facet.class, new FacetTranslator());
		TRANSLATORS.put(UserFilter.class, new UserFilterTranslator());
	}

	/**
	 * Reference to the query context that allows to access entity bodies, indexes, original request and much more.
	 */
	@Delegate @Getter private final QueryContext queryContext;
	/**
	 * Collection contains all alternative {@link TargetIndexes} sets that might already contain precalculated information
	 * related to {@link EntityIndex} that can be used to partially resolve input filter although the target index set
	 * is not used to resolve entire query filter.
	 */
	private final List<TargetIndexes> targetIndexes;
	/**
	 * This instance contains the {@link EntityIndex} set that is used to resolve passed query filter.
	 */
	private final TargetIndexes indexSetToUse;
	/**
	 * Field is set to TRUE when it's already known that filtering constraint contains constraint that uses data from
	 * the {@link #indexSetToUse} - i.e. constraint implementing {@link IndexUsingConstraint}. This situation allows
	 * certain translators to entirely skip themselves because the constraint will be implicitly evaluated by the other
	 * constraints using already limited subset from the {@link #indexSetToUse}.
	 */
	@Getter private final boolean targetIndexQueriedByOtherConstraints;
	/**
	 * Contemporary stack for keeping results resolved for each level of the query.
	 */
	private final Deque<List<Formula>> stack = new LinkedList<>();
	/**
	 * Contemporary stack for keeping results resolved for each level of the query.
	 */
	private final Deque<ProcessingScope> scope = new LinkedList<>();
	/**
	 * Contains list of registered post processors. Formula post processor is used to transform final {@link Formula}
	 * tree constructed in {@link FilterByVisitor} before computing the result. Post processors should analyze created
	 * tree and optimize it to achieve maximal impact of memoization process or limit the scope of processed records
	 * as soon as possible. We may take advantage of transitivity in boolean algebra to exchange formula placement
	 * the way it's most performant.
	 */
	private final List<FormulaPostProcessor> postProcessors = new LinkedList<>();
	/**
	 * Contains the translated formula from the filtering constraint source tree.
	 */
	private Formula computedFormula;

	public FilterByVisitor(
		@Nonnull QueryContext queryContext,
		@Nonnull List<TargetIndexes> targetIndexes,
		@Nonnull TargetIndexes indexSetToUse,
		boolean targetIndexQueriedByOtherConstraints
	) {
		this.queryContext = queryContext;
		this.targetIndexes = targetIndexes;
		this.indexSetToUse = indexSetToUse;
		this.targetIndexQueriedByOtherConstraints = targetIndexQueriedByOtherConstraints;
		this.stack.push(new LinkedList<>());
		this.scope.push(
			new ProcessingScope(
				indexSetToUse.getIndexes(),
				attributeName -> getSchema().getAttribute(attributeName)
			)
		);
	}

	/**
	 * Returns true if passed `groupId` of `entityType` facets are requested to be joined by conjunction (AND) instead
	 * of default disjunction (OR).
	 */
	public boolean isFacetGroupConjunction(@Nonnull Serializable entityType, @Nullable Integer groupId) {
		return groupId != null && queryContext.getEvitaRequest().isFacetGroupConjunction(entityType, groupId);
	}

	/**
	 * Returns true if passed `groupId` of `entityType` is requested to be joined with other facet groups by
	 * disjunction (OR) instead of default conjunction (AND).
	 */
	public boolean isFacetGroupDisjunction(@Nonnull Serializable entityType, @Nullable Integer groupId) {
		return groupId != null && queryContext.getEvitaRequest().isFacetGroupDisjunction(entityType, groupId);
	}

	/**
	 * Returns true if passed `groupId` of `entityType` facets are requested to be joined by negation (AND NOT) instead
	 * of default disjunction (OR).
	 */
	public boolean isFacetGroupNegation(@Nonnull Serializable entityType, @Nullable Integer groupId) {
		return groupId != null && queryContext.getEvitaRequest().isFacetGroupNegation(entityType, groupId);
	}

	/**
	 * Returns the moment when the query is requested.
	 */
	public ZonedDateTime getNow() {
		return queryContext.getEvitaRequest().getAlignedNow();
	}

	/**
	 * Returns true if `referencedType` points to hierarchical entity.
	 */
	public boolean isReferencingHierarchicalEntity(Serializable referencedType) {
		final EntitySchema entitySchema = getSchema();
		final ReferenceSchema referenceSchema = entitySchema.getReference(referencedType);
		notNull(referenceSchema, "Reference of type `" + referencedType + "` is not present in schema of `" + entitySchema.getName() + "` entity.");
		return referenceSchema.isEntityTypeRelatesToEntity() &&
			ofNullable(getSchema(referencedType))
				.map(EntitySchema::isWithHierarchy)
				.orElseThrow(() -> new IllegalArgumentException("Referenced schema for entity " + referencedType + " was not found!"));
	}

	/**
	 * Returns {@link EntityIndex} that contains indexed entities that reference `referencedType` and `referencedEntityId`.
	 * Argument `referencesHierarchicalEntity` should be evaluated first by {@link #isReferencingHierarchicalEntity(Serializable)} method.
	 */
	@Nullable
	public EntityIndex getReferencedEntityIndex(Serializable referencedType, boolean referencesHierarchicalEntity, int referencedEntityId) {
		if (referencesHierarchicalEntity) {
			return queryContext.getEntityIndex(
				new EntityIndexKey(
					EntityIndexType.REFERENCED_HIERARCHY_NODE,
					new EntityReference(referencedType, referencedEntityId)
				)
			);
		} else {
			return queryContext.getEntityIndex(
				new EntityIndexKey(
					EntityIndexType.REFERENCED_ENTITY,
					new EntityReference(referencedType, referencedEntityId)
				)
			);
		}
	}

	/**
	 * Returns the computed formula that represents the filter constraint visited by this implementation.
	 */
	@Nonnull
	public Formula getFormula() {
		return ofNullable(this.computedFormula)
			.orElseGet(this::getSuperSetFormula);
	}

	/**
	 * Returns super set formula - i.e. formula that contains all primary keys.
	 */
	@Nonnull
	public Formula getSuperSetFormula() {
		return applyOnIndexes(EntityIndex::getAllPrimaryKeysFormula);
	}

	/**
	 * Method is expected to be used in {@link FilteringConstraintTranslator} to get collection of formulas
	 * in the current "level" of the filtering constraint query.
	 */
	@Nonnull
	public Formula[] getCollectedIntegerFormulasOnCurrentLevel() {
		return ofNullable(stack.peek())
			.map(it -> it.toArray(Formula[]::new))
			.orElse(EMPTY_INTEGER_FORMULA);
	}

	/**
	 * Method resets formulas that have been collected so far on this level of constraint tree processing.
	 */
	public void resetCollectedFormulasOnCurrentLevel() {
		ofNullable(stack.peek())
			.ifPresent(List::clear);
	}

	/**
	 * Returns stream of indexes that should be all considered for record lookup.
	 */
	@Nonnull
	public Stream<EntityIndex> getEntityIndexStream() {
		return scope.isEmpty() ? Stream.empty() : scope.peek().getIndexStream();
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
			isTrue(attributeDefinition.isFilterable() || attributeDefinition.isUnique(), "Attribute `" + attributeName + "` is not marked as filterable or unique in entity `" + getSchema().getName() + "`!");
			return attributeDefinition;
		}
	}

	/**
	 * Initializes new set of target {@link ProcessingScope} to be used in the visitor.
	 */
	@SafeVarargs
	public final <T> T executeInContext(@Nonnull List<EntityIndex> targetIndexes, @Nonnull Function<String, AttributeSchema> attributeSchemaAccessor, @Nonnull Supplier<T> lambda, Class<? extends FilterConstraint>... suppressedConstraints) {
		try {
			this.scope.push(new ProcessingScope(targetIndexes, attributeSchemaAccessor, suppressedConstraints));
			return lambda.get();
		} finally {
			this.scope.pop();
		}
	}

	/**
	 * Method executes the logic on the current entity set.
	 */
	@Nonnull
	public Formula applyOnIndexes(@Nonnull Function<EntityIndex, Formula> formulaFunction) {
		return joinFormulas(getEntityIndexStream().map(formulaFunction));
	}

	/**
	 * Method executes the logic on the current entity set.
	 */
	public Formula applyStreamOnIndexes(Function<EntityIndex, Stream<Formula>> formulaFunction) {
		return joinFormulas(getEntityIndexStream().flatMap(formulaFunction));
	}

	/**
	 * Method executes the logic on unique index of certain attribute.
	 */
	@Nonnull
	public Formula applyOnUniqueIndexes(@Nonnull AttributeSchema attributeDefinition, @Nonnull Function<UniqueIndex, Formula> formulaFunction) {
		return applyOnIndexes(entityIndex -> {
			final UniqueIndex uniqueIndex = entityIndex.getUniqueIndex(attributeDefinition.getName(), queryContext.getLanguage());
			if (uniqueIndex == null) {
				return EmptyFormula.INSTANCE;
			}
			return formulaFunction.apply(uniqueIndex);
		});
	}

	/**
	 * Method executes the logic on unique index of certain attribute.
	 */
	@Nonnull
	public Formula applyStreamOnUniqueIndexes(@Nonnull AttributeSchema attributeDefinition, @Nonnull Function<UniqueIndex, Stream<Formula>> formulaFunction) {
		return applyStreamOnIndexes(entityIndex -> {
			final UniqueIndex uniqueIndex = entityIndex.getUniqueIndex(attributeDefinition.getName(), queryContext.getLanguage());
			if (uniqueIndex == null) {
				return Stream.empty();
			}
			return formulaFunction.apply(uniqueIndex);
		});
	}

	/**
	 * Method executes the logic on filter index of certain attribute.
	 */
	@Nonnull
	public Formula applyOnFilterIndexes(@Nonnull AttributeSchema attributeDefinition, @Nonnull Function<FilterIndex, Formula> formulaFunction) {
		return applyOnIndexes(entityIndex -> {
			final FilterIndex filterIndex = entityIndex.getFilterIndex(attributeDefinition.getName(), queryContext.getLanguage());
			if (filterIndex == null) {
				return EmptyFormula.INSTANCE;
			}
			return formulaFunction.apply(filterIndex);
		});
	}

	/**
	 * Method executes the logic on filter index of certain attribute.
	 */
	@Nonnull
	public Formula applyStreamOnFilterIndexes(@Nonnull AttributeSchema attributeDefinition, @Nonnull Function<FilterIndex, Stream<Formula>> formulaFunction) {
		return applyStreamOnIndexes(entityIndex -> {
			final FilterIndex filterIndex = entityIndex.getFilterIndex(attributeDefinition.getName(), queryContext.getLanguage());
			if (filterIndex == null) {
				return Stream.empty();
			}
			return formulaFunction.apply(filterIndex);
		});
	}

	@SuppressWarnings("unchecked")
	@Override
	public void visit(@Nonnull FilterConstraint constraint) {
		final FilteringConstraintTranslator<FilterConstraint> translator =
			(FilteringConstraintTranslator<FilterConstraint>) TRANSLATORS.get(constraint.getClass());
		isTrue(
			translator != null,
			() -> new IllegalStateException("No translator found for constraint `" + constraint.getClass() + "`!")
		);

		final ProcessingScope theScope = this.scope.peek();
		Assert.isTrue(theScope != null, () -> new IllegalStateException(ERROR_SCOPE_COULD_NEVER_BE_NULL));
		if (theScope.isSuppressed(constraint.getClass())) {
			return;
		}

		try {
			theScope.pushConstraint(constraint);
			final Formula constraintFormula;
			// if constraint is a container constraint
			if (constraint instanceof ConstraintContainer) {
				@SuppressWarnings("unchecked") final ConstraintContainer<FilterConstraint> container = (ConstraintContainer<FilterConstraint>) constraint;
				// initialize new level of the query
				stack.push(new LinkedList<>());
				if (!(translator instanceof SelfTraversingTranslator)) {
					// process children constraints
					for (FilterConstraint subConstraint : container) {
						subConstraint.accept(this);
					}
				}
				// process the container constraint itself
				constraintFormula = translator.translate(constraint, this);
				// close the level
				stack.pop();
			} else if (constraint instanceof ConstraintLeaf) {
				// process the leaf constraint
				constraintFormula = translator.translate(constraint, this);
			} else {
				// sanity check only
				throw new IllegalStateException("Should never happen");
			}

			// if current constraint is FilterBy we know we're at the top of the filtering constraint tree
			if (constraint instanceof FilterBy) {
				// so we can assign the result of the visitor
				this.computedFormula = constructFinalFormula(constraintFormula);
			} else if (!(constraintFormula instanceof SkipFormula)) {
				// we add the formula to the current level in the query stack
				addFormula(constraintFormula);
			}
		} finally {
			theScope.popConstraint();
		}
	}

	/**
	 * Method returns true if parent of the currently examined constraint matches passed type.
	 */
	public boolean isParentConstraint(Class<? extends FilterConstraint> constraintType) {
		final ProcessingScope theScope = this.scope.peek();
		isTrue(theScope != null, () -> new IllegalStateException(ERROR_SCOPE_COULD_NEVER_BE_NULL));
		return ofNullable(theScope.getParentConstraint())
			.map(constraintType::isInstance)
			.orElse(false);
	}

	/**
	 * Method returns true if any of the siblings of the currently examined constraint matches any of the passed types.
	 */
	@SuppressWarnings("unchecked")
	public boolean isAnySiblingConstraintPresent(Class<? extends FilterConstraint>... constraintType) {
		final ProcessingScope theScope = this.scope.peek();
		isTrue(theScope != null, () -> new IllegalStateException(ERROR_SCOPE_COULD_NEVER_BE_NULL));
		final FilterConstraint parentConstraint = theScope.getParentConstraint();
		if (parentConstraint instanceof ConstraintContainer) {
			//noinspection unchecked
			for (FilterConstraint examinedType : (ConstraintContainer<FilterConstraint>) parentConstraint) {
				for (Class<? extends FilterConstraint> lookedUpType : constraintType) {
					if (lookedUpType.isInstance(examinedType)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/**
	 * Method will apply `lambda` in a parent scope of currently processed {@link FilterConstraint} that shares same
	 * conjunction relation. For example in this formula:
	 *
	 *
	 * <pre>
	 * AND
	 *    USER_FILTER
	 *       LESS_THAN()
	 *       IN_CURRENCY
	 *    OR
	 *       EQ()
	 *       GREATER_THAN()
	 * </pre>
	 *
	 * Conjunction block for `IN_CURRENCY` is: LESS_THAN, USER_FILTER, AND, OR
	 * Conjunction block for `USER_FILTER` is: AND,OR
	 * Conjunction block for `EQ` is none.
	 */
	@Nullable
	public <T extends FilterConstraint> T findInConjunctionTree(Class<T> constraintType) {
		final ProcessingScope theScope = this.scope.peek();
		final List<T> foundConstraints = new LinkedList<>();
		isTrue(theScope != null, () -> new IllegalStateException(ERROR_SCOPE_COULD_NEVER_BE_NULL));
		theScope.doInConjunctionBlock(theConstraint -> {
			if (constraintType.isInstance(theConstraint)) {
				//noinspection unchecked
				foundConstraints.add((T) theConstraint);
			}
		});
		if (foundConstraints.isEmpty()) {
			return null;
		} else {
			return foundConstraints.get(0);
		}
	}

	/**
	 * Method returns TRUE if {@link #indexSetToUse} fully represents the passed filtering constraint (i.e. disjunction
	 * of all {@link EntityIndex#getAllPrimaryKeys()} would produce the correct result for passed constraint).
	 */
	public boolean isTargetIndexRepresentingConstraint(FilterConstraint filterConstraint) {
		return indexSetToUse.getRepresentedConstraint() == filterConstraint;
	}

	/**
	 * Method returns variant of {@link TargetIndexes} that fully represents the passed filtering constraint (i.e. disjunction
	 * of all {@link EntityIndex#getAllPrimaryKeys()} would produce the correct result for passed constraint).
	 */
	@Nullable
	public TargetIndexes findTargetIndexSet(FilterConstraint filterConstraint) {
		return targetIndexes
			.stream()
			.filter(it -> it.getRepresentedConstraint() == filterConstraint)
			.findFirst()
			.orElse(null);
	}

	/**
	 * Registers new {@link FormulaPostProcessor} to the list of processors that will be called just before
	 * FilterByVisitor hands the result of its work to the calling logic.
	 */
	public void registerFormulaPostProcessorIfNotPresent(@Nonnull FormulaPostProcessor formulaPostProcessor) {
		this.postProcessors.add(formulaPostProcessor);
	}

	/**
	 * Finds registered post processor of passed type (if any).
	 */
	@Nullable
	public <T extends FormulaPostProcessor> T findPostProcessor(@Nonnull Class<T> postProcessorType) {
		//noinspection unchecked
		return (T) this.postProcessors.stream().filter(postProcessorType::isInstance).findFirst().orElse(null);
	}

	/**
	 * Processes the result formula tree by applying all registered {@link #postProcessors}. When multiple identical
	 * (those with same {@link FormulaPostProcessor#equals(Object)}) post processors are registered only first is
	 * executed.
	 */
	@Nonnull
	private Formula constructFinalFormula(@Nonnull Formula constraintFormula) {
		Formula finalFormula = constraintFormula;
		if (!postProcessors.isEmpty()) {
			final Set<FormulaPostProcessor> executedProcessors = new HashSet<>();
			for (FormulaPostProcessor postProcessor : postProcessors) {
				if (!executedProcessors.contains(postProcessor)) {
					postProcessor.visit(finalFormula);
					finalFormula = postProcessor.getPostProcessedFormula();
					executedProcessors.add(postProcessor);
				}
			}
		}
		return finalFormula;
	}

	/*
		PRIVATE METHODS
	 */

	private void addFormula(@Nonnull Formula formula) {
		final List<Formula> peekFormulas = stack.peek();
		isTrue(peekFormulas != null, () -> new IllegalStateException("Top formulas unexpectedly empty!"));
		peekFormulas.add(formula);
	}

	@Nonnull
	private Formula joinFormulas(@Nonnull Stream<Formula> formulaStream) {
		final Formula[] formulas = formulaStream
			.filter(it -> !(it instanceof EmptyFormula))
			.toArray(Formula[]::new);
		switch (formulas.length) {
			case 0:
				return EmptyFormula.INSTANCE;
			case 1:
				return formulas[0];
			default:
				return FormulaFactory.or(this::getSuperSetFormula, formulas);
		}
	}

	/**
	 * Processing scope contains contextual information that could be overridden in {@link FilteringConstraintTranslator}
	 * implementations to exchange indexes that are being used, suppressing certain constraint evaluation or accessing
	 * attribute schema information.
	 */
	private static class ProcessingScope {
		/**
		 * Contains set of indexes, that should be used for accessing final indexes.
		 */
		private final List<EntityIndex> entityIndexes;
		/**
		 * Suppressed constraints contains set of {@link FilterConstraint} that will not be evaluated by this visitor
		 * in current scope.
		 */
		private final Set<Class<? extends FilterConstraint>> suppressedConstraints;
		/**
		 * Function provides access to the attribute schema in {@link EntitySchema}
		 */
		private final Function<String, AttributeSchema> attributeSchemaAccessor;
		/**
		 * This stack contains parent chain of the current constraint.
		 */
		private final Deque<FilterConstraint> processedConstraints = new LinkedList<>();

		@SafeVarargs
		private ProcessingScope(List<EntityIndex> targetIndexes, Function<String, AttributeSchema> attributeSchemaAccessor, Class<? extends FilterConstraint>... suppressedConstraints) {
			this.entityIndexes = targetIndexes;
			this.attributeSchemaAccessor = attributeSchemaAccessor;
			if (suppressedConstraints.length > 0) {
				this.suppressedConstraints = new HashSet<>(suppressedConstraints.length);
				this.suppressedConstraints.addAll(Arrays.asList(suppressedConstraints));
			} else {
				this.suppressedConstraints = Collections.emptySet();
			}
		}

		/**
		 * Returns stream of indexes that should be used for searching.
		 */
		public Stream<EntityIndex> getIndexStream() {
			return entityIndexes.stream();
		}

		/**
		 * Returns true if passed constraint should be ignored.
		 */
		public boolean isSuppressed(Class<? extends FilterConstraint> constraint) {
			return this.suppressedConstraints.contains(constraint);
		}

		/**
		 * Adds currently processed constraint to the parent chain.
		 * Remember to call {@link #popConstraint()} at the end of processing this constraint.
		 */
		public void pushConstraint(@Nonnull FilterConstraint parent) {
			this.processedConstraints.push(parent);
		}

		/**
		 * Removes constraint from the list of currently processed constraints.
		 */
		public void popConstraint() {
			this.processedConstraints.pop();
		}

		/**
		 * Returns parent of currently processed constraint.
		 */
		@Nullable
		public FilterConstraint getParentConstraint() {
			if (this.processedConstraints.size() > 1) {
				final Iterator<FilterConstraint> it = this.processedConstraints.iterator();
				it.next();
				return it.next();
			} else {
				return null;
			}
		}

		/**
		 * Method will apply `lambda` in a parent scope of currently processed {@link FilterConstraint} that shares same
		 * conjunction relation.
		 *
		 * @see FilterByVisitor#findInConjunctionTree(Class)
		 */
		public void doInConjunctionBlock(@Nonnull Consumer<FilterConstraint> lambda) {
			final Predicate<FilterConstraint> isConjunction = fc -> fc instanceof And || fc instanceof UserFilter;
			final Iterator<FilterConstraint> it = this.processedConstraints.iterator();
			if (it.hasNext()) {
				// this will get rid of "this" constraint and first examines its parent
				it.next();
				while (it.hasNext()) {
					final FilterConstraint parentConstraint = it.next();
					if (isConjunction.test(parentConstraint)) {
						//noinspection unchecked
						examineChildren(lambda, isConjunction, (ConstraintContainer<FilterConstraint>) parentConstraint);
					} else {
						break;
					}
				}
			}
		}

		private void examineChildren(@Nonnull Consumer<FilterConstraint> lambda, Predicate<FilterConstraint> isConjunction, ConstraintContainer<FilterConstraint> parentConstraint) {
			for (FilterConstraint constraint : parentConstraint.getConstraints()) {
				lambda.accept(constraint);
				if (isConjunction.test(constraint)) {
					//noinspection unchecked
					examineChildren(lambda, isConjunction, (ConstraintContainer<FilterConstraint>) constraint);
				}
			}
		}

		/**
		 * Returns attribute schema for attribute of passed name.
		 */
		public AttributeSchema getAttribute(String attributeName) {
			return attributeSchemaAccessor.apply(attributeName);
		}
	}

}
