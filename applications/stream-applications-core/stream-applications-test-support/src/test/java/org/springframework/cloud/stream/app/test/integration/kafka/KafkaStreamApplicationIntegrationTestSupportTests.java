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

package org.springframework.cloud.stream.app.test.integration.kafka;

import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.TestTopicListener;
import org.springframework.kafka.core.KafkaTemplate;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.test.integration.AbstractTestTopicListener.STREAM_APPLICATIONS_TEST_TOPIC;

public class KafkaStreamApplicationIntegrationTestSupportTests extends KafkaStreamApplicationIntegrationTestSupport {

	@Autowired
	private KafkaTemplate kafkaTemplate;

	@Autowired
	private TestTopicListener testTopicListener;

	@AfterEach
	void reset() {
		testTopicListener.clearOutputVerifiers();
	}

	@Test
	void payloadVerifiers() {
		testTopicListener.addOutputPayloadVerifier((s -> s.equals("hello test1")));
		testTopicListener.addOutputPayloadVerifier((s -> s.equals("hello test2")));
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test1");
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test2");
		await().atMost(Duration.ofSeconds(10))
				.until(verifyOutputMessages());
	}

	@Test
	void verifierOnTheFly() {
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test3");
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test4");
		await().atMost(Duration.ofSeconds(30))
				.until(verifyOutputPayload((s -> s.equals("hello test3"))));
		await().atMost(Duration.ofSeconds(30))
				.until(verifyOutputPayload((s -> s.equals("hello test4"))));

	}

	@Test
	void verifierOnTheFlyOutOfOrder() {
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test5");
		kafkaTemplate.send(STREAM_APPLICATIONS_TEST_TOPIC, "hello test6");
		await().atMost(Duration.ofSeconds(30))
				.until(verifyOutputPayload((s -> s.equals("hello test6"))));
		await().atMost(Duration.ofSeconds(30))
				.until(verifyOutputPayload((s -> s.equals("hello test5"))));
	}
}
