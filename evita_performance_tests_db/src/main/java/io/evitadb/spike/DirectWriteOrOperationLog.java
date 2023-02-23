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

package io.evitadb.spike;

import lombok.Data;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;

/**
 * This spike test answers the question whether there is really big impact to keep changes in the form of operations
 * or mutable state.
 *
 * Creation                                                       Mode  Cnt        Score       Error  Units
 * DirectWriteOrOperationLog.retrieveValueFromDirectlyWrittenMap  thrpt   25  1580800.743 ± 16093.624  ops/s
 * DirectWriteOrOperationLog.retrieveValueFromOperationMap        thrpt   25   295822.689 ±  2330.564  ops/s
 *
 * Retrieving changes
 * DirectWriteOrOperationLog.retrieveValueFromDirectlyWrittenMap  thrpt   25   233163.410 ±  2958.510  ops/s
 * DirectWriteOrOperationLog.retrieveValueFromOperationMap        thrpt   25  1195781.543 ± 23327.547  ops/s
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DirectWriteOrOperationLog {
	private static final Random random = new Random();
	public static final int INITIAL_CAPACITY = 50;

	/**
	 * Common interface for accessing associated values.
	 */
	private interface ImmutableContainerBuilder {

		/**
		 * Adds new key-value.
		 * @param key
		 * @param value
		 */
		void putValue(String key, Integer value);

		/**
		 * Removes key with value.
		 * @param key
		 */
		void removeValue(String key);

		/**
		 * Returns container with all changes applied.
		 * @return
		 */
		Map<String, Integer> getContainer();

		/**
		 * Returns list of operations that when applied on old container produce the result same as {@link #getContainer()}.
		 * @return
		 */
		Collection<WriteOperation> getChanges();

	}

	/**
	 * Implementation that writes changes directly to result map. Original map is held so that the result changelog
	 * can be generated on demand.
	 */
	@Data
	private static class DirectMapWriter implements ImmutableContainerBuilder {
		private final Map<String, Integer> base;
		private final Map<String, Integer> values;

		public DirectMapWriter(Map<String, Integer> baseMap) {
			this.base = baseMap;
			this.values = new LinkedHashMap<>(baseMap);
		}

		@Override
		public void putValue(String key, Integer value) {
			values.put(key, value);
		}

		@Override
		public void removeValue(String key) {
			values.remove(key);
		}

		@Override
		public Map<String, Integer> getContainer() {
			return values;
		}

		@Override
		public Collection<WriteOperation> getChanges() {
			final List<WriteOperation> changes = new LinkedList<>();
			for (Entry<String, Integer> entry : values.entrySet()) {
				final Object original = base.get(entry.getKey());
				if (original == null || !original.equals(entry.getValue())) {
					changes.add(new WriteOperation(entry.getKey(), entry.getValue(), OperationType.SET));
				}
			}
			for (Entry<String, Integer> baseEntry : base.entrySet()) {
				if (!values.containsKey(baseEntry.getKey())) {
					changes.remove(new WriteOperation(baseEntry.getKey(), baseEntry.getValue(), OperationType.REMOVE));
				}
			}
			return changes;
		}
	}

	/**
	 * Implementation that keeps the operations that can be applied on the map to produce result. Inversion implementation
	 * thatn the {@link DirectMapWriter}.
	 */
	@Data
	private static class OperationWriter implements ImmutableContainerBuilder {
		private final Map<String, Integer> baseValues;
		private final Map<String, WriteOperation> operations;

		public OperationWriter(Map<String, Integer> baseValues) {
			this.baseValues = baseValues;
			this.operations = new HashMap<>();
		}

		@Override
		public void putValue(String key, Integer value) {
			operations.put(key, new WriteOperation(key, value, OperationType.SET));
		}

		@Override
		public void removeValue(String key) {
			operations.put(key, new WriteOperation(key, null, OperationType.REMOVE));
		}

		@Override
		public Map<String, Integer> getContainer() {
			final LinkedHashMap<String, Integer> resultMap = new LinkedHashMap<>(baseValues.size() + operations.size());
			resultMap.putAll(baseValues);
			for (Entry<String, WriteOperation> entry : operations.entrySet()) {
				entry.getValue().apply(entry.getKey(), resultMap);
			}
			return resultMap;
		}

		@Override
		public Collection<WriteOperation> getChanges() {
			return operations.values();
		}

	}

	@Data
	public static class WriteOperation {
		private final String key;
		private final Integer value;
		private final OperationType operation;

		public void apply(String key, Map<String, Integer> resultMap) {
			if (operation == OperationType.SET) {
				resultMap.put(key, value);
			} else {
				resultMap.remove(key);
			}
		}
	}

	/**
	 * Type of the operation set or remove.
	 */
	public enum OperationType {

		SET, REMOVE

	}

	/**
	 * Randomizes the initial values in maps and asks builderFactory to create instance of {@link ImmutableContainerBuilder} implementation.
	 * @param builderFactory
	 * @return
	 */
	private static ImmutableContainerBuilder createValueHolder(Function<Map<String, Integer>, ImmutableContainerBuilder> builderFactory) {
		final Map<String, Integer> data = new HashMap<>(INITIAL_CAPACITY);
		for (int i = 0; i < INITIAL_CAPACITY; i++) {
			String key;
			do {
				key = String.valueOf(100 + random.nextInt((int) (INITIAL_CAPACITY * 1.5)));
			} while (data.containsKey(key));

			data.put(key, i);
		}
		return builderFactory.apply(data);
	}

	@State(Scope.Benchmark)
	@Data
	public static class DirectWriteState {

		/**
		 * Value holder implementation reference.
		 */
		private ImmutableContainerBuilder valueHolder;

		/**
		 * This setup is called once for each round.
		 */
		@Setup(Level.Trial)
		public void setUp() {
			valueHolder = createValueHolder(DirectMapWriter::new);
		}

	}
	@State(Scope.Benchmark)
	@Data
	public static class OperationState {

		/**
		 * Value holder implementation reference.
		 */
		private ImmutableContainerBuilder valueHolder;

		/**
		 * This setup is called once for each round.
		 */
		@Setup(Level.Trial)
		public void setUp() {
			valueHolder = createValueHolder(OperationWriter::new);
		}

	}

	@State(Scope.Thread)
	@Data
	public static class NewDataState {
		private Map<String, Integer> newData;

		/**
		 * This is called once per invocation and randomly generates random change set of key and values to be applied
		 * on {@link ImmutableContainerBuilder}.
		 */
		@Setup(Level.Invocation)
		public void setUp() {
			newData = new HashMap<>(INITIAL_CAPACITY);
			for (int i = 0; i < 25; i++) {
				newData.put(String.valueOf(100 + random.nextInt((int) (INITIAL_CAPACITY * 1.25))), i);
			}
		}

	}

	/**
	 * Direct write to hashmap and generate changelog lazily benchmark.
	 * @param plan
	 * @param newData
	 * @param blackhole
	 */
	@Benchmark
	@BenchmarkMode({Mode.Throughput})
	public void retrieveValueFromDirectlyWrittenMap(DirectWriteState plan, NewDataState newData, Blackhole blackhole) {
		final ImmutableContainerBuilder valueHolder = plan.valueHolder;
		for (Entry<String, Integer> entry : newData.getNewData().entrySet()) {
			if (entry.getValue() % 3 == 0) {
				valueHolder.removeValue(entry.getKey());
			} else {
				valueHolder.putValue(entry.getKey(), entry.getValue());
			}
		}
		blackhole.consume(
			valueHolder.getChanges()
		);
	}

	/**
	 * Write set of operations, have changelog ready and produce result map on demand.
	 * @param plan
	 * @param newData
	 * @param blackhole
	 */
	@Benchmark
	@BenchmarkMode({Mode.Throughput})
	public void retrieveValueFromOperationMap(OperationState plan, NewDataState newData, Blackhole blackhole) {
		final ImmutableContainerBuilder valueHolder = plan.valueHolder;
		for (Entry<String, Integer> entry : newData.getNewData().entrySet()) {
			if (entry.getValue() % 3 == 0) {
				valueHolder.removeValue(entry.getKey());
			} else {
				valueHolder.putValue(entry.getKey(), entry.getValue());
			}
		}
		blackhole.consume(
				valueHolder.getChanges()
		);
	}


	public static void main(String[] args) throws Exception {
		org.openjdk.jmh.Main.main(args);
	}

}
