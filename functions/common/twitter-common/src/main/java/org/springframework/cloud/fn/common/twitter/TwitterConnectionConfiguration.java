/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.fn.common.twitter;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.Twitter;
import twitter4j.TwitterFactory;
import twitter4j.TwitterObjectFactory;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties({ TwitterConnectionProperties.class })
public class TwitterConnectionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterConnectionConfiguration.class);

	@Bean
	public twitter4j.conf.Configuration twitterConfiguration(TwitterConnectionProperties properties,
			Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder) {
		return toConfigurationBuilder.apply(properties).build();
	}

	@Bean
	public Twitter twitter(twitter4j.conf.Configuration configuration) {
		return new TwitterFactory(configuration).getInstance();
	}

	@Bean
	public TwitterStream twitterStream(twitter4j.conf.Configuration configuration) {
		return new TwitterStreamFactory(configuration).getInstance();
	}

	@Bean
	public Function<TwitterConnectionProperties, ConfigurationBuilder> toConfigurationBuilder() {
		return properties -> new ConfigurationBuilder()
				.setJSONStoreEnabled(properties.isRawJson())
				.setDebugEnabled(properties.isDebugEnabled())
				.setOAuthConsumerKey(properties.getConsumerKey())
				.setOAuthConsumerSecret(properties.getConsumerSecret())
				.setOAuthAccessToken(properties.getAccessToken())
				.setOAuthAccessTokenSecret(properties.getAccessTokenSecret());
	}

	@Bean
	public Function<Object, Message<byte[]>> json(ObjectMapper mapper) {
		return objects -> {
			try {
				String json = mapper.writeValueAsString(objects);

				return MessageBuilder
						.withPayload(json.getBytes())
						.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
						.build();
			}
			catch (JsonProcessingException e) {
				logger.error("Status to JSON conversion error!", e);
			}
			return null;
		};
	}

	/**
	 *  Retrieves the raw JSON form of the provided object.
	 *
	 *  Note that raw JSON forms can be retrieved only from the same thread invoked the last method
	 *  call and will become inaccessible once another method call.
	 *
	 * @return Function that can retrieve the raw JSON object from the objects returned by the Twitter4J's APIs.
	 */
	@Bean
	public Function<Object, Object> rawJsonExtractor() {
		return response -> {
			if (response instanceof List) {
				List responses = (List) response;
				List<String> rawJsonList = new ArrayList<>();
				for (Object object : responses) {
					rawJsonList.add(TwitterObjectFactory.getRawJSON(object));
				}
				return rawJsonList;
			}
			else {
				return TwitterObjectFactory.getRawJSON(response);
			}
		};
	}

	@Bean
	public Function<Object, Message<byte[]>> managedJson(TwitterConnectionProperties properties,
			Function<Object, Object> rawJsonExtractor, Function<Object, Message<byte[]>> json) {
		return list -> (properties.isRawJson()) ? rawJsonExtractor.andThen(json).apply(list) : json.apply(list);
	}
}
