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
package io.jenetics.incubator.beans.property;

import static io.jenetics.incubator.beans.reflect.Reflect.toRawType;

import java.lang.reflect.Type;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import io.jenetics.incubator.beans.Dtor;
import io.jenetics.incubator.beans.Filters;
import io.jenetics.incubator.beans.Path;
import io.jenetics.incubator.beans.PathValue;
import io.jenetics.incubator.beans.PreOrderIterator;
import io.jenetics.incubator.beans.description.Accessor;
import io.jenetics.incubator.beans.description.Description;
import io.jenetics.incubator.beans.description.Descriptions;
import io.jenetics.incubator.beans.description.IndexedAccessor;
import io.jenetics.incubator.beans.description.IndexedDescription;
import io.jenetics.incubator.beans.description.SimpleDescription;
import io.jenetics.incubator.beans.property.Value.Immutable;
import io.jenetics.incubator.beans.property.Value.Mutable;
import io.jenetics.incubator.beans.reflect.ArrayType;
import io.jenetics.incubator.beans.reflect.BeanType;
import io.jenetics.incubator.beans.reflect.ListType;
import io.jenetics.incubator.beans.reflect.OptionalType;
import io.jenetics.incubator.beans.reflect.RecordType;
import io.jenetics.incubator.beans.reflect.Reflect;
import io.jenetics.incubator.beans.reflect.ElementType;

/**
 * This class contains helper methods for extracting the properties from a given
 * root object. It is the main entry point for the extracting properties from
 * an object graph.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @version 7.2
 * @since 7.2
 */
public final class Properties {

//	/**
//	 * Standard filter for the source types. It excludes all JDK types from being
//	 * used, but includes arrays (except byte[] arrays) and list types.
//	 */
//	public static final Predicate<? super PathValue<?>>
//		STANDARD_SOURCE_FILTER =
//		Filters.filtering(
//			pv -> PathValue.of(
//				pv.path(),
//				pv.value() != null ? pv.value().getClass() : Object.class
//			),
//			Descriptions.STANDARD_SOURCE_FILTER
//		);
//
//	/**
//	 * Standard filter for the target types. It excludes all JDK types from being
//	 * part except they are array- or list properties or the enclosure type are
//	 * array- or list classes.
//	 */
//	public static final Predicate<? super Property>
//		STANDARD_TARGET_FILTER =
//		prop -> Reflect.isNonJdkType(prop.value().enclosure().getClass()) ||
//				Reflect.trait(prop.value().enclosure().getClass()) instanceof IndexedType ||
//				prop instanceof IndexedProperty;


	private Properties() {
	}

	/**
	 * This method extracts the direct properties of the given {@code root}
	 * object.
	 *
	 * @param root the root object from which the properties are extracted
	 * @return all direct properties of the given {@code root} object
	 */
	public static Stream<Property> unapply(final PathValue<?> root) {
		if (root == null || root.value() == null) {
			return Stream.empty();
		}

		final var type = PathValue.<Type>of(root.value().getClass());
		final var descriptions = Descriptions.unapply(type);

		return descriptions
			.flatMap(description -> unapply(root, description));
	}

	private static Value toValue(
		final Object enclosing,
		final Object value,
		final SimpleDescription description
	) {
		return switch (description.accessor()) {
			case Accessor.Readonly accessor -> new Immutable(
				enclosing,
				value,
				toRawType(description.type())
			);
			case Accessor.Writable accessor -> new Mutable(
				enclosing,
				value,
				toRawType(description.type()),
				accessor.getter(),
				accessor.setter()
			);
		};
	}

	private static Stream<Property> unapply(
		final PathValue<?> root,
		final Description description
	) {
		final var enclosing = root.value();

		return switch (description) {
			case SimpleDescription sd -> {
				final var path = root.path().append(sd.path().element());
				final var value = toValue(
					enclosing,
					sd.accessor().getter().get(root.value()),
					sd
				);

				final Property prop = switch (Reflect.trait(sd.type())) {
					case ElementType t -> new SimpleProperty(path, value);
					case RecordType t -> new RecordProperty(path, value);
					case BeanType t -> new BeanProperty(path, value);
					case OptionalType t -> new OptionalProperty(path, value);
					case ArrayType t -> new ArrayProperty(path, value);
					case ListType t -> new ListProperty(path, value);
				};

				yield Stream.of(prop);
			}
			case IndexedDescription id -> {
				final var path = description.path().element() instanceof Path.Index
					? root.path()
					: root.path().append(id.path().element());

				final int size = id.accessor().size().get(enclosing);

				yield  IntStream.range(0, size).mapToObj(i -> {
					final Object value = id.accessor().getter().get(enclosing, i);
					final Class<?> type = value != null
						? value.getClass()
						: toRawType(id.type());

					var v = switch (id.accessor()) {
						case IndexedAccessor.Readonly accessor -> new Immutable(
							enclosing,
							value,
							type
						);
						case IndexedAccessor.Writable accessor -> new Mutable(
							enclosing,
							value,
							type,
							object -> accessor.getter().get(object, i),
							(object, val) -> accessor.setter().set(object, i, val)
						);
					};

					return new IndexProperty(
						path.append(new Path.Index(i)),
						i,
						v
					);
				});
			}
		};
	}



