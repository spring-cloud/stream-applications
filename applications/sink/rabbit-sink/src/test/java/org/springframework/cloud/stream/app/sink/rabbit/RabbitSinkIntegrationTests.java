/*
 * Copyright 2016-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.sink.rabbit;

import org.junit.jupiter.api.Tag;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.consumer.rabbit.RabbitConsumerConfiguration;
import org.springframework.cloud.fn.consumer.rabbit.RabbitConsumerProperties;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(
		properties = {"spring.cloud.function.definition=rabbitConsumer"},
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@Tag("integration")
@Import(RabbitSinkIntegrationTests.FooConfiguration.class)
abstract class RabbitSinkIntegrationTests implements RabbitMqTestContainerSupport {

	@DynamicPropertySource
	static void rabbitProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.rabbitmq.port", RabbitMqTestContainerSupport::getPort);
	}

	@Qualifier("rabbitConsumer-in-0")
	@Autowired
	protected SubscribableChannel channels;

	@Autowired
	protected RabbitConsumerProperties rabbitSinkProperties;

	@Autowired
	protected RabbitTemplate rabbitTemplate;

	@Autowired
	protected RabbitAdmin rabbitAdmin;

	@Autowired(required = false)
	protected MessageConverter myConverter;

	@Autowired
	protected CachingConnectionFactory bootFactory;

	static class FooConfiguration {

		@Bean
		public Queue queue() {
			return new Queue("scsapp-testq", false, false, true);
		}

		@Bean
		public DirectExchange exchange() {
			return new DirectExchange("scsapp-testex", false, true);
		}

		@Bean
		public Binding binding() {
			return BindingBuilder.bind(queue()).to(exchange()).with("scsapp-testrk");
		}

	}

	@SpringBootApplication
	@Import({RabbitConsumerConfiguration.class, TestChannelBinderConfiguration.class})
	public static class RabbitSinkTestApplication {
	}
}
