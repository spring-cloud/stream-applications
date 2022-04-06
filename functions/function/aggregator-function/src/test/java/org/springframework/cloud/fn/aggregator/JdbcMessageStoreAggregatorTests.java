/*
 * Copyright 2020-2022 the original author or authors.
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

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
@TestPropertySource(properties = "aggregator.message-store-type=jdbc")
public class JdbcMessageStoreAggregatorTests extends AbstractAggregatorFunctionTests {

	@Test
	public void test() {
		Flux<Message<?>> input =
				Flux.just(MessageBuilder.withPayload("2")
								.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
								.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 2)
								.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
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

		assertThat(this.messageGroupStore).isInstanceOf(JdbcMessageStore.class);

		assertThat(this.aggregatingMessageHandler.getMessageStore()).isSameAs(this.messageGroupStore);

		// Also verify geode ssl flag not enabled for non-geode message stores
		assertThat(this.geodeSslEnable).isFalse();
	}

}
