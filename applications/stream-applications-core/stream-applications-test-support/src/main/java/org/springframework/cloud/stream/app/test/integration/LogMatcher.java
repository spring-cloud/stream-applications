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

/**
 * Utility for matching test container log contents using Awaitility. Example:
 * {@code await().until(logMatcher.matches();}
 * @author David Turanski
 */
public final class LogMatcher implements Consumer<OutputFrame> {
	private static Logger logger = LoggerFactory.getLogger(LogMatcher.class);

	private AtomicBoolean matched = new AtomicBoolean();

	private AtomicInteger count;

	private Pattern pattern;

	private LogMatcher(Pattern pattern, int times) {
		count = new AtomicInteger(times);
		this.pattern = pattern;
	}

	public static LogMatcher contains(String string) {
		return LogMatcher.matchesRegex(".*" + string + ".*");
	}

	public static LogMatcher endsWith(String string) {
		return LogMatcher.matchesRegex(".*" + string);
	}

	public static LogMatcher matchesRegex(String regex) {
		return new LogMatcher(Pattern.compile(regex), 1);
	}

	public LogMatcher times(int times) {
		count.set(times);
		return this;
	}

	@Override
	public void accept(OutputFrame outputFrame) {
		if (count.get() > 0) {
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
			if (matched.compareAndSet(true, false)) {
				count.decrementAndGet();
			}
		}
	}

	public Callable<Boolean> matches() {
		return () -> count.get() == 0;
	}

}
