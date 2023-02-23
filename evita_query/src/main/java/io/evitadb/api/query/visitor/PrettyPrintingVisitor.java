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

import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.query.Constraint;
import io.evitadb.api.query.ConstraintContainer;
import io.evitadb.api.query.ConstraintLeaf;
import io.evitadb.api.query.ConstraintVisitor;

import javax.annotation.Nonnull;
import java.io.Serializable;

import static io.evitadb.api.query.Constraint.ARG_CLOSING;
import static io.evitadb.api.query.Constraint.ARG_OPENING;

/**
 * This visitor can pretty print {@link io.evitadb.api.query.Query} constraints so that the output format is easily
 * readable to humans.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class PrettyPrintingVisitor<T extends Constraint<T>> implements ConstraintVisitor<T> {
	private final StringBuilder result = new StringBuilder();
	private int indent;
	private boolean firstConstraint = true;

	private PrettyPrintingVisitor(int indent) {
		this.indent = indent;
	}

	public static <T extends Constraint<T>> String toString(@Nonnull T constraint, int indent) {
		final PrettyPrintingVisitor<T> visitor = new PrettyPrintingVisitor<>(indent);
		constraint.accept(visitor);
		return visitor.getResult();
	}

	public static <T extends Constraint<T>> String toString(@Nonnull T constraint) {
		final PrettyPrintingVisitor<T> visitor = new PrettyPrintingVisitor<>(3);
		constraint.accept(visitor);
		return visitor.getResult();
	}

	@Override
	public void visit(@Nonnull T constraint) {
		if (firstConstraint) {
			firstConstraint = false;
		} else {
			result.append("\n");
		}
		indent(indent, result);
		result.append(constraint.getName()).append(ARG_OPENING);
		if (constraint instanceof ConstraintContainer<?>) {
			//noinspection unchecked
			printContainer((ConstraintContainer<T>) constraint);
		} else if (constraint instanceof ConstraintLeaf) {
			printLeaf(constraint);
		}
	}

	public String getResult() {
		return result.toString();
	}

	/*
		PRIVATE METHODS
	 */

	private void printContainer(ConstraintContainer<T> constraint) {
		if (constraint.getConstraints().length == 0) {
			//noinspection unchecked
			printLeaf((T) constraint);
			return;
		}

		indent++;
		if (constraint.isApplicable()) {
			final Serializable[] arguments = constraint.getArguments();
			for (final Serializable argument : arguments) {
				result.append("\n");
				indent(indent, result);
				result.append(EvitaDataTypes.formatValue(argument));
				result.append(",");
			}

			final T[] constraints = constraint.getConstraints();
			for (int i = 0; i < constraints.length; i++) {
				final T innerConstraint = constraints[i];
				innerConstraint.accept(this);
				if (i + 1 < constraints.length) {
					result.append(",");
				}
			}
		}
		indent--;
		result.append("\n");
		indent(indent, result);
		result.append(ARG_CLOSING);
	}

	private void printLeaf(T constraint) {
		final Serializable[] arguments = constraint.getArguments();
		for (int i = 0; i < arguments.length; i++) {
			final Serializable argument = arguments[i];
			result.append(EvitaDataTypes.formatValue(argument));
			if (i + 1 < arguments.length) {
				result.append(", ");
			}
		}
		result.append(ARG_CLOSING);
	}

	private static void indent(int repeatCount, StringBuilder stringBuilder) {
		stringBuilder.append("\t".repeat(Math.max(0, repeatCount)));
	}

}