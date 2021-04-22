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
package io.jenetics.incubator.util;

import static java.util.Objects.requireNonNull;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class TaskCompletionTest {

	static class TestRunToCompletion {
		private final TaskCompletion _completion;

		private final int _submits;
		private final Duration _timeout;

		private final Lock _lock = new ReentrantLock();
		private final Condition _finished = _lock.newCondition();

		final List<String> _tasks = new ArrayList<>();
		volatile int _id = 0;

		TestRunToCompletion(
			final Executor executor,
			final int size,
			final int submits,
			final Duration timeout
		) {
			_completion = new TaskCompletion(executor, size);
			_submits = submits;
			_timeout = requireNonNull(timeout);
		}

		TestRunToCompletion(final Executor executor, final int size, final int submits) {
			this(executor, size, submits, Duration.ofMinutes(2));
		}

		boolean task(final int id, final long pause, final String name)
			throws InterruptedException
		{
			println("Submit: " + name);

			return _completion.submit(() -> {
				println("Start: " + name);
				_lock.lock();
				try {
					pause(pause);
					_id = id;
					_tasks.add(name);
					_finished.signal();
				} finally {
					_lock.unlock();
					println("Finish: " + name);
				}
			}, _timeout);
		}

		private void println(final String msg) {
			//System.out.println(msg);
		}

		void await() throws InterruptedException {
			_lock.lock();
			try {
				while (_tasks.size() < _submits) {
					_finished.await();
				}
			} finally {
				_lock.unlock();
			}
		}


		static void pause(final long pause) {
			try {
				if (pause > 0) {
					Thread.sleep(pause);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new CancellationException(e.getMessage());
			}
		}
	}

	@Test(dataProvider = "parameters", timeOut = 5_000)
	public void sequential(final int completionSize, final int submits, final int pause)
		throws Exception
	{
		final var executor = ForkJoinPool.commonPool();
		final var test = new TestRunToCompletion(executor, completionSize, submits);
		for (int i = 0; i < submits; ++i) {
			final var name = "Task_" + i;
			Assert.assertTrue(test.task(0, pause, name));
		}

		test.await();

		Assert.assertEquals(test._tasks.size(), submits);
		for (int i = 0; i < submits; ++i) {
			final var name = "Task_" + i;
			Assert.assertEquals(test._tasks.get(i), name);
		}
	}

	@DataProvider
	public Object[][] parameters() {
		return new Object[][] {
			{1, 1, 0},
			{2, 1, 0},
			{1, 100, 0},
			{10, 100, 0},
			{10, 100, 1},
			{1, 100, 1},
			{10, 100, 0},
			{10, 300, 1},
			{3, 10, 0},
			{6, 150, 1}
		};
	}

	@Test(dataProvider = "parameters", timeOut = 5_000)
	public void asynchronous(final int completionSize, final int submits, final int pause)
		throws Exception
	{
		final int threadCount = 3;
		final int testCount = 3;

		final var executor = Executors.newFixedThreadPool(threadCount);
		final var tests = new TestRunToCompletion[testCount];
		for (int i = 0; i < testCount; ++i) {
			tests[i] = new TestRunToCompletion(executor, completionSize, submits);
		}

		for (int j = 0; j < testCount; ++j) {
			final var test = tests[j];

			for (int i = 0; i < submits; ++i) {
				final var name = "Task_" + j + "_" + i;
				Assert.assertTrue(test.task(0, pause, name));
			}
		}

		for (int i = 0; i < testCount; ++i) {
			tests[i].await();
		}
		executor.shutdown();

		for (int j = 0; j < testCount; ++j) {
			final var test = tests[j];
			Assert.assertEquals(test._tasks.size(), submits);

			for (int i = 0; i < submits; ++i) {
				final var name = "Task_" + j + "_" + i;
				Assert.assertEquals(test._tasks.get(i), name);
			}
		}
	}

	@Test(dataProvider = "parameters", timeOut = 5_000)
	public void raceCondition(final int completionSize, final int submits, final int pause)
		throws Exception
	{
		final int threadCount = 3;
		final int testCount = 3;

		final var executor = ForkJoinPool.commonPool();
		final var tests = new TestRunToCompletion[testCount];
		for (int i = 0; i < testCount; ++i) {
			tests[i] = new TestRunToCompletion(executor, completionSize, submits);
		}
		for (int i = 0; i < submits; ++i) {
			for (int j = 0; j < testCount; ++j) {
				final var test = tests[j];
				final var name = "Task_" + j + "_" + i;
				Assert.assertTrue(test.task((i + 1)*10, pause, name));
			}
		}

		for (int i = 0; i < testCount; ++i) {
			tests[i].await();
		}

		for (int j = 0; j < testCount; ++j) {
			final var test = tests[j];
			Assert.assertEquals(test._tasks.size(), submits);
			Assert.assertEquals(test._id, submits*10);

			for (int i = 0; i < submits; ++i) {
				final var name = "Task_" + j + "_" + i;
				Assert.assertEquals(test._tasks.get(i), name);
			}
		}
	}

	@Test(timeOut = 5_000)
	public void exceedingNumberOfTasks() throws Exception {
		final int threadCount = 3;
		final int completionSize = 4;
		final int submits = 100;
		final int pause = 1000;

		final var executor = Executors.newFixedThreadPool(threadCount);
		final var test = new TestRunToCompletion(
			executor,
			completionSize,
			submits,
			Duration.ofMillis(500)
		);

		for (int i = 0; i < submits; ++i) {
			final var name = "Task_" + i;
			final var submitted = test.task(0, pause, name);
			if (!submitted) {
				return;
			}
		}

		throw new IllegalStateException("Tasks must have been rejected.");
	}

}
