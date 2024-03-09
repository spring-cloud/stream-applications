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
import java.util.function.Function;

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
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 */
public class TwitterTrendProcessorIntegrationTests {

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	private static HttpRequest trendsRequest;

	@BeforeAll
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer();
		mockClient = new MockServerClient("localhost", mockServer.getPort());

		trendsRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/trends/place.json")
				.withQueryStringParameter("id", "2972"));
	}

	@AfterAll
	public static void stopServer() {
		mockServer.stop();
	}

	@Test
	public void testTwitterTrendPayload() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterTrendProcessorApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterTrendFunction",

						"--twitter.trend.trend-query-type=trend",
						"--twitter.trend.locationId='2972'",
						"--twitter.connection.rawJson=true",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination input = context.getBean(InputDestination.class);
			OutputDestination output = context.getBean(OutputDestination.class);
			assertThat(input).isNotNull();
			assertThat(output).isNotNull();

			input.send(new GenericMessage<>("Hello".getBytes(StandardCharsets.UTF_8)));

			Message<byte[]> outputMessage = output.receive(Duration.ofSeconds(300).toMillis(), "twitterTrendFunction-out-0");
			assertThat(outputMessage).isNotNull();

			mockClient.verify(trendsRequest, once());
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
						.withBody(TwitterTestUtils.asString("classpath:/response/trends.json"))
				);
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestTwitterTrendProcessorApplication {

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
