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

package org.springframework.cloud.stream.app.source.twitter.message;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.cloud.fn.supplier.twitter.message.TwitterMessageSupplierConfiguration;
import org.springframework.cloud.fn.supplier.twitter.message.TwitterMessageSupplierProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 */
public class TwitterMessageSourceIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = SocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest messageRequest;


	@BeforeAll
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		messageRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/direct_messages/events/list.json")
				.withQueryStringParameter("count", "15"));
	}

	@AfterAll
	public static void stopServer() {
		mockServer.stop();
	}

	@Test
	public void twitterMessageSourceTests() throws JsonProcessingException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TestTwitterMessageSourceApplication.class))

				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterMessageSupplier",

						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666",

						"--twitter.message.source.count=15",
						"--spring.cloud.stream.poller.fixed-delay=3000")) {

			TwitterConnectionProperties twitterConnectionProperties = context.getBean(TwitterConnectionProperties.class);
			assertThat(twitterConnectionProperties.getConsumerKey()).isEqualTo("consumerKey666");
			assertThat(twitterConnectionProperties.getConsumerSecret()).isEqualTo("consumerSecret666");
			assertThat(twitterConnectionProperties.getAccessToken()).isEqualTo("accessToken666");
			assertThat(twitterConnectionProperties.getAccessTokenSecret()).isEqualTo("accessTokenSecret666");

//			DefaultPollerProperties defaultPollerProperties = context.getBean(DefaultPollerProperties.class);
//			assertThat(defaultPollerProperties.getFixedDelay()).isEqualTo(3000);

			TwitterMessageSupplierProperties twitterMessageSupplierProperties = context.getBean(TwitterMessageSupplierProperties.class);
			assertThat(twitterMessageSupplierProperties.getCount()).isEqualTo(15);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(300).toMillis(), "twitterMessageSupplier-out-0");
			assertThat(message).isNotNull();
			String payload = new String(message.getPayload());

			List tweets = new ObjectMapper().readValue(payload, List.class);
			assertThat(tweets).hasSize(4);
			mockClient.verify(messageRequest, once());
		}
	}

	private static HttpRequest setExpectation(HttpRequest request) {
		mockClient.when(request, exactly(1))
				.respond(
						response()
								.withStatusCode(200)
								.withHeaders(
										new Header("Content-Type", "application/json; charset=utf-8"),
										new Header("Cache-Control", "public, max-age=86400"))
								.withBody(TwitterTestUtils.asString("classpath:/response/messages.json"))
								.withDelay(TimeUnit.SECONDS, 10));
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterMessageSupplierConfiguration.class)
	public static class TestTwitterMessageSourceApplication {

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
