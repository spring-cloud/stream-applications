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

package org.springframework.cloud.fn.supplier.twitter.status.stream;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"twitter.connection.consumerKey=consumerKey666",
				"twitter.connection.consumerSecret=consumerSecret666",
				"twitter.connection.accessToken=accessToken666",
				"twitter.connection.accessTokenSecret=accessTokenSecret666"
		})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TwitterStreamSupplierTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = SocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest streamFilterRequest;
	private static HttpRequest streamSampleRequest;
	private static HttpRequest streamFirehoseRequest;

	@Autowired
	protected Supplier<Flux<Message<?>>> twitterStreamSupplier;

	@BeforeClass
	public static void startServer() {

		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);

		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		streamFilterRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/filter.json")
				.withBody(new StringBody("count=0&track=Java%2CPython&stall_warnings=true")));

		streamSampleRequest = mockClientRecordRequest(request()
				.withMethod("GET")
				.withPath("/stream/statuses/sample.json"));

		streamFirehoseRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/links.json")
				.withBody(new StringBody("count=0&stall_warnings=true")));

		streamFirehoseRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/firehose.json")
				.withBody(new StringBody("count=0&stall_warnings=true")));
	}

	@AfterClass
	public static void stopServer() {
		mockServer.stop();
	}

	private static HttpRequest mockClientRecordRequest(HttpRequest request) {
		mockClient.when(request, /*unlimited())*/ exactly(1))
				.respond(
						response()
								.withStatusCode(200)
								.withHeaders(
										new Header("Content-Type", "application/json; charset=utf-8"),
										new Header("Cache-Control", "public, max-age=86400"))
								.withBody(TwitterTestUtils.asString("classpath:/response/stream_test_1.json"))
								.withDelay(TimeUnit.SECONDS, 10));
		return request;
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=sample"
	})
	public static class TwitterStreamSampleTests extends TwitterStreamSupplierTests {

		@Test
		public void testOne() {
			final Flux<Message<?>> messageFlux = twitterStreamSupplier.get();

			final StepVerifier stepVerifier = StepVerifier.create(messageFlux)
					.assertNext((message) -> assertThat(new String((byte[]) message.getPayload()))
							.contains("\"id\":1075751718749659136"))
					.thenCancel()
					.verifyLater();

			stepVerifier.verify();

			mockClient.verify(streamSampleRequest, once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=filter",
			"twitter.stream.filter.track=Java,Python"
	})
	public static class TwitterStreamFilterTests extends TwitterStreamSupplierTests {

		@Test
		public void testOne() {
			final Flux<Message<?>> messageFlux = twitterStreamSupplier.get();

			final StepVerifier stepVerifier = StepVerifier.create(messageFlux)
					.assertNext((message) -> assertThat(new String((byte[]) message.getPayload()))
							.contains("\"id\":1075751718749659136"))
					.thenCancel()
					.verifyLater();

			stepVerifier.verify();

			mockClient.verify(streamFilterRequest, once());
		}
	}

	@TestPropertySource(properties = {
			"twitter.stream.type=firehose"
	})
	public static class TwitterStreamFirehoseTests extends TwitterStreamSupplierTests {

		@Test
		public void testOne() {
			final Flux<Message<?>> messageFlux = twitterStreamSupplier.get();

			final StepVerifier stepVerifier = StepVerifier.create(messageFlux)
					.assertNext((message) -> assertThat(new String((byte[]) message.getPayload()))
							.contains("\"id\":1075751718749659136"))
					.thenCancel()
					.verifyLater();

			stepVerifier.verify();

			mockClient.verify(streamFirehoseRequest, once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterStreamSupplierConfiguration.class)
	public static class TwitterStreamSupplierTestApplication {

		@Bean
		@Primary
		public twitter4j.conf.Configuration twitterConfiguration2(TwitterConnectionProperties properties,
				Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder) {

			Function<TwitterConnectionProperties, ConfigurationBuilder> mockedConfiguration =
					toConfigurationBuilder.andThen(
							new TwitterTestUtils().mockTwitterUrls(
									String.format("http://%s:%s", MOCK_SERVER_IP, MOCK_SERVER_PORT)));

			return mockedConfiguration.apply(properties).build();
		}
	}
}
