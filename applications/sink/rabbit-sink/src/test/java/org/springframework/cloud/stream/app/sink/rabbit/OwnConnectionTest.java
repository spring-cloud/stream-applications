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

package org.springframework.cloud.stream.app.sink.rabbit;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 * @author Chris Bono
 */
@TestPropertySource(properties = {"rabbit.routingKey=scsapp-testOwn",
		"rabbit.own-connection=true"})
public class OwnConnectionTest extends RabbitSinkIntegrationTests {

	@Test
	public void test() {
		this.rabbitAdmin.declareQueue(
				new Queue("scsapp-testOwn", false, false, true));

		// Destroy the boot connection factory - should not matter to outbound adapter as it SHOULD be using its own connection factory
		this.bootFactory.destroy();
		assertThat(this.bootFactory.getCacheProperties().getProperty("localPort")).isEqualTo("0");

		// Send to the channel - should still be consumed by outbound adapter as its using its own connection factory
		this.channels.send(MessageBuilder.withPayload("foo".getBytes()).build());

		// RabbitTemplate also using its own connection factory - should still be able to get the message
		assertThat(this.rabbitTemplate.getConnectionFactory()).isNotSameAs(bootFactory);
		this.rabbitTemplate.setReceiveTimeout(10000);
		Message received = this.rabbitTemplate.receive("scsapp-testOwn");
		assertThat(new String(received.getBody())).isEqualTo("foo");
	}
}
