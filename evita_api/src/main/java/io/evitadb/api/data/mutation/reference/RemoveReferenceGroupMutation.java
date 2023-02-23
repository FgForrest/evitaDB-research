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
import io.evitadb.api.data.ReferenceContract.GroupEntityReference;
import io.evitadb.api.data.structure.EntityReference;
import io.evitadb.api.data.structure.Reference;
import io.evitadb.api.exception.InvalidMutationException;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.utils.Assert;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This mutation allows to remove {@link GroupEntityReference} in the {@link Reference}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class RemoveReferenceGroupMutation extends ReferenceMutation<EntityReference> {
	private static final long serialVersionUID = -564814790916765844L;

	public RemoveReferenceGroupMutation(EntityReference referenceKey, EntitySchema entitySchema) {
		super(entitySchema, referenceKey);
	}

	@Nonnull
	@Override
	public ReferenceContract mutateLocal(@Nullable ReferenceContract existingReference) {
		Assert.isTrue(
			existingReference != null && existingReference.exists(),
			() -> new InvalidMutationException("Cannot remove reference " + referenceKey + " - reference doesn't exist!")
		);

		final GroupEntityReference existingReferenceGroup = existingReference.getGroup();
		Assert.isTrue(
			existingReferenceGroup != null && existingReferenceGroup.exists(),
			() -> new InvalidMutationException("Cannot remove reference group - no reference group is set on reference " + referenceKey + "!")
		);
		return new Reference(
			entitySchema,
			existingReference.getVersion() + 1,
			referenceKey,
			new GroupEntityReference(
				existingReferenceGroup.getVersion() + 1,
				existingReferenceGroup.getType(),
				existingReferenceGroup.getPrimaryKey(),
				true
			),
			existingReference.getAttributeValues(),
			existingReference.getAttributeLocales(),
			existingReference.isDropped()
		);
	}

	@Override
	public long getPriority() {
		// we need that this mutation is before insert/remove reference itself
		return PRIORITY_REMOVAL * 2;
	}

	@Override
	public EntityReference getComparableKey() {
		return referenceKey;
	}
}
