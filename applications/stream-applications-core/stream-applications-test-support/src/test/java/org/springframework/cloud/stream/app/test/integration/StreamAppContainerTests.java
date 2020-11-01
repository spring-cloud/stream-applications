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

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.TestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC;

public abstract class StreamAppContainerTests {

	@Autowired
	OutputMatcher outputMatcher;

	@Autowired
	TestTopicSender testTopicSender;

	@AfterEach
	void reset() {
		outputMatcher.clearMessageMatchers();
	}

	@Test
	void payloadVerifiers() {
		outputMatcher.addMessageMatcher(MessageMatcher.payloadMatcher((s -> s.equals("hello test1"))));
		outputMatcher.addMessageMatcher(MessageMatcher.payloadMatcher(s -> s.equals("hello test2")));
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test1");
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test2");
		await().atMost(Duration.ofSeconds(10))
				.until(outputMatcher.messagesMatch());
	}

	@Test
	void verifierOnTheFly() {
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test3");
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test4");
		await().atMost(Duration.ofSeconds(30))
				.until(outputMatcher.payloadMatches(s -> s.equals("hello test3"), s -> s.equals("hello test4")));
	}

	@Test
	void verifierOnTheFlyOutOfOrder() {
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test5");
		testTopicSender.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test6");
		await().atMost(Duration.ofSeconds(30))
				.until(outputMatcher.payloadMatches(s -> s.equals("hello test6"), s -> s.equals("hello test5")));

	}

}
