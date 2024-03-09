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

package org.springframework.cloud.stream.app.sink.twitter.update;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.cloud.fn.consumer.twitter.status.update.TwitterUpdateConsumerProperties;
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
public class TwitterUpdateSinkIntegrationTests {

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@BeforeAll
	public static void startMockServer() {
		mockServer = ClientAndServer.startClientAndServer();
		mockClient = new MockServerClient("localhost", mockServer.getPort());

		mockClient
				.when(
						request().withMethod("POST").withPath("/statuses/update.json"),
						unlimited())
				.respond(response()
						.withStatusCode(200)
						.withHeader("Content-Type", "application/json; charset=utf-8")
						.withBody(TwitterTestUtils.asString("classpath:/response/update_test_1.json")));
	}

	@AfterAll
	public static void stopMockServer() {
		mockServer.stop();
	}

	@Test
	public void testUpdateStatus() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterUpdateSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterStatusUpdateConsumer",
						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			source.send(new GenericMessage<>("Test Update 678".getBytes(StandardCharsets.UTF_8)));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=Test%20Update%20678" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());

		}
	}

	@Test
	public void updateWithPayloadExpression() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterUpdateSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterStatusUpdateConsumer",
						"--twitter.update.text=payload.toUpperCase().concat(\" With Suffix\")",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			source.send(new GenericMessage<>("1 Expression Test".getBytes(StandardCharsets.UTF_8)));
			source.send(new GenericMessage<>("2 Expression Test".getBytes(StandardCharsets.UTF_8)));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=1%20EXPRESSION%20TEST%20With%20Suffix" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=2%20EXPRESSION%20TEST%20With%20Suffix" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());

		}
	}

	@Test
	public void updateWithAllParams() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(TestTwitterUpdateSinkApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|twitterStatusUpdateConsumer",

						"--twitter.update.attachmentUrl='http://attachementUrl'",
						"--twitter.update.placeId='myPlaceId'",
						"--twitter.update.inReplyToStatusId='666666'",
						"--twitter.update.displayCoordinates='true'",
						"--twitter.update.mediaIds='471592142565957632, 471592142565957633'",
						"--twitter.update.location.lat='37.78217'",
						"--twitter.update.location.lon='-122.40062'",

						"--twitter.connection.consumerKey=myConsumerKey",
						"--twitter.connection.consumerSecret=myConsumerSecret",
						"--twitter.connection.accessToken=myAccessToken",
						"--twitter.connection.accessTokenSecret=myAccessTokenSecret")) {

			TwitterConnectionProperties twitterConnectionProperties = context.getBean(TwitterConnectionProperties.class);
			assertThat(twitterConnectionProperties.getConsumerKey()).isEqualTo("myConsumerKey");
			assertThat(twitterConnectionProperties.getConsumerSecret()).isEqualTo("myConsumerSecret");
			assertThat(twitterConnectionProperties.getAccessToken()).isEqualTo("myAccessToken");
			assertThat(twitterConnectionProperties.getAccessTokenSecret()).isEqualTo("myAccessTokenSecret");

			TwitterUpdateConsumerProperties twitterUpdateConsumerProperties = context.getBean(TwitterUpdateConsumerProperties.class);
			assertThat(twitterUpdateConsumerProperties.getAttachmentUrl().getValue()).isEqualTo("http://attachementUrl");
			assertThat(twitterUpdateConsumerProperties.getPlaceId().getValue()).isEqualTo("myPlaceId");
			assertThat(twitterUpdateConsumerProperties.getInReplyToStatusId().getValue()).isEqualTo("666666");
			assertThat(twitterUpdateConsumerProperties.getDisplayCoordinates().getValue()).isEqualTo("true");
			assertThat(twitterUpdateConsumerProperties.getMediaIds().getValue()).isEqualTo("471592142565957632, 471592142565957633");
			assertThat(twitterUpdateConsumerProperties.getLocation().getLat().getValue()).isEqualTo("37.78217");
			assertThat(twitterUpdateConsumerProperties.getLocation().getLon().getValue()).isEqualTo("-122.40062");

			InputDestination source = context.getBean(InputDestination.class);
			assertThat(source).isNotNull();

			source.send(new GenericMessage<>("Test Tweet".getBytes(StandardCharsets.UTF_8)));

			mockClient.verify(request()
							.withMethod("POST")
							.withPath("/statuses/update.json")
							.withBody(new StringBody("status=Test%20Tweet" +
									"&in_reply_to_status_id=666666" +
									"&lat=37.78217&long=-122.40062" +
									"&place_id=myPlaceId" +
									"&media_ids=471592142565957632%2C471592142565957633" +
									"&auto_populate_reply_metadata=true" +
									"&attachment_url=http%3A%2F%2FattachementUrl" +
									"&include_entities=true" +
									"&include_ext_alt_text=true" +
									"&tweet_mode=extended")),
					once());
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestTwitterUpdateSinkApplication {

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
