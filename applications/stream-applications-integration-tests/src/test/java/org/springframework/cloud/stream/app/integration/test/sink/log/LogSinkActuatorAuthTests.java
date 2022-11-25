/*
 * Copyright 2022-2022 the original author or authors.
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

package org.springframework.cloud.stream.app.integration.test.sink.log;

import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;

import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
public abstract class LogSinkActuatorAuthTests {

	@Container
	private static StreamAppContainer sink = BaseContainerExtension.containerInstance()
		.withExposedPorts(8080)
		.withEnv("SPRING_CLOUD_STREAMAPP_SECURITY_ADMIN-PASSWORD", "password")
		.withEnv("SPRING_CLOUD_STREAMAPP_SECURITY_ADMIN-USER", "user")
		.withEnv("MANAGEMENT_ENDPOINTS_WEB_EXPOSURE_INCLUDE", "*")
		.waitingFor(Wait.forLogMessage(".*Started LogSink.*", 1))
		.withStartupTimeout(Duration.ofSeconds(120))
		.withStartupAttempts(3);

	@Test
	void testActuatorGetWithAdminAuth() {
		WebClient webClient = WebClient.create();
		webClient.get()
				.uri("http://localhost:" + sink.getMappedPort(8080) + "/actuator/health")
				.headers(h -> h.setBasicAuth("user", "password"))
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.toBodilessEntity();
				}).block();
		webClient.get()
				.uri("http://localhost:" + sink.getMappedPort(8080) + "/actuator/env")
				.headers(h -> h.setBasicAuth("user", "password"))
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.toBodilessEntity();
				}).block();
	}

	@Test
	void actuatorPostAnonymousShouldFail() {
		WebClient webClient = WebClient.create();
		webClient.post()
				.uri("http://localhost:" + sink.getMappedPort(8080) + "/actuator/bindings/input")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"state\":\"STOPPED\"}")
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
					return response.toBodilessEntity();
				}).block();
	}

	@Test
	void actuatorPostWithAdminAuth() {
		WebClient webClient = WebClient.create();
		webClient.post()
				.uri("http://localhost:" + sink.getMappedPort(8080) + "/actuator/bindings/input")
				.headers(h -> h.setBasicAuth("user", "password"))
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue("{\"state\":\"STOPPED\"}")
				.exchangeToMono(response -> {
					assertThat(response.statusCode().is2xxSuccessful()).isTrue();
					return response.toBodilessEntity();
				}).block();
		String state = webClient.get()
				.uri("http://localhost:" + sink.getMappedPort(8080) + "/actuator/bindings/input")
				.headers(h -> h.setBasicAuth("user", "password"))
				.exchangeToMono(response -> {
					assertThat(response.statusCode()).isEqualTo(HttpStatus.OK);
					return response.bodyToMono(String.class);
				}).block();
		assertThat(state).contains("\"state\":\"stopped\"");
	}
}
