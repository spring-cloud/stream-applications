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

package org.springframework.cloud.stream.app.integration.test.processor.httprequest;

import java.io.IOException;
import java.net.InetAddress;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.app.test.integration.OutputMatcher;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainer;
import org.springframework.cloud.stream.app.test.integration.StreamAppContainerTestUtils;
import org.springframework.cloud.stream.app.test.integration.TestTopicSender;
import org.springframework.http.HttpStatus;

import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.integration.test.common.Configuration.DEFAULT_DURATION;
import static org.springframework.cloud.stream.app.test.integration.AppLog.appLog;

@Tag("integration")
abstract class HttpRequestProcessorTests {
	private final static Logger logger = LoggerFactory.getLogger(HttpRequestProcessorTests.class);
	private static MockWebServer server;

	private static int serverPort;

	@Autowired
	private TestTopicSender testTopicSender;

	@Autowired
	private OutputMatcher outputMatcher;

	private static StreamAppContainer processor;

	protected static StreamAppContainer configureProcessor(StreamAppContainer baseContainer) {
		serverPort = StreamAppContainerTestUtils.findAvailablePort();
		processor = baseContainer.withLogConsumer(appLog("http-request-processor"))
				.withEnv("HTTP_REQUEST_URL_EXPRESSION",
						"'http://" + StreamAppContainerTestUtils.localHostAddress() + ":" + serverPort + "'")
				.withEnv("HTTP_REQUEST_HTTP_METHOD_EXPRESSION", "'POST'");
		return processor;
	}

	@BeforeAll
	static void startServer() throws Exception {
		server = new MockWebServer();
		server.start(InetAddress.getLocalHost(), serverPort);
	}

	@Test
	void get() {
		server.setDispatcher(new Dispatcher() {
			@Override
			public MockResponse dispatch(RecordedRequest recordedRequest) {
				return new MockResponse()
						.setBody("{\"response\":\"" + recordedRequest.getBody().readUtf8() + "\"}")
						.setResponseCode(HttpStatus.OK.value());
			}
		});
		testTopicSender.send(processor.getInputDestination(), "ping");
		await().atMost(DEFAULT_DURATION)
				.until(outputMatcher.messageMatches(message -> {
					Object payload = message.getPayload();
					logger.info("payload:{}", payload);
					return payload.equals("{\"response\":\"ping\"}");
				}));
		        // See https://github.com/spring-cloud/spring-cloud-stream/issues/2190 .This condition is no longer true.
				//		&& message.getHeaders().get(MessageHeaders.CONTENT_TYPE)
				//				.equals(MediaType.APPLICATION_JSON_VALUE)));
	}

	@AfterAll
	static void cleanUp() throws IOException {
		server.shutdown();
	}

}
