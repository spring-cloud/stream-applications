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
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

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
	void requestUsingExpressions() throws IOException {
		applicationContextRunner
				.withPropertyValues(
						"http.request.processor.url-expression=headers['url']",
						"http.request.processor.http-method-expression=headers['method']",
						"http.request.processor.body-expression=headers['body']",
						"http.request.processor.headers-expression={Accept:'application/json'}",
						"http.request.processor.reply-expression=#root")
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
					Message<byte[]> reply = outputDestination.receive(100);

					// Cannot deserialize ResponseEntity directly.
					Map responseEntityAsMap = objectMapper.readValue(reply.getPayload(), HashMap.class);

					System.out.println(responseEntityAsMap);

					assertThat(responseEntityAsMap.get("statusCode")).isEqualTo("OK");
					assertThat(responseEntityAsMap.get("body")).isEqualTo(message.getHeaders().get("body"));
					assertThat(reply.getHeaders().get(MessageHeaders.CONTENT_TYPE))
							.isEqualTo(MediaType.APPLICATION_JSON);
				});
	}

	@Test
	void requestUsingReturnType() throws IOException {
		applicationContextRunner
				.withPropertyValues(
						"http.request.processor.url=" + url(),
						"http.request.processor.httpMethod=POST",
						"http.request.processor.headers[Accept]=application/octet-stream",
						"http.request.processor.expectedResponseType=byte[]",
						"spring.cloud.stream.bindings.httpRequestProcessor-out-0.contentType=application/octet-stream")
				.run(context -> {
					Message<?> message = MessageBuilder.withPayload("hello")
							.build();
					InputDestination inputDestination = context.getBean(InputDestination.class);
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					inputDestination.send(message);
					Message<byte[]> reply = outputDestination.receive(100);
					assertThat(new String(reply.getPayload())).isEqualTo(message.getPayload());
					assertThat(reply.getHeaders().get(MessageHeaders.CONTENT_TYPE))
							.isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
				});
	}

	@Test
	void requestUsingJsonPathMethodExpression() throws IOException {
		applicationContextRunner
				.withPropertyValues(
						"http.request.processor.url=" + url(),
						"http.request.processor.httpMethodExpression=#jsonPath(payload,'$.myMethod')")
				.run(context -> {
					Message<?> message = MessageBuilder
							.withPayload("{\"name\":\"Fred\",\"age\":41, \"myMethod\":\"POST\"}")
							.build();
					InputDestination inputDestination = context.getBean(InputDestination.class);
					OutputDestination outputDestination = context.getBean(OutputDestination.class);

					inputDestination.send(message);
					Message<byte[]> reply = outputDestination.receive(100);
					assertThat(new String(reply.getPayload())).isEqualTo(message.getPayload());
				});
	}

	@Test
	void cannotSpecifyBothUrlandUrlExpression() {
		applicationContextRunner
				.withPropertyValues("http.request.processor.url=http://example.com",
						"http.request.processor.url-expression=headers['url']")
				.run(context -> {
					assertThatIllegalStateException().isThrownBy(() -> {
						context.start();
					});
				});
	}

	@Test
	void cannotSpecifyBothHttpMethosdandHttpMethodExpression() {
		applicationContextRunner
				.withPropertyValues("http.request.processor.http-method=POST",
						"http.request.processor.http-method-expression=headers['method']")
				.run(context -> {
					assertThatIllegalStateException().isThrownBy(() -> {
						context.start();
					});
				});
	}

	@Test
	void cannotSpecifyBothHeadersAndHeadersExpression() {
		applicationContextRunner
				.withPropertyValues("http.request.processor.headers[Content-Type]=application/json",
						"http.request.processor.headers-expression={'Content-Type': headers['content']}")
				.run(context -> {
					assertThatIllegalStateException().isThrownBy(() -> {
						context.start();
					});
				});
	}

	@SpringBootApplication
	static class HttpRequestProcessorApp {
	}
}
