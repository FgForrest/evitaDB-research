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

package io.evitadb.api.data;

import io.evitadb.api.dataType.ComplexDataObject;
import io.evitadb.api.dataType.ComplexDataObject.*;
import io.evitadb.api.dataType.EvitaDataTypes;
import io.evitadb.api.exception.IncompleteDeserializationException;
import io.evitadb.api.exception.InvalidDataObjectException;
import io.evitadb.api.exception.SerializationFailedException;
import io.evitadb.api.function.TriFunction;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.ReflectionLookup;
import io.evitadb.api.utils.ReflectionLookup.ArgumentKey;
import lombok.Getter;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static io.evitadb.api.dataType.ComplexDataObject.*;
import static io.evitadb.api.dataType.EvitaDataTypes.isSupportedType;
import static io.evitadb.api.utils.CollectionUtils.createHashMap;

/**
 * This class converts unknown POJO class data to Evita serialized form. This serialized form can be then converted
 * back again to the object of the original class. Deserialization is tolerant to minor change in the original class
 * definition - such as adding fields. Renaming or removals must be covered with annotations {@link DiscardedData} or
 * {@link RenamedData}. You can also use annotation {@link NonSerializedData} for marking getters that should not
 * be serialized by the automatic process.
 *
 * {@link IncompleteDeserializationException} will occur when there is data value that cannot be stored to the passed
 * POJO class to avoid data loss in case that entity gets read and then written again.
 *
 * Client code should ALWAYS contain test that serializes fully filled POJO and verifies that after deserialization
 * new POJO instance equals to the original one. Deserialization is controlled by the system, but there is no way
 * how to verify that serialization didn't omit some data values.
 *
 * BEWARE only non-null properties are persisted and recovered. Also, NULL values in the lists / arrays are not serialized
 * so you cannot rely on collection indexes when they may allow nulls inside them. All {@link EvitaDataTypes} are supported
 * and also List, Set, Map and array are supported for the sake of (de)serialization. Map key may be any of {@link EvitaDataTypes}
 * but nothing else, value can be arbitrary object.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class DataObjectConverter<T extends Serializable> {
	private final ReflectionLookup reflectionLookup;
	private final T container;
	private final Class<T> containerClass;

	/**
	 * Creates converter instance for (de)serialization of the passed object.
	 */
	public DataObjectConverter(@Nonnull T container) {
		this.reflectionLookup = new ReflectionLookup(ReflectionCachingBehaviour.NO_CACHE);
		this.container = container;
		//noinspection unchecked
		this.containerClass = (Class<T>) container.getClass();
	}

	/**
	 * Creates converter instance and prepares empty container for the sake of deserialization.
	 *
	 * @throws InvalidDataObjectException when containerClass doesn't have default constructor
	 */
	public DataObjectConverter(Class<T> containerClass, ReflectionLookup reflectionLookup) throws InvalidDataObjectException {
		this.reflectionLookup = reflectionLookup;
		this.container = null;
		this.containerClass = containerClass;
	}

	/**
	 * Method guarantees returning one supported {@link EvitaDataTypes}. If {@link #container}
	 * doesn't represent one, it is converted to {@link ComplexDataObject} automatically.
	 */
	public static Serializable getSerializableForm(@Nonnull Serializable container) {
		if (container.getClass().isArray() ? isSupportedType(container.getClass().getComponentType()) : isSupportedType(container.getClass())) {
			return container;
		} else {
			return new DataObjectConverter<>(container).getSerializableForm();
		}
	}

	/**
	 * Method deserializes internal object to original one. If inner object is one of the {@link EvitaDataTypes} it is
	 * returned immediately. If not deserialization from {@link ComplexDataObject} occurs.
	 */
	public static <T extends Serializable> T getOriginalForm(@Nonnull Serializable container, @Nonnull Class<T> requestedClass, @Nonnull ReflectionLookup reflectionLookup) {
		//noinspection unchecked
		return isSupportedType(container.getClass()) ?
			(T) container :
			new DataObjectConverter<>(requestedClass, reflectionLookup).getOriginalForm(container);
	}

	/**
	 * Method guarantees returning one supported {@link EvitaDataTypes}. If {@link #container}
	 * doesn't represent one, it is converted to {@link ComplexDataObject} automatically.
	 */
	public Serializable getSerializableForm() {
		final Class<?> type = container.getClass().isArray() ?
			container.getClass().getComponentType() : container.getClass();
		return isSupportedType(type) ? container : convertToGenericType(container);
	}

	/**
	 * Method deserializes internal object to original one. If inner object is one of the {@link EvitaDataTypes} it is
	 * returned immediately. If not deserialization from {@link ComplexDataObject} occurs.
	 */
	public T getOriginalForm(@Nonnull Serializable serializedForm) {
		Assert.notNull(reflectionLookup, "Reflection lookup required!");
		if (serializedForm instanceof Object[] && isSupportedType(serializedForm.getClass().getComponentType()))
			//noinspection unchecked
			return (T) serializedForm;
		if (isSupportedType(serializedForm.getClass())) {
			//noinspection unchecked
			return (T) serializedForm;
		} else {
			//noinspection ConstantConditions
			final ComplexDataObject complexDataObject = (ComplexDataObject) serializedForm;
			final ExtractionContext extractionCtx = new ExtractionContext(complexDataObject.size());
			final T result;
			// we need to take care only of array - other collection types, such as List, Set, Map cannot be used as
			// top level containers because generics cannot be propagated to the methods due to JVM limitations
			if (containerClass.isArray()) {
				try {
					//noinspection unchecked
					result = (T) deserializeArray(reflectionLookup, containerClass, complexDataObject, "", extractionCtx);
				} catch (IllegalAccessException | InvocationTargetException e) {
					throw new SerializationFailedException(
						"Failed to deserialize root array.", e
					);
				}
			} else {
				// usually there will by single top level POJO object
				result = convertFromGenericType(complexDataObject, containerClass, extractionCtx);
			}
			// check whether all data has been serialized or marked as discarded
			if (complexDataObject.size() != extractionCtx.size()) {
				// if not - raise an error - data loss would occur, this cannot happen unwillingly
				final Set<String> notExtractedValues = extractionCtx.getNotExtractedValues(
					complexDataObject.getPropertyNames()
				);
				// we need to take discarded properties into an account - so error might not occur
				if (!notExtractedValues.isEmpty()) {
					throw new IncompleteDeserializationException(notExtractedValues);
				}
			}
			return result;
		}
	}

	private ComplexDataObject convertToGenericType(T container) {
		final ComplexDataObject result = new ComplexDataObject();
		if (container instanceof Object[]) {
			final Object[] containerArray = (Object[]) container;
			for (int i = 0; i < containerArray.length; i++) {
				final Object containerItem = containerArray[i];
				collectData((Serializable) containerItem, result, reflectionLookup, "[" + i + "].");
			}
		} else {
			collectData(container, result, reflectionLookup, "");
		}
		Assert.isTrue(
			result.size() > 0,
			"No usable properties found on class " + container.getClass() + ". This is probably a problem."
		);
		return result;
	}

	private void collectData(Serializable container, ComplexDataObject result, ReflectionLookup reflectionLookup, String propertyPrefix) {
		final Collection<Method> getters = reflectionLookup.findAllGettersHavingCorrespondingSetterOrConstructorArgument(container.getClass());
		for (Method getter : getters) {
			if (reflectionLookup.getAnnotationInstanceForProperty(getter, NonSerializedData.class) != null) {
				// skip property
				continue;
			}
			collectDataForGetter(container, result, reflectionLookup, propertyPrefix, getter);
		}
	}

	private void collectDataForGetter(Serializable container, ComplexDataObject result, ReflectionLookup reflectionLookup, String propertyPrefix, Method getter) {
		try {
			final Object propertyValue = getter.invoke(container);
			final String propertyName = propertyPrefix + ReflectionLookup.getPropertyNameFromMethodName(getter.getName());
			if (propertyValue != null) {
				if (isSupportedType(propertyValue.getClass())) {
					result.setProperty(propertyName, (Serializable) propertyValue);
				} else if (propertyValue instanceof Set) {
					final Set<?> set = (Set<?>) propertyValue;
					if (set.isEmpty()) {
						result.setProperty(propertyName, EmptyValue.INSTANCE);
					} else {
						serializeSet(result, reflectionLookup, set, propertyName);
					}
				} else if (propertyValue instanceof List) {
					final List<?> list = (List<?>) propertyValue;
					if (list.isEmpty()) {
						result.setProperty(propertyName, EmptyValue.INSTANCE);
					} else {
						serializeList(result, reflectionLookup, list, propertyName);
					}
				} else if (propertyValue instanceof Object[]) {
					final Object[] array = (Object[]) propertyValue;
					if (array.length == 0) {
						result.setProperty(propertyName, EmptyValue.INSTANCE);
					} else {
						serializeArray(result, reflectionLookup, array, propertyName);
					}
				} else if (propertyValue.getClass().isArray()) {
					if (Array.getLength(propertyValue) == 0) {
						result.setProperty(propertyName, EmptyValue.INSTANCE);
					} else {
						serializePrimitiveArray(result, reflectionLookup, propertyValue, propertyName);
					}
				} else if (propertyValue instanceof Map) {
					final Map<?, ?> map = (Map<?, ?>) propertyValue;
					if (map.isEmpty()) {
						result.setProperty(propertyName, EmptyValue.INSTANCE);
					} else {
						serializeMap(result, reflectionLookup, map, propertyName);
					}
				} else if (propertyValue.getClass().getPackageName().startsWith("java.")) {
					throw new SerializationFailedException("Unsupported data type " + propertyValue.getClass() + ": " + propertyValue);
				} else {
					collectData((Serializable) propertyValue, result, reflectionLookup, propertyName + ".");
				}
			} else {
				result.setProperty(propertyName, NullValue.INSTANCE);
			}
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new SerializationFailedException(
				"Failed to retrieve value from getter: " + getter.toGenericString(), e
			);
		}
	}

	private void serializeMap(ComplexDataObject result, ReflectionLookup reflectionLookup, Map<?, ?> propertyValue, String propertyName) {
		for (Entry<?, ?> entry : propertyValue.entrySet()) {
			final Object itemKey = entry.getKey();
			Assert.isTrue(
				itemKey instanceof Serializable,
				() -> new SerializationFailedException("Map key " + itemKey + " in property " + propertyName + " is not serializable!")
			);
			final Integer keyId = result.registerMapKey(DataObjectConverter.getSerializableForm((Serializable) itemKey));

			final Object itemValue = entry.getValue();
			final String itemName = propertyName + INDEX_CHAR_START + MAP_KEY_PREFIX + keyId + INDEX_CHAR_END;
			if (itemValue == null) {
				result.setProperty(itemName, NullValue.INSTANCE);
			} else if (isSupportedType(itemValue.getClass())) {
				result.setProperty(itemName, (Serializable) itemValue);
			} else {
				collectData((Serializable) itemValue, result, reflectionLookup, itemName + ".");
			}
		}
	}

	private void serializeArray(ComplexDataObject result, ReflectionLookup reflectionLookup, Object[] propertyValue, String propertyName) {
		for (int i = 0; i < propertyValue.length; i++) {
			final String itemName = propertyName + INDEX_CHAR_START + i + INDEX_CHAR_END;
			final Object itemValue = propertyValue[i];
			if (itemValue == null) {
				result.setProperty(itemName, NullValue.INSTANCE);
			} else if (isSupportedType(itemValue.getClass())) {
				result.setProperty(itemName, (Serializable) itemValue);
			} else {
				collectData((Serializable) itemValue, result, reflectionLookup, itemName + ".");
			}
		}
	}

	private void serializePrimitiveArray(ComplexDataObject result, ReflectionLookup reflectionLookup, Object propertyValue, String propertyName) {
		for (int i = 0; i < Array.getLength(propertyValue); i++) {
			final String itemName = propertyName + INDEX_CHAR_START + i + INDEX_CHAR_END;
			final Object itemValue = Array.get(propertyValue, i);
			if (itemValue == null) {
				result.setProperty(itemName, NullValue.INSTANCE);
			} else if (isSupportedType(itemValue.getClass())) {
				result.setProperty(itemName, (Serializable) itemValue);
			} else {
				collectData((Serializable) itemValue, result, reflectionLookup, itemName + ".");
			}
		}
	}

	private void serializeList(ComplexDataObject result, ReflectionLookup reflectionLookup, List<?> propertyValue, String propertyName) {
		for (int i = 0; i < propertyValue.size(); i++) {
			final String itemName = propertyName + INDEX_CHAR_START + i + INDEX_CHAR_END;
			final Object itemValue = propertyValue.get(i);
			if (itemValue == null) {
				result.setProperty(itemName, NullValue.INSTANCE);
			} else if (isSupportedType(itemValue.getClass())) {
				result.setProperty(itemName, (Serializable) itemValue);
			} else {
				collectData((Serializable) itemValue, result, reflectionLookup, itemName + ".");
			}
		}
	}

	private void serializeSet(ComplexDataObject result, ReflectionLookup reflectionLookup, Set<?> propertyValue, String propertyName) {
		final Iterator<?> it = propertyValue.iterator();
		int i = 0;
		while (it.hasNext()) {
			final String itemName = propertyName + INDEX_CHAR_START + i++ + INDEX_CHAR_END;
			final Object itemValue = it.next();
			if (itemValue == null) {
				result.setProperty(itemName, NullValue.INSTANCE);
			} else if (isSupportedType(itemValue.getClass())) {
				result.setProperty(itemName, (Serializable) itemValue);
			} else {
				collectData((Serializable) itemValue, result, reflectionLookup, itemName + ".");
			}
		}
	}

	private T convertFromGenericType(ComplexDataObject serializedForm, Class<T> containerClass, ExtractionContext extractionCtx) {
		return extractData(containerClass, serializedForm, reflectionLookup, "", extractionCtx);
	}

	private <X> X extractData(Class<X> containerClass, ComplexDataObject serializedForm, ReflectionLookup reflectionLookup, String propertyPrefix, ExtractionContext extractionCtx) {
		final Collection<Method> setters = reflectionLookup.findAllSetters(containerClass);
		final Set<String> propertyNamesHavingSetters = setters
			.stream()
			.map(it -> ReflectionLookup.getPropertyNameFromMethodName(it.getName()))
			.collect(Collectors.toSet());
		final Set<ArgumentKey> propertyNamesWithoutSetter = serializedForm.getTopPropertyNames(propertyPrefix)
			.stream()
			.filter(it -> !propertyNamesHavingSetters.contains(it))
			.map(it -> new ArgumentKey(it, Object.class))
			.collect(Collectors.toSet());

		final X resultContainer = instantiateContainerByMatchingConstructor(
			containerClass, reflectionLookup, extractionCtx, propertyNamesWithoutSetter,
			(propertyName, requiredType, requiredGenericType) -> extractValueAndRegisterIt(
				serializedForm, reflectionLookup, propertyPrefix,
				extractionCtx, propertyName, requiredType, requiredGenericType
			)
		);

		for (Method setter : setters) {
			if (reflectionLookup.getAnnotationInstanceForProperty(setter, NonSerializedData.class) != null) {
				// skip property
				continue;
			}
			extractDataToSetter((Serializable) resultContainer, serializedForm, reflectionLookup, propertyPrefix, extractionCtx, setter);
		}

		return resultContainer;
	}

	@Nullable
	private Object extractValueAndRegisterIt(ComplexDataObject serializedForm, ReflectionLookup reflectionLookup, String propertyPrefix, ExtractionContext extractionCtx, String propertyName, Class<?> requiredType, Type requiredGenericType) {
		try {
			final String composedPropertyName = propertyPrefix + propertyName;
			final Object extractedValue = extractValue(
				serializedForm, reflectionLookup, composedPropertyName, requiredType, requiredGenericType, extractionCtx
			);
			if (extractedValue instanceof Object[]) {
				for (int i = 0; i < ((Object[]) extractedValue).length; i++) {
					extractionCtx.addProperty(composedPropertyName + INDEX_CHAR_START + i + INDEX_CHAR_END);
				}
			} else if (extractedValue instanceof List) {
				for (int i = 0; i < ((List) extractedValue).size(); i++) {
					extractionCtx.addProperty(composedPropertyName + INDEX_CHAR_START + i + INDEX_CHAR_END);
				}
			} else if (extractedValue instanceof Map) {
				for (Object key : ((Map) extractedValue).keySet()) {
					final int id = serializedForm.getIdForKey((Serializable) key);
					extractionCtx.addProperty(composedPropertyName + INDEX_CHAR_START + MAP_KEY_PREFIX + id + INDEX_CHAR_END);
				}
			} else {
				extractionCtx.addProperty(composedPropertyName);
			}
			return extractedValue;
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new SerializationFailedException(
				"Failed to deserialize value for constructor parameter: " + requiredGenericType.toString() + " " + propertyName, e
			);
		}
	}

	@Nonnull
	private <X> X instantiateContainerByMatchingConstructor(
		Class<X> containerClass, ReflectionLookup reflectionLookup,
		ExtractionContext extractionCtx, Set<ArgumentKey> propertyNamesWithoutSetter,
		TriFunction<String, Class<?>, Type, Object> argumentFetcher
	) {
		final Set<String> discards = identifyPropertyDiscard(containerClass, reflectionLookup, extractionCtx, propertyNamesWithoutSetter);
		Map<String, String> renames = null;
		Constructor<X> appropriateConstructor;
		try {
			appropriateConstructor = reflectionLookup.findConstructor(containerClass, propertyNamesWithoutSetter);
		} catch (IllegalArgumentException ex) {
			// try to find renamed and discarded fields to find alternative constructor
			// we do this in catch block to avoid performance penalty - most of the DTOs will not have any rename / discard annotations
			renames = identifyPropertyReplacement(containerClass, reflectionLookup, propertyNamesWithoutSetter);
			if (renames.isEmpty() && discards.isEmpty()) {
				// fallback to default constructor
				appropriateConstructor = reflectionLookup.findConstructor(containerClass, Collections.emptySet());
			} else {
				appropriateConstructor = instantiateContainerByFallbackConstructorWithRenamedAndDiscardedArguments(
					containerClass, reflectionLookup, propertyNamesWithoutSetter
				);
			}
		}

		final X resultContainer;
		try {
			final Object[] initArgs = getInitArgs(argumentFetcher, renames, appropriateConstructor);
			resultContainer = appropriateConstructor.newInstance(initArgs);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			throw new InvalidDataObjectException(
				"Error invoking constructor " + appropriateConstructor.toGenericString() +
					" on class: " + containerClass
			);
		}
		return resultContainer;
	}

	private <X> Set<String> identifyPropertyDiscard(Class<X> containerClass, ReflectionLookup reflectionLookup, ExtractionContext extractionCtx, Set<ArgumentKey> propertyNamesWithoutSetter) {
		final DiscardedData discardedData = reflectionLookup.getClassAnnotation(containerClass, DiscardedData.class);
		if (discardedData == null) {
			return Collections.emptySet();
		} else {
			final Set<String> discardedProperties = new HashSet<>(discardedData.value().length);
			for (String discardedProperty : discardedData.value()) {
				discardedProperties.add(discardedProperty);
				extractionCtx.addDiscardedProperty(discardedProperty);
				propertyNamesWithoutSetter.remove(new ArgumentKey(discardedProperty, Object.class));
			}
			return discardedProperties;
		}
	}

	@Nonnull
	private <X> Constructor<X> instantiateContainerByFallbackConstructorWithRenamedAndDiscardedArguments(Class<X> containerClass, ReflectionLookup reflectionLookup, Set<ArgumentKey> propertyNamesWithoutSetter) {
		Constructor<X> appropriateConstructor;
		try {
			// try to find constructor with renamed properties instead
			appropriateConstructor = reflectionLookup.findConstructor(
				containerClass,
				propertyNamesWithoutSetter
			);
		} catch (IllegalArgumentException ex) {
			// fallback to default constructor
			appropriateConstructor = reflectionLookup.findConstructor(containerClass, Collections.emptySet());
		}
		return appropriateConstructor;
	}

	@Nonnull
	private <X> Object[] getInitArgs(TriFunction<String, Class<?>, Type, Object> argumentFetcher, Map<String, String> renames, Constructor<X> appropriateConstructor) {
		final Map<String, String> finalRenames = renames;
		return Arrays.stream(appropriateConstructor.getParameters())
			.map(it ->
				argumentFetcher.apply(
					finalRenames == null ? it.getName() : finalRenames.getOrDefault(it.getName(), it.getName()),
					it.getType(), it.getParameterizedType()
				)
			)
			.toArray();
	}

	private <X> Map<String, String> identifyPropertyReplacement(Class<X> containerClass, ReflectionLookup reflectionLookup, Set<ArgumentKey> propertyNamesWithoutSetter) {
		final Map<String, String> renames = createHashMap(propertyNamesWithoutSetter.size());
		final Map<Field, List<RenamedData>> renamedFields = reflectionLookup.getFields(containerClass, RenamedData.class);
		renamedFields
			.entrySet()
			.stream()
			.map(fieldEntry ->
				fieldEntry.getValue()
					.stream()
					.flatMap(renamedAnnotation -> Arrays.stream(renamedAnnotation.value()))
					.filter(renamedAnnotationValue -> propertyNamesWithoutSetter.contains(new ArgumentKey(renamedAnnotationValue, Object.class)))
					.map(renamedAnnotationValue ->
						new ArgumentKeyWithRename(
							fieldEntry.getKey().getName(), fieldEntry.getKey().getType(), renamedAnnotationValue
						)
					)
					.findFirst()
					.orElse(null)
			)
			.filter(Objects::nonNull)
			.forEach(it -> {
				propertyNamesWithoutSetter.remove(new ArgumentKey(it.getOriginalProperty(), it.getType()));
				propertyNamesWithoutSetter.add(it);
				renames.put(it.getName(), it.getOriginalProperty());
			});
		return renames;
	}

	private void extractDataToSetter(Serializable container, ComplexDataObject serializedForm, ReflectionLookup reflectionLookup, String propertyPrefix, ExtractionContext extractionCtx, Method setter) {
		try {
			final String propertyName = propertyPrefix + ReflectionLookup.getPropertyNameFromMethodName(setter.getName());
			final Class<?> propertyType = setter.getParameterTypes()[0];
			final Object propertyValue = extractValue(
				serializedForm,
				reflectionLookup, propertyName, propertyType,
				setter.getGenericParameterTypes()[0],
				extractionCtx
			);
			final RenamedData renamed = reflectionLookup.getAnnotationInstanceForProperty(setter, RenamedData.class);
			if (propertyValue == null && renamed != null) {
				final Object fallbackPropertyValue = extractFallbackData(
					serializedForm, reflectionLookup, setter, propertyType, renamed, extractionCtx
				);
				setter.invoke(container, fallbackPropertyValue);
			} else {
				setter.invoke(container, propertyValue);
				if (propertyValue == null || isSupportedType(propertyType)) {
					extractionCtx.addProperty(propertyName);
				}
			}
		} catch (IllegalAccessException | InvocationTargetException e) {
			throw new SerializationFailedException(
				"Failed to set value via setter: " + setter.toGenericString(), e
			);
		}
	}

	private Object extractFallbackData(ComplexDataObject serializedForm, ReflectionLookup reflectionLookup, Method setter, Class<?> propertyType, RenamedData renamed, ExtractionContext extractionCtx) throws IllegalAccessException, InvocationTargetException {
		for (String aliasPropertyName : renamed.value()) {
			final Object fallbackPropertyValue = extractValue(serializedForm, reflectionLookup, aliasPropertyName, propertyType, setter.getGenericParameterTypes()[0], extractionCtx);
			if (fallbackPropertyValue != null && isSupportedType(propertyType)) {
				extractionCtx.addProperty(aliasPropertyName);
				return fallbackPropertyValue;
			}
		}
		return null;
	}

	private Object extractValue(ComplexDataObject serializedForm, ReflectionLookup reflectionLookup, String propertyName, Class<?> propertyType, Type genericReturnType, ExtractionContext extractionCtx) throws IllegalAccessException, InvocationTargetException {
		if (serializedForm.hasAnyPropertyFor(propertyName)) {
			final Object propertyValue;
			if (isSupportedType(propertyType)) {
				propertyValue = serializedForm.getProperty(propertyName);
			} else if (Set.class.isAssignableFrom(propertyType)) {
				final Serializable entirePropertyValue = serializedForm.getProperty(propertyName);
				if (entirePropertyValue instanceof EmptyValue) {
					return new HashSet<>();
				} else {
					propertyValue = deserializeSet(reflectionLookup, genericReturnType, serializedForm, propertyName, extractionCtx);
				}
			} else if (List.class.isAssignableFrom(propertyType)) {
				final Serializable entirePropertyValue = serializedForm.getProperty(propertyName);
				if (entirePropertyValue instanceof EmptyValue) {
					return new LinkedList<>();
				} else {
					propertyValue = deserializeList(reflectionLookup, genericReturnType, serializedForm, propertyName, extractionCtx);
				}
			} else if (propertyType.isArray()) {
				final Serializable entirePropertyValue = serializedForm.getProperty(propertyName);
				if (entirePropertyValue instanceof EmptyValue) {
					return Array.newInstance(propertyType, 0);
				} else {
					propertyValue = deserializeArray(reflectionLookup, genericReturnType, serializedForm, propertyName, extractionCtx);
				}
			} else if (Map.class.isAssignableFrom(propertyType)) {
				final Serializable entirePropertyValue = serializedForm.getProperty(propertyName);
				if (entirePropertyValue instanceof EmptyValue) {
					return new HashMap<>();
				} else {
					propertyValue = deserializeMap(reflectionLookup, genericReturnType, serializedForm, propertyName, extractionCtx);
				}
			} else {
				final Serializable entirePropertyValue = serializedForm.getProperty(propertyName);
				if (entirePropertyValue instanceof NullValue) {
					return null;
				} else {
					return extractData(propertyType, serializedForm, reflectionLookup, propertyName + ".", extractionCtx);
				}
			}
			return propertyValue;
		} else {
			return null;
		}
	}

	private Set<Serializable> deserializeSet(ReflectionLookup reflectionLookup, Type genericReturnType, ComplexDataObject serializedForm, String propertyName, ExtractionContext extractionCtx) throws InvocationTargetException, IllegalAccessException {
		final Class<? extends Serializable> innerClass = reflectionLookup.extractGenericType(genericReturnType, 0);
		assertSerializable(innerClass);

		final Set<Serializable> result = new LinkedHashSet<>();
		final Set<String> indexes = serializedForm.getIndexStrings(propertyName);
		for (String indexKey : indexes) {
			final Object propertyValue = extractValue(
				serializedForm, reflectionLookup,
				propertyName + INDEX_CHAR_START + indexKey + INDEX_CHAR_END,
				innerClass,
				genericReturnType,
				extractionCtx
			);
			if (propertyValue != null) {
				result.add((Serializable) propertyValue);
			}
		}
		return result.isEmpty() ? null : result;
	}

	private List<Serializable> deserializeList(ReflectionLookup reflectionLookup, Type genericReturnType, ComplexDataObject serializedForm, String propertyName, ExtractionContext extractionCtx) throws InvocationTargetException, IllegalAccessException {
		final Class<? extends Serializable> innerClass = reflectionLookup.extractGenericType(genericReturnType, 0);
		assertSerializable(innerClass);
		return deserializeList(reflectionLookup, genericReturnType, serializedForm, propertyName, innerClass, extractionCtx);
	}

	private Serializable[] deserializeArray(ReflectionLookup reflectionLookup, Type genericReturnType, ComplexDataObject serializedForm, String propertyName, ExtractionContext extractionCtx) throws InvocationTargetException, IllegalAccessException {
		//noinspection unchecked
		final Class<? extends Serializable> innerClass = (Class<? extends Serializable>) ((Class<?>) genericReturnType).getComponentType();
		assertSerializable(innerClass);
		return Optional.ofNullable(deserializeList(reflectionLookup, genericReturnType, serializedForm, propertyName, innerClass, extractionCtx))
			.map(it -> it.toArray((Serializable[]) Array.newInstance(innerClass, 0)))
			.orElse(null);
	}

	private List<Serializable> deserializeList(ReflectionLookup reflectionLookup, Type genericReturnType, ComplexDataObject serializedForm, String propertyName, Class<? extends Serializable> innerClass, ExtractionContext extractionCtx) throws IllegalAccessException, InvocationTargetException {
		final List<Serializable> result = new LinkedList<>();
		final Set<String> indexes = serializedForm.getIndexStrings(propertyName);
		for (String indexKey : indexes) {
			final Object propertyValue = extractValue(
				serializedForm, reflectionLookup,
				propertyName + INDEX_CHAR_START + indexKey + INDEX_CHAR_END,
				innerClass,
				genericReturnType,
				extractionCtx
			);
			if (propertyValue != null) {
				result.add((Serializable) propertyValue);
			}
		}
		return result.isEmpty() ? null : result;
	}

	private Map<Serializable, Serializable> deserializeMap(ReflectionLookup reflectionLookup, Type genericReturnType, ComplexDataObject serializedForm, String propertyName, ExtractionContext extractionCtx) throws InvocationTargetException, IllegalAccessException {
		final Class<? extends Serializable> keyClass = reflectionLookup.extractGenericType(genericReturnType, 0);
		assertSerializable(keyClass);
		final Class<? extends Serializable> valueClass = reflectionLookup.extractGenericType(genericReturnType, 1);
		assertSerializable(valueClass);

		final Map<Serializable, Serializable> result = new LinkedHashMap<>();
		final Set<String> indexes = serializedForm.getIndexStrings(propertyName);
		for (String indexKey : indexes) {
			Assert.isTrue(
				indexKey.startsWith(MAP_KEY_PREFIX),
				() -> new SerializationFailedException("Map key in serialized form must start with " + MAP_KEY_PREFIX + "! Invalid data format.")
			);
			final Object propertyValue = extractValue(
				serializedForm, reflectionLookup,
				propertyName + INDEX_CHAR_START + indexKey + INDEX_CHAR_END,
				valueClass,
				genericReturnType,
				extractionCtx
			);
			final int indexPrimaryKey = Integer.parseInt(indexKey.substring(1));
			final Serializable originalKey = serializedForm.getKeyForId(indexPrimaryKey);
			Assert.isTrue(
				originalKey != null,
				() -> new SerializationFailedException("No map key found for id " + indexPrimaryKey + "! This means data were corrupted!")
			);
			final Serializable propertyKey = DataObjectConverter.getOriginalForm(originalKey, keyClass, reflectionLookup);
			if (propertyValue != null) {
				result.put(
					propertyKey,
					(Serializable) propertyValue
				);
			}
		}

		return result.isEmpty() ? null : result;
	}

	private void assertSerializable(Class<? extends Serializable> theClass) {
		Assert.isTrue(Serializable.class.isAssignableFrom(theClass), "Class " + theClass + " doesn't implement Serializable!");
	}

	/**
	 * This class is used to propagate renamed property name along with argument key.
	 */
	public static class ArgumentKeyWithRename extends ArgumentKey {
		@Getter private final String originalProperty;

		public ArgumentKeyWithRename(String name, Class<?> type, String originalProperty) {
			super(name, type);
			this.originalProperty = originalProperty;
		}

	}

	/**
	 * This internal extraction context allows us to detect a data that were part of {@link ComplexDataObject} but
	 * were not extracted to the target "user class". We need to monitor this, because there is high risk of losing data.
	 * Context also allows us to process {@link @DiscardedData} annotations and allow continuous model evolution.
	 */
	private static class ExtractionContext {
		/**
		 * List of properties that has been extracted and mapped to user class.
		 */
		private final Set<String> extractedProperties;
		/**
		 * List of properties that was found on "user class" marked as discarded data (i.e. may be dropped).
		 */
		private final Set<String> discardedProperties;

		public ExtractionContext(int expectedSize) {
			this.extractedProperties = new HashSet<>(expectedSize);
			this.discardedProperties = new HashSet<>();
		}

		/**
		 * Adds property that which value was migrated to user class instance.
		 */
		public void addProperty(String propertyName) {
			this.extractedProperties.add(propertyName);
		}

		/**
		 * Adds property that has been found on user class as "discarded".
		 */
		public void addDiscardedProperty(String propertyName) {
			this.discardedProperties.add(propertyName);
		}

		/**
		 * Returns count of extracted values to allow quick checksum.
		 */
		public int size() {
			return extractedProperties.size();
		}

		/**
		 * Returns set of all properties that has some data attached but were not mapped to the user class and were
		 * not marked as discarded as well.
		 */
		public Set<String> getNotExtractedValues(Set<String> allProperties) {
			return allProperties
				.stream()
				.filter(it -> !extractedProperties.contains(it) && !discardedProperties.contains(it))
				.collect(Collectors.toSet());
		}
	}

}
