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

package org.springframework.cloud.stream.app.sink.jdbc;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.jdbc.JdbcConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class JdbcSinkTests {

	@Test
	public void testSimpleInserts() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(JdbcSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=jdbcConsumer")) {

			Payload sent = new Payload("hello", 42);
			final Message<Payload> message = MessageBuilder.withPayload(sent).build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);

			final JdbcOperations jdbcOperations = context.getBean(JdbcOperations.class);
			String result = jdbcOperations.queryForObject("select payload from messages", String.class);
			assertThat(result).isEqualTo(("hello42"));
		}
	}

	@SpringBootApplication
	@Import(JdbcConsumerConfiguration.class)
	public static class JdbcSinkTestApplication {
	}

	static class Payload {

		private String a;

		private Integer b;

		Payload() {
		}

		Payload(String a, Integer b) {
			this.a = a;
			this.b = b;
		}

		public String getA() {
			return a;
		}

		public void setA(String a) {
			this.a = a;
		}

		public Integer getB() {
			return b;
		}

		public void setB(Integer b) {
			this.b = b;
		}

		@Override
		public String toString() {
			return a + b;
		}

	}
}
