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

package org.springframework.cloud.fn.twitter.users;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockserver.client.MockServerClient;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.matchers.Times.exactly;
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
public abstract class TwitterUsersFunctionTests {

	private static final String MOCK_SERVER_IP = "127.0.0.1";

	private static final Integer MOCK_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();

	private static ClientAndServer mockServer;

	private static MockServerClient mockClient;
	private static HttpRequest searchUsersRequest;
	private static HttpRequest lookupUsersRequest;
	private static HttpRequest lookupUsersRequest2;

	@Autowired
	protected ObjectMapper mapper;

	@Autowired
	Function<Message<?>, Message<byte[]>> twitterUsersFunction;

	public static HttpRequest setExpectation(HttpRequest request, String responseUri) {
		mockClient
				.when(request, exactly(1))
				.respond(response()
						.withStatusCode(200)
						.withHeaders(
								new Header("Content-Type", "application/json; charset=utf-8"),
								new Header("Cache-Control", "public, max-age=86400"))
						.withBody(TwitterTestUtils.asString(responseUri))
						.withDelay(TimeUnit.SECONDS, 1)
				);
		return request;
	}

	@BeforeAll
	public static void startServer() {
		mockServer = ClientAndServer.startClientAndServer(MOCK_SERVER_PORT);
		mockClient = new MockServerClient(MOCK_SERVER_IP, MOCK_SERVER_PORT);

		searchUsersRequest = setExpectation(request()
						.withMethod("GET")
						.withPath("/users/search.json")
						.withQueryStringParameter("q", "tzolov")
						.withQueryStringParameter("page", "3"),
				"classpath:/response/search_users.json");

		lookupUsersRequest = setExpectation(request()
						.withMethod("GET")
						.withPath("/users/lookup.json")
						.withQueryStringParameter("user_id",
								"710705860343963648,326896547,267603736,781497571629989888,838754923"),
				"classpath:/response/lookup_users_id.json");

		lookupUsersRequest2 = setExpectation(request()
						.withMethod("GET")
						.withPath("/users/lookup.json")
						.withQueryStringParameter("screen_name",
								"TzolovMarto,Rabotnik57,antzolov,peyo_tzolov,ivantzolov"),
				"classpath:/response/lookup_users_id.json");
	}

	@AfterAll
	public static void stopServer() {
		mockServer.stop();
	}

	@TestPropertySource(properties = {
			"twitter.users.type=search",
			"twitter.users.search.query=payload"
	})
	public static class TwitterSearchUsersTests extends TwitterUsersFunctionTests {

		@Test
		public void testOne() throws IOException {
			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload("tzolov").build());
			mockClient.verify(searchUsersRequest, once());
			assertThat(received).isNotNull();
			List list = mapper.readValue(new String((byte[]) received.getPayload()), List.class);
			assertThat(list).hasSize(20);
		}
	}

	@TestPropertySource(properties = {
			"twitter.users.type=lookup",
			"twitter.users.lookup.userId='710705860343963648,326896547,267603736,781497571629989888,838754923'"
	})
	public static class TwitterLookupUserIdLiteralTests extends TwitterUsersFunctionTests {

		@Test
		public void testOne() throws IOException {

			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload("tzolov").build());

			mockClient.verify(lookupUsersRequest, once());
			assertThat(received).isNotNull();
			List list = mapper.readValue(new String((byte[]) received.getPayload()), List.class);
			assertThat(list).hasSize(5);
		}
	}

	@TestPropertySource(properties = {
			"twitter.users.type=lookup",
			"twitter.users.lookup.screenName=#jsonPath(payload,'$..[*].code')"
	})
	public static class TwitterLookupScreenNamePayloadTests extends TwitterUsersFunctionTests {

		@Test
		public void testOne() throws IOException {

			Object payload = "[{\"code\":\"TzolovMarto\"},{\"code\":\"Rabotnik57\"},{\"code\":\"antzolov\"},{\"code\":\"peyo_tzolov\"},{\"code\":\"ivantzolov\"}]";

			Message<?> received = twitterUsersFunction.apply(MessageBuilder.withPayload(payload).build());
			assertThat(received).isNotNull();

			mockClient.verify(lookupUsersRequest2, once());
			assertThat(received).isNotNull();
			List list = mapper.readValue(new String((byte[]) received.getPayload()), List.class);
			assertThat(list).hasSize(5);
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration
	@Import(TwitterUsersFunctionConfiguration.class)
	static class TwitterUsersFunctionTestApplication {
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
