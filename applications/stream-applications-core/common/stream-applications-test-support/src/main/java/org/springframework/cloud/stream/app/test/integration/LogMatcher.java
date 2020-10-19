/*
 * Copyright 2020-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.app.test.integration;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;

import org.springframework.util.Assert;

/**
 * Utility for matching test container log contents using Awaitility. Example:
 * {@code await().until(logMatcher.matches();}
 * @author David Turanski
 */
public class LogMatcher implements Consumer<OutputFrame> {
	private static Logger logger = LoggerFactory.getLogger(LogMatcher.class);

	protected AtomicBoolean matched = new AtomicBoolean();

	private Pattern pattern;

	private LogMatcher() {
	}

	public Callable<Boolean> matches() {
		return () -> matched.get();
	}

	private LogMatcher(Pattern pattern) {
		this.pattern = pattern;
	}

	public static LogMatcher contains(String string) {
		return LogMatcher.matchesRegex(".*" + string + ".*");
	}

	public static LogMatcher endsWith(String string) {
		return LogMatcher.matchesRegex(".*" + string);
	}

	public static LogMatcher matchesRegex(String regex) {
		return new LogMatcher(Pattern.compile(regex));
	}

	public LogMatcher times(int times) {
		return new CountingLogMatcher(this.pattern, times);
	}

	@Override
	public void accept(OutputFrame outputFrame) {
		synchronized (matched) {
			if (!matched.get()) {
				String str = outputFrame.getUtf8String().trim();
				logger.trace("matching {} using pattern {}", str, pattern.pattern());
				if (pattern.matcher(str).matches()) {
					matched.set(true);
					logger.debug(" MATCHED {}", str);
				}
			}
		}
	}

	public final static class CountingLogMatcher extends LogMatcher {

		private final AtomicInteger count = new AtomicInteger();

		private CountingLogMatcher(Pattern pattern, int count) {
			super(pattern);
			Assert.isTrue(count >= 1, "'count' must be greater than 0");
			this.count.set(count);
		}

		@Override
		public void accept(OutputFrame outputFrame) {
			if (count.get() > 0) {
				super.accept(outputFrame);
				if (matched.compareAndSet(true, false)) {
					count.decrementAndGet();
				}
			}
		}

		@Override
		public Callable<Boolean> matches() {
			return () -> count.get() == 0;
		}
	}
}
