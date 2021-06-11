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

package org.springframework.cloud.stream.app.integration.test.stream.tiktok;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.StreamApps;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.KafkaStreamAppTest;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaConfig;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.VERSION;
import static org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamApps.kafkaStreamApps;

@Tag("integration")
@KafkaStreamAppTest
public class KafkaTikTokTests {

	private static LogMatcher logMatcher = LogMatcher.matchesRegex(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}")
			.times(3);

	@Container
	private static final StreamApps streamApp = kafkaStreamApps(KafkaTikTokTests.class.getSimpleName(),
			KafkaConfig.kafka)
					.withSourceContainer(
							new KafkaStreamAppContainer(StreamAppContainerTestUtils.imageName(
									"time-source-kafka",
									VERSION)))
					.withSinkContainer(
							new KafkaStreamAppContainer(StreamAppContainerTestUtils.imageName(
									"log-sink-kafka",
									VERSION)).withLogConsumer(logMatcher)
											.log())
					.build();

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION).until(logMatcher.matches());
	}
}
