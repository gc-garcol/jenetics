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

import static java.lang.Integer.MAX_VALUE;
import static io.jenetics.incubator.grammar.Sentence.Expansion.LEFT_FIRST;
import static io.jenetics.incubator.grammar.Sentence.Expansion.LEFT_TO_RIGHT;

import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.function.Function;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.jenetics.Chromosome;
import io.jenetics.Gene;
import io.jenetics.Genotype;
import io.jenetics.IntegerChromosome;
import io.jenetics.IntegerGene;
import io.jenetics.engine.Codec;
import io.jenetics.incubator.grammar.Cfg.NonTerminal;
import io.jenetics.incubator.grammar.Cfg.Rule;
import io.jenetics.incubator.grammar.Cfg.Symbol;
import io.jenetics.incubator.grammar.Cfg.Terminal;
import io.jenetics.util.BaseSeq;
import io.jenetics.util.Factory;
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

	/**
	 * Defines the expansion strategy used when generating the sentences.
	 */
	public enum Expansion {

		/**
		 * The symbol replacement always starting from the leftmost nonterminal
		 * as described in
		 * <a href="https://www.brinckerhoff.org/tmp/grammatica_evolution_ieee_tec_2001.pdf">
		 * Grammatical Evolution</a>.
		 */
		LEFT_FIRST,

		/**
		 * The symbol replacement is performed from left to right and is repeated
		 * until all non-terminal symbol has been expanded.
		 */
		LEFT_TO_RIGHT;
	}

	/**
	 * Generates a new sentence from the given grammar, <em>cfg</em>.
	 * <p>
	 * The following code snippet shows how to create a random sentence from a
	 * given grammar:
	 * <pre>{@code
	 * final Cfg cfg = Bnf.parse("""
	 *     <expr> ::= ( <expr> <op> <expr> ) | <num> | <var> |  <fun> ( <arg>, <arg> )
	 *     <fun>  ::= FUN1 | FUN2
	 *     <arg>  ::= <expr> | <var> | <num>
	 *     <op>   ::= + | - | * | /
	 *     <var>  ::= x | y
	 *     <num>  ::= 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9
	 *     """
	 * );
	 *
	 * final RandomGenerator random = RandomGenerator.of("L64X256MixRandom");
	 * final List<Terminal> sentence = Sentences.generate(
	 *     cfg, random::nextInt, LEFT_TO_RIGHT, 1_000
	 * );
	 * final String string = sentence.stream()
	 *     .map(Symbol::value)
	 *     .collect(Collectors.joining());
	 *
	 * System.out.println(string);
	 * }</pre>
	 * <em>Some sample output:</em>
	 * <pre>{@code
	 * > ((x-FUN1(5,5))+8)
	 * > (FUN2(y,5)-FUN2(0,x))
	 * > x
	 * > FUN2(x,x)
	 * > 5
	 * > FUN2(y,FUN2((FUN1(5,FUN1(y,2))*9),y))
	 * > ((FUN1(x,5)*9)*(x/(y*FUN2(x,y))))
	 * > (9-(y*(x+x)))
	 * > }</pre>
	 *
	 * @see #generate(Cfg, SymbolIndex)
	 *
	 * @param cfg the generating grammar
	 * @param index the symbol index strategy
	 * @param expansion the expansion strategy to use for building the sentence
	 * @param maxSentenceLength the maximal number of symbols
	 * @return a newly created terminal list (sentence)
	 */
	public static List<Terminal> generate(
		final Cfg cfg,
		final SymbolIndex index,
		final Expansion expansion,
		final int maxSentenceLength
	) {
		final var sentence = new LinkedList<Symbol>();
		expand(cfg, index, sentence, expansion, maxSentenceLength);

		return sentence.stream()
			.map(Terminal.class::cast)
			.toList();
	}

	/**
	 * Generates a new sentence from the given grammar, <em>cfg</em>.
	 *
	 * @param cfg the generating grammar
	 * @param index the symbol index strategy
	 * @param expansion the expansion strategy to use for building the sentence
	 * @return a newly created terminal list (sentence)
	 */
	public static List<Terminal> generate(
		final Cfg cfg,
		final SymbolIndex index,
		final Expansion expansion
	) {
		return generate(cfg, index, expansion, MAX_VALUE);
	}

	/**
	 * Generates a new sentence from the given grammar, <em>cfg</em>.
	 *
	 * @param cfg the generating grammar
	 * @param index the symbol index strategy
	 * @return a newly created terminal list (sentence)
	 */
	public static List<Terminal> generate(
		final Cfg cfg,
		final SymbolIndex index,
		final int maxSentenceSize
	) {
		return generate(cfg, index, LEFT_FIRST, maxSentenceSize);
	}

	/**
	 * Generates a new sentence from the given grammar, <em>cfg</em>.
	 *
	 * @param cfg the generating grammar
	 * @param index the symbol index strategy
	 * @return a newly created terminal list (sentence)
	 */
	public static List<Terminal> generate(final Cfg cfg, final SymbolIndex index) {
		return generate(cfg, index, LEFT_TO_RIGHT, MAX_VALUE);
	}

	static void expand(
		final Cfg cfg,
		final SymbolIndex index,
		final List<Symbol> symbols,
		final Expansion expansion,
		final int maxSentenceLength
	) {
		symbols.add(cfg.start());

		boolean proceed;
		do {
			proceed = false;

			final ListIterator<Symbol> sit = symbols.listIterator();
			while (sit.hasNext() && (expansion == LEFT_TO_RIGHT || !proceed)) {
				if (sit.next() instanceof NonTerminal nt) {
					sit.remove();
					expand(cfg, nt, index).forEach(sit::add);
					proceed = true;
				}
			}

			if (symbols.size() > maxSentenceLength) {
				symbols.clear();
				proceed = false;
			}
		} while (proceed);
	}

	static List<Symbol> expand(
		final Cfg cfg,
		final NonTerminal symbol,
		final SymbolIndex index
	) {
		return cfg.rule(symbol)
			.map(rule -> rule.alternatives()
				.get(index.next(rule))
				.symbols())
			.orElse(List.of(symbol));
	}


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
	 * @param factory the chromosome factory
	 * @param codons generation functions for codons of a gene sequence
	 * @param maxSentenceLength the maximal number of symbols
	 * @return sentence codec
	 */
	public static <G extends Gene<?, G>> Codec<List<Terminal>, G> codec(
		final Cfg cfg,
		final Factory<? extends Chromosome<G>> factory,
		final Function<? super BaseSeq<G>, ? extends SymbolIndex> codons,
		final int maxSentenceLength
	) {
		return Codec.of(
			Genotype.of(factory, 1),
			gt -> generate(cfg, codons.apply(gt.chromosome()), maxSentenceLength)
		);
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
		return codec(
			cfg,
			IntegerChromosome.of(codonRange, codonCount),
			Codons::ofIntegerGenes,
			maxSentenceLength
		);
	}

	public static Codec<List<Terminal>, IntegerGene>
	codec(final Cfg cfg, final IntUnaryOperator length, final int limit) {
		final Map<Rule, Integer> indexes = IntStream.range(0, cfg.rules().size())
			.mapToObj(i -> Map.entry(cfg.rules().get(i), i))
			.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

		final SymbolIndex.Factor<IntegerGene> factory = gt -> {
			final List<Codons> codons = gt.stream()
				.map(Codons::ofIntegerGenes)
				.toList();

			return rule -> codons.get(indexes.get(rule)).next(rule);
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
			gt -> generate(cfg, factory.create(gt), limit)
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

}
