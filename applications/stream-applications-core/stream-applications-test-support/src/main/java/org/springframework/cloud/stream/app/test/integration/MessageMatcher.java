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

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import jakarta.validation.constraints.NotNull;

import org.springframework.messaging.Message;

/**
 * Provided to a {@link junit.framework.TestListener} to verify output messages.
 * @author David Turanski
 */
public class MessageMatcher implements Predicate<Message<?>> {
	private final Predicate<Message<?>> predicate;

	private final AtomicBoolean satisfied;

	MessageMatcher(@NotNull Predicate<Message<?>> predicate) {
		this.predicate = predicate;
		this.satisfied = new AtomicBoolean(false);
	}

	@Override
	public boolean test(Message<?> m) {
		boolean result = predicate.test(m);
		setSatisfied(result);
		return result;
	}

	public void setSatisfied(boolean value) {
		this.satisfied.compareAndSet(false, value);
	}

	public boolean isSatisfied() {
		return this.satisfied.get();
	}

	Predicate<Message<?>> getPredicate() {
		if (this.predicate instanceof MessagePredicate) {
			return ((MessagePredicate) (predicate)).delegate;
		}
		return this.predicate;
	}

	public static <T> MessageMatcher payloadMatcher(Predicate<T> predicate) {
		return new MessageMatcher(new MessagePredicate(predicate));
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (getClass() != o.getClass()) {
			return this.getPredicate() == o;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		MessageMatcher messageMatcher = (MessageMatcher) o;
		return getPredicate().equals(messageMatcher.getPredicate());
	}

	@Override
	public int hashCode() {
		return Objects.hash(getPredicate());
	}

	static class MessagePredicate<T> implements Predicate<Message<T>> {

		private final Predicate<T> delegate;

		MessagePredicate(Predicate<T> predicate) {
			this.delegate = predicate;
		}

		private Predicate<T> getDelegate() {
			return this.delegate;
		}

		@Override
		public boolean test(Message<T> message) {
			return this.delegate.test(message.getPayload());
		}
	}
}
