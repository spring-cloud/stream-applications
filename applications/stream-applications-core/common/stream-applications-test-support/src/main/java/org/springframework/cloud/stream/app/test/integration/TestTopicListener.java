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
	 * Default Output Destination.
	 */
	String STREAM_APPLICATIONS_TEST_TOPIC = "stream-applications-test";

	/**
	 * Register a Message payload verifier verifier on the given destination.
	 * @param topic the destination for the output Message.
	 * @param outputVerifier a {code Predicate} to test the payload.
	 * @param <P> the expected payload type
	 * @return true if it is registered, false if it is already registered.
	 */
	<P> boolean addOutputPayloadVerifier(String topic, Predicate<P> outputVerifier);

	/**
	 * Register a Message payload verifier on the default destination.
	 * @param outputVerifier a {code Predicate} to test the payload.
	 * @param <P> the expected payload type
	 * @return true if it is registered, false if it is already registered.
	 */
	default <P> boolean addOutputPayloadVerifier(Predicate<P> outputVerifier) {
		return addOutputPayloadVerifier(STREAM_APPLICATIONS_TEST_TOPIC, outputVerifier);
	}

	/**
	 * Register a {@link Message} verifier on the given destination.
	 * @param topic the destination for the output Message.
	 * @param outputVerifier a {code Predicate} to test the payload.
	 * @return true if it is registered, false if it is already registered.
	 */
	boolean addOutputMessageVerifier(String topic, Predicate<Message<?>> outputVerifier);

	/**
	 * Register a Message payload verifier on the default destination.
	 * @param outputVerifier a {code Predicate} to test the payload.e
	 * @return true if it is registered, false if it is already registered.
	 */
	default boolean addOutputMessageVerifier(Predicate<Message<?>> outputVerifier) {
		return addOutputMessageVerifier(STREAM_APPLICATIONS_TEST_TOPIC, outputVerifier);
	}

	/**
	 * Remove all verifiers.
	 */
	void clearOutputVerifiers();

	/**
	 * Set all verifiers to the initial state.
	 */
	void resetOutputVerifiers();

	/**
	 * A method that may be polled to wait for all verifiers on a given destination to be
	 * satisfied.
	 * @param topic the destination.
	 * @return true if all verifiers are satisfied.
	 */
	AtomicBoolean isVerified(String topic);

	/**
	 * A method that may be polled to wait for all verifiers on the default destination to be
	 * satisfied.
	 * @return true if all verifiers are satisfied.
	 */
	default AtomicBoolean isVerified() {
		return isVerified(STREAM_APPLICATIONS_TEST_TOPIC);
	}

	/**
	 * A message listener to a topic and tests all verifiers on an incoming Message.
	 * @param message the Message.
	 */
	void listen(Message<?> message);
}
