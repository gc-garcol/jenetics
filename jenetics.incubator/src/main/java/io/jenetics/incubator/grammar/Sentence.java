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
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Codec;
import io.jenetics.incubator.grammar.Cfg.NonTerminal;
import io.jenetics.incubator.grammar.Cfg.Rule;
import io.jenetics.incubator.grammar.Cfg.Symbol;
import io.jenetics.incubator.grammar.Cfg.Terminal;
import io.jenetics.util.ISeq;
import io.jenetics.util.IntRange;

/**
 * This class contains low-level methods for creating <em>sentences</em> from a
 * given context-free grammar ({@link Cfg}). A sentence is defined as list of
 * {@link Terminal}s, {@code List<Cfg.Terminal>}.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 * @since !__version__!
 * @version !__version__!
 */
public final class Sentence {
	private Sentence() {}

	/* *************************************************************************
	 * Codec factories
	 * ************************************************************************/

	public static String toString(final List<? extends Symbol> symbols) {
		return symbols.stream()
			.map(symbol -> symbol instanceof NonTerminal nt
				? "<%s>".formatted(nt)
				: symbol.value())
			.collect(Collectors.joining());
	}

	/**
	 * Codec for sentences, generated by a grammar.
	 *
	 * @param cfg the creating grammar
	 * @param codonRange the value range of the <em>codons</em> used for the
	 *        sentence generation
	 * @param codonCount the length of the chromosomes
	 * @param maxSentenceLength the maximal number of symbols
	 * @return sentence codec
	 */
	public static Codec<List<Terminal>, IntegerGene> codec(
		final Cfg cfg,
		final IntRange codonRange,
		final IntRange codonCount,
		final int maxSentenceLength
	) {
		return Codec.of(
			Genotype.of(IntegerChromosome.of(codonRange, codonCount)),
			gt -> new StandardSentenceGenerator(
				Codons.ofIntegerGenes(gt.chromosome()),
				StandardSentenceGenerator.Expansion.LEFT_TO_RIGHT,
				maxSentenceLength).generate(cfg)
		);
	}

	public static Codec<List<Terminal>, IntegerGene> codec(
		final Cfg cfg,
		final IntUnaryOperator length,
		final Function<? super SymbolIndex, SentenceGenerator> generator
	) {
		final Map<Rule, Integer> ruleIndex = IntStream.range(0, cfg.rules().size())
			.mapToObj(i -> Map.entry(cfg.rules().get(i), i))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final SymbolIndex.Factor<IntegerGene> symbolIndex = gt -> {
			final List<Codons> codons = gt.stream()
				.map(Codons::ofIntegerGenes)
				.toList();

			return rule -> codons.get(ruleIndex.get(rule)).next(rule);
		};

		return Codec.of(
			Genotype.of(
				cfg.rules().stream()
					.map(rule -> {
						final int size = rule.alternatives().size();
						return IntegerChromosome.of(IntRange.of(0, size), length.applyAsInt(size));
					})
					.collect(ISeq.toISeq())
			),
			gt -> generator.apply(symbolIndex.create(gt)).generate(cfg)
		);
	}

	public static Codec<List<Terminal>, IntegerGene> codec(
		final Cfg cfg,
		final IntUnaryOperator length,
		final int maxSentenceLength
	) {
		return codec(
			cfg,
			length,
			index -> new StandardSentenceGenerator(index, StandardSentenceGenerator.Expansion.LEFT_TO_RIGHT, maxSentenceLength)
		);
	}

}
