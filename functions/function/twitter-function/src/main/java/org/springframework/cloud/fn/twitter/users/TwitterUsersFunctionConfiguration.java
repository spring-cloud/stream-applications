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

import java.util.List;
import java.util.function.Function;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.ResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(TwitterUsersFunctionProperties.class)
@Import(TwitterConnectionConfiguration.class)
public class TwitterUsersFunctionConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterUsersFunctionConfiguration.class);

	@Bean
	@ConditionalOnProperty(name = "twitter.users.type", havingValue = "search")
	public Function<Message<?>, List<User>> userSearch(Twitter twitter, TwitterUsersFunctionProperties properties) {

		return message -> {
			String query = properties.getSearch().getQuery().getValue(message, String.class);
			try {
				ResponseList<User> users = twitter.searchUsers(query, properties.getSearch().getPage());
				return users;
			}
			catch (TwitterException e) {
				logger.error("Twitter API error!", e);
			}
			return null;
		};
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.users.type", havingValue = "lookup")
	public Function<Message<?>, List<User>> userLookup(Twitter twitter, TwitterUsersFunctionProperties properties) {

		return message -> {

			try {
				TwitterUsersFunctionProperties.Lookup lookup = properties.getLookup();
				if (lookup.getScreenName() != null) {
					String[] screenNames = lookup.getScreenName().getValue(message, String[].class);
					return twitter.lookupUsers(screenNames);
				}
				else if (lookup.getUserId() != null) {
					long[] ids = lookup.getUserId().getValue(message, long[].class);
					return twitter.lookupUsers(ids);
				}
			}
			catch (TwitterException e) {
				logger.error("Twitter API error!", e);
			}
			return null;
		};
	}

	@Bean
	/**
	 * queryUsers - depends on the `twitter.users.type` property is either userSearch or userLookup.
	 * managedJson - converts Users into JSON message payload.
	 */
	public Function<Message<?>, Message<byte[]>> twitterUsersFunction(Function<Message<?>, List<User>> queryUsers,
			Function<Object, Message<byte[]>> managedJson) {
		return queryUsers.andThen(managedJson);
	}
}
