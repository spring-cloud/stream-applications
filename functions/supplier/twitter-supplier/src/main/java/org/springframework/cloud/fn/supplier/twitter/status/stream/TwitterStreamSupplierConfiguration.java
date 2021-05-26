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

package org.springframework.cloud.fn.supplier.twitter.status.stream;

import java.util.function.Supplier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import reactor.core.publisher.Flux;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionConfiguration;
import org.springframework.cloud.fn.common.twitter.TwitterConnectionProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.integration.channel.FluxMessageChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.MimeTypeUtils;

/**
 *
 * @author Christian Tzolov
 */

@EnableConfigurationProperties({ TwitterStreamSupplierProperties.class, TwitterConnectionProperties.class })
@Import(TwitterConnectionConfiguration.class)
public class TwitterStreamSupplierConfiguration {

	private static final Log logger = LogFactory.getLog(TwitterStreamSupplierConfiguration.class);

	@Bean
	public FluxMessageChannel twitterStatusInputChannel() {
		return new FluxMessageChannel();
	}

	@Bean
	public StatusListener twitterStatusListener(FluxMessageChannel twitterStatusInputChannel, TwitterStream twitterStream,
			ObjectMapper objectMapper) {

		StatusListener statusListener = new StatusListener() {

			@Override
			public void onException(Exception e) {
				logger.error("Status Error: ", e);
				throw new RuntimeException("Status Error: ", e);
			}

			@Override
			public void onDeletionNotice(StatusDeletionNotice arg) {
				logger.info("StatusDeletionNotice: " + arg);
			}

			@Override
			public void onScrubGeo(long userId, long upToStatusId) {
				logger.info("onScrubGeo: " + userId + ", " + upToStatusId);
			}

			@Override
			public void onStallWarning(StallWarning warning) {
				logger.warn("Stall Warning: " + warning);
				throw new RuntimeException("Stall Warning: " + warning);
			}

			@Override
			public void onStatus(Status status) {

				try {
					String json = objectMapper.writeValueAsString(status);
					Message<byte[]> message = MessageBuilder.withPayload(json.getBytes())
							.setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
							.build();
					twitterStatusInputChannel.send(message);
				}
				catch (JsonProcessingException e) {
					logger.error("Status to JSON conversion error!", e);
					throw new RuntimeException("Status to JSON conversion error!", e);
				}
			}

			@Override
			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				logger.warn("Track Limitation Notice: " + numberOfLimitedStatuses);
			}
		};

		twitterStream.addListener(statusListener);

		return statusListener;
	}

	@Bean
	public Supplier<Flux<Message<?>>> twitterStreamSupplier(TwitterStream twitterStream,
			FluxMessageChannel twitterStatusInputChannel, TwitterStreamSupplierProperties streamProperties) {

		return () -> Flux.from(twitterStatusInputChannel)
				.doOnSubscribe(subscription -> {
					try {
						switch (streamProperties.getType()) {

						case filter:
							twitterStream.filter(streamProperties.getFilter().toFilterQuery());
							return;

						case sample:
							twitterStream.sample();
							return;

						case firehose:
							twitterStream.firehose(streamProperties.getFilter().getCount());
							return;

						case link:
							twitterStream.links(streamProperties.getFilter().getCount());
							return;
						default:
							throw new IllegalArgumentException("Unknown stream type:" + streamProperties.getType());
						}
					}
					catch (Exception e) {
						this.logger.error("Filter is not property set");
					}
				})
				.doAfterTerminate(() -> {
					this.logger.info("Proactive cancel for twitter stream");
					twitterStream.shutdown();
				})
				.doOnError(throwable -> {
					this.logger.error(throwable.getMessage(), throwable);
				});
	}
}
