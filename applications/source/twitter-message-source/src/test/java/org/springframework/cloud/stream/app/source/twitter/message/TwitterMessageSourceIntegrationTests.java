/*
 * Copyright 2020-2024 the original author or authors.
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
import org.springframework.cloud.fn.supplier.twitter.message.TwitterMessageSupplierProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 */
public class TwitterMessageSourceIntegrationTests {

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	private static HttpRequest messageRequest;


	@BeforeAll
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer();
		mockClient = new MockServerClient("localhost", mockServer.getPort());

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
				.run("--spring.cloud.function.definition=twitterMessagesSupplier",

						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666",

						"--twitter.message.source.enabled=true",
						"--twitter.message.source.count=15")) {

			TwitterConnectionProperties twitterConnectionProperties = context.getBean(TwitterConnectionProperties.class);
			assertThat(twitterConnectionProperties.getConsumerKey()).isEqualTo("consumerKey666");
			assertThat(twitterConnectionProperties.getConsumerSecret()).isEqualTo("consumerSecret666");
			assertThat(twitterConnectionProperties.getAccessToken()).isEqualTo("accessToken666");
			assertThat(twitterConnectionProperties.getAccessTokenSecret()).isEqualTo("accessTokenSecret666");

			TwitterMessageSupplierProperties twitterMessageSupplierProperties = context.getBean(TwitterMessageSupplierProperties.class);
			assertThat(twitterMessageSupplierProperties.getCount()).isEqualTo(15);

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(10).toMillis(), "twitterMessagesSupplier-out-0");
			assertThat(message).isNotNull();
			String payload = new String(message.getPayload());

			List<?> tweets = new ObjectMapper().readValue(payload, List.class);
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
								.withBody(TwitterTestUtils.asString("classpath:/response/messages.json")));
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestTwitterMessageSourceApplication {

		@Bean
		@Primary
		public twitter4j.conf.Configuration twitterConfiguration2(TwitterConnectionProperties properties,
				Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder) {

			Function<TwitterConnectionProperties, ConfigurationBuilder> mockedConfiguration =
					toConfigurationBuilder.andThen(
							new TwitterTestUtils().mockTwitterUrls("http://localhost:" + mockServer.getPort()));

			return mockedConfiguration.apply(properties).build();
		}

	}

}
