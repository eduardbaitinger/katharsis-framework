package io.katharsis.utils.parser;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Parses {@link String} into an instance of provided {@link Class}. It support
 * the following classes:
 * <ol>
 * <li>{@link String}</li>
 * <li>{@link Byte} and <i>byte</i></li>
 * <li>{@link Short} and <i>short</i></li>
 * <li>{@link Integer} and <i>int</i></li>
 * <li>{@link Long} and <i>long</i></li>
 * <li>{@link Float} and <i>float</i></li>
 * <li>{@link Double} and <i>double</i></li>
 * <li>{@link BigInteger}</li>
 * <li>{@link BigDecimal}</li>
 * <li>{@link Character} and <i>char</i></li>
 * <li>{@link Boolean} and <i>boolean</i></li>
 * <li>{@link java.util.UUID}</li>
 * <li>An {@link Enum}</li>
 * <li>A class with a {@link String} only constructor</li>
 * </ol>
 */
public class TypeParser {

	public final Map<Class, StringParser> parsers;

	public TypeParser() {
		parsers = new HashMap<>();
		parsers.putAll(DefaultStringParsers.get());
	}

	/**
	 * Adds a custom parser for the given type.
	 * 
	 * @param clazz
	 * @param parser
	 */
	public <T> void addParser(Class<T> clazz, StringParser<T> parser) {
		parsers.put(clazz, parser);
	}

	/**
	 * Parses an {@link Iterable} of String instances to {@link Iterable} of
	 * parsed values.
	 * 
	 * @param inputs
	 *            list of Strings
	 * @param clazz
	 *            type to be parsed to
	 * @param <T>
	 *            type of class
	 * @return {@link Iterable} of parsed values
	 */
	public <T extends Serializable> Iterable<T> parse(Iterable<String> inputs, Class<T> clazz) {
		List<T> parsedValues = new LinkedList<>();
		for (String input : inputs) {
			parsedValues.add(parse(input, clazz));
		}

		return parsedValues;
	}

	/**
	 * Parses a {@link String} to an instance of passed {@link Class}
	 * 
	 * @param input
	 *            String value
	 * @param clazz
	 *            type to be parsed to
	 * @param <T>
	 *            type of class
	 * @return instance of parsed value
	 */
	public <T extends Serializable> T parse(String input, Class<T> clazz) {
		try {
			return parseInput(input, clazz);
		} catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException | NumberFormatException | ParserException e) {
			throw new ParserException(e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends Serializable> T parseInput(String input, Class<T> clazz) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
		if (String.class.equals(clazz)) {
			return (T) input;
		} else if (parsers.containsKey(clazz)) {
			StringParser standardTypeParser = parsers.get(clazz);

			return (T) standardTypeParser.parse(input);
		} else if (isEnum(clazz)) {
			return (T) Enum.valueOf((Class<Enum>) clazz.asSubclass(Enum.class), input.trim());
		} else if (containsStringConstructor(clazz)) {
			return clazz.getDeclaredConstructor(String.class).newInstance(input);
		} else {
			Method method;
			try {
				method = clazz.getMethod("parse", String.class);
				return (T) method.invoke(clazz, input);
			} catch (NoSuchMethodException e) { // NOSONAR
				// not available
				throw new ParserException(String.format("Cannot parse to %s : %s", clazz.getName(), input));
			} catch (IllegalAccessException | IllegalArgumentException | SecurityException | InvocationTargetException e) {
				throw new IllegalStateException(e);
			}
		}
	}

	private static <T extends Serializable> boolean isEnum(Class<T> clazz) {
		return clazz.isEnum();
	}

	private boolean containsStringConstructor(Class<?> clazz) throws NoSuchMethodException {
		boolean result = false;
		for (Constructor constructor : clazz.getDeclaredConstructors()) {

			if (constructor.getParameterTypes().length == 1 && constructor.getParameterTypes()[0] == String.class) {
				result = true;
			}
		}
		return result;
	}
}
