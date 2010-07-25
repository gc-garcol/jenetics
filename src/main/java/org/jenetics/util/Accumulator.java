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
package org.jenetics.util;

/**
 * Interface for accumulating values of a given type. Here a usage example:
 * 
 * [code]
 * 	 final Accumulators.MinMax<Double> minMax = new Accumulators.MinMax<Double>();
 * 	 final Accumulators.Variance<Double> variance = new Accumulators.Variance<Double>();
 * 	 final Accumulators.Qunatile<Double> quantile = new Accumulators.Quantile<Double>();
 * 	 
 * 	 final List<Double> values = ...;
 * 	 Accumulators.accumulate(values, minMax, variance, quantile);
 * [/code]
 * 
 * @see Accumulators
 * 
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @version $Id$
 */
public interface Accumulator<T> {

	/**
	 * Accumulate the given value.
	 * 
	 * @param value the value to accumulate.
	 */
	public void accumulate(final T value);
	
}