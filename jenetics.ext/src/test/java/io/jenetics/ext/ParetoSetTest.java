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
package io.jenetics.ext;

import static java.lang.Math.sqrt;
import static java.lang.String.format;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.stream.Stream;

import org.testng.Assert;
import org.testng.annotations.Test;

import io.jenetics.util.ISeq;

/**
 * @author <a href="mailto:franz.wilhelmstoetter@gmail.com">Franz Wilhelmstötter</a>
 */
public class ParetoSetTest {

	static final class Point implements Comparable<Point> {
		final double x;
		final double y;

		private Point(final double x, final double y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int compareTo(final Point point) {
			boolean adom = false;
			boolean bdom = false;

			int cmp = Double.compare(x, point.x);
			if (cmp > 0) {
				adom = true;
				if (bdom) {
					return 0;
				}
			} else if (cmp < 0) {
				bdom = true;
				if (adom) {
					return 0;
				}
			}

			cmp = Double.compare(y, point.y);
			if (cmp > 0) {
				adom = true;
				if (bdom) {
					return 0;
				}
			} else if (cmp < 0) {
				bdom = true;
				if (adom) {
					return 0;
				}
			}

			if (adom == bdom) {
				return 0;
			} else if (adom) {
				return 1;
			} else {
				return -1;
			}
		}

		@Override
		public int hashCode() {
			return Arrays.hashCode(new double[]{x, y});
		}

		@Override
		public boolean equals(final Object obj) {
			return obj instanceof Point &&
				Double.compare(((Point)obj).x, x) == 0 &&
				Double.compare(((Point)obj).y, y) == 0;
		}

		@Override
		public String toString() {
			return format("Point[%f, %f: %f]", x, y, Math.sqrt(x*x + y*y));
		}

		static Point of(final double x, final double y) {
			return new Point(x, y);
		}
	}

	static ISeq<Point> points() {
		final Random random = new Random(123);

		return random.doubles(5)
			.mapToObj(x -> Point.of(x, sqrt(1 - x*x)))
			.collect(ISeq.toISeq());
	}

	static ISeq<Point> cpoints() {
		final Random random = new Random(123);

		return random.doubles(7)
			.mapToObj(x -> Point.of(x, random.nextDouble()))
			.filter(p -> p.x*p.x + p.y*p.y < 0.9)
			.collect(ISeq.toISeq());
	}

	@Test
	public void set() {
		final ISeq<Point> outline = points();
		final ISeq<Point> cpoints = cpoints();
		final ISeq<Point> pareto = ParetoSet.pareto(points().append(cpoints).asList());

		System.out.println(outline);
		System.out.println(pareto);
		System.out.println(cpoints);

		System.out.println(cpoints().length());
		System.out.println(pareto.length());

		Assert.assertEquals(
			new HashSet<>(pareto.asList()),
			new HashSet<>(outline.asList())
		);
	}

}
