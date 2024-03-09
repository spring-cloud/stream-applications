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

package org.springframework.cloud.stream.app.source.twitter.search;

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
import org.springframework.cloud.fn.supplier.twitter.status.search.TwitterSearchSupplierProperties;
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
public class TwitterSearchSourceIntegrationTests {

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	private static HttpRequest searchVratsaRequest;

	private static HttpRequest searchAmsterdamRequest;

	@BeforeAll
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer();
		mockClient = new MockServerClient("localhost", mockServer.getPort());

		searchVratsaRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/search/tweets.json")
				.withQueryStringParameter("q", "Vratsa")
				.withQueryStringParameter("count", "3"));

		searchAmsterdamRequest = setExpectation(request()
				.withMethod("GET")
				.withPath("/search/tweets.json")
				.withQueryStringParameter("q", "Amsterdam")
				.withQueryStringParameter("count", "3")
				.withQueryStringParameter("result_type", "popular")
				.withQueryStringParameter("geocode", "52.1,4.8,10.0km")
				.withQueryStringParameter("since", "2018-01-01")
				.withQueryStringParameter("lang", "en"));
	}

	@AfterAll
	public static void stopServer() {
		mockServer.stop();
	}

	@Test
	public void twitterSearchTests() throws JsonProcessingException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TestTwitterSearchSourceApplication.class))

				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterSearchSupplier",
						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666",
						"--twitter.search.query=Vratsa",
						"--twitter.search.enabled=true",
						"--twitter.search.count=3",
						"--twitter.search.page=3")) {

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(300).toMillis(), "twitterSearchSupplier-out-0");
			assertThat(message).isNotNull();
			String payload = new String(message.getPayload());

			List<?> tweets = new ObjectMapper().readValue(payload, List.class);
			assertThat(tweets).hasSize(3);
			mockClient.verify(searchVratsaRequest, once());

		}
	}

	@Test
	public void twitterSearchTestsAmsterdam() throws JsonProcessingException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(TestTwitterSearchSourceApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=twitterSearchSupplier",
						"--twitter.connection.consumerKey=consumerKey666",
						"--twitter.connection.consumerSecret=consumerSecret666",
						"--twitter.connection.accessToken=accessToken666",
						"--twitter.connection.accessTokenSecret=accessTokenSecret666",
						"--twitter.search.query=Amsterdam",
						"--twitter.search.count=3",
						"--twitter.search.page=3",
						"--twitter.search.lang=en",
						"--twitter.search.enabled=true",
						"--twitter.search.geocode.latitude=52.1",
						"--twitter.search.geocode.longitude=4.8",
						"--twitter.search.geocode.radius=10",
						"--twitter.search.since=2018-01-01",
						"--twitter.search.resultType=popular")) {

			TwitterConnectionProperties twitterConnectionProperties = context.getBean(TwitterConnectionProperties.class);
			assertThat(twitterConnectionProperties.getConsumerKey()).isEqualTo("consumerKey666");
			assertThat(twitterConnectionProperties.getConsumerSecret()).isEqualTo("consumerSecret666");
			assertThat(twitterConnectionProperties.getAccessToken()).isEqualTo("accessToken666");
			assertThat(twitterConnectionProperties.getAccessTokenSecret()).isEqualTo("accessTokenSecret666");

			TwitterSearchSupplierProperties searchSupplierProperties =
					context.getBean(TwitterSearchSupplierProperties.class);

			assertThat(searchSupplierProperties.getQuery()).isEqualTo("Amsterdam");
			assertThat(searchSupplierProperties.getCount()).isEqualTo(3);
			assertThat(searchSupplierProperties.getPage()).isEqualTo(3);
			assertThat(searchSupplierProperties.getLang()).isEqualTo("en");
			assertThat(searchSupplierProperties.getGeocode().getLatitude()).isEqualTo(52.1D);
			assertThat(searchSupplierProperties.getGeocode().getLongitude()).isEqualTo(4.8D);
			assertThat(searchSupplierProperties.getGeocode().getRadius()).isEqualTo(10D);
			assertThat(searchSupplierProperties.getSince()).isEqualTo("2018-01-01");

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			Message<byte[]> message = outputDestination.receive(Duration.ofSeconds(300).toMillis(), "twitterSearchSupplier-out-0");
			assertThat(message).isNotNull();
			String payload = new String(message.getPayload());

			List<?> tweets = new ObjectMapper().readValue(payload, List.class);
			assertThat(tweets).hasSize(3);
			mockClient.verify(searchAmsterdamRequest, once());

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
						.withBody(TwitterTestUtils.asString("classpath:/response/search_3.json"))
				);
		return request;
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	public static class TestTwitterSearchSourceApplication {

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
