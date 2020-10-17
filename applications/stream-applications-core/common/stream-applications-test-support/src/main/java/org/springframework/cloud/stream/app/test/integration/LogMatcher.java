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

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.OutputFrame;

/**
 * Utility for matching test container log contents.
 * @author David Turanski
 */
public class LogMatcher implements Consumer<OutputFrame> {
	private static Logger logger = LoggerFactory.getLogger(LogMatcher.class);

	private List<Consumer<String>> listeners = new LinkedList<>();

	public Callable<Boolean> verifies(Consumer<LogListener> consumer) {
		LogListener logListener = new LogListener();
		consumer.accept(logListener);
		logListener.runnable.ifPresent(runnable -> runnable.run());
		listeners.add(logListener);
		return () -> logListener.matches().get();
	}

	@Override
	public void accept(OutputFrame outputFrame) {
		listeners.forEach(m -> m.accept(outputFrame.getUtf8String()));
	}

	public class LogListener implements Consumer<String> {
		private AtomicBoolean matched = new AtomicBoolean();

		private Optional<Runnable> runnable = Optional.empty();

		private Pattern pattern;

		@Override
		public void accept(String s) {
			logger.trace(this + "matching " + s.trim() + " using pattern " + pattern.pattern());
			if (pattern.matcher(s.trim()).matches()) {
				logger.debug(" MATCHED " + s.trim());
				matched.set(true);
				listeners.remove(this);
			}
		}

		public LogListener contains(String string) {
			return matchesRegex(".*" + string + ".*");
		}

		public LogListener endsWith(String string) {
			return matchesRegex(".*" + string);
		}

		public LogListener matchesRegex(String regex) {
			this.pattern = Pattern.compile(regex);
			return this;
		}

		public LogListener when(Runnable runnable) {
			this.runnable = Optional.of(runnable);
			return this;
		}

		public AtomicBoolean matches() {
			return matched;
		}
	}
}
