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

package org.springframework.cloud.fn.twitter.geo;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.cloud.fn.twitter.TestSocketUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.util.MimeTypeUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.unlimited;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;
import static org.mockserver.verify.VerificationTimes.once;

/**
 * @author Christian Tzolov
 */
@SpringBootTest(
		webEnvironment = SpringBootTest.WebEnvironment.NONE,
		properties = {
				"twitter.connection.consumerKey=consumerKey666",
				"twitter.connection.consumerSecret=consumerSecret666",
				"twitter.connection.accessToken=accessToken666",
				"twitter.connection.accessTokenSecret=accessTokenSecret666"
		})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class TwitterGeoFunctionTest {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;

	@Autowired
	protected Function<Message<?>, Message<byte[]>> twitterUsersFunction;

	public static void recordRequestExpectation(Map<String, List<String>> parameters) {

		mockClient
				.when(
						request()
								.withMethod("GET")
								.withPath("/geo/search.json")
								.withQueryStringParameters(parameters),
						unlimited())
				.respond(
						response()
								.withStatusCode(200)
								.withHeader("Content-Type", "application/json; charset=utf-8")
								.withBody(TwitterTestUtils.asString("classpath:/response/search_places_amsterdam.json"))
								.withDelay(TimeUnit.SECONDS, 1));

	}

	@BeforeAll
	public static void startMockServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);
	}

	@AfterAll
	public static void stopMockServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.geo.search.ip='127.0.0.1'",
			"twitter.geo.search.query=payload.toUpperCase()"
	})
	public static class TwitterGeoSearchByIPAndQueryTests extends TwitterGeoFunctionTest {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("ip", Collections.singletonList("127.0.0.1"));
			queryParameters.put("query", Collections.singletonList("Amsterdam"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload(inPayload).build());

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("ip", "127.0.0.1")
							.withQueryStringParameter("query", "AMSTERDAM"),
					once());

			String outPayload = new String((byte[]) received.getPayload());

			assertThat(outPayload).isNotNull();

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places).hasSize(12);
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.location.lat='52.378'",
			"twitter.geo.location.lon='4.9'",
			"twitter.geo.search.query=payload.toUpperCase()"
	})
	public static class TwitterGeoSearchByLocationTests extends TwitterGeoFunctionTest {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Collections.singletonList("52.378"));
			queryParameters.put("long", Collections.singletonList("4.9"));
			queryParameters.put("query", Collections.singletonList("Amsterdam"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload(inPayload).build());

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.378")
							.withQueryStringParameter("long", "4.9")
							.withQueryStringParameter("query", "Amsterdam"),
					once());

			String outPayload = new String((byte[]) received.getPayload());

			assertThat(outPayload).isNotNull();

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places).hasSize(12);
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.type=reverse",
			"twitter.geo.location.lat='52.378'",
			"twitter.geo.location.lon='4.9'"
	})
	public static class TwitterGeoSearchByLocation2Tests extends TwitterGeoFunctionTest {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Arrays.asList("52.378"));
			queryParameters.put("long", Arrays.asList("4.9"));

			recordRequestExpectation(queryParameters);

			String inPayload = "Amsterdam";

			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload(inPayload).build());

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.378")
							.withQueryStringParameter("long", "4.9"),
					once());

			String outPayload = new String((byte[]) received.getPayload());

			assertThat(outPayload).isNotNull();

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places).hasSize(12);
		}
	}

	@TestPropertySource(properties = {
			"twitter.geo.location.lat=#jsonPath(new String(payload),'$.location.lat')",
			"twitter.geo.location.lon=#jsonPath(new String(payload),'$.location.lon')",
			"twitter.geo.search.query=#jsonPath(new String(payload),'$.country')"
	})
	public static class TwitterGeoSearchJsonPathTests extends TwitterGeoFunctionTest {

		@Test
		public void testOne() throws IOException {

			Map<String, List<String>> queryParameters = new HashMap<>();
			queryParameters.put("lat", Collections.singletonList("52.0"));
			queryParameters.put("long", Collections.singletonList("5.0"));
			queryParameters.put("query", Collections.singletonList("Netherlands"));

			recordRequestExpectation(queryParameters);

			String inPayload = "{ \"country\" : \"Netherlands\", \"location\" : { \"lat\" : 52.00 , \"lon\" : 5.0 } }";

			Message<?> received = twitterUsersFunction.apply(MessageBuilder
					.withPayload(inPayload)
					.setHeader("contentType", MimeTypeUtils.APPLICATION_JSON_VALUE)
					.build());

			mockClient.verify(request()
							.withMethod("GET")
							.withPath("/geo/search.json")
							.withQueryStringParameter("lat", "52.0")
							.withQueryStringParameter("long", "5.0")
							.withQueryStringParameter("query", "Netherlands"),
					once());

			String outPayload = new String((byte[]) received.getPayload());

			assertThat(outPayload).isNotNull();

			List places = new ObjectMapper().readValue(outPayload, List.class);
			assertThat(places).hasSize(12);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterGeoFunctionConfiguration.class)
	public static class TwitterGeoFunctionTestApplication {
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
