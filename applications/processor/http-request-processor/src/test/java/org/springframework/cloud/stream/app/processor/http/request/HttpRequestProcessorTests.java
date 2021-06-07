/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.processor.http.request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.http.request.HttpRequestFunctionConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpRequestProcessorTests {

	private static MockWebServer server;

	private ApplicationContextRunner applicationContextRunner;

	@BeforeEach
	void setup() {
		applicationContextRunner = new ApplicationContextRunner().withUserConfiguration(
				TestChannelBinderConfiguration.getCompleteConfiguration(HttpRequestProcessorApp.class));
	}

	@BeforeAll
	static void startServer() {
		server = new MockWebServer();
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse()
						.setBody(recordedRequest.getBody())
						.setResponseCode(HttpStatus.OK.value());
			}
		});
	}

	@AfterAll
	static void shutdownServer() throws IOException {
		server.shutdown();
	}

	private String url() {
		return String.format("http://localhost:%d", server.getPort());
	}

	@Test
	void requestUsingExpressions() {
		applicationContextRunner
				.withPropertyValues(
						"http.request.reply-expression=#root",
						"http.request.url-expression=headers['url']",
						"http.request.http-method-expression=headers['method']",
						"http.request.body-expression=headers['body']"
						)
				.run(context -> {
					Message<?> message = MessageBuilder.withPayload("")
							.setHeader("url", url())
							.setHeader("method", "POST")
							.setHeader("body", "{\"hello\":\"world\"}")
							.build();
					InputDestination inputDestination = context.getBean(InputDestination.class);
					OutputDestination outputDestination = context.getBean(OutputDestination.class);
					ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

					inputDestination.send(message);
					Message<byte[]> reply = outputDestination.receive(10000, "httpRequestFunction-out-0");

					// Cannot deserialize ResponseEntity directly.
					Map responseEntityAsMap = objectMapper.readValue(reply.getPayload(), HashMap.class);

					assertThat(responseEntityAsMap.get("statusCode")).isEqualTo("OK");
					assertThat(responseEntityAsMap.get("body")).isEqualTo(message.getHeaders().get("body"));
					assertThat(reply.getHeaders().get(MessageHeaders.CONTENT_TYPE))
							.isEqualTo(MediaType.APPLICATION_JSON.toString());
				});
	}

	@Test
	void requestUsingReturnType() {
		applicationContextRunner
				.withPropertyValues(
						"http.request.url-expression='" + url() + "'",
						"http.request.http-method-expression='POST'",
						"http.request.headers-expression={Accept:'application/octet-stream'}",
						"http.request.expected-response-type=byte[]",
						"spring.cloud.stream.bindings.httpRequestFunction-out-0.contentType=application/octet-stream")
				.run(context -> {
					Message<?> message = MessageBuilder.withPayload("hello")
							.build();
					InputDestination inputDestination = context.getBean(InputDestination.class);
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					inputDestination.send(message);
					Message<byte[]> reply = outputDestination.receive(10000, "httpRequestFunction-out-0");
					assertThat(new String(reply.getPayload())).isEqualTo(message.getPayload());
					assertThat(reply.getHeaders().get(MessageHeaders.CONTENT_TYPE))
							.isEqualTo(MediaType.APPLICATION_OCTET_STREAM.toString());
				});
	}

	@Test
	void requestUsingJsonPathMethodExpression() {
		applicationContextRunner
				.withPropertyValues(
						"http.request.url-expression='" + url() + "'",
						"http.request.http-method-expression=#jsonPath(payload,'$.myMethod')")
				.run(context -> {
					Message<?> message = MessageBuilder
							.withPayload("{\"name\":\"Fred\",\"age\":41, \"myMethod\":\"POST\"}")
							.build();
					InputDestination inputDestination = context.getBean(InputDestination.class);
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					inputDestination.send(message);
					Message<byte[]> reply = outputDestination.receive(10000, "httpRequestFunction-out-0");
					assertThat(new String(reply.getPayload())).isEqualTo(message.getPayload());
				});
	}

	@SpringBootApplication
	@Import(HttpRequestFunctionConfiguration.class)
	static class HttpRequestProcessorApp {
	}
}
