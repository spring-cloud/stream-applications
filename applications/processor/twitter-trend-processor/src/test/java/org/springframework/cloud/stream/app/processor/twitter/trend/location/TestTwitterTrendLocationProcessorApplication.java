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

package org.springframework.cloud.stream.app.processor.twitter.trend.location;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpRequest;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.cloud.fn.common.twitter.util.TwitterTestUtils;
import org.springframework.cloud.fn.twitter.trend.TwitterTrendFunctionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.util.TestSocketUtils;

import static org.mockserver.matchers.Times.exactly;
import static org.mockserver.model.HttpResponse.response;

/**
 * @author Christian Tzolov
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(TwitterTrendFunctionConfiguration.class)
public class TestTwitterTrendLocationProcessorApplication {

	public static final String MOCK_SERVER_IP = "127.0.0.1";

	public static final Integer MOCK_SERVER_PORT = TestSocketUtils.findAvailableTcpPort();

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

	public static HttpRequest setExpectation(HttpRequest request, MockServerClient mockClient) {
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
}
