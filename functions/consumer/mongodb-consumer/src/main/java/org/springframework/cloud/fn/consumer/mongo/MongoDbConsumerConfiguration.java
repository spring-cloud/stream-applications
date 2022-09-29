/*
 * Copyright 2017-2022 the original author or authors.
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

import java.util.function.Consumer;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.outbound.ReactiveMongoDbStoringMessageHandler;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.ReactiveMessageHandler;

/**
 * A configuration for MongoDB Consumer function. Uses a
 * {@link ReactiveMongoDbStoringMessageHandler} to save payload contents to Mongo DB.
 *
 * @author Artem Bilan
 * @author David Turanski
 *
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ MongoDbConsumerProperties.class })
public class MongoDbConsumerConfiguration {

	private final MongoDbConsumerProperties properties;

	private final ReactiveMongoTemplate mongoTemplate;

	public MongoDbConsumerConfiguration(MongoDbConsumerProperties properties, ReactiveMongoTemplate mongoTemplate) {
		this.properties = properties;
		this.mongoTemplate = mongoTemplate;
	}

	@Bean
	public Consumer<Message<?>> mongodbConsumer(Function<Message<?>, Mono<Void>> mongodbConsumerFunction) {
		return message -> mongodbConsumerFunction.apply(message).subscribe();
	}

	@Bean
	public Function<Message<?>, Mono<Void>> mongodbConsumerFunction(
			ReactiveMessageHandler mongoConsumerMessageHandler) {

		return mongoConsumerMessageHandler::handleMessage;
	}

	@Bean
	public ReactiveMessageHandler mongoConsumerMessageHandler(
			@Nullable ComponentCustomizer<ReactiveMongoDbStoringMessageHandler> mongoDbMessageHandlerCustomizer) {

		ReactiveMongoDbStoringMessageHandler mongoDbMessageHandler = new ReactiveMongoDbStoringMessageHandler(
				this.mongoTemplate);
		Expression collectionExpression = this.properties.getCollectionExpression();
		if (collectionExpression == null) {
			collectionExpression = new LiteralExpression(this.properties.getCollection());
		}
		mongoDbMessageHandler.setCollectionNameExpression(collectionExpression);
		if (mongoDbMessageHandlerCustomizer != null) {
			mongoDbMessageHandlerCustomizer.customize(mongoDbMessageHandler);
		}
		return mongoDbMessageHandler;
	}

}
