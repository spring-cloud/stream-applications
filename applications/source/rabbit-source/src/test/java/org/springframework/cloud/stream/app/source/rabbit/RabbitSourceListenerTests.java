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
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
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
import org.testcontainers.containers.RabbitMQContainer;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for RabbitSource.
 *
 * @author Gary Russell
 * @author Chris Schaefer
 */
public class RabbitSourceListenerTests {

    static {
        RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.7-management-alpine")
                .withQueue("scsapp-testq", false, false, new HashMap<>())
                .withQueue("scsapp-testq2", false, false, new HashMap<>())
                .withQueue("scsapp-testOwnSource", false, false, new HashMap<>())
                .withExchange("scsapp-testex", "fanout")
                .withBinding("scsapp-testex", "scsapp-testq");
        rabbitmq.start();

        System.setProperty("spring.rabbitmq.test.port", rabbitmq.getAmqpPort().toString());
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
                     "--spring.rabbitmq.port=" +
                             "${spring.rabbitmq.test.port}"
                )) {

            final RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
            rabbitTemplate.convertAndSend("scsapp-testex", "", "hello");

            OutputDestination target = context.getBean(OutputDestination.class);
            Message<byte[]> sourceMessage = target.receive(600000);

            final String actual = new String(sourceMessage.getPayload());
            assertThat(actual).isEqualTo("hello");
        }
    }

    @Test
    public void testOwnConnection() {
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
                TestChannelBinderConfiguration
                        .getCompleteConfiguration(RabbitSourceTestApplication.class))
                .web(WebApplicationType.NONE)
                .run("--spring.cloud.function.definition=rabbitSupplier",
                     "--rabbit.supplier.queues=scsapp-testOwnSource",
                     "--rabbit.supplier.enableRetry=false",
                     "--rabbit.supplier.own-connection=true",
                     "--spring.rabbitmq.port=" +
                             "${spring.rabbitmq.test.port}"
                )) {

            final RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
            final CachingConnectionFactory bootFactory = context.getBean(CachingConnectionFactory.class);
            rabbitTemplate.convertAndSend("scsapp-testOwnSource", "foo");

            bootFactory.resetConnection();

            OutputDestination target = context.getBean(OutputDestination.class);
            Message<byte[]> sourceMessage = target.receive(600000);

            final String actual = new String(sourceMessage.getPayload());
            assertThat(actual).isEqualTo("foo");
            assertThat(bootFactory.getCacheProperties().getProperty("localPort")).isEqualTo("0");
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
                     "--spring.rabbitmq.listener.simple.batchSize = 5",
                     "--spring.rabbitmq.port=" +
                             "${spring.rabbitmq.test.port}"
                )) {

            final RabbitTemplate rabbitTemplate = context.getBean(RabbitTemplate.class);
            final SimpleMessageListenerContainer container = context.getBean(SimpleMessageListenerContainer.class);
            Advice[] adviceChain = TestUtils.getPropertyValue(container, "adviceChain", Advice[].class);
            assertEquals(1, adviceChain.length);
            RetryTemplate retryTemplate = TestUtils.getPropertyValue(adviceChain[0], "retryOperations",
                                                                     RetryTemplate.class);
            assertEquals(5, TestUtils.getPropertyValue(retryTemplate, "retryPolicy.maxAttempts"));
            assertEquals(123L, TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.initialInterval"));
            assertEquals(345L, TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.maxInterval"));
            assertEquals(1.5, TestUtils.getPropertyValue(retryTemplate, "backOffPolicy.multiplier"));
            assertEquals("scsapp-testq2", container.getQueueNames()[0]);
            assertFalse(TestUtils.getPropertyValue(container, "defaultRequeueRejected", Boolean.class));
            assertEquals(2, TestUtils.getPropertyValue(container, "concurrentConsumers"));
            assertEquals(3, TestUtils.getPropertyValue(container, "maxConcurrentConsumers"));
            assertEquals(AcknowledgeMode.NONE, TestUtils.getPropertyValue(container, "acknowledgeMode"));
            assertEquals(10, TestUtils.getPropertyValue(container, "prefetchCount"));
            assertEquals(5, TestUtils.getPropertyValue(container, "batchSize"));

            rabbitTemplate.convertAndSend("", "scsapp-testq2", "foo", message -> {
                message.getMessageProperties().getHeaders().put("bar", "baz");
                return message;
            });

            OutputDestination target = context.getBean(OutputDestination.class);
            Message<byte[]> sourceMessage = target.receive(600000);

            final String actual = new String(sourceMessage.getPayload());
            assertEquals("foo", actual);
            assertEquals("baz", sourceMessage.getHeaders().get("bar"));
            assertNull(sourceMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
        }
    }

    @SpringBootApplication
    @Import(RabbitSupplierConfiguration.class)
    public static class RabbitSourceTestApplication {
    }
}
