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

package io.evitadb.api.data.mutation.reference;

import io.evitadb.api.data.ReferenceContract;
import io.evitadb.api.data.mutation.attribute.RemoveAttributeMutation;
import io.evitadb.api.data.structure.Entity;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.stream.Stream;

/**
 * This mutation allows to remove {@link Reference} from the {@link Entity}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RemoveReferenceMutation extends ReferenceMutation<EntityReference> {
	private static final long serialVersionUID = 5452632579216311397L;

	public RemoveReferenceMutation(EntityReference referenceKey, EntitySchema entitySchema) {
		super(entitySchema, referenceKey);
	}

	@Override
	public @Nonnull
	Class<ReferenceContract> affects() {
		return ReferenceContract.class;
	}

	@Nonnull
	@Override
	public ReferenceContract mutateLocal(@Nullable ReferenceContract existingReference) {
		Assert.isTrue(
			existingReference != null && existingReference.exists(),
			() -> new InvalidMutationException("Cannot remove reference " + referenceKey + " - reference doesn't exist!")
		);
		return new Reference(
			entitySchema,
			existingReference.getVersion() + 1,
			existingReference.getReferencedEntity(),
			existingReference.getGroup(),
			existingReference.getAttributeValues(),
			existingReference.getAttributeLocales(),
			true
		);
	}

	@Override
	public long getPriority() {
		return PRIORITY_REMOVAL;
	}

	@Override
	public EntityReference getComparableKey() {
		return referenceKey;
	}

	/**
	 * This method is used to unify creation process for removing the reference itself and also all its attributes
	 * for which there is special case of the mutation.
	 */
	public Stream<ReferenceMutation<?>> generateMutations(@Nullable ReferenceContract existingReference) {
		Assert.isTrue(
			existingReference != null && existingReference.exists(),
			() -> new InvalidMutationException("Cannot remove reference " + referenceKey + " - reference doesn't exist!")
		);
		return Stream.concat(
			Stream.of(this),
			existingReference.getAttributeValues()
				.stream()
				.map(x ->
					new ReferenceAttributesUpdateMutation(
						existingReference.getReferencedEntity(),
						entitySchema,
						new RemoveAttributeMutation(x.getKey())
					)
				)
		);
	}

}
