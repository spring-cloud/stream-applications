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

package org.springframework.cloud.stream.app.sink.mongodb;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.fn.consumer.mongo.MongoDbConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.messaging.support.GenericMessage;

public class MongodbSinkTests {
	@Test
	void testInsert() {
		TestChannelBinderConfiguration.applicationContextRunner(TestApp.class)
				.withPropertyValues(
						"spring.data.mongodb.port=0",
						"spring.data.mongodb.database=test",
						"mongodb.consumer.collection=demo",
						"spring.cloud.function.definition=mongodbConsumer")
				.run(context -> {
					ReactiveMongoTemplate template = context.getBean(ReactiveMongoTemplate.class);
					InputDestination inputDestination = context.getBean(InputDestination.class);
					inputDestination.send(new GenericMessage("{\"foo\":\"bar\"}".getBytes()));
					StepVerifier.create(template.findAll(Document.class, "demo"))
							.assertNext(document -> document.containsKey("foo"))
							.thenCancel()
							.verify();
				});
	}

	@SpringBootApplication
	@Import(MongoDbConsumerConfiguration.class)
	static class TestApp {

	}
}
