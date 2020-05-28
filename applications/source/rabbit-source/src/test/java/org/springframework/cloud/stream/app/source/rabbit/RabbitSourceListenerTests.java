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

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.rabbit.RabbitSupplierConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.testcontainers.containers.RabbitMQContainer;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class RabbitSourceListenerTests {
    static {
        RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.7-management-alpine")
                .withQueue("scsapp-testq", false, false, new HashMap<>())
                .withExchange("scsapp-testex", "fanout")
                .withBinding("scsapp-testex", "scsapp-testq");
        rabbitmq.start();

        System.setProperty("spring.rabbitmq.test.port", rabbitmq.getAmqpPort().toString());
    }

    @Test
    public void testMqttSource() {
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

    @SpringBootApplication
    @Import(RabbitSupplierConfiguration.class)
    public static class RabbitSourceTestApplication {
    }
}
