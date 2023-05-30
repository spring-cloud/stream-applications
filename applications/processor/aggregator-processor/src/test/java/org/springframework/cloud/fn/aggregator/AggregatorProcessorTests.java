/*
 * Copyright 2020-2023 the original author or authors.
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

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
	public void testWithJdbcMessageStore() throws IOException {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(AggregatorProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=jsonBytesToMap|aggregatorFunction",
						"--aggregator.message-store-type=jdbc",
						"--aggregator.release=size()==2 or one.payload instanceof T(java.util.Map)")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			processorInput.send(createMessage("2", 2, 2));

			processorInput.send(createMessage("1", 1, 2));

			Message<byte[]> receive = processorOutput.receive(10_000, "jsonBytesToMapaggregatorFunction-out-0");

			assertThat(receive)
					.extracting(Message::getPayload)
					.extracting(String::new)
					.isEqualTo("[\"2\",\"1\"]");

			ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

			Person person = new Person("First1 Last1", "St. #1");
			processorInput.send(createMessage(objectMapper.writeValueAsBytes(person), 2, 2));

			receive = processorOutput.receive(10_000, "jsonBytesToMapaggregatorFunction-out-0");

			assertThat(receive).isNotNull();
			List<Person> result =
					objectMapper.readValue(receive.getPayload(),
							objectMapper.constructType(new TypeReference<List<Person>>() { }));

			assertThat(result).containsOnly(person);
		}
	}

	private static Message<?> createMessage(Object payload, int sequenceNumber, int sequenceSize) {
		return MessageBuilder.withPayload(payload)
				.setHeader(IntegrationMessageHeaderAccessor.CORRELATION_ID, "my_correlation")
				.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber)
				.setHeader(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize)
				.build();
	}

	@SpringBootApplication
	public static class AggregatorProcessorTestApplication {

	}

	private record Person(String name, String address) {
	}

}
