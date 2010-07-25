/*
 * Java Genetic Algorithm Library (@!identifier!@).
 * Copyright (c) @!year!@ Franz Wilhelmstötter
 *  
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.	See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 *
 * Author:
 * 	 Franz Wilhelmstötter (franz.wilhelmstoetter@gmx.at)
 * 	 
 */
package org.jenetics;

import java.util.Random;

import org.jenetics.util.Array;
import org.jenetics.util.RandomRegistry;


/** 
 * <strong><p>Single point crossover</p></strong>
 * 
 * <p>
 * One or two children are created by taking two parent strings and cutting 
 * them at some randomly chosen site. E.g.
 * </p>
 * <div align="center">
 *	<img src="doc-files/SinglePointCrossover.gif" >
 * </div>
 * <p>
 * If we create a child and its complement we preserving the total number of 
 * genes in the population, preventing any genetic drift.
 * Single-point crossover is the classic form of crossover. However, it produces
 * very slow mixing compared with multi-point crossover or uniform crossover. 
 * For problems where the site position has some intrinsic meaning to the 
 * problem single-point crossover can lead to small disruption than multi-point 
 * or uniform crossover.
 * </p>
 * 
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @version $Id$
 */
public class SinglePointCrossover<G extends Gene<?, G>> extends Crossover<G> {
	private static final long serialVersionUID = 1L;

	/**
	 * Create a new single point crossover object with crossover probability of
	 * {@code 0.05}.
	 */
	public SinglePointCrossover() {
		this(0.05);
	}
	
	/**
	 * Constructs an alterer with a given recombination probability.
	 * 
	 * @param probability the crossover probability.
	 * @throws IllegalArgumentException if the {@code probability} is not in the
	 * 		  valid range of {@code [0, 1]}.
	 */
	public SinglePointCrossover(final double probability) {
		super(probability);
	}
	
	@Override
	protected void crossover(final Array<G> that, final Array<G> other) {
		final Random random = RandomRegistry.getRandom();
		final int index = random.nextInt(that.length());
		
		for (int j = 0; j <= index; ++j) {
			final G temp = that.get(j);
			that.set(j, other.get(j));
			other.set(j, temp);
		}
	}
	
}
