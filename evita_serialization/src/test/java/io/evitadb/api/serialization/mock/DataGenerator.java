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

package io.evitadb.api.serialization.mock;

import io.evitadb.api.dataType.DateTimeRange;
import io.evitadb.api.dataType.Multiple;
import io.evitadb.api.schema.EntitySchema;
import io.evitadb.api.schema.EntitySchemaBuilder;
import io.evitadb.api.schema.EvolutionMode;
import lombok.Data;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Locale;

/**
 * This class prepares mock data for tests.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DataGenerator {

	@Nonnull
	@SuppressWarnings("Convert2MethodRef")
	public static EntitySchema constructSomeSchema(@Nonnull EntitySchemaBuilder schemaBuilder) {
		return schemaBuilder
			/* all is strictly verified but associated data and facets can be added on the fly */
			.verifySchemaButAllow(EvolutionMode.ADDING_ASSOCIATED_DATA, EvolutionMode.ADDING_REFERENCES)
			/* product are not organized in the tree */
			.withHierarchy()
			/* prices are referencing another entity stored in Evita */
			.withPrice()
			/* en + cs localized attributes and associated data are allowed only */
			.withLocale(Locale.ENGLISH, new Locale("cs", "CZ"))
			/* here we define list of attributes with indexes for search / sort */
			.withAttribute("code", String.class, whichIs -> whichIs.unique())
			.withAttribute("url", String.class, whichIs -> whichIs.unique().localized())
			.withAttribute("oldEntityUrls", String[].class, whichIs -> whichIs.filterable().localized())
			.withAttribute("name", String.class, whichIs -> whichIs.filterable().sortable())
			.withAttribute("ean", String.class, whichIs -> whichIs.filterable())
			.withAttribute("priority", Long.class, whichIs -> whichIs.sortable())
			.withAttribute("validity", DateTimeRange.class, whichIs -> whichIs.filterable())
			.withAttribute("quantity", BigDecimal.class, whichIs -> whichIs.filterable().indexDecimalPlaces(2))
			.withAttribute("alias", Boolean.class, whichIs -> whichIs.filterable())
			.withAttribute("multiple", Multiple.class, whichIs -> whichIs.sortable())
			/* here we define set of associated data, that can be stored along with entity */
			.withAssociatedData("referencedFiles", ReferencedFileSet.class)
			.withAssociatedData("labels", Labels.class, whichIs -> whichIs.localized())
			/* here we define facets that relate to another entities stored in Evita */
			.withReferenceToEntity(
				Entities.CATEGORY,
				whichIs ->
					/* we can specify special attributes on relation */
					whichIs.withAttribute("categoryPriority", Long.class, thatIs -> thatIs.sortable())
			)
			/* for indexed facets we can compute "counts" */
			.withReferenceToEntity(Entities.BRAND, whichIs -> whichIs.faceted())
			/* facets may be also represented be entities unknown to Evita */
			.withReferenceTo("stock", whichIs -> whichIs.faceted())
			/* finally apply schema changes */
			.applyChanges();
	}

	@Data
	public static class ReferencedFileSet implements Serializable {
		private static final long serialVersionUID = -1355676966187183143L;
		private String someField = "someValue";

	}

	@Data
	public static class Labels implements Serializable {
		private static final long serialVersionUID = 1121150156843379388L;
		private String someField = "someValue";

	}

}
