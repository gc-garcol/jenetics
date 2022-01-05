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
package io.jenetics.incubator.grammar;

import java.util.List;

import io.jenetics.incubator.grammar.Cfg.Terminal;
import io.jenetics.incubator.parser.IterableTokenizer;
import io.jenetics.incubator.parser.Token;

/**
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since 7.0
 * @version 7.0
 */
public class MathSentenceTokenizer extends IterableTokenizer<Terminal, Terminal> {

	public static final Token.Type LPAREN = Token.Type.of(1, "LPAREN");
	public static final Token.Type RPAREN = Token.Type.of(1, "RPAREN");
	public static final Token.Type COMMA = Token.Type.of(1, "COMMA");

	public MathSentenceTokenizer(final List<Terminal> sentence) {
		super(sentence, MathSentenceTokenizer::toToken);
	}

	private static Token<Terminal> toToken(final Terminal terminal) {
		return switch (terminal.value()) {
			case "(" -> new Token<>(LPAREN, terminal);
			case ")" -> new Token<>(RPAREN, terminal);
			case "," -> new Token<>(COMMA, terminal);
			default -> null;
		};
	}

}
