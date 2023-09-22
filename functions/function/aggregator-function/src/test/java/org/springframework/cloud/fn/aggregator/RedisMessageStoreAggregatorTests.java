/*
 * Copyright 2020-2023 the original author or authors.
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

package org.springframework.cloud.fn.aggregator;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.cloud.fn.consumer.redis.RedisTestContainerSupport;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.redis.store.RedisMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
@TestPropertySource(properties = "aggregator.message-store-type=redis")
public class RedisMessageStoreAggregatorTests extends AbstractAggregatorFunctionTests implements RedisTestContainerSupport {
	@DynamicPropertySource
	static void redisProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.redis.url", RedisTestContainerSupport::getUri);
	}

	@Test
	public void test() {
		InputStream fakeNonSerializableKafkaConsumer = new ByteArrayInputStream(new byte[0]);

		Flux<Message<?>> input =
			Flux.just(MessageBuilder.withPayload("2")
					.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
					.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 2)
					.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
					.setHeader("kafka_consumer", new ProxyFactory(fakeNonSerializableKafkaConsumer).getProxy())
					.build(),
				MessageBuilder.withPayload("1")
					.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
					.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 1)
					.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
					.build());

		Flux<Message<?>> output = this.aggregatorFunction.apply(input);

		output.as(StepVerifier::create)
			.assertNext((message) ->
				assertThat(message)
					.extracting(Message::getPayload)
					.isInstanceOf(List.class)
					.asList()
					.hasSize(2)
					.contains("1", "2"))
			.thenCancel()
			.verify(Duration.ofSeconds(10));

		assertThat(this.messageGroupStore).isInstanceOf(RedisMessageStore.class);

		assertThat(this.aggregatingMessageHandler.getMessageStore()).isSameAs(this.messageGroupStore);
	}

}
