/*
 * Copyright 2019-2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.mongo;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@SpringBootTest(properties = {
		"spring.data.mongodb.port=0",
		"mongodb.supplier.collection=testing"})
class MongodbSupplierApplicationTests {

	private ObjectMapper objectMapper = new ObjectMapper();

	@Autowired
	private Supplier<Flux<Message<?>>> mongodbSupplier;

	@Autowired
	private MongoClient mongo;

	@BeforeEach
	public void setUp() {
		MongoDatabase database = this.mongo.getDatabase("test");
		database.createCollection("testing");
		MongoCollection<Document> collection = database.getCollection("testing");
		collection.insertOne(
				new Document("greeting", "hello")
						.append("name", "foo"));
		collection.insertOne(
				new Document("greeting", "hola")
						.append("name", "bar"));
	}

	@Test
	void testMongodbSupplier() {
		Flux<Message<?>> messageFlux = this.mongodbSupplier.get();
			StepVerifier.create(messageFlux)
					.assertNext((message) ->
						assertThat(payload(message)).contains(
								entry("greeting","hello"),
								entry("name", "foo")))
					.assertNext((message) ->
							assertThat(payload(message)).contains(
									entry("greeting","hola"),
									entry("name", "bar")))
					.thenCancel()
					.verify();
	}

	private  Map<String,Object> payload(Message<?> message) {
		Map<String,Object> map = null;
		try {
			map = objectMapper.readValue(message.getPayload().toString(),HashMap.class);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return map;
	}

	@SpringBootApplication
	static class TestApplication {}
}