	/**
	 * Return a Stream that is lazily populated with {@code Property} by
	 * searching for all properties in an object tree rooted at a given
	 * starting {@code root} object. If used with the {@link #unapply(PathValue)}
	 * method, all found descriptions are returned, including the descriptions
	 * from the Java classes.
	 * {@snippet lang="java":
	 * final var object = "Some Value";
	 * Properties.walk(PathEntry.of(object), Properties::extract)
	 *     .forEach(System.out::println);
	 * }
	 * The code snippet above will create the following output:
	 * <pre>
	 * SingleProperty[path=blank, value=Mutable[value=false, type=boolean, enclosureType=java.lang.String]]
	 * SingleProperty[path=bytes, value=Mutable[value=[B@41e1455d, type=[B, enclosureType=java.lang.String]]
	 * SingleProperty[path=empty, value=Mutable[value=false, type=boolean, enclosureType=java.lang.String]]
	 * </pre>
	 *
	 * If you are not interested in the property descriptions of the Java
	 * classes, you should the {@link #walk(PathValue, String...)} )} instead.
	 *
	 * @see #walk(PathValue, String...)
	 * @see #walk(Object, String...)
	 *
	 * @param root the root of the object tree
	 * @param dtor the first level property extractor used for extracting
	 *        the object properties
	 * @return a property stream
	 */
	public static Stream<Property> walk(
		final PathValue<?> root,
		final Dtor<? super PathValue<?>, ? extends Property> dtor
	) {
		final Dtor<? super PathValue<?>, Property> recursiveDtor =
			PreOrderIterator.dtor(
				dtor,
				property -> PathValue.of(property.path(), property.value().value()),
				PathValue::value
			);

		return recursiveDtor.unapply(root);
	}

	/**
	 * Return a {@code Stream} that is lazily populated with {@code Property}
	 * by walking the object tree rooted at a given starting object.
	 *
	 * {@snippet lang="java":
	 * record Author(String forename, String surname) { }
	 * record Book(String title, int pages, List<Author> authors) { }
	 *
	 * final var object = new Book(
	 *     "Oliver Twist",
	 *     366,
	 *     List.of(new Author("Charles", "Dickens"))
	 * );
	 *
	 * Properties.walk(PathEntry.of(object))
	 *     .forEach(System.out::println);
	 * }
	 *
	 * The code snippet above will create the following output:
	 *
	 * {@snippet lang="java":
	 * ListProperty[path=authors, value=Immutable[value=[Author[forename=Charles, surname=Dickens]], type=java.util.List, enclosureType=Book]]
	 * IndexProperty[path=authors[0], value=Mutable[value=Author[forename=Charles, surname=Dickens], type=Author, enclosureType=java.util.ImmutableCollections$List12]]
	 * SingleProperty[path=authors[0].forename, value=Immutable[value=Charles, type=java.lang.String, enclosureType=Author]]
	 * SingleProperty[path=authors[0].surname, value=Immutable[value=Dickens, type=java.lang.String, enclosureType=Author]]
	 * SingleProperty[path=pages, value=Immutable[value=366, type=int, enclosureType=Book]]
	 * SingleProperty[path=title, value=Immutable[value=Oliver Twist, type=java.lang.String, enclosureType=Book]]
	 * }
	 *
	 * @see #walk(Object, String...)
	 *
	 * @param root the root of the object tree
	 * @param includes the included object name (glob) patterns
	 * @return a property stream
	 */
	public static Stream<Property>
	walk(final PathValue<?> root, final String... includes) {
		final Dtor<PathValue<?>, Property> dtor = Properties::unapply;

		return walk(
			root,
			dtor.sourceFilter(includesFilter(includes))
		);
	}

	private static Predicate<? super PathValue<?>>
	includesFilter(final String... includes) {
		final Predicate<? super Path> filter = Stream.of(includes)
			.map(include -> Filters
				.filtering(
					Path::toString,
					Filters.ofGlob(include)
				)
			)
			.reduce((a, b) -> path -> a.test(path) || b.test(path))
			.orElse(a -> true);

		return entry -> filter.test(entry.path());
	}

	/**
	 * Return a {@code Stream} that is lazily populated with {@code Property}
	 * by walking the object tree rooted at a given starting object.
	 *
	 * @see #walk(PathValue, String...)
	 *
	 * @param root the root of the object tree
	 * @param includes the included object name (glob) patterns
	 * @return a property stream
	 */
	public static Stream<Property>
	walk(final Object root, final String... includes) {
		return walk(
			root instanceof PathValue<?> pe ? pe : PathValue.of(root),
			includes
		);
	}

	static String toString(final String name, final Property property) {
		return "%s[path=%s, value=%s]".formatted(
			name,
			property.path(),
			property.value()
		);
	}

}
