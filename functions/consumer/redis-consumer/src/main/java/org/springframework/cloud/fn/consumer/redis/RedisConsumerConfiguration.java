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

package org.springframework.cloud.fn.consumer.redis;

import java.util.function.Consumer;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

/**
 * @author Eric Bottard
 * @author Mark Pollack
 * @author Gary Russell
 * @author Soby Chacko
 * @author Artem Bilan
 */
@Configuration
@EnableConfigurationProperties(RedisConsumerProperties.class)
public class RedisConsumerConfiguration {

	@Bean
	public Consumer<Message<?>> redisConsumer(MessageHandler redisConsumerMessageHandler) {
		return redisConsumerMessageHandler::handleMessage;
	}

	@Bean
	public MessageHandler redisConsumerMessageHandler(RedisConnectionFactory redisConnectionFactory,
			RedisConsumerProperties redisConsumerProperties) {
		if (redisConsumerProperties.isKeyPresent()) {
			RedisStoreWritingMessageHandler redisStoreWritingMessageHandler = new RedisStoreWritingMessageHandler(
					redisConnectionFactory);
			redisStoreWritingMessageHandler.setKeyExpression(redisConsumerProperties.keyExpression());
			return redisStoreWritingMessageHandler;
		}
		else if (redisConsumerProperties.isQueuePresent()) {
			return new RedisQueueOutboundChannelAdapter(redisConsumerProperties.queueExpression(),
					redisConnectionFactory);
		}
		else { // must be topic
			RedisPublishingMessageHandler redisPublishingMessageHandler = new RedisPublishingMessageHandler(
					redisConnectionFactory);
			redisPublishingMessageHandler.setTopicExpression(redisConsumerProperties.topicExpression());
			return redisPublishingMessageHandler;
		}
	}

}
