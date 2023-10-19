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

import java.time.Duration;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.wait.strategy.Wait;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;

@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
abstract class TimeSourceTests {

	static StreamAppContainer source;

	// "MM/dd/yy HH:mm:ss";
	private final static Pattern pattern = Pattern.compile(".*\\d{2}/\\d{2}/\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}");

	@Autowired
	private OutputMatcher outputMatcher;

	@Test
	void test() {
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.payloadMatches((String s) -> pattern.matcher(s).matches()));
	}

	@BeforeAll
	static void configureSource() {
		source = BaseContainerExtension.containerInstance()
			.withExposedPorts(8080)
			.withEnv("SPRING_CLOUD_STREAMAPP_SECURITY_ADMIN-PASSWORD", "password")
			.withEnv("SPRING_CLOUD_STREAMAPP_SECURITY_ADMIN-USER", "user")
			.withLogConsumer(appLog("time-source"))
			.waitingFor(Wait.forLogMessage(".*Started TimeSource.*", 1))
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);
		source.start();
	}

	@Test
	void testActuator() {
		WebClient webClient = WebClient.create();
		webClient.get()
				.uri("http://" + source.getHost() + ":" + source.getMappedPort(8080) + "/actuator/health")
				.headers(h -> h.setBasicAuth("user", "password"))
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.toBodilessEntity();
				}).block();
		webClient.get()
				.uri("http://" + source.getHost() + ":" + source.getMappedPort(8080) + "/actuator/info")
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.toBodilessEntity();
				}).block();
		webClient.get()
				.uri("http://" + source.getHost() + ":" + source.getMappedPort(8080) + "/actuator/bindings")
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.toBodilessEntity();
				}).block();
		webClient.get()
				.uri("http://" + source.getHost() + ":" + source.getMappedPort(8080) + "/actuator/env")
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					return response.toBodilessEntity();
				}).block();
	}

	@AfterEach
	void cleanUp() {
		outputMatcher.clearMessageMatchers();
	}

	@AfterAll
	static void stop() {
		source.stop();
	}
}
