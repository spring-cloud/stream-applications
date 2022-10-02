/*
 * Copyright 2019-2022 the original author or authors.
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

package org.springframework.cloud.fn.consumer.mongo;

import java.time.Duration;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author David Turanski
 * @author Chris Bono
 */
@SpringBootTest(properties = {
		"mongodb.consumer.collection=testing"
})
class MongoDbConsumerApplicationTests implements MongoDbTestContainerSupport {

	@DynamicPropertySource
	static void mongoDbProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.data.mongodb.uri", MongoDbTestContainerSupport::mongoDbUri);
		registry.add("spring.data.mongodb.database", () -> "test");
	}

	@Autowired
	private MongoDbConsumerProperties properties;

	@Autowired
	private Consumer<Message<?>> mongodbConsumer;

	@Autowired
	private ReactiveMongoTemplate mongoTemplate;

	@Test
	void testMongodbConsumer() {
		Map<String, String> data1 = new HashMap<>();
		data1.put("foo", "bar");

		Map<String, String> data2 = new HashMap<>();
		data2.put("firstName", "Foo");
		data2.put("lastName", "Bar");

		Flux<Message<?>> messages = Flux.just(
				new GenericMessage<>(data1),
				new GenericMessage<>(data2),
				new GenericMessage<>("{\"my_data\": \"THE DATA\"}"));

		messages.map(message -> {
			mongodbConsumer.accept(message);
			return message;

		}).subscribe();

		await().timeout(Duration.ofSeconds(10))
				.until(() -> mongoTemplate.findAll(Document.class, properties.getCollection()).count().block() == 3L);

		StepVerifier.create(this.mongoTemplate.findAll(Document.class, properties.getCollection())
				.sort(Comparator.comparing(d -> d.get("_id").toString())))
				.assertNext(document -> {
					assertThat(document.get("foo")).isEqualTo("bar");
				})
				.assertNext(document -> {
					assertThat(document.get("firstName")).isEqualTo("Foo");
					assertThat(document.get("lastName")).isEqualTo("Bar");
				})
				.assertNext(document -> {
					assertThat(document.get("my_data")).isEqualTo("THE DATA");
				})
				.verifyComplete();
	}

	@SpringBootApplication
	static class MongoDbConsumerTestApplication {
	}
}
