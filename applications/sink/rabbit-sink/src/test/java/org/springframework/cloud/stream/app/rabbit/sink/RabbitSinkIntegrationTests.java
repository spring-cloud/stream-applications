/*
 * Copyright 2016-2019 the original author or authors.
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

package org.springframework.cloud.stream.app.rabbit.sink;

import java.util.function.Consumer;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.GenericContainer;

@SpringBootTest(
		properties = {"spring.cloud.stream.function.definition=rabbitConsumer"},
		webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext
@Import(RabbitSinkIntegrationTests.FooConfiguration.class)
abstract class RabbitSinkIntegrationTests {

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

	static {
		Consumer<CreateContainerCmd> cmd = e -> e.withPortBindings(new PortBinding(Ports.Binding.bindPort(5672), new ExposedPort(5672)));

		GenericContainer rabbitMq = new GenericContainer("rabbitmq:3.5.3")
				.withExposedPorts(5672)
				.withCreateContainerCmdModifier(cmd);
		rabbitMq.start();
	}

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
	@Import({RabbitConsumerConfiguration.class})
	public static class RabbitSinkConfiguration {}
}
