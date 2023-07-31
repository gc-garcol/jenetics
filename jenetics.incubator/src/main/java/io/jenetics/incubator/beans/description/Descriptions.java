/*
 * Java Genetic Algorithm Library (@__identifier__@).
 * Copyright (c) @__year__@ Franz Wilhelmstötter
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Author:
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmail.com)
 */
package io.jenetics.incubator.beans.description;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import io.jenetics.incubator.beans.Extractor;
import io.jenetics.incubator.beans.Path;
import io.jenetics.incubator.beans.PathValue;
import io.jenetics.incubator.beans.PreOrderIterator;
import io.jenetics.incubator.beans.Types.ArrayType;
import io.jenetics.incubator.beans.Types.BeanType;
import io.jenetics.incubator.beans.Types.ListType;
import io.jenetics.incubator.beans.Types.RecordType;

/**
 * This class contains methods for extracting the <em>static</em> bean property
 * information from a given object. It is the main entry point for the extracting
 * properties from an object graph.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version !__version__!
 * @since !__version__!
 */
public final class Descriptions {

	public static final Predicate<? super PathValue<? extends Type>>
		STANDARD_SOURCE_DESCRIPTION_FILTER =
		type -> {
			final var cls = type.value() instanceof ParameterizedType pt
				? (Class<?>)pt.getRawType()
				: (Class<?>)type.value();

			final var name = cls.getName();

			return
				// Allow native Java arrays, except byte[] arrays.
				(name.startsWith("[") && !name.endsWith("[B")) ||
					// Allow Java collection classes.
					Collection.class.isAssignableFrom(cls) ||
					(
						!name.startsWith("java") &&
						!name.startsWith("com.sun") &&
						!name.startsWith("sun") &&
						!name.startsWith("jdk")
					);
		};
	public static final Predicate<? super PathValue<?>>
		STANDARD_SOURCE_FILTER =
		object -> {
			final var type = object.value() != null
				? object.value().getClass()
				: Object.class;

			return STANDARD_SOURCE_DESCRIPTION_FILTER
				.test(PathValue.of(object.path(), type));
		};

	public static final Predicate<? super Description>
		STANDARD_TARGET_DESCRIPTION_FILTER =
		prop -> !(prop.value() instanceof Description.Value.Single &&
			prop.value().enclosure().getName().startsWith("java"));

	private Descriptions() {
	}

	/**
	 * Extracts the <em>directly</em> available property descriptions for the
	 * given {@code type} and start path, {@link PathValue#path()}.
	 *
	 * @param type the enclosure type + start <em>path</em>
	 * @return the <em>directly</em> available property descriptions
	 */
	public static Stream<Description> extract(final PathValue<? extends Type> type) {
		if (type == null || type.value() == null) {
			return Stream.empty();
		}

		if (ArrayType.of(type.value()) instanceof ArrayType at) {
			return Stream.of(new Description(
				type.path().append(new Path.Index(0)),
				new Description.Value.Indexed(
					at.arrayType(), at.componentType(),
					Array::getLength, Array::get, Array::set
				)
			));
		} else if (ListType.of(type.value()) instanceof ListType lt) {
			return Stream.of(new Description(
				type.path().append(new Path.Index(0)),
				new Description.Value.Indexed(
					lt.listType(), lt.componentType(),
					Lists::size, Lists::get, Lists::set
				)
			));
		} else {
			return toDescriptions(type)
				.sorted(Comparator.comparing(PathValue::name));
		}
	}

	private static Stream<Description>
	toDescriptions(final PathValue<? extends Type> type) {
		if (RecordType.of(type.value()) instanceof RecordType rt) {
			return Stream.of(rt.type().getRecordComponents())
				.filter(d -> d.getAccessor().getReturnType() != Class.class)
				.map(c -> toDescription(type.path(), c));
		} else if (BeanType.of(type.value()) instanceof BeanType bt) {
			try {
				final PropertyDescriptor[] descriptors = Introspector
					.getBeanInfo(bt.type())
					.getPropertyDescriptors();

				return Stream.of(descriptors)
					.filter(d -> d.getReadMethod() != null)
					.filter(d -> d.getReadMethod().getReturnType() != Class.class)
					.map(d -> toDescription(type.path(), d));
			} catch (IntrospectionException e) {
				throw new IllegalArgumentException(
					"Can't introspect class '%s'.".formatted(type.value()),
					e
				);
			}
		} else {
			return Stream.of();
		}
	}

	private static Description
	toDescription(final Path path, final PropertyDescriptor descriptor) {
		return new Description(
			path.append(descriptor.getName()),
			new Description.Value.Single(
				descriptor.getReadMethod().getDeclaringClass(),
				descriptor.getReadMethod().getGenericReturnType(),
				Methods.toGetter(descriptor.getReadMethod()),
				Methods.toSetter(descriptor.getWriteMethod())
			)
		);
	}

