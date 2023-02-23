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

package io.evitadb.api.dataType;

import io.evitadb.api.exception.InconvertibleDataTypeException;
import io.evitadb.api.exception.UnsupportedDataTypeException;
import io.evitadb.api.utils.Assert;
import io.evitadb.api.utils.MemoryMeasuringConstants;
import io.evitadb.api.utils.NumberUtils;
import lombok.Data;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * This class contains validation logic for Evita DB data types.
 *
 * @author Jan Novotný (novotny@fg.cz), FG Forrest a.s. (c) 2021
 */
public class EvitaDataTypes {
	private static final Set<Class<?>> SUPPORTED_QUERY_DATA_TYPES;
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPING_TYPES;
	private static final char CHAR_STRING_DELIMITER = '\'';
	private static final char CHAR_LOCALE_DELIMITER = '`';
	private static final String STRING_DELIMITER = "" + CHAR_STRING_DELIMITER;
	private static final Function<String, ZonedDateTime> PARSE_TO_ZONED_DATE_TIME = string -> {
		try {
			return ZonedDateTime.parse(string, DateTimeFormatter.ISO_ZONED_DATE_TIME);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	};
	private static final Function<String, LocalDateTime> PARSE_TO_LOCAL_DATE_TIME = string -> {
		try {
			return LocalDateTime.parse(string, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	};
	private static final Function<String, LocalDate> PARSE_TO_LOCAL_DATE = string -> {
		try {
			return LocalDate.parse(string, DateTimeFormatter.ISO_LOCAL_DATE);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	};
	private static final Function<String, LocalTime> PARSE_TO_LOCAL_TIME = string -> {
		try {
			return LocalTime.parse(string, DateTimeFormatter.ISO_LOCAL_TIME);
		} catch (DateTimeParseException ignored) {
			return null;
		}
	};
	private static final BiFunction<Class<?>, Serializable, Number> BIG_DECIMAL_FUNCTION = (requestedType, unknownObject) -> {
		try {
			if (unknownObject instanceof Number) {
				return (Number) unknownObject;
			} else {
				return new BigDecimal(unknownObject.toString());
			}
		} catch (ArithmeticException | NumberFormatException ex) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Supplier<Number>, Supplier<InconvertibleDataTypeException>, Number> WRAPPING_FUNCTION = (number, exception) -> {
		try {
			return number.get();
		} catch (ArithmeticException ex) {
			throw exception.get();
		}
	};
	private static final BiFunction<Class<?>, Serializable, Boolean> BOOLEAN_FUNCTION = (requestedType, unknownObject) -> {
		if (unknownObject instanceof Boolean) {
			return (Boolean) unknownObject;
		} else if (unknownObject instanceof Number) {
			return Objects.equals(1L, ((Number)unknownObject).longValue());
		} else {
			return Boolean.parseBoolean(unknownObject.toString());
		}
	};
	private static final BiFunction<Class<?>, Serializable, Character> CHAR_FUNCTION = (requestedType, unknownObject) -> {
		if (unknownObject instanceof Character) {
			return (Character) unknownObject;
		} else if (unknownObject instanceof Number) {
			return (char)((byte) WRAPPING_FUNCTION.apply(() -> NumberUtils.convertToByte(BIG_DECIMAL_FUNCTION.apply(requestedType, unknownObject)), () -> new InconvertibleDataTypeException(requestedType, unknownObject)));
		} else {
			final String str = unknownObject.toString();
			if (str.length() == 1) {
				return str.charAt(0);
			} else {
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
		}
	};
	private static final BiFunction<Class<?>, Serializable, Currency> CURRENCY_FUNCTION = (requestedType, unknownObject) -> {
		try {
			return Currency.getInstance(unknownObject.toString());
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Class<?>, Serializable, Locale> LOCALE_FUNCTION = (requestedType, unknownObject) -> {
		if (unknownObject instanceof Locale) {
			return (Locale) unknownObject;
		} else {
			final Locale locale = new Locale(unknownObject.toString());
			try {
				Assert.notNull(locale.getISO3Language(), "This should return language at all costs!");
			} catch (MissingResourceException ex) {
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
			return locale;
		}
	};
	private static final BiFunction<Class<?>, Serializable, ZonedDateTime> ZONED_DATE_TIME_FUNCTION = (requestedType, unknownObject) -> {
		try {
			if (unknownObject instanceof ZonedDateTime) {
				return (ZonedDateTime) unknownObject;
			} else if (unknownObject instanceof LocalDateTime) {
				return ((LocalDateTime)unknownObject).atZone(ZoneId.systemDefault());
			} else if (unknownObject instanceof LocalDate) {
				return ((LocalDate)unknownObject).atStartOfDay(ZoneId.systemDefault());
			} else {
				final String value = unknownObject.toString();
				final ZonedDateTime parsedZoneDateTime = PARSE_TO_ZONED_DATE_TIME.apply(value);
				if (parsedZoneDateTime != null) {
					return parsedZoneDateTime;
				}
				final LocalDateTime parsedLocalDateTime = PARSE_TO_LOCAL_DATE_TIME.apply(value);
				if (parsedLocalDateTime != null) {
					return parsedLocalDateTime.atZone(ZoneId.systemDefault());
				}
				final LocalDate parsedLocalDate = PARSE_TO_LOCAL_DATE.apply(value);
				if (parsedLocalDate != null) {
					return parsedLocalDate.atStartOfDay(ZoneId.systemDefault());
				}
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Class<?>, Serializable, LocalDateTime> LOCAL_DATE_TIME_FUNCTION = (requestedType, unknownObject) -> {
		try {
			if (unknownObject instanceof LocalDateTime) {
				return (LocalDateTime) unknownObject;
			} else if (unknownObject instanceof ZonedDateTime) {
				return ((ZonedDateTime)unknownObject).toLocalDateTime();
			} else if (unknownObject instanceof LocalDate) {
				return ((LocalDate)unknownObject).atStartOfDay();
			} else {
				final String value = unknownObject.toString();
				final LocalDateTime parsedLocalDateTime = PARSE_TO_LOCAL_DATE_TIME.apply(value);
				if (parsedLocalDateTime != null) {
					return parsedLocalDateTime;
				}
				final ZonedDateTime parsedZoneDateTime = PARSE_TO_ZONED_DATE_TIME.apply(value);
				if (parsedZoneDateTime != null) {
					return parsedZoneDateTime.toLocalDateTime();
				}
				final LocalDate parsedLocalDate = PARSE_TO_LOCAL_DATE.apply(value);
				if (parsedLocalDate != null) {
					return parsedLocalDate.atStartOfDay();
				}
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Class<?>, Serializable, LocalDate> LOCAL_DATE_FUNCTION = (requestedType, unknownObject) -> {
		try {
			if (unknownObject instanceof LocalDate) {
				return (LocalDate) unknownObject;
			} else if (unknownObject instanceof ZonedDateTime) {
				return ((ZonedDateTime)unknownObject).toLocalDate();
			} else if (unknownObject instanceof LocalDateTime) {
				return ((LocalDateTime)unknownObject).toLocalDate();
			} else {
				final String value = unknownObject.toString();
				final LocalDate parsedLocalDate = PARSE_TO_LOCAL_DATE.apply(value);
				if (parsedLocalDate != null) {
					return parsedLocalDate;
				}
				final ZonedDateTime parsedZoneDateTime = PARSE_TO_ZONED_DATE_TIME.apply(value);
				if (parsedZoneDateTime != null) {
					return parsedZoneDateTime.toLocalDate();
				}
				final LocalDateTime parsedLocalDateTime = PARSE_TO_LOCAL_DATE_TIME.apply(value);
				if (parsedLocalDateTime != null) {
					return parsedLocalDateTime.toLocalDate();
				}
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Class<?>, Serializable, LocalTime> LOCAL_TIME_FUNCTION = (requestedType, unknownObject) -> {
		try {
			if (unknownObject instanceof LocalTime) {
				return (LocalTime) unknownObject;
			} else if (unknownObject instanceof ZonedDateTime) {
				return ((ZonedDateTime)unknownObject).toLocalTime();
			} else if (unknownObject instanceof LocalDateTime) {
				return ((LocalDateTime)unknownObject).toLocalTime();
			} else {
				final String value = unknownObject.toString();
				final LocalTime parsedLocalTime = PARSE_TO_LOCAL_TIME.apply(value);
				if (parsedLocalTime != null) {
					return parsedLocalTime;
				}
				final ZonedDateTime parsedZoneDateTime = PARSE_TO_ZONED_DATE_TIME.apply(value);
				if (parsedZoneDateTime != null) {
					return parsedZoneDateTime.toLocalTime();
				}
				final LocalDateTime parsedLocalDateTime = PARSE_TO_LOCAL_DATE_TIME.apply(value);
				if (parsedLocalDateTime != null) {
					return parsedLocalDateTime.toLocalTime();
				}
				throw new InconvertibleDataTypeException(requestedType, unknownObject);
			}
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<Class<?>, Serializable, DateTimeRange> DATE_TIME_RANGE_FUNCTION = (requestedType, unknownObject) -> {
		if (unknownObject instanceof DateTimeRange) {
			return (DateTimeRange) unknownObject;
		} else {
			final String value = unknownObject.toString();
			final String[] parsedResult = DateTimeRange.PARSE_FCT.apply(value);
			if (parsedResult != null) {
				if (parsedResult[0] == null && parsedResult[1] != null) {
					final ZonedDateTime to = ZONED_DATE_TIME_FUNCTION.apply(DateTimeRange.class, parsedResult[1]);
					return DateTimeRange.until(to);
				} else if (parsedResult[0] != null && parsedResult[1] == null) {
					final ZonedDateTime from = ZONED_DATE_TIME_FUNCTION.apply(DateTimeRange.class, parsedResult[0]);
					return DateTimeRange.since(from);
				} else {
					final ZonedDateTime from = ZONED_DATE_TIME_FUNCTION.apply(DateTimeRange.class, parsedResult[0]);
					final ZonedDateTime to = ZONED_DATE_TIME_FUNCTION.apply(DateTimeRange.class, parsedResult[1]);
					return DateTimeRange.between(from, to);
				}
			}
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};
	private static final BiFunction<TypeWithPrecision, Serializable, NumberRange> NUMBER_RANGE_FUNCTION = (typeWithPrecision, unknownObject) -> {
		try {
			if (unknownObject instanceof NumberRange) {
				return (NumberRange) unknownObject;
			} else {
				final String value = unknownObject.toString();
				final String[] parsedResult = NumberRange.PARSE_FCT.apply(value);
				if (parsedResult != null) {
					if (parsedResult[0] == null && parsedResult[1] != null) {
						final Number to = BIG_DECIMAL_FUNCTION.apply(NumberRange.class, parsedResult[1]);
						return to instanceof BigDecimal ? NumberRange.to((BigDecimal) to, typeWithPrecision.getPrecision()) : NumberRange.to(to);
					} else if (parsedResult[0] != null && parsedResult[1] == null) {
						final Number from = BIG_DECIMAL_FUNCTION.apply(NumberRange.class, parsedResult[0]);
						return from instanceof BigDecimal ? NumberRange.from((BigDecimal) from, typeWithPrecision.getPrecision()) : NumberRange.from(from);
					} else {
						final Number from = BIG_DECIMAL_FUNCTION.apply(NumberRange.class, parsedResult[0]);
						final Number to = BIG_DECIMAL_FUNCTION.apply(NumberRange.class, parsedResult[1]);
						return from instanceof BigDecimal && to instanceof BigDecimal ?
							NumberRange.between((BigDecimal) from, (BigDecimal) to, typeWithPrecision.getPrecision()) :
							NumberRange.between(from, to);
					}
				}
				throw new InconvertibleDataTypeException(typeWithPrecision.getRequestedType(), unknownObject);
			}
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(typeWithPrecision.getRequestedType(), unknownObject);
		}
	};
	@SuppressWarnings("rawtypes")
	private static final BiFunction<Class<?>, Serializable, Enum> ENUM_FUNCTION = (requestedType, unknownObject) -> {
		try {
			//noinspection rawtypes,unchecked
			return Enum.valueOf((Class<Enum>)requestedType, unknownObject.toString());
		} catch (IllegalArgumentException ignored) {
			throw new InconvertibleDataTypeException(requestedType, unknownObject);
		}
	};

	static {
		final LinkedHashSet<Class<?>> queryDataTypes = new LinkedHashSet<>();
		queryDataTypes.add(String.class);
		queryDataTypes.add(byte.class);
		queryDataTypes.add(Byte.class);
		queryDataTypes.add(short.class);
		queryDataTypes.add(Short.class);
		queryDataTypes.add(int.class);
		queryDataTypes.add(Integer.class);
		queryDataTypes.add(long.class);
		queryDataTypes.add(Long.class);
		queryDataTypes.add(boolean.class);
		queryDataTypes.add(Boolean.class);
		queryDataTypes.add(char.class);
		queryDataTypes.add(Character.class);
		queryDataTypes.add(BigDecimal.class);
		queryDataTypes.add(ZonedDateTime.class);
		queryDataTypes.add(LocalDateTime.class);
		queryDataTypes.add(LocalDate.class);
		queryDataTypes.add(LocalTime.class);
		queryDataTypes.add(DateTimeRange.class);
		queryDataTypes.add(NumberRange.class);
		queryDataTypes.add(Multiple.class);
		queryDataTypes.add(Locale.class);
		queryDataTypes.add(Enum.class);
		queryDataTypes.add(EnumWrapper.class);
		queryDataTypes.add(Currency.class);
		SUPPORTED_QUERY_DATA_TYPES = Collections.unmodifiableSet(queryDataTypes);

		final LinkedHashMap<Class<?>, Class<?>> primitiveWrappers = new LinkedHashMap<>();
		primitiveWrappers.put(boolean.class, Boolean.class);
		primitiveWrappers.put(byte.class, Byte.class);
		primitiveWrappers.put(short.class, Short.class);
		primitiveWrappers.put(int.class, Integer.class);
		primitiveWrappers.put(long.class, Long.class);
		primitiveWrappers.put(char.class, Character.class);
		PRIMITIVE_WRAPPING_TYPES = Collections.unmodifiableMap(primitiveWrappers);
	}

	private EvitaDataTypes() {
	}

	/**
	 * Returns set of all supported data types in Evita DB.
	 */
	public static Set<Class<?>> getSupportedDataTypes() {
		return SUPPORTED_QUERY_DATA_TYPES;
	}

	/**
	 * Returns true if type is directly supported by Evita DB.
	 */
	public static boolean isSupportedType(Class<?> type) {
		return SUPPORTED_QUERY_DATA_TYPES.contains(type) || type.isEnum();
	}

	/**
	 * Returns true if type (may be array type) is directly supported by Evita DB.
	 */
	public static boolean isSupportedTypeOrItsArray(Class<?> type) {
		@SuppressWarnings("unchecked") final Class<? extends Serializable> typeToCheck = type.isArray() ? (Class<? extends Serializable>) type.getComponentType() : (Class<? extends Serializable>) type;
		return EvitaDataTypes.isSupportedType(typeToCheck);
	}

	/**
	 * Method validates input value for use in Evita query.
	 *
	 * @return possible converted object to known type
	 * @throws UnsupportedDataTypeException if non supported type is used
	 */
	public static Serializable toSupportedType(Serializable unknownObject) throws UnsupportedDataTypeException {
		if (unknownObject == null) {
			// nulls are allowed
			return null;
		} else if (unknownObject instanceof Float || unknownObject instanceof Double) {
			// normalize floats and doubles to big decimal
			return new BigDecimal(unknownObject.toString());
		} else if (unknownObject instanceof LocalDateTime) {
			// always convert local date time to zoned date time
			return ((LocalDateTime) unknownObject).atZone(ZoneId.systemDefault());
		} else if (unknownObject.getClass().isEnum()) {
			return unknownObject;
		} else if (SUPPORTED_QUERY_DATA_TYPES.contains(unknownObject.getClass())) {
			return unknownObject;
		} else {
			throw new UnsupportedDataTypeException(unknownObject.getClass(), SUPPORTED_QUERY_DATA_TYPES);
		}
	}

	/**
	 * Method converts unknown object to the requested type supported by by Evita.
	 *
	 * @return unknownObject converted to requested type
	 * @throws UnsupportedDataTypeException when unknownObject cannot be converted to any of Evita supported types
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Serializable> T toTargetType(Serializable unknownObject, Class<T> requestedType) throws UnsupportedDataTypeException {
		return toTargetType(unknownObject, requestedType, 0);
	}

	/**
	 * Method converts unknown object to the requested type supported by by Evita.
	 *
	 * @return unknownObject converted to requested type
	 * @throws UnsupportedDataTypeException when unknownObject cannot be converted to any of Evita supported types
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	public static <T extends Serializable> T toTargetType(@Nullable Serializable unknownObject, @Nonnull Class<T> requestedType, int allowedDecimalPlaces) throws UnsupportedDataTypeException {
		final Class<?> baseRequestedType = requestedType.isArray() ? requestedType.getComponentType() : requestedType;
		Assert.isTrue(isSupportedType(baseRequestedType), "Requested type `" + requestedType + "` is not supported by Evita!");
		if (requestedType.isInstance(unknownObject) || unknownObject == null) {
			return (T) unknownObject;
		}

		if (String.class.isAssignableFrom(baseRequestedType)) {
			return (T) unknownObject.toString();
		} else if (Byte.class.isAssignableFrom(baseRequestedType)) {
			return (T) WRAPPING_FUNCTION.apply(() -> NumberUtils.convertToByte(BIG_DECIMAL_FUNCTION.apply(baseRequestedType, unknownObject)), () -> new InconvertibleDataTypeException(baseRequestedType, unknownObject));
		} else if (Short.class.isAssignableFrom(baseRequestedType)) {
			return (T) WRAPPING_FUNCTION.apply(() -> NumberUtils.convertToShort(BIG_DECIMAL_FUNCTION.apply(baseRequestedType, unknownObject)), () -> new InconvertibleDataTypeException(baseRequestedType, unknownObject));
		} else if (Integer.class.isAssignableFrom(baseRequestedType)) {
			return (T) WRAPPING_FUNCTION.apply(() -> NumberUtils.convertToInt(BIG_DECIMAL_FUNCTION.apply(baseRequestedType, unknownObject)), () -> new InconvertibleDataTypeException(baseRequestedType, unknownObject));
		} else if (Long.class.isAssignableFrom(baseRequestedType)) {
			return (T) WRAPPING_FUNCTION.apply(() -> NumberUtils.convertToLong(BIG_DECIMAL_FUNCTION.apply(baseRequestedType, unknownObject)), () -> new InconvertibleDataTypeException(baseRequestedType, unknownObject));
		} else if (BigDecimal.class.isAssignableFrom(baseRequestedType)) {
			return (T) NumberUtils.convertToBigDecimal(BIG_DECIMAL_FUNCTION.apply(baseRequestedType, unknownObject));
		} else if (Boolean.class.isAssignableFrom(baseRequestedType)) {
			return (T) BOOLEAN_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (Character.class.isAssignableFrom(baseRequestedType)) {
			return (T) CHAR_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (ZonedDateTime.class.isAssignableFrom(baseRequestedType)) {
			return (T) ZONED_DATE_TIME_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (LocalDateTime.class.isAssignableFrom(baseRequestedType)) {
			return (T) LOCAL_DATE_TIME_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (LocalDate.class.isAssignableFrom(baseRequestedType)) {
			return (T) LOCAL_DATE_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (LocalTime.class.isAssignableFrom(baseRequestedType)) {
			return (T) LOCAL_TIME_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (DateTimeRange.class.isAssignableFrom(baseRequestedType)) {
			return (T) DATE_TIME_RANGE_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (NumberRange.class.isAssignableFrom(baseRequestedType)) {
			return (T) NUMBER_RANGE_FUNCTION.apply(new TypeWithPrecision(baseRequestedType, allowedDecimalPlaces), unknownObject);
		} else if (Multiple.class.isAssignableFrom(baseRequestedType)) {
			if (unknownObject instanceof Multiple) {
				return (T) unknownObject;
			} else {
				throw new InconvertibleDataTypeException(baseRequestedType, unknownObject);
			}
		} else if (Locale.class.isAssignableFrom(baseRequestedType)) {
			return (T) LOCALE_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (Currency.class.isAssignableFrom(baseRequestedType)) {
			return (T) CURRENCY_FUNCTION.apply(baseRequestedType, unknownObject);
		} else if (baseRequestedType.isEnum()) {
			return (T) ENUM_FUNCTION.apply(baseRequestedType, unknownObject);
		} else {
			throw new UnsupportedDataTypeException(unknownObject.getClass(), SUPPORTED_QUERY_DATA_TYPES);
		}
	}

	/**
	 * Method formats the value for printing in the Evita query.
	 */
	@Nonnull
	public static String formatValue(@Nullable Serializable value) {
		if (value instanceof String) {
			return CHAR_STRING_DELIMITER + ((String) value).replaceAll(STRING_DELIMITER, "\\\\'") + STRING_DELIMITER;
		} else if (value instanceof Character) {
			return CHAR_STRING_DELIMITER + ((Character) value).toString().replaceAll(STRING_DELIMITER, "\\\\'") + STRING_DELIMITER;
		} else if (value instanceof Number) {
			return value.toString();
		} else if (value instanceof Boolean) {
			return value.toString();
		} else if (value instanceof Range) {
			return value.toString();
		} else if (value instanceof Multiple) {
			return value.toString();
		} else if (value instanceof ZonedDateTime) {
			return DateTimeFormatter.ISO_ZONED_DATE_TIME.format((TemporalAccessor) value);
		} else if (value instanceof LocalDateTime) {
			return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format((TemporalAccessor) value);
		} else if (value instanceof LocalDate) {
			return DateTimeFormatter.ISO_LOCAL_DATE.format((TemporalAccessor) value);
		} else if (value instanceof LocalTime) {
			return DateTimeFormatter.ISO_LOCAL_TIME.format((TemporalAccessor) value);
		} else if (value instanceof Locale) {
			return CHAR_LOCALE_DELIMITER + value.toString() + CHAR_LOCALE_DELIMITER;
		} else if (value instanceof Currency) {
			return CHAR_LOCALE_DELIMITER + value.toString() + CHAR_LOCALE_DELIMITER;
		} else if (value instanceof Enum) {
			return value.toString();
		} else if (value instanceof EnumWrapper) {
			return value.toString();
		} else if (value == null) {
			throw new IllegalStateException(
				"Null argument value should never ever happen. Null values are excluded in constructor of the class!"
			);
		}
		throw new UnsupportedDataTypeException(value.getClass(), EvitaDataTypes.getSupportedDataTypes());
	}

	/**
	 * Method returns wrapping class for primitive type.
	 */
	public static <T extends Serializable> Class<T> getWrappingPrimitiveClass(Class<T> type) {
		final Class<?> wrappingClass = PRIMITIVE_WRAPPING_TYPES.get(type);
		Assert.notNull(wrappingClass, "Class " + type + " is not a primitive class!");
		//noinspection unchecked
		return (Class<T>) wrappingClass;
	}

	/**
	 * Method returns gross estimation of the in-memory size of this instance. The estimation is expected not to be
	 * a precise one. Please use constants from {@link MemoryMeasuringConstants} for size computation.
	 */
	public static int estimateSize(@Nonnull Serializable unknownObject) {
		if (unknownObject.getClass().isArray()) {
			int size = MemoryMeasuringConstants.ARRAY_BASE_SIZE +
				Array.getLength(unknownObject) * MemoryMeasuringConstants.getElementSize(unknownObject.getClass().getComponentType());
			for (int i = 0; i < Array.getLength(unknownObject); i++) {
				size += EvitaDataTypes.estimateSize((Serializable) Array.get(unknownObject, i));
			}
			return size;
		} else if (unknownObject instanceof String) {
			return MemoryMeasuringConstants.computeStringSize((String) unknownObject);
		} else if (unknownObject instanceof Byte) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.BYTE_SIZE;
		} else if (unknownObject instanceof Short) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.SMALL_SIZE;
		} else if (unknownObject instanceof Integer) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.INT_SIZE;
		} else if (unknownObject instanceof Long) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LONG_SIZE;
		} else if (unknownObject instanceof Boolean) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.BYTE_SIZE;
		} else if (unknownObject instanceof Character) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.CHAR_SIZE;
		} else if (unknownObject instanceof BigDecimal) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.BIG_DECIMAL_SIZE;
		} else if (unknownObject instanceof ZonedDateTime) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LOCAL_DATE_TIME_SIZE + MemoryMeasuringConstants.REFERENCE_SIZE;
		} else if (unknownObject instanceof LocalDateTime) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LOCAL_DATE_TIME_SIZE;
		} else if (unknownObject instanceof LocalDate) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LOCAL_DATE_SIZE;
		} else if (unknownObject instanceof LocalTime) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LOCAL_TIME_SIZE;
		} else if (unknownObject instanceof DateTimeRange) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
				2 * (MemoryMeasuringConstants.OBJECT_HEADER_SIZE + MemoryMeasuringConstants.LOCAL_DATE_TIME_SIZE + MemoryMeasuringConstants.REFERENCE_SIZE) +
				2 * (MemoryMeasuringConstants.LONG_SIZE);
		} else if (unknownObject instanceof NumberRange) {
			final NumberRange numberRange = (NumberRange) unknownObject;
			final Number innerDataType = Optional.ofNullable(numberRange.getPreciseFrom())
				.orElseGet(numberRange::getPreciseTo);
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE
				+ 2 * (estimateSize(innerDataType)) +
				MemoryMeasuringConstants.REFERENCE_SIZE + MemoryMeasuringConstants.INT_SIZE +
				2 * (MemoryMeasuringConstants.LONG_SIZE);
		} else if (unknownObject instanceof Multiple) {
			final Multiple multiple = (Multiple) unknownObject;
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE + EvitaDataTypes.estimateSize(multiple.getValues());
		} else if (unknownObject instanceof Locale) {
			return 0;
		} else if (unknownObject instanceof Enum) {
			return 0;
		} else if (unknownObject instanceof EnumWrapper) {
			return MemoryMeasuringConstants.OBJECT_HEADER_SIZE +
				MemoryMeasuringConstants.computeStringSize(((EnumWrapper)unknownObject).getValue());
		} else if (unknownObject instanceof Currency) {
			return 0;
		} else if (unknownObject instanceof ComplexDataObject) {
			final ComplexDataObject complexDataObject = (ComplexDataObject) unknownObject;
			return MemoryMeasuringConstants.REFERENCE_SIZE + complexDataObject.estimateSize();
		} else {
			throw new UnsupportedDataTypeException(unknownObject.getClass(), SUPPORTED_QUERY_DATA_TYPES);
		}
	}

	@Data
	private static class TypeWithPrecision {
		private final Class<?> requestedType;
		private final int precision;
	}

}
