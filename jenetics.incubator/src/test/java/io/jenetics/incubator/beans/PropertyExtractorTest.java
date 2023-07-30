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
package io.jenetics.incubator.beans;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.Test;

import io.jenetics.incubator.beans.description.Descriptions;
import io.jenetics.incubator.beans.property.Properties;

public class PropertyExtractorTest {

	private record Data(
		String string,
		Integer integer,
		int i,
		int[] ints,
		Integer[] integers,
		List<String> list,
		Set<Integer> set,
		Map<String, Integer> map
	) {}


	@Test
	public void extract() {
		final var data = new Data(
			"stringValue",
			123,
			456,
			new int[] {1, 2, 3},
			new Integer[] {4, 5, 6},
			List.of("a", "b", "c"),
			Set.of(1, 2, 3),
			Map.of("a", 1, "b", 2)
		);

		Properties.walk(data)
			.forEach(System.out::println);
	}

	@Test
	public void extractIntArray() {
		final var data = new Object[] {1, 2};

		Properties.extract(PathValue.of(data))
			.forEach(System.out::println);
	}


	@Test
	public void foo() {
		record Author(String forename, String surname) { }
		record Book(String title, int pages, List<Author> authors) { }

		final var object = new Book(
			"Oliver Twist",
			366,
			List.of(new Author("Charles", "Dickens"))
		);

		Properties.walk(PathValue.of(object))
			.forEach(System.out::println);

		System.out.println("----");
		Descriptions.walk(PathValue.of(Book.class))
			.forEach(System.out::println);

	}

}
