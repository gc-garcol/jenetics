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
package io.jenetics.incubator.parser;

import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Interface for all tokenizers.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 7.0
 * @version 7.0
 */
@FunctionalInterface
interface Tokenizer<T extends Token> {

	/**
	 * Return the next available <em>token</em>, or {@link Token#EOF} if no
	 * further tokens are available.
	 *
	 * @return the next available token
	 */
	T next();

	default Tokenizer<T> filter(final Predicate<? super T> filter) {
		return () -> {
			var token = Tokenizer.this.next();
			while (!filter.test(token) && token != Token.EOF) {
				token = Tokenizer.this.next();
			}
			return token;
		};
	}

	default Stream<T> tokens() {
		return Stream.generate(this::next)
			.takeWhile(token -> token.type().code() != Token.Type.EOF.code());
	}

}
