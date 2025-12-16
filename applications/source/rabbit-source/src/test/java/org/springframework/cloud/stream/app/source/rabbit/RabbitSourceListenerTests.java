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

package org.springframework.cloud.stream.app.source.rabbit;

import org.aopalliance.aop.Advice;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.RabbitMQContainer;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.utils.test.TestUtils;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.rabbit.RabbitSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.retry.support.RetryTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RabbitSource.
 *
 * @author Gary Russell
 * @author Chris Schaefer
 * @author Corneil du Plessis
 * @author Artem Bilan
 */
@Tag("integration")
public class RabbitSourceListenerTests {

	static {
		RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:management");
		rabbitmq.start();

		CachingConnectionFactory connectionFactory = new CachingConnectionFactory();
		connectionFactory.setHost(rabbitmq.getHost());
		connectionFactory.setPort(rabbitmq.getAmqpPort());
		connectionFactory.setUsername(rabbitmq.getAdminUsername());
		connectionFactory.setPassword(rabbitmq.getAdminPassword());

		try {
			RabbitAdmin admin = new RabbitAdmin(connectionFactory);

			Queue testq = new Queue("scsapp-testq", false, false, false);
			Queue testq2 = new Queue("scsapp-testq2", false, false, false);
			admin.declareQueue(testq);
			admin.declareQueue(testq2);

			FanoutExchange exchange = new FanoutExchange("scsapp-testex", false, false);
			admin.declareExchange(exchange);

			Binding binding = BindingBuilder.bind(testq).to(exchange);
			admin.declareBinding(binding);
		}
		catch (Exception ex) {
			throw new IllegalStateException("Failed to create RabbitMQ test infrastructure", ex);
		}
		finally {
			connectionFactory.destroy();
		}

		System.setProperty("spring.rabbitmq.port", rabbitmq.getAmqpPort().toString());
	}

	@Test
	public void testRabbitSource() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(RabbitSourceTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=rabbitSupplier",
						"--rabbit.supplier.queues=scsapp-testq",
						"--rabbit.persistentDeliveryMode=true",
						"--spring.rabbitmq.listener.simple.concurrency=2",
						"--spring.rabbitmq.listener.simple.maxConcurrency=3",
						"--spring.rabbitmq.listener.simple.acknowledgeMode=AUTO",
						"--spring.rabbitmq.listener.simple.prefetch=10",
						"--spring.rabbitmq.listener.simple.transactionSize=5",
						"--spring.cloud.stream.function.bindings.rabbitSupplier-out-0=output"
				)) {

			final RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
			final OutputDestination target = context.getBean(OutputDestination.class);
			rabbitTemplate.convertAndSend("scsapp-testex", "", "hello");

			Message<byte[]> sourceMessage = target.receive(600000, "output");

			final String actual = new String(sourceMessage.getPayload());
			assertThat(actual).isEqualTo("hello");
		}
	}


	@Test
	public void testPropertiesPopulated() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(RabbitSourceTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=rabbitSupplier",
						"--rabbit.supplier.queues = scsapp-testq2",
						"--rabbit.supplier.enableRetry = true",
						"--rabbit.supplier.initialRetryInterval = 123",
						"--rabbit.supplier.maxRetryInterval = 345",
						"--rabbit.supplier.retryMultiplier = 1.5",
						"--rabbit.supplier.maxAttempts = 5",
						"--rabbit.supplier.requeue = false",
						"--rabbit.supplier.mappedRequestHeaders = STANDARD_REQUEST_HEADERS,bar",
						"--spring.rabbitmq.listener.simple.concurrency = 2",
						"--spring.rabbitmq.listener.simple.maxConcurrency = 3 ",
						"--spring.rabbitmq.listener.simple.acknowledgeMode = NONE",
						"--spring.rabbitmq.listener.simple.prefetch = 10",
						"--spring.rabbitmq.listener.simple.batchSize = 5"
				)) {

			final RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
			final SimpleMessageListenerContainer container = context.getBean(SimpleMessageListenerContainer.class);
			final OutputDestination target = context.getBean(OutputDestination.class);

			Advice[] adviceChain = TestUtils.getPropertyValue(container, "adviceChain", Advice[].class);
			assertThat(adviceChain.length).isEqualTo(1);
			RetryTemplate retryTemplate = TestUtils.getPropertyValue(adviceChain[0], "retryOperations",
					RetryTemplate.class);
			assertThat(TestUtils.getPropertyValue(retryTemplate, "retryPolicy.maxAttempts")).isEqualTo(5);
			assertThat(TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.initialInterval")).isEqualTo(123L);
			assertThat(TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.maxInterval")).isEqualTo(345L);
			assertThat(TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.multiplier")).isEqualTo(1.5);
			assertThat(container.getQueueNames()[0]).isEqualTo("scsapp-testq2");
			assertThat(TestUtils.getPropertyValue(container, "defaultRequeueRejected", Boolean.class)).isFalse();
			assertThat(TestUtils.getPropertyValue(container, "concurrentConsumers")).isEqualTo(2);
			assertThat(TestUtils.getPropertyValue(container, "maxConcurrentConsumers")).isEqualTo(3);
			assertThat(TestUtils.getPropertyValue(container, "acknowledgeMode")).isEqualTo(AcknowledgeMode.NONE);
			assertThat(TestUtils.getPropertyValue(container, "prefetchCount")).isEqualTo(10);
			assertThat(TestUtils.getPropertyValue(container, "batchSize")).isEqualTo(5);

			rabbitTemplate.convertAndSend("", "scsapp-testq2", "foo", message -> {
				message.getMessageProperties().getHeaders().put("bar", "baz");
				return message;
			});

			Message<byte[]> sourceMessage = target.receive(600000, "rabbitSupplier-out-0");

			final String actual = new String(sourceMessage.getPayload());
			assertThat(actual).isEqualTo("foo");
			assertThat(sourceMessage.getHeaders().get("bar")).isEqualTo("baz");
			assertThat(sourceMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE)).isNull();
		}
	}

	@SpringBootApplication
	@Import(RabbitSupplierConfiguration.class)
	public static class RabbitSourceTestApplication {
	}

}
