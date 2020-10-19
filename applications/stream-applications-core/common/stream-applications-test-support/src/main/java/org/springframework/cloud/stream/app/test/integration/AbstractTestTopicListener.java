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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.Message;

/**
 * The base class for
 * {@link org.springframework.cloud.stream.app.test.integration.TestTopicListener}s.
 * Registers and tests verifiers on incoming messages. Subclasses delegate to this
 * listener.
 * @author David Turanski
 */
public abstract class AbstractTestTopicListener implements TestTopicListener {

	protected final Map<String, List<Verifier>> verifiers = new ConcurrentHashMap<>();

	protected final Logger logger = LoggerFactory.getLogger(this.getClass());

	@Override
	public <T> boolean addOutputPayloadVerifier(String topic, Predicate<T> verifier) {
		Predicate<Message<?>> messagePredicate = message -> verifier.test((T) message.getPayload());
		return addOutputMessageVerifier(topic, messagePredicate);
	}

	@Override
	public boolean addOutputMessageVerifier(String topic, Predicate<Message<?>> verifier) {
		AtomicBoolean isNewTopic = new AtomicBoolean(!verifiers.containsKey(topic));

		if (isRegisteredOutputVerifier(topic, verifier)) {
			return false;
		}

		if (isNewTopic.get()) {
			logger.trace("Listener is consuming from topic {}", topic);
		}

		logger.trace("Setting new output verifier on topic {}", topic);
		verifiers.putIfAbsent(STREAM_APPLICATIONS_TEST_TOPIC, new LinkedList<>());
		verifiers.get(STREAM_APPLICATIONS_TEST_TOPIC).add(new Verifier(verifier));
		logger.trace("There are {} output verifiers on topic {}", verifiers.get(STREAM_APPLICATIONS_TEST_TOPIC).size(),
				topic);
		return true;
	}

	private boolean isRegisteredOutputVerifier(String topic, Predicate<Message<?>> newOutputVerifier) {
		if (!verifiers.containsKey(topic)) {
			return false;
		}
		AtomicBoolean registered = new AtomicBoolean(false);
		verifiers.get(topic).forEach(verifier -> {
			if (newOutputVerifier.equals(verifier)) {
				logger.debug("This verifier is already registered on topic {}", topic);
				registered.set(true);
				return;
			}
		});
		return registered.get();
	}

	@Override
	public AtomicBoolean isVerified(String topic) {
		AtomicBoolean all = new AtomicBoolean(true);
		if (verifiers.containsKey(topic)) {
			verifiers.get(topic).forEach(v -> all.compareAndSet(true, v.isSatisfied()));
		}

		logger.trace("Verified topic {} is {}", topic, all.get());
		return all;
	}

	@Override
	public void clearOutputVerifiers() {
		verifiers.clear();
	}

	@Override
	public void resetOutputVerifiers() {
		verifiers.values().forEach((List<Verifier> l) -> l.forEach(v -> v.setSatisfied(false)));
	}

	protected abstract Function<Message<?>, String> topicForMessage();

	@Override
	public void listen(Message<?> message) {
		String topic = topicForMessage().apply(message);
		logger.debug("Received message: {} on topic {}", message, topic);
		if (!verifiers.containsKey(topic)) {
			return;
		}

		logger.trace("Verifying message: {} on topic {}", message, topic);
		AtomicBoolean any = new AtomicBoolean(false);
		verifiers.get(topic).forEach(v -> {
			any.compareAndSet(false, v.test(message));
			v.setSatisfied(any.get());
		});
		if (any.get()) {
			logger.debug("Verified message: {} on topic {}", message, topic);
		}
	}

	protected final static class Verifier implements Predicate<Message<?>> {
		private final Predicate<Message<?>> predicate;

		private final AtomicBoolean satisfied;

		private Verifier(Predicate<Message<?>> predicate) {
			this.predicate = predicate;
			this.satisfied = new AtomicBoolean(false);
		}

		@Override
		public boolean test(Message<?> message) {
			return predicate.test(message);
		}

		public void setSatisfied(boolean value) {
			this.satisfied.compareAndSet(false, value);
		}

		public boolean isSatisfied() {
			return this.satisfied.get();
		}
	}
}
