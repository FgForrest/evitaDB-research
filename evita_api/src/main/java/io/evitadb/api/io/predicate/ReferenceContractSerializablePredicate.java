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

package io.evitadb.api.io.predicate;

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.structure.SerializablePredicate;
import io.evitadb.api.io.EvitaRequestBase;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * This predicate allows limiting number of references visible to the client based on query constraints.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class ReferenceContractSerializablePredicate implements SerializablePredicate<ReferenceContract> {
	public static final ReferenceContractSerializablePredicate DEFAULT_INSTANCE = new ReferenceContractSerializablePredicate(Collections.emptySet(), true);
	private static final long serialVersionUID = -3182607338600238414L;
	@Getter private final boolean requiresEntityReferences;
	@Nonnull @Getter private final Set<Serializable> referenceSet;

	public ReferenceContractSerializablePredicate() {
		this.requiresEntityReferences = false;
		this.referenceSet = Collections.emptySet();
	}

	public ReferenceContractSerializablePredicate(@Nonnull EvitaRequestBase evitaRequest) {
		this.requiresEntityReferences = evitaRequest.isRequiresEntityReferences();
		this.referenceSet = evitaRequest.getEntityReferenceSet();
	}

	ReferenceContractSerializablePredicate(@Nonnull Set<Serializable> referenceSet, boolean requiresEntityReferences) {
		this.referenceSet = referenceSet;
		this.requiresEntityReferences = requiresEntityReferences;
	}

	@Override
	public boolean test(@Nonnull ReferenceContract reference) {
		if (requiresEntityReferences) {
			final Serializable referencedEntityType = reference.getReferencedEntity().getType();
			return reference.exists() &&
				(referenceSet.isEmpty() || referenceSet.contains(referencedEntityType));
		} else {
			return false;
		}
	}

	public ReferenceContractSerializablePredicate createRicherCopyWith(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Serializable> requiredReferencedEntities = combineReferencedEntities(evitaRequest);
		final boolean doesRequireEntityReferences = evitaRequest.isRequiresEntityReferences();
		if ((this.requiresEntityReferences || this.requiresEntityReferences == doesRequireEntityReferences) &&
			Objects.equals(this.referenceSet, requiredReferencedEntities)) {
			return this;
		} else {
			return new ReferenceContractSerializablePredicate(
				requiredReferencedEntities,
				this.requiresEntityReferences || doesRequireEntityReferences
			);
		}
	}

	@Nonnull
	private Set<Serializable> combineReferencedEntities(@Nonnull EvitaRequestBase evitaRequest) {
		final Set<Serializable> requiredReferences;
		final Set<Serializable> newlyRequiredReferences = evitaRequest.getEntityReferenceSet();
		if (!this.requiresEntityReferences) {
			requiredReferences = newlyRequiredReferences;
		} else if (evitaRequest.isRequiresEntityReferences()) {
			requiredReferences = new HashSet<>(this.referenceSet.size() + newlyRequiredReferences.size());
			requiredReferences.addAll(this.referenceSet);
			requiredReferences.addAll(newlyRequiredReferences);
		} else {
			requiredReferences = this.referenceSet;
		}
		return requiredReferences;
	}
}
