/*
 * Copyright 2019 the original author or authors.
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

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.cloud.fn.splitter.SplitterFunctionConfiguration;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;
import org.springframework.messaging.Message;

/**
 * A configuration for MongoDB Source applications. Produces
 * {@link MongoDbMessageSource} which polls collection with the query after startup
 * according to the polling properties.
 *
 * @author Adam Zwickey
 * @author Artem Bilan
 * @author David Turanski
 *
 */
@Configuration
@EnableConfigurationProperties({ MongodbSupplierProperties.class })
@Import(SplitterFunctionConfiguration.class)
public class MongodbSupplierConfiguration {

	private final MongodbSupplierProperties properties;

	private final MongoTemplate mongoTemplate;

	public MongodbSupplierConfiguration(MongodbSupplierProperties properties, MongoTemplate mongoTemplate) {
		this.properties = properties;
		this.mongoTemplate = mongoTemplate;
	}

	@Bean(name = "mongodbSupplier")
	@PollableBean(splittable = true)
	@ConditionalOnProperty(prefix = "mongodb", name = "split", matchIfMissing = true)
	public Supplier<Flux<Message<?>>> splittedSupplier(Function<Message<?>, List<Message<?>>> splitterFunction) {
		return () -> {
			Message<?> received = mongoSource().receive();
			if (received != null) {
				return Flux.fromIterable(splitterFunction.apply(received)); // multiple Message<Map<String, Object>>
			}
			else {
				return Flux.empty();
			}
		};
	}

	@Bean
	@ConditionalOnProperty(prefix = "mongodb", name = "split", havingValue = "false")
	public Supplier<Message<?>> mongodbSupplier() {
		return () -> mongoSource().receive();
	}

	/**
	 * The inheritors can consider to override this method for their purpose or just adjust
	 * options for the returned instance
	 * @return a {@link MongoDbMessageSource} instance
	 */
	@Bean
	public MongoDbMessageSource mongoSource() {
		Expression queryExpression = (this.properties.getQueryExpression() != null
				? this.properties.getQueryExpression()
				: new LiteralExpression(this.properties.getQuery()));
		MongoDbMessageSource mongoDbMessageSource = new MongoDbMessageSource(this.mongoTemplate, queryExpression);
		mongoDbMessageSource.setCollectionNameExpression(new LiteralExpression(this.properties.getCollection()));
		mongoDbMessageSource.setEntityClass(String.class);
		return mongoDbMessageSource;
	}

}
