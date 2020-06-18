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

package org.springframework.cloud.fn.supplier.twitter.friendships;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.PagableResponseList;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.twitter.Cursor;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@Configuration
@EnableConfigurationProperties(TwitterFriendshipsSupplierProperties.class)
@Import(TwitterConnectionConfiguration.class)
public class TwitterFriendshipsSupplierConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterFriendshipsSupplierConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public MetadataStore metadataStore() {
		return new SimpleMetadataStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public Cursor cursor() {
		return new Cursor();
	}


	@Bean
	@ConditionalOnProperty(name = "twitter.friendships.source.type", havingValue = "followers")
	public Supplier<List<User>> followersSupplier(TwitterFriendshipsSupplierProperties properties,
			Twitter twitter, Cursor cursorState) {
		return () -> {
			try {
				PagableResponseList<User> users;
				if (properties.getUserId() != null) {
					users = twitter.getFollowersList(properties.getUserId(), cursorState.getCursor(),
							properties.getCount(), properties.isSkipStatus(), properties.isIncludeUserEntities());
				}
				else { // by ScreenName
					users = twitter.getFollowersList(properties.getScreenName(), cursorState.getCursor(),
							properties.getCount(), properties.isSkipStatus(), properties.isIncludeUserEntities());
				}

				if (users != null) {
					cursorState.updateCursor(users.getNextCursor());
					return users;
				}

				logger.error(String.format("NULL users response for properties: %s and cursor: %s!", properties, cursorState));
				cursorState.updateCursor(-1);
			}
			catch (TwitterException e) {
				logger.error("Twitter API error:", e);
			}

			return new ArrayList<>();
		};
	}

	@Bean
	@ConditionalOnProperty(name = "twitter.friendships.source.type", havingValue = "friends")
	public Supplier<List<User>> friendsSupplier(TwitterFriendshipsSupplierProperties properties,
			Twitter twitter, Cursor cursorState) {
		return () -> {
			try {
				PagableResponseList<User> users;
				if (properties.getUserId() != null) {
					users = twitter.getFriendsList(properties.getUserId(), cursorState.getCursor(),
							properties.getCount(), properties.isSkipStatus(), properties.isIncludeUserEntities());
				}
				else { // by ScreenName
					users = twitter.getFriendsList(properties.getScreenName(), cursorState.getCursor(),
							properties.getCount(), properties.isSkipStatus(), properties.isIncludeUserEntities());
				}

				if (users != null) {
					cursorState.updateCursor(users.getNextCursor());
					return users;
				}

				logger.error(String.format("NULL users response for properties: %s and cursor: %s!", properties, cursorState));
				cursorState.updateCursor(-1);
			}
			catch (TwitterException e) {
				logger.error("Twitter API error:", e);
			}

			return new ArrayList<>();
		};
	}

	@Bean
	public Function<List<User>, List<User>> userDeduplicate(MetadataStore metadataStore) {
		return users -> {
			List<User> uniqueUsers = new ArrayList<>();
			for (User user : users) {
				if (metadataStore.get(user.getId() + "") == null) {
					metadataStore.put(user.getId() + "", user.getName());
					uniqueUsers.add(user);
				}
			}
			return uniqueUsers;
		};
	}

	@Bean
	public Supplier<Message<byte[]>> deduplicatedFriendsJsonSupplier(Function<List<User>, List<User>> userDeduplication,
			Supplier<List<User>> userRetriever, Function<Object, Message<byte[]>> managedJson) {
		return () -> userDeduplication.andThen(managedJson).apply(userRetriever.get());
	}
}
