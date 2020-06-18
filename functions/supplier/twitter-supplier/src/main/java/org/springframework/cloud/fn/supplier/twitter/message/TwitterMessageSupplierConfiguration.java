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

package org.springframework.cloud.fn.supplier.twitter.message;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import twitter4j.DirectMessage;
import twitter4j.DirectMessageList;
import twitter4j.Twitter;
import twitter4j.TwitterException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.messaging.Message;

/**
 *
 * @author Christian Tzolov
 */
@EnableConfigurationProperties({ TwitterMessageSupplierProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterMessageSupplierConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterMessageSupplierConfiguration.class);

	@Bean
	@ConditionalOnMissingBean
	public MetadataStore metadataStore() {
		return new SimpleMetadataStore();
	}

	@Bean
	@ConditionalOnMissingBean
	public MessageCursor cursor() {
		return new MessageCursor();
	}

	@Bean
	public Supplier<List<DirectMessage>> directMessagesSupplier(TwitterMessageSupplierProperties properties,
			Twitter twitter, MessageCursor cursorState) {
		return () -> {
			try {
				String cs = cursorState.getCursor();
				DirectMessageList messages = (cursorState.getCursor() == null) ?
						twitter.getDirectMessages(properties.getCount()) :
						twitter.getDirectMessages(properties.getCount(), cursorState.getCursor());

				if (messages != null) {
					cursorState.updateCursor(messages.getNextCursor());
					return messages;
				}

				logger.error(String.format("NULL messages response for properties: %s and cursor: %s!", properties, cursorState));
				cursorState.updateCursor(null);
			}
			catch (TwitterException e) {
				logger.error("Twitter API error:", e);
			}

			return new ArrayList<>();
		};
	}

	@Bean
	public Function<List<DirectMessage>, List<DirectMessage>> messageDeduplicate(MetadataStore metadataStore) {
		return messages -> {
			List<DirectMessage> uniqueMessages = new ArrayList<>();
			for (DirectMessage message : messages) {
				if (metadataStore.get(message.getId() + "") == null) {
					metadataStore.put(message.getId() + "", message.getCreatedAt() + "");
					uniqueMessages.add(message);
				}
			}
			return uniqueMessages;
		};
	}

	@Bean
	public Supplier<Message<byte[]>> twitterMessageSupplier(Function<List<DirectMessage>, List<DirectMessage>> messageDeduplicate,
			Function<Object, Message<byte[]>> managedJson, Supplier<List<DirectMessage>> directMessagesSupplier) {
		return () -> messageDeduplicate.andThen(managedJson).apply(directMessagesSupplier.get());
	}

	public static class MessageCursor {
		private String cursor = null;

		public String getCursor() {
			return cursor;
		}

		public void updateCursor(String newCursor) {
			this.cursor = newCursor;
		}

		@Override
		public String toString() {
			return "Cursor{" +
					"cursor=" + cursor +
					'}';
		}
	}
}
