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

package org.springframework.cloud.fn.consumer.redis;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.Topic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 */
@TestPropertySource(properties = "redis.consumer.topic = foo-topic")
public class RedisConsumerTopicTests extends AbstractRedisConsumerTests {

	@Autowired
	RedisConnectionFactory connectionFactory;

	@Test
	public void testWithTopic() throws Exception {

		int numToTest = 10;
		String topic = "foo-topic";
		final CountDownLatch latch = new CountDownLatch(numToTest);

		MessageListenerAdapter listener = new MessageListenerAdapter();
		listener.setDelegate(new Listener(latch));
		listener.setSerializer(new StringRedisSerializer());
		listener.afterPropertiesSet();

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);
		container.afterPropertiesSet();
		container.addMessageListener(listener, Collections.<Topic>singletonList(new ChannelTopic(topic)));
		container.start();

		Awaitility.await().until(() -> TestUtils.getPropertyValue(container, "subscriptionTask.connection",
				RedisConnection.class) != null);

		Message<String> message = MessageBuilder.withPayload("hello").build();
		for (int i = 0; i < numToTest; i++) {
			redisConsumer.accept(message);
		}

		assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
		container.stop();
	}

	private static class Listener {

		private final CountDownLatch latch;

		Listener(CountDownLatch latch) {
			this.latch = latch;
		}

		@SuppressWarnings("unused")
		public void handleMessage(String s) {
			this.latch.countDown();
		}
	}
}
