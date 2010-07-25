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

import static java.lang.Math.round;

import java.util.Random;

import javolution.context.ObjectFactory;
import javolution.xml.XMLFormat;
import javolution.xml.stream.XMLStreamException;

import org.jenetics.util.RandomRegistry;
import org.jscience.mathematics.number.Integer64;

/**
 * @author <a href="mailto:franz.wilhelmstoetter@gmx.at">Franz Wilhelmstötter</a>
 * @version $Id$
 */
public class Integer64Gene 
	extends NumberGene<Integer64, Integer64Gene> 
{
	private static final long serialVersionUID = 1L;
	
	protected Integer64Gene() {
	}

	public Integer64Gene divide(final Integer64Gene gene) {
		return newInstance(_value.divide(gene._value));
	}
	
	/**
	 * Create a new, <em>random</em> chromosome.
	 */
	@Override
	public Integer64Gene newInstance() {
		final Random random = RandomRegistry.getRandom();
		final double rv = random.nextDouble();
		final long value = round(rv*(_max.longValue() - _min.longValue())) + 
										_min.longValue();
		return newInstance(value);
	}
	
	@Override
	public Integer64Gene newInstance(final java.lang.Number number) {
		return valueOf(Integer64.valueOf(number.longValue()), _min, _max);
	}
	
	/**
	 * Create a new IntegerGene with the same limits and the given value.
	 * 
	 * @param number The value of the new NumberGene.
	 * @return The new NumberGene.
	 * @throws IllegalArgumentException if the gene value is not in the range
	 *		(value < min || value > max).
	 */
	public Integer64Gene newInstance(final long number) {
		return newInstance(Integer64.valueOf(number));
	}

	@Override
	public Integer64Gene mean(final Integer64Gene that) {
		return newInstance((_value.longValue() + that._value.longValue()) >>> 1);
	}
	
	
	private static final ObjectFactory<Integer64Gene> FACTORY = 
	new ObjectFactory<Integer64Gene>() {
		@Override protected Integer64Gene create() {
			return new Integer64Gene();
		}
	};
	
	/**
	 * Create a new random IntegerGene with the given value and the given range. 
	 * If the {@code value} isn't within the closed interval [min, max], no 
	 * exception is thrown. In this case the method {@link Integer64Gene#isValid()} 
	 * returns {@code false}.
	 * 
	 * @param value the value of the IntegerGene.
	 * @param min the minimal valid value of this IntegerGene.
	 * @param max the maximal valid value of this IntegerGene.
	 * @return the new created IntegerGene with the given {@code value}.
	 * @throws NullPointerException if one of the arguments is {@code null}.
	 */
	public static Integer64Gene valueOf(
		final Integer64 value, 
		final Integer64 min, 
		final Integer64 max
	) {
		Integer64Gene gene = FACTORY.object();
		gene.set(value, min, max);
		return gene;
	}
	
	/**
	 * Create a new random IntegerGene with the given value and the given range. 
	 * If the {@code value} isn't within the closed interval [min, max], no 
	 * exception is thrown. In this case the method {@link Integer64Gene#isValid()} 
	 * returns {@code false}.
	 * 
	 * @param value the value of the IntegerGene.
	 * @param min the minimal valid value of this IntegerGene.
	 * @param max the maximal valid value of this IntegerGene.
	 * @return the new created IntegerGene with the given {@code value}.
	 */
	public static Integer64Gene valueOf(long value, long min, long max) {
		return valueOf(
			Integer64.valueOf(value), 
			Integer64.valueOf(min), 
			Integer64.valueOf(max)
		);
	}
	
	/**
	 * Create a new random IntegerGene. It is guaranteed that the value of the 
	 * IntegerGene lies in the closed interval [min, max].
	 * 
	 * @param min the minimal value of the IntegerGene to create.
	 * @param max the maximal value of the IntegerGene to create.
	 * @return the new created Integer gene.
	 * @throws NullPointerException if one of the arguments is {@code null}.
	 */
	public static Integer64Gene valueOf(final Integer64 min, final Integer64 max) {
		final Random random = RandomRegistry.getRandom();
		final int value = min.intValue() + 
						random.nextInt(max.intValue() - min.intValue() + 1);
		return valueOf(Integer64.valueOf(value), min, max);
	}
	
	/**
	 * Create a new random IntegerGene. It is guaranteed that the value of the 
	 * IntegerGene lies in the closed interval [min, max].
	 * 
	 * @param min the minimal value of the IntegerGene to create.
	 * @param max the maximal value of the IntegerGene to create.
	 * @return the new created Integer gene.
	 */
	public static Integer64Gene valueOf(final int min, final int max) {
		return valueOf(Integer64.valueOf(min), Integer64.valueOf(max));
	}
	
	static final XMLFormat<Integer64Gene> 
	XML = new XMLFormat<Integer64Gene>(Integer64Gene.class) 
	{
		private static final String MIN = "min";
		private static final String MAX = "max";
		
		@Override
		public Integer64Gene newInstance(
			final Class<Integer64Gene> cls, final InputElement element
		)
			throws XMLStreamException
		{
			final long min = element.getAttribute(MIN, 0);
			final long max = element.getAttribute(MAX, 100);
			final long value = element.<Long>getNext();
			return Integer64Gene.valueOf(value, min, max);
		}
		@Override
		public void write(final Integer64Gene gene, final OutputElement element) 
			throws XMLStreamException 
		{
			element.setAttribute(MIN, gene.getMin().longValue());
			element.setAttribute(MAX, gene.getMax().longValue());
			element.add(gene.getAllele().longValue());
		}
		@Override
		public void read(final InputElement element, final Integer64Gene gene) 
			throws XMLStreamException 
		{
		}
	};
	
}






