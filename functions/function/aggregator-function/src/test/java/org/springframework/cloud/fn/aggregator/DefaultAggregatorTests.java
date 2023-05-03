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

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Corneil du Plessis
 */
@Disabled("Fails on CI sporadically")
@TestPropertySource(properties = "aggregator.message-store-type=simple")
public class DefaultAggregatorTests extends AbstractAggregatorFunctionTests {
	private static final Logger logger = LoggerFactory.getLogger(DefaultAggregatorTests.class);

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

		Flux<Message<?>> output = this.aggregatorFunction.apply(input.log("DefaultAggregatorTests:input"));
		output.log("DefaultAggregatorTests:output")
			.as(StepVerifier::create)
			.assertNext((message) -> {
				assertThat(message)
					.extracting(Message::getPayload)
					.asList()
					.hasSize(2)
					.contains("1", "2");
			})
			.thenCancel()
			.verify(Duration.ofSeconds(30));

		assertThat(this.messageGroupStore).isNull();
		assertThat(this.aggregatingMessageHandler.getMessageStore()).isInstanceOf(SimpleMessageStore.class);

	}
}
