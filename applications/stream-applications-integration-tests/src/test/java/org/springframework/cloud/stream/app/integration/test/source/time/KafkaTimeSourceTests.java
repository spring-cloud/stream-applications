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

package org.springframework.cloud.stream.app.integration.test.source.time;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.KafkaStreamAppTest;
import org.springframework.cloud.stream.app.test.integration.kafka.KafkaStreamAppContainer;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.VERSION;

@KafkaStreamAppTest
class KafkaTimeSourceTests extends TimeSourceTests {

	@Container
	static StreamAppContainer source = new KafkaStreamAppContainer(StreamAppContainerTestUtils
			.imageName(StreamAppContainerTestUtils.SPRINGCLOUDSTREAM_REPOSITOTRY, "time-source-kafka", VERSION))
					.withCommand("--server.port", "8080")
					.withExposedPorts(8080);

	@Test
	void testActuator() {
		WebClient webClient = WebClient.create();
		ClientResponse response = webClient.get()
				.uri("http://localhost:" + source.getMappedPort(8080) + "/actuator/health").exchange().block();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
		response = webClient.get().uri("http://localhost:" + source.getMappedPort(8080) + "/actuator/info").exchange()
				.block();
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);

	}

}
