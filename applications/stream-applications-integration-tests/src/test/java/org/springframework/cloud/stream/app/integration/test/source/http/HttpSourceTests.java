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

package org.springframework.cloud.stream.app.integration.test.source.http;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testcontainers.containers.wait.strategy.Wait;
import reactor.core.publisher.Mono;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.AppLog;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.junit.jupiter.BaseContainerExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
@Tag("integration")
@ExtendWith(BaseContainerExtension.class)
public abstract class HttpSourceTests {

	private static int serverPort = StreamAppContainerTestUtils.findAvailablePort();

	private static WebClient webClient = WebClient.builder().build();

	private static StreamAppContainer source;

	@BeforeAll
	static void configureSource() {
		source = BaseContainerExtension.containerInstance()
					.withEnv("SERVER_PORT", String.valueOf(serverPort))
					.withExposedPorts(serverPort)
					.waitingFor(Wait.forListeningPort()
					.withStartupTimeout(DEFAULT_DURATION));
		source.withLogConsumer(AppLog.appLog("http")).start();
	}

	@Autowired
	private OutputMatcher outputMatcher;

	@AfterEach
	void reset() {
		outputMatcher.clearMessageMatchers();
	}

	@Test
	void plaintext() throws InterruptedException {
		CountDownLatch countDownLatch = new CountDownLatch(1);
		AtomicReference<HttpStatusCode> httpStatus = new AtomicReference<>();
		webClient
				.post()
				.uri("http://localhost:" + source.getMappedPort(serverPort))
				.contentType(MediaType.valueOf("application/x-www-form-url-encoded"))
				.body(Mono.just("Hello".getBytes()), byte[].class)
				.exchange()
				.subscribe(r -> {
					httpStatus.set(r.statusCode());
					countDownLatch.countDown();
				});
		countDownLatch.await(30, TimeUnit.SECONDS);
		assertThat(httpStatus.get().is2xxSuccessful()).isTrue();
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.payloadMatches(s -> s.equals("Hello")));
	}

	@Test
	void json() throws InterruptedException {

		CountDownLatch countDownLatch = new CountDownLatch(1);
		webClient
				.post()
				.uri("http://localhost:" + source.getMappedPort(serverPort))
				.contentType(MediaType.APPLICATION_JSON)
				.body(Mono.just("{\"Hello\":\"world\"}"), String.class)
				.exchange()
				.subscribe(r -> {
					countDownLatch.countDown();
					assertThat(r.statusCode().is2xxSuccessful()).isTrue();
				});
		countDownLatch.await(30, TimeUnit.SECONDS);
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.payloadMatches(s -> s.equals("{\"Hello\":\"world\"}")));
	}

	@AfterAll
	static void cleanUp() {
		source.stop();
	}

}
