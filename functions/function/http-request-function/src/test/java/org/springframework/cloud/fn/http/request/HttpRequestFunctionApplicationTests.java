/*
 * Copyright 2018-2020 the original author or authors.
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

package org.springframework.cloud.fn.http.request;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.http.request.HttpRequestFunctionConfiguration.HttpRequestFunction;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestFunctionApplicationTests {
	private MockWebServer server;

	private ApplicationContextRunner runner;

	@BeforeEach
	void setup() {
		this.server = new MockWebServer();
		this.runner = new ApplicationContextRunner()
				.withUserConfiguration(HttpRequestFunctionApplication.class)
				.withPropertyValues(
						"http.request.url=" + url());
	}

	@AfterEach
	void shutdown() throws IOException {
		this.server.shutdown();
	}

	@Test
	void shouldReturnString() {

		server.enqueue(new MockResponse()
				.setResponseCode(HttpStatus.OK.value())
				.setBody("hello"));

		runner.run(context -> {
			HttpRequestFunction httpRequestFunction = context.getBean(HttpRequestFunction.class);
			Message<?> message = MessageBuilder.withPayload("").build();
			StepVerifier.create(httpRequestFunction.apply(Flux.just(message)))
					.assertNext((ResponseEntity r) -> {
						assertThat(r.getBody()).isEqualTo("hello");
						assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
					})
					.expectComplete()
					.verify();
		});
	}

	@Test
	void shouldPostJson() {

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE,
						recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE))
						.setBody(recordedRequest.getBody())
						.setResponseCode(HttpStatus.CREATED.value());
			}
		});

		runner.withPropertyValues("http.request.http-method=POST", "http.request.headers[Content-Type]=application/json").run(context -> {
			HttpRequestFunction httpRequestFunction = context.getBean(HttpRequestFunction.class);
			String json = "{\"hello\":\"world\"}";
			Message<?> message = MessageBuilder.withPayload(json)
					.build();
			StepVerifier.create(httpRequestFunction.apply(Flux.just(message)))
					.assertNext((ResponseEntity r) -> {
						assertThat(r.getBody()).isEqualTo(json);
						assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
						assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
					})
					.expectComplete()
					.verify();
			RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
			assertThat(request.getMethod()).isEqualTo("POST");
		});
	}

	@Test
	void shouldPostPojoAsJson() {

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE,
						recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE))
						.setBody(recordedRequest.getBody())
						.setResponseCode(HttpStatus.CREATED.value());
			}
		});

		runner.withPropertyValues("http.request.http-method=POST",
				"http.request.headers[Content-Type]=application/json",
				"http.request.expected-response-type=" + Map.class.getName()).run(context -> {
					HttpRequestFunction httpRequestFunction = context.getBean(HttpRequestFunction.class);
					Map<String, String> json = Collections.singletonMap("hello", "world");
					Message<?> message = MessageBuilder.withPayload(json)
							.build();
					StepVerifier.create(httpRequestFunction.apply(Flux.just(message)))
							.assertNext((ResponseEntity r) -> {
								assertThat(r.getBody()).isEqualTo(json);
								assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
								assertThat(r.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
							})
							.expectComplete()
							.verify();
					RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
					assertThat(request.getMethod()).isEqualTo("POST");
				});
	}

	@Test
	void shouldDelete() {

		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse().setHeader(HttpHeaders.CONTENT_TYPE,
						recordedRequest.getHeader(HttpHeaders.CONTENT_TYPE))
						.setBody(recordedRequest.getBody())
						.setResponseCode(HttpStatus.ACCEPTED.value());
			}
		});

		runner.withPropertyValues("http.request.http-method=DELETE",
				"http.request.expected-response-type=" + Void.class.getName()).run(context -> {
					HttpRequestFunction httpRequestFunction = context.getBean(HttpRequestFunction.class);
					Message<?> message = MessageBuilder.withPayload("")
							.build();
					StepVerifier.create(httpRequestFunction.apply(Flux.just(message)))
							.assertNext((ResponseEntity r) -> {
								assertThat(r.getBody()).isNull();
								assertThat(r.getStatusCode().is2xxSuccessful()).isTrue();
							})
							.expectComplete()
							.verify();
					RecordedRequest request = server.takeRequest(100, TimeUnit.MILLISECONDS);
					assertThat(request.getMethod()).isEqualTo("DELETE");
				});
	}

	@Test
	void shouldThrowErrorIfCannotConnect() throws IOException {
		server.shutdown();
		runner.run(context -> {
			HttpRequestFunction httpRequestFunction = context.getBean(HttpRequestFunction.class);
			StepVerifier.create(httpRequestFunction.apply(Flux.just(new GenericMessage(""))))
					.expectErrorMatches(throwable -> throwable.getMessage().contains("Connection refused"))
					.verify();
		});
	}

	private String url() {
		return String.format("http://localhost:%d", server.getPort());
	}

	@SpringBootApplication
	static class HttpRequestFunctionApplication {
	}
}
