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

package org.springframework.cloud.stream.app.source.twitter.stream;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import org.mockserver.model.StringBody;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.cloud.fn.supplier.twitter.status.stream.TwitterStreamSupplierConfiguration;
import org.springframework.cloud.fn.supplier.twitter.status.stream.TwitterStreamSupplierProperties;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.TestSocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

public class TwitterStreamSourceTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest streamFilterRequest;
	private static HttpRequest streamSampleRequest;
	private static HttpRequest streamFirehoseRequest;
	private static HttpRequest streamLinknsRequest;

	@BeforeAll
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

		streamLinknsRequest = mockClientRecordRequest(request()
				.withMethod("GET")
				.withPath("/stream/statuses/links.json"));
		//.withBody(new StringBody("count=0&stall_warnings=true")));

		streamFirehoseRequest = mockClientRecordRequest(request()
				.withMethod("POST")
				.withPath("/stream/statuses/firehose.json")
				.withBody(new StringBody("count=0&stall_warnings=true")));
	}

	@AfterAll
	public static void stopServer() {
		mockServer.stop();
	}

	@Test
	@Disabled
	public void testSourceFromSupplier() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterStreamSourceApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterStreamSupplier",

						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666",

						"--twitter.stream.type=filter",
						"--twitter.stream.filter.track=Java,Python",
						"--twitter.stream.filter.count=3")) {

			TwitterConnectionProperties twitterConnectionProperties = context.getBean(TwitterConnectionProperties.class);
			assertThat(twitterConnectionProperties.getConsumerKey()).isEqualTo("consumerKey666");
			assertThat(twitterConnectionProperties.getConsumerSecret()).isEqualTo("consumerSecret666");
			assertThat(twitterConnectionProperties.getAccessToken()).isEqualTo("accessToken666");
			assertThat(twitterConnectionProperties.getAccessTokenSecret()).isEqualTo("accessTokenSecret666");

			TwitterStreamSupplierProperties twitterStreamSupplierProperties =
					context.getBean(TwitterStreamSupplierProperties.class);
			assertThat(twitterStreamSupplierProperties.getType()).isEqualTo(TwitterStreamSupplierProperties.StreamType.filter);
			assertThat(twitterStreamSupplierProperties.getFilter().getTrack()).contains("Java", "Python");

			//OutputDestination target = context.getBean(OutputDestination.class);
			//Message<byte[]> sourceMessage = target.receive(10000);
			//final String actual = new String(sourceMessage.getPayload());

			mockClient.verify(streamFilterRequest, once());
		}
	}

	private static HttpRequest mockClientRecordRequest(HttpRequest request) {
		mockClient.when(request, exactly(1))
				.respond(response()
						.withStatusCode(200)
						.withHeaders(
								new Header("Content-Type", "application/json; charset=utf-8"),
								new Header("Cache-Control", "public, max-age=86400"))
						.withBody(TwitterTestUtils.asString("classpath:/response/stream_test_1.json"))
						.withDelay(TimeUnit.SECONDS, 10));
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterStreamSupplierConfiguration.class)
	public static class TestTwitterStreamSourceApplication {

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
