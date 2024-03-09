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

package org.springframework.cloud.stream.app.sink.twitter.message;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.StringBody;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.unlimited;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 * @author Artem Bilan
 */
public class TwitterMessageSinkIntegrationTests {

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@BeforeEach
	public void startMockServer() {
		mockServer = ClientAndServer.startClientAndServer();
		mockClient = new MockServerClient("localhost", mockServer.getPort());

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/users/show.json")
								.withQueryStringParameter("screen_name", "user666")
								.withQueryStringParameter("include_entities", "true")
								.withQueryStringParameter("include_ext_alt_text", "true")
								.withQueryStringParameter("tweet_mode", "extended"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/user_1075751718749659136.json"))
								.withDelay(TimeUnit.SECONDS, 1));

		mockClient
				.when(
						request()
								.withMethod("POST")
								.withPath("/direct_messages/events/new.json"),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/test_direct_message.json"))
								.withDelay(TimeUnit.SECONDS, 1));
	}

	@AfterEach
	public void stopMockServer() {
		mockServer.stop();
	}

	@Test
	public void directMessageScreenName() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterMessageSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterSendMessageConsumer",

						"--twitter.message.update.screenName='user666'",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			source.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8)));

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/users/show.json")
							.withQueryStringParameter("screen_name", "user666")
							.withQueryStringParameter("include_entities", "true")
							.withQueryStringParameter("include_ext_alt_text", "true")
							.withQueryStringParameter("tweet_mode", "extended"),
					once());

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\"," +
									"\"message_create\":{\"target\":{\"recipient_id\":1075751718749659136}," +
									"\"message_data\":{\"text\":\"hello\"}}}}")),
					once());
		}
	}

	@Test
	public void directMessageUserId() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterMessageSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterSendMessageConsumer",

						"--twitter.message.update.userId='1075751718749659136'",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			source.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8)));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\"," +
									"\"message_create\":{\"target\":{\"recipient_id\":1075751718749659136}," +
									"\"message_data\":{\"text\":\"hello\"}}}}")),
					once());
		}
	}

	@Test
	public void directMessageDefaults() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterMessageSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterSendMessageConsumer",

						"--twitter.message.update.userId=headers['user']",
						"--twitter.message.update.text=payload.concat(\" with suffix \")",
						"--twitter.message.update.mediaId='666'",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			Map<String, Object> headers = Collections.singletonMap("user", "1075751718749659136");
			source.send(new GenericMessage<>("hello".getBytes(StandardCharsets.UTF_8), headers));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/direct_messages/events/new.json")
							.withBody(new StringBody("{\"event\":{\"type\":\"message_create\",\"message_create\":" +
									"{\"target\":{\"recipient_id\":1075751718749659136},\"message_data\":" +
									"{\"text\":\"hello with suffix \",\"attachment\":{\"type\":\"media\",\"media\":" +
									"{\"id\":666}}}}}}")),
					once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestTwitterMessageSinkApplication {

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
