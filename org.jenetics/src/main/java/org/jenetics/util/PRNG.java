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
 *    Franz Wilhelmstötter (franz.wilhelmstoetter@gmx.at)
 */
package org.jenetics.util;

import java.util.Random;

/**
 * Abstract {@code Random} class with additional <i>next</i> random number
 * methods.
 *
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @since 1.2
 * @version 1.2 &mdash; <em>$Date: 2013-09-16 $</em>
 */
abstract class PRNG extends Random {

	private static final long serialVersionUID = 1L;

	/**
	 * Create a new {@code PRNG} instance with the given {@code seed}.
	 *
	 * @param seed the seed of the new {@code PRNG} instance.
	 */
	protected PRNG(final long seed) {
		super(seed);
	}

	/**
	 * Create a new {@code PRNG} instance with a seed created with the
	 * {@link math.random#seed()} value.
	 */
	protected PRNG() {
		this(math.random.seed());
	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between min and
	 * max (end points included).
	 *
	 * @param min lower bound for generated integer
	 * @param max upper bound for generated integer
	 * @return a random integer greater than or equal to {@code min} and less
	 *         than or equal to {@code max}
	 * @throws IllegalArgumentException if {@code min >= max}
	 *
	 * @see math.random#nextInt(Random, int, int)
	 */
	public int nextInt(final int min, final int max) {
		return math.random.nextInt(this, min, max);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed int value between min
	 * and max (end points included).
	 *
	 * @param min lower bound for generated long integer
	 * @param max upper bound for generated long integer
	 * @return a random long integer greater than or equal to {@code min}
	 *         and less than or equal to {@code max}
	 * @throws IllegalArgumentException if {@code min >= max}
	 *
	 * @see math.random#nextLong(Random, long, long)
	 */
	public long nextLong(final long min, final long max) {
		return math.random.nextLong(this, min, max);
	}


	/**
	 * Returns a pseudorandom, uniformly distributed int value between 0
	 * (inclusive) and the specified value (exclusive), drawn from the given
	 * random number generator's sequence.
	 *
	 * @param n the bound on the random number to be returned. Must be
	 *        positive.
	 * @return the next pseudorandom, uniformly distributed int value
	 *         between 0 (inclusive) and n (exclusive) from the given random
	 *         number generator's sequence
	 * @throws IllegalArgumentException if n is smaller than 1.
	 *
	 * @see math.random#nextLong(Random, long)
	 */
	public long nextLong(final long n) {
		return math.random.nextLong(this, n);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed double value between
	 * min (inclusively) and max (exclusively).
	 *
	 * @param min lower bound for generated float value
	 * @param max upper bound for generated float value
	 * @return a random float greater than or equal to {@code min} and less
	 *         than to {@code max}
	 *
	 * @see math.random#nextFloat(Random, float, float)
	 */
	public float nextFloat(final float min, final float max) {
		return math.random.nextFloat(this, min, max);
	}

	/**
	 * Returns a pseudorandom, uniformly distributed double value between
	 * min (inclusively) and max (exclusively).
	 *
	 * @param min lower bound for generated double value
	 * @param max upper bound for generated double value
	 * @return a random double greater than or equal to {@code min} and less
	 *         than to {@code max}
	 *
	 * @see math.random#nextDouble(Random, double, double)
	 */
	public double nextDouble(final double min, final double max) {
		return math.random.nextDouble(this, min, max);
	}

}
