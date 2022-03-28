/*
 * Copyright 2020-2022 the original author or authors.
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

package org.springframework.cloud.fn.aggregator;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

public class AggregatorProcessorTests {

	@Test
	public void testWithJdbcMessageStore() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(AggregatorProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=aggregatorFunction",
						"--aggregator.message-store-type=jdbc")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(
					MessageBuilder.withPayload("2")
							.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 2)
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
							.build());
			processorInput.send(
					MessageBuilder.withPayload("1")
							.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, 1)
							.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, 2)
							.build());

			Message<byte[]> receive = processorOutput.receive(10_000, "aggregatorFunction-out-0");

			assertThat(receive).isNotNull()
					.extracting(Message::getPayload)
					.extracting(String::new)
					.isEqualTo("[\"2\",\"1\"]");
		}
	}

	@SpringBootApplication
	public static class AggregatorProcessorTestApplication {

	}

}
