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

package io.evitadb.test.annotation;

import io.evitadb.test.TestConstants;

import java.lang.annotation.*;

/**
 * This annotation allows to set different catalog names for tests. If annotation is not used, catalog name defaults to
 * {@link TestConstants#TEST_CATALOG}.
 *
 * Catalog name can be injected to test by using parameter `String catalogName` in the method arguments.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface CatalogName {

	/**
	 * Catalog name Evita DB should use.
	 * Default is {@link TestConstants#TEST_CATALOG}.
	 * @return
	 */
	String value() default TestConstants.TEST_CATALOG;

}
