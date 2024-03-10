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

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.Message;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for RabbitSource with headersMappedLast.
 *
 * @author Nicolas Labrot
 */
public class HeadersMappedLastTests {

	@TestPropertySource(properties = "rabbit.consumer.routingKey=scsapp-testq")
	public static class HeadersMappedLastTrueTests extends RabbitSinkIntegrationTests {

		@Test
		public void test() {
			this.channels.send(MessageBuilder.withPayload("{}".getBytes())
					.setHeader("content-type", "application/json")
					.build());
			this.rabbitTemplate.setReceiveTimeout(10000);
			Message received = this.rabbitTemplate.receive("scsapp-testq");
			assertThat(new String(received.getBody())).isEqualTo("{}");
			assertThat(received.getMessageProperties().getContentType()).isEqualTo("application/json");
		}
	}

	@TestPropertySource(properties = { "rabbit.consumer.routingKey=scsapp-testq",
			"rabbit.consumer.headersMappedLast=false" })
	public static class HeadersMappedLastFalseTests extends RabbitSinkIntegrationTests {

		@Test
		public void test() {
			this.channels.send(MessageBuilder.withPayload("{}".getBytes())
					.setHeader("content-type", "application/json")
					.build());
			this.rabbitTemplate.setReceiveTimeout(10000);
			Message received = this.rabbitTemplate.receive("scsapp-testq");
			assertThat(new String(received.getBody())).isEqualTo("{}");
			//TODO: What should be the actual content type?
			assertThat(received.getMessageProperties().getContentType()).isEqualTo("application/octet-stream");
		}
	}
}
