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

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.springframework.messaging.Message;

/**
 * The contract for TestTopicListener implementations.
 *
 * @author David Turanski
 */
public interface TestTopicListener {

	/**
	 * Default Output topic.
	 */
	String STREAM_APPLICATIONS_TEST_TOPIC = "stream-applications-test";

	/**
	 * Register a {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}
	 * on the given topic.
	 * @param topic the topic for the output Message.
	 * @param messageMatcher a {code Predicate} to test the payload.
	 * @return true if it is registered, false if it is already registered.
	 */
	boolean addMessageMatcher(String topic, MessageMatcher messageMatcher);

	/**
	 * Register a {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}
	 * on the default topic.
	 * @param messageMatcher a {code Predicate} to test the payload.e
	 * @return true if it is registered, false if it is already registered.
	 */
	default boolean addMessageMatcher(MessageMatcher messageMatcher) {
		return addMessageMatcher(STREAM_APPLICATIONS_TEST_TOPIC, messageMatcher);
	}

	/**
	 * Remove all
	 * {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}s.
	 */
	void clearMessageMatchers();

	/**
	 * Set all {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}s
	 * to the initial state.
	 */
	void resetMessageMatchers();

	/**
	 * A method that may be polled to wait for all
	 * {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}s on a
	 * given topic to be satisfied.
	 * @param topic the topic.
	 * @return true if all message matchers are satisfied.
	 */
	AtomicBoolean matches(String topic, Predicate<?>... p);

	/**
	 * A method that may be polled to wait for all
	 * {@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}s on a
	 * given topic to be satisfied.
	 * @param topic the topic.
	 * @return true if all message matchers are satisfied.
	 */
	AtomicBoolean allMatch(String topic);

	/**
	 * A method that may be polled to wait for
	 * all{@link org.springframework.cloud.stream.app.test.integration.MessageMatcher}s on the
	 * default topic to be satisfied.
	 * @return true if all message matchers are satisfied.
	 */
	default AtomicBoolean allMatch() {
		return allMatch(STREAM_APPLICATIONS_TEST_TOPIC);
	}

	/**
	 * A message listener to a topic and tests all message matchers on an incoming Message.
	 * @param message the Message.
	 */
	void listen(Message<?> message);
}