	private static Description
	toDescription(final Path path, final RecordComponent component) {
		return new Description(
			path.append(component.getName()),
			new Description.Value.Single(
				component.getDeclaringRecord(),
				component.getAccessor().getGenericReturnType(),
				Methods.toGetter(component.getAccessor()),
				null
			)
		);
	}

	/**
	 * Return a Stream that is lazily populated with {@code Description} by
	 * searching for all property descriptions in an object tree rooted at a
	 * given starting {@code root} object. Only the <em>statically</em>
	 * available property descriptions are returned. If used with the
	 * {@link #extract(PathValue)} method, all found descriptions are returned,
	 * including the descriptions from the Java classes.
	 * <pre>{@code
	 * Descriptions
	 *     .walk(PathEntry.of(String.class), Descriptions::extract)
	 *     .forEach(System.out::println);
	 * }</pre>
	 *
	 * The code snippet above will create the following output:
	 *
	 * <pre>
	 * Description[path=blank, value=Single[value=boolean, enclosure=java.lang.String]]
	 * Description[path=bytes, value=Single[value=class [B, enclosure=java.lang.String]]
	 * Description[path=empty, value=Single[value=boolean, enclosure=java.lang.String]]
	 * </pre>
	 *
	 * If you are not interested in the property descriptions of the Java
	 * classes, you should the {@link #walk(PathValue)} instead.
	 *
	 * @see #walk(PathValue)
	 *
	 * @param root the root class of the object graph
	 * @param extractor the extractor used for fetching the directly available
	 *        descriptions. See {@link #extract(PathValue)}.
	 * @return all <em>statically</em> fetch-able property descriptions
	 */
	public static Stream<Description> walk(
		final PathValue<? extends Type> root,
		final Extractor<
			? super PathValue<? extends Type>,
			? extends Description
		> extractor
	) {
		final Extractor<? super PathValue<? extends Type>, Description>
			recursiveExtractor = PreOrderIterator.extractor(
				extractor,
				desc -> PathValue.of(desc.path(), desc.value().value()),
				PathValue::value
			);

		return recursiveExtractor.extract(root);
	}

	/**
	 * Return a Stream that is lazily populated with {@code Description} by
	 * searching for all property descriptions in an object tree rooted at a
	 * given starting {@code root} object. Only the <em>statically</em>
	 * available property descriptions are returned, and the property
	 * descriptions from Java classes are not part of the result.
	 *
	 * <pre>{@code
	 * record Author(String forename, String surname) { }
	 * record Book(String title, int pages, List<Author> authors) { }
	 *
	 * Descriptions.walk(PathEntry.of(Book.class))
	 *     .forEach(System.out::println);
	 * }</pre>
	 *
	 * The code snippet above will create the following output:
	 *
	 * <pre>{@code
	 * Description[path=authors, value=Single[value=java.util.List<Author>, enclosure=Book]]
	 * Description[path=authors[0], value=Indexed[value=Author, enclosure=java.util.List]]
	 * Description[path=authors[0].forename, value=Single[value=java.lang.String, enclosure=Author]]
	 * Description[path=authors[0].surname, value=Single[value=java.lang.String, enclosure=Author]]
	 * Description[path=pages, value=Single[value=int, enclosure=Book]]
	 * Description[path=title, value=Single[value=java.lang.String, enclosure=Book]]
	 * }</pre>
	 *
	 * @see #walk(PathValue, Extractor)
	 * @see #walk(Type)
	 *
	 * @param root the root class of the object graph
	 * @return all <em>statically</em> fetch-able property descriptions
	 */
	public static Stream<Description> walk(final PathValue<? extends Type> root) {
		final Extractor<PathValue<? extends Type>, Description>
			extractor = Descriptions::extract;

		return walk(
			root,
			extractor
				.sourceFilter(STANDARD_SOURCE_DESCRIPTION_FILTER)
				.targetFilter(STANDARD_TARGET_DESCRIPTION_FILTER)
		);
	}

	/**
	 * Return a Stream that is lazily populated with {@code Description} by
	 * searching for all property descriptions in an object tree rooted at a
	 * given starting {@code root} object. Only the <em>statically</em>
	 * available property descriptions are returned, and the property
	 * descriptions from Java classes are not part of the result.
	 *
	 * @see #walk(PathValue, Extractor)
	 * @see #walk(PathValue)
	 *
	 * @param root the root class of the object graph
	 * @return all <em>statically</em> fetch-able property descriptions
	 */
	public static Stream<Description> walk(final Type root) {
		return walk(PathValue.of(root));
	}

}
