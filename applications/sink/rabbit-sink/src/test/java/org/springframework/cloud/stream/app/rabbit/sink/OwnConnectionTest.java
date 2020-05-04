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
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@TestPropertySource(properties = {"rabbit.routingKey=scsapp-testOwn",
			"rabbit.own-connection=true"})
public class OwnConnectionTest extends RabbitSinkIntegrationTests {

	/**
	 * RabbitMQ
	 */
//	@ClassRule
//	public static GenericContainer rabbitMq = new GenericContainer("rabbitmq:3.5.3")
//			.withExposedPorts(5672);



	@Test
	public void test() {
		this.rabbitAdmin.declareQueue(
				new Queue("scsapp-testOwn", false, false, true));
		this.bootFactory.resetConnection();
		this.channels.send(MessageBuilder.withPayload("foo".getBytes())
				.build());
		this.rabbitTemplate.setReceiveTimeout(10000);
		Message received = this.rabbitTemplate.receive("scsapp-testOwn");
		assertEquals("foo", new String(received.getBody()));
		assertThat(this.bootFactory.getCacheProperties().getProperty("localPort")).isEqualTo("0");
	}
}
