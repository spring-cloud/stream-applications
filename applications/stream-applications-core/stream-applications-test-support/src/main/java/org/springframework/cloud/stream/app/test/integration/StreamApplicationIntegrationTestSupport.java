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

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import org.springframework.util.SocketUtils;

import static org.springframework.cloud.stream.app.test.integration.TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC;

/**
 * Support utility for stream application integration testing .
 * @author David Turanski
 */
@Testcontainers
@Component
public abstract class StreamApplicationIntegrationTestSupport {

	protected static final String DOCKER_ORG = "springcloudstream";

	@Autowired
	private AbstractTestTopicListener testListener;

	protected static String prePackagedStreamAppImageName(String appName, String binderName, String version) {
		return DOCKER_ORG + "/" + appName + "-" + binderName + ":" + version;
	}

	public static String localHostAddress() {
		try {
			return InetAddress.getLocalHost().getHostAddress();
		}
		catch (UnknownHostException e) {
			throw new IllegalStateException(e.getMessage(), e);
		}
	}

	protected static File resourceAsFile(String path) {
		try {
			return new ClassPathResource(path).getFile();
		}
		catch (IOException e) {
			throw new IllegalStateException("Unable to access resource " + path);
		}
	}

	protected static final int findAvailablePort() {
		return SocketUtils.findAvailableTcpPort(10000, 20000);
	}

	protected Callable<Boolean> messagesMatch() {
		return () -> testListener.allMatch().get();
	}

	protected <P> Callable<Boolean> payloadMatches(Predicate<P>... payloadMatchers) {
		return payloadMatches(STREAM_APPLICATIONS_TEST_TOPIC, payloadMatchers);
	}

	protected Callable<Boolean> messageMatches(Predicate<Message<?>>... messageMatchers) {
		return messageMatches(STREAM_APPLICATIONS_TEST_TOPIC, messageMatchers);
	}

	// TODO: Implement support for multiple topics.
	private <P> Callable<Boolean> payloadMatches(String topic, Predicate<P>... payloadMatchers) {
		for (Predicate<P> payloadMatcher : payloadMatchers) {
			MessageMatcher messageMatcher = MessageMatcher.payloadMatcher(payloadMatcher);
			testListener.addMessageMatcher(messageMatcher);
		}
		return () -> testListener.matches(topic, payloadMatchers).get();
	}

	private <P> Callable<Boolean> payloadMatches(Map<String, Predicate<P>> payloadMatcherMap) {
		List<Callable<Boolean>> aggregate = new LinkedList<>();
		payloadMatcherMap.forEach((topic, payloadMatcher) -> {
			MessageMatcher messageMatcher = MessageMatcher.payloadMatcher(payloadMatcher);
			testListener.addMessageMatcher(topic, messageMatcher);
			aggregate.add(() -> testListener.matches(topic, messageMatcher).get());
		});

		return () -> {
			AtomicBoolean all = new AtomicBoolean(true);
			aggregate.forEach(cb -> {
				try {
					all.compareAndSet(true, cb.call());
				}
				catch (Exception e) {
					throw new IllegalStateException(e.getMessage(), e);
				}
			});
			return all.get();
		};
	}

	private <T> FluentMap<String, Predicate<T>> topicPayloadMatchers() {
		return new FluentMap<>();
	}

	private Callable<Boolean> messageMatches(String topic, Predicate<Message<?>>... messageMatchers) {
		for (Predicate<Message<?>> messageMatcher : messageMatchers) {
			testListener.addMessageMatcher(topic, new MessageMatcher(messageMatcher));
		}
		return () -> testListener.matches(topic, messageMatchers).get();
	}
}
