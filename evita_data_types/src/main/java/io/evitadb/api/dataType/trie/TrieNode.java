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

package io.evitadb.api.dataType.trie;

import io.evitadb.api.utils.ArrayUtils;
import io.evitadb.api.utils.ArrayUtils.InsertionPosition;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

/**
 * Single node of the trie tree.
 * This node is not optimal for heavy write / delete as it uses copy on write / delete array when adding new value.
 * Also, values are not sorted so that looking for value when it should be removed executes full-scan.
 * This implementation is very effective memory wise.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class TrieNode<T extends Serializable> implements Serializable {
	private static final long serialVersionUID = 4764146105228063621L;

	/**
	 * Starting character to TrieNode/StringBuilder index map.
	 */
	int[] charIndex;
	/**
	 * Contains references to children nodes.
	 */
	TrieNode<T>[] children;
	/**
	 * Contains references to compressed word parts.
	 */
	String[] edgeLabel;
	/**
	 * Contains all values associated with this word.
	 */
	@Getter T[] values;

	TrieNode(int[] charIndex, TrieNode<T>[] children, String[] edgeLabel, T[] values) {
		this.charIndex = charIndex;
		this.children = children;
		this.edgeLabel = edgeLabel;
		this.values = values;
	}

	public TrieNode(@Nonnull IntFunction<T[]> arrayFactory) {
		//noinspection unchecked
		this.children = new TrieNode[0];
		this.charIndex = new int[0];
		this.edgeLabel = new String[0];
		this.values = arrayFactory.apply(0);
	}

	public TrieNode(@Nonnull IntFunction<T[]> arrayFactory, @Nonnull T value) {
		//noinspection unchecked
		this.children = new TrieNode[0];
		this.charIndex = new int[0];
		this.edgeLabel = new String[0];
		this.values = arrayFactory.apply(1);
		this.values[0] = value;
	}

	/**
	 * Returns all gathered edge labels.
	 */
	public String[] getEdgeLabels() {
		return edgeLabel;
	}

	/**
	 * Returns all gathered children.
	 */
	public TrieNode<T>[] getChildren() {
		return children;
	}

	/**
	 * Adds new value to the node.
	 * Enlarges inner array by one.
	 * Doesn't check value duplicities!
	 */
	public final void addValue(@Nonnull IntFunction<T[]> arrayFactory, @Nonnull T addedValue) {
		final T[] newValues = arrayFactory.apply(values.length + 1);
		System.arraycopy(values, 0, newValues, 0, values.length);
		newValues[newValues.length - 1] = addedValue;
		this.values = newValues;
	}

	/**
	 * Adds new value to the node.
	 * Enlarges inner array by one.
	 * Doesn't check value duplicities!
	 */
	public final void addValue(@Nonnull IntFunction<T[]> arrayFactory, @Nonnull T[] addedValue) {
		final T[] newValues = arrayFactory.apply(values.length + addedValue.length);
		System.arraycopy(values, 0, newValues, 0, values.length);
		System.arraycopy(addedValue, 0, newValues, values.length, addedValue.length);
		this.values = newValues;
	}

	/**
	 * Removes possible existing value from the inner value list.
	 * Shrinks array by one if value is found and removed.
	 *
	 * @return true when value was found and removed
	 */
	public boolean removeValue(@Nonnull IntFunction<T[]> arrayFactory, @Nonnull T removedValue) {
		int index = -1;
		for (int i = 0; i < values.length; i++) {
			if (removedValue.equals(values[i])) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			return false;
		} else {
			final T[] newValues = arrayFactory.apply(values.length - 1);
			System.arraycopy(values, 0, newValues, 0, index);
			System.arraycopy(values, index + 1, newValues, index, values.length - index - 1);
			this.values = newValues;
			return true;
		}
	}

	/**
	 * Returns true if there is label for the edge and passed char.
	 */
	public boolean hasEdgeLabel(char character) {
		final InsertionPosition insertionPosition = ArrayUtils.computeInsertPositionOfIntInOrderedArray(character, this.charIndex);
		return insertionPosition.isAlreadyPresent();
	}

	/**
	 * Returns label at the position.
	 */
	@Nonnull
	public String getEdgeLabel(char character) {
		final InsertionPosition position = getCharIndex(character);
		assertCharacterPresent(character, position);
		return this.edgeLabel[position.getPosition()];
	}

	/**
	 * Trims label at the position to specified length.
	 */
	public void setEdgeLabelLength(char character, int length) {
		final InsertionPosition position = getCharIndex(character);
		assertCharacterPresent(character, position);
		final String label = this.edgeLabel[position.getPosition()];
		this.edgeLabel[position.getPosition()] = String.copyValueOf(label.toCharArray(), 0, length);
	}

	/**
	 * Returns children at the position.
	 */
	@Nonnull
	public TrieNode<T> getChildren(char character) {
		final InsertionPosition position = getCharIndex(character);
		assertCharacterPresent(character, position);
		return this.children[position.getPosition()];
	}

	/**
	 * Sets children at the position.
	 */
	public void setChildrenAndEdgeLabel(char character, TrieNode<T> newChild, String edgeLabel) {
		final InsertionPosition childrenIndex = getCharIndex(character);
		if (childrenIndex.isAlreadyPresent()) {
			setChildrenAndEdgeLabelAtTheIndex(character, newChild, edgeLabel, childrenIndex.getPosition());
		} else {
			insertChildrenAndEdgeLabelAtTheIndex(character, newChild, edgeLabel, childrenIndex.getPosition());
		}
	}

	/**
	 * Swaps existing child with new node at the position.
	 */
	public TrieNode<T> swapChildren(char character, TrieNode<T> withChild) {
		final InsertionPosition index = getCharIndex(character);
		assertCharacterPresent(character, index);
		final int position = index.getPosition();
		final TrieNode<T> swapped = this.children[position];
		this.children[position] = withChild;
		return swapped;
	}

	/**
	 * Returns true if the node represents key with associated value.
	 */
	public boolean isEndOfWord() {
		return this.values.length > 0;
	}

	/**
	 * Returns true if all childrens are empty.
	 */
	public boolean hasNoChildren() {
		for (TrieNode<T> child : children) {
			if (child != null) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public int estimateSize(@Nonnull ToIntFunction<Serializable> valueSizeEstimator) {
		return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
			MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.ARRAY_BASE_SIZE + MemoryMeasuringConstants.CHAR_SIZE * charIndex.length +
			MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.ARRAY_BASE_SIZE + MemoryMeasuringConstants.REFERENCE_SIZE * children.length + Arrays.stream(children).mapToInt(it -> it.estimateSize(valueSizeEstimator)).sum() +
			MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.ARRAY_BASE_SIZE + MemoryMeasuringConstants.REFERENCE_SIZE * edgeLabel.length + Arrays.stream(edgeLabel).mapToInt(MemoryMeasuringConstants::computeStringSize).sum() +
			MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.ARRAY_BASE_SIZE + MemoryMeasuringConstants.REFERENCE_SIZE * values.length + Arrays.stream(values).mapToInt(valueSizeEstimator::applyAsInt).sum();
	}

	/**
	 * Finds proper index to lookup for {@link TrieNode}.
	 */
	private InsertionPosition getCharIndex(char character) {
		return ArrayUtils.computeInsertPositionOfIntInOrderedArray(character, this.charIndex);
	}

	/**
	 * Inserts new Children at the specific index.
	 */
	private void insertChildrenAndEdgeLabelAtTheIndex(char character, TrieNode<T> newNode, String edgeLabel, int position) {
		this.charIndex = ArrayUtils.insertIntIntoArrayOnIndex(character, this.charIndex, position);
		this.children = ArrayUtils.insertRecordIntoArray(newNode, this.children, position);
		this.edgeLabel = ArrayUtils.insertRecordIntoArray(edgeLabel, this.edgeLabel, position);
	}

	/**
	 * Overwrites new Children at the specific index.
	 */
	private void setChildrenAndEdgeLabelAtTheIndex(char character, TrieNode<T> newNode, String edgeLabel, int position) {
		this.charIndex[position] = character;
		this.children[position] = newNode;
		this.edgeLabel[position] = edgeLabel;
	}

	/**
	 * These methods are meant to be used only in tests!
	 */
	public Map<String, TrieNode<T>> getChildrenAsMap() {
		final AtomicInteger index = new AtomicInteger();
		return Arrays.stream(this.charIndex)
			.boxed()
			.collect(
				Collectors.toMap(
					Character::toString,
					it -> this.children[index.getAndIncrement()]
				)
			);
	}

	/**
	 * These methods are meant to be used only in tests!
	 */
	public Map<String, String> getEdgeLabelAsMap() {
		final AtomicInteger index = new AtomicInteger();
		return Arrays.stream(this.charIndex)
			.boxed()
			.collect(
				Collectors.toMap(
					Character::toString,
					it -> this.edgeLabel[index.getAndIncrement()]
				)
			);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		TrieNode<?> trieNode = (TrieNode<?>) o;

		if (!Arrays.equals(charIndex, trieNode.charIndex)) return false;
		if (!Arrays.equals(children, trieNode.children)) return false;

		if (edgeLabel.length != trieNode.edgeLabel.length) return false;
		for (int i = 0; i < edgeLabel.length; i++) {
			if (!Objects.equals(edgeLabel[i], trieNode.edgeLabel[i]))
				return false;
		}

		return Arrays.equals(values, trieNode.values);
	}

	@Override
	public int hashCode() {
		int result = Arrays.hashCode(charIndex);
		result = 31 * result + Arrays.hashCode(children);
		result = 31 * result + Arrays.hashCode(edgeLabel);
		result = 31 * result + Arrays.hashCode(values);
		return result;
	}

	@Override
	public String toString() {
		return Arrays.toString(values);
	}

	private void assertCharacterPresent(char character, InsertionPosition position) {
		Assert.isTrue(position.isAlreadyPresent(), "Character " + (character) + " unexpectedly present in the trie.");
	}

}