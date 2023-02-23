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

package io.evitadb.api.query.visitor;

import io.evitadb.api.query.FilterConstraint;
import io.evitadb.api.query.filter.Between;
import io.evitadb.api.query.filter.IsFalse;
import io.evitadb.api.query.filter.IsTrue;
import io.evitadb.api.query.visitor.FinderVisitor.MoreThanSingleResultException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static io.evitadb.api.query.QueryConstraints.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * This test verifies expected behaviour of {@link FinderVisitor}.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
class FinderVisitorTest {
	private FilterConstraint constraint;

	@BeforeEach
	void setUp() {
		constraint = and(
			eq("a", "b"),
			or(
				isNotNull("def"),
				isTrue("xev"),
				between("c", 1, 78),
				not(
					isTrue("utr")
				)
			)
		);
	}

	@Test
	void shouldNotFindMissingConstraint() {
		assertNull(FinderVisitor.findConstraint(constraint, fc -> fc instanceof IsFalse));
	}

	@Test
	void shouldFindExistingConstraint() {
		assertEquals(between("c", 1, 78), FinderVisitor.findConstraint(constraint, fc -> fc instanceof Between));
	}

	@Test
	void shouldFindExistingConstraintByName() {
		assertEquals(
			between("c", 1, 78),
			FinderVisitor.findConstraint(constraint, fc -> {
				final Serializable[] args = fc.getArguments();
				return args.length >= 1 && "c".equals(args[0]);
			})
		);
	}

	@Test
	void shouldFindMultipleConstraints() {
		assertEquals(2, FinderVisitor.findConstraints(constraint, fc -> fc instanceof IsTrue).size());
	}

	@Test
	void shouldReportExceptionWhenExpectingSingleResultButMultipleFound() {
		assertThrows(MoreThanSingleResultException.class, () -> FinderVisitor.findConstraint(constraint, fc -> fc instanceof IsTrue));
	}

}