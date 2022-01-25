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

import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo;
import org.springframework.integration.mongodb.store.ConfigurableMongoDbMessageStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 */
@TestPropertySource(properties = {
		"aggregator.correlation=T(Thread).currentThread().id",
		"aggregator.release=!messages.?[payload == 'bar'].empty",
		"aggregator.aggregation=#this.?[payload == 'foo'].![payload]",
		"aggregator.messageStoreType=mongodb",
		"aggregator.message-store-entity=aggregatorTest",
		"spring.mongodb.embedded.version=3.5.5" })
@AutoConfigureDataMongo
public class CustomPropsAndMongoMessageStoreAggregatorTests extends AbstractAggregatorFunctionTests {

	@Test
	public void test() {
		Flux<Message<?>> input =
				Flux.just("foo", "bar")
						.map(GenericMessage::new);

		Flux<Message<?>> output = this.aggregatorFunction.apply(input);

		output.as(StepVerifier::create)
				.assertNext((message) ->
						assertThat(message)
								.extracting(Message::getPayload)
								.isInstanceOf(List.class)
								.asList()
								.hasSize(1)
								.element(0).isEqualTo("foo"))
				.thenCancel()
				.verify(Duration.ofSeconds(10));

		assertThat(this.messageGroupStore).isInstanceOf(ConfigurableMongoDbMessageStore.class);
		assertThat(TestUtils.getPropertyValue(this.messageGroupStore, "collectionName")).isEqualTo("aggregatorTest");
		assertThat(this.aggregatingMessageHandler.getMessageStore()).isSameAs(this.messageGroupStore);
	}

}
