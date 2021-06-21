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

package org.springframework.cloud.stream.app.processor.twitter.trend;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.cloud.fn.twitter.trend.TwitterTrendFunctionConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.SocketUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 */
public class TwitterTrendLocationProcessorIntegrationTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = SocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest availableTrendsRequest;
	private static HttpRequest closestTrendsRequest;

	@BeforeEach
	public void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		availableTrendsRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/trends/available.json"));

		closestTrendsRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/trends/closest.json")
				.withQueryStringParameter("lat", "52.379189")
				.withQueryStringParameter("long", "4.899431"));
	}

	@AfterEach
	public void stopServer() {
		mockServer.stop();
		while (!mockServer.hasStopped(3,100L, TimeUnit.MILLISECONDS)){}
	}

	@Test
	public void testTwitterAvailableTrends() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterTrendLocationProcessorApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterTrendFunction",

						"--twitter.trend.trendQueryType=trendLocation",
						"--twitter.connection.rawJson=false",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination input = context.getBean(InputDestination.class);
			OutputDestination output = context.getBean(OutputDestination.class);
			assertThat(input).isNotNull();
			assertThat(output).isNotNull();

			input.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8)));

			Message<byte[]> outputMessage = output.receive(Duration.ofSeconds(300).toMillis(), "twitterTrendFunction-out-0");
			assertThat(outputMessage).isNotNull();

			mockClient.verify(availableTrendsRequest, once());
			assertThat(outputMessage);

			String payload = new String(outputMessage.getPayload());
			assertThat(payload).containsSequence("countryName");
			assertThat(payload).contains("placeCode");
			assertThat(payload).doesNotContain("placeType");
		}
	}

	@Test
	public void testTwitterAvailableTrendsTwitterJson() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterTrendLocationProcessorApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterTrendFunction",

						"--twitter.trend.trendQueryType=trendLocation",
						"--twitter.connection.rawJson=true",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination input = context.getBean(InputDestination.class);
			OutputDestination output = context.getBean(OutputDestination.class);
			assertThat(input).isNotNull();
			assertThat(output).isNotNull();

			input.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8)));

			Message<byte[]> outputMessage = output.receive(Duration.ofSeconds(300).toMillis(), "twitterTrendFunction-out-0");
			assertThat(outputMessage).isNotNull();

			mockClient.verify(availableTrendsRequest, once());
			assertThat(outputMessage).isNotNull();

			String payload = new String(outputMessage.getPayload());

			assertThat(payload).contains("placeType");
			assertThat(payload).doesNotContain("placeCode");
			assertThat(payload).doesNotContain("countryName");
		}
	}

	@Test
	public void testTwitterClosestTrends() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterTrendLocationProcessorApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterTrendFunction",

						"--twitter.trend.trendQueryType=trendLocation",
						"--twitter.connection.rawJson=true",
						"--twitter.trend.closest.lat='52.379189'",
						"--twitter.trend.closest.lon='4.899431'",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination input = context.getBean(InputDestination.class);
			OutputDestination output = context.getBean(OutputDestination.class);
			assertThat(input).isNotNull();
			assertThat(output).isNotNull();

			input.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8)));

			Message<byte[]> outputMessage = output.receive(Duration.ofSeconds(300).toMillis(), "twitterTrendFunction-out-0");
			assertThat(outputMessage).isNotNull();

			mockClient.verify(closestTrendsRequest, once());
			assertThat(outputMessage).isNotNull();
		}
	}

	public static HttpRequest setExpectation(HttpRequest request) {
		mockClient
				.when(request, exactly(1))
				.respond(response()
						.withStatusCode(200)
						.withHeaders(
								new Header("Content-Type", "application/json; charset=utf-8"),
								new Header("Cache-Control", "public, max-age=86400"))
						.withBody(TwitterTestUtils.asString("classpath:/response/trend_locations.json"))
						.withDelay(TimeUnit.SECONDS, 1)
				);
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterTrendFunctionConfiguration.class)
	public static class TestTwitterTrendLocationProcessorApplication {
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
