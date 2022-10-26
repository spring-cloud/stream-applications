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

package org.springframework.cloud.stream.app.processor.filter;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.filter.FilterFunctionConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class FilterProcessorTests {

	@Test
	public void testFilterProcessor() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(FilterProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|filterFunction",
						"--filter.function.expression=payload.length() > 5")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			String longMessage = "hello world message";
			processorInput.send(new GenericMessage<>(longMessage.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo(longMessage);

			String shortMessage = "foo";
			processorInput.send(new GenericMessage<>(shortMessage.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage2 = processorOutput.receive(5000);
			assertThat(sourceMessage2).isNull();
		}
	}

	@Test
	@Disabled
	public void testFilterProcessorJson() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(FilterProcessorTestApplication.class))
			.web(WebApplicationType.NONE)
			.run("--spring.cloud.function.definition=byteArrayTextToString|filterFunction",
				"--filter.function.expression=#jsonPath(payload, '$.code') != '09'")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			// How do we configure a DeadLetter destination?
			String jsonCodeOne = "{\"code\":\"01\"}";
			processorInput.send(new GenericMessage<>(jsonCodeOne.getBytes()));
			Message<byte[]> sourceMessage2 = processorOutput.receive(10000);
			assertThat(sourceMessage2).isNotNull();
			assertThat(sourceMessage2.getPayload()).isNotNull();
			assertThat(new String(sourceMessage2.getPayload())).isEqualTo(jsonCodeOne);

			String jsonCodeNine = "{\"code\":\"09\"}";
			processorInput.send(new GenericMessage<>(jsonCodeNine.getBytes()));
			Message<byte[]> sourceMessage3 = processorOutput.receive(10000);
			assertThat(sourceMessage3).isNull();

			String jsonCdOne = "{\"cd\":\"01\"}";
			processorInput.send(new GenericMessage<>(jsonCdOne.getBytes()));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			// If the jsonPath throws an exception we expect the message to be sent to DLQ
			// If the jsonPath doesn't throw and exception and evaluates to null we expect the message to pass through the filter
			boolean expectFiltered = true;
			if(expectFiltered) {
				assertThat(sourceMessage).isNull();
			} else {
				assertThat(sourceMessage).isNotNull();
				assertThat(sourceMessage.getPayload()).isNotNull();
				assertThat(new String(sourceMessage.getPayload())).isEqualTo(jsonCdOne);
			}

			processorInput.send(new GenericMessage<>(jsonCdOne.getBytes()));
			Message<byte[]> sourceMessageX = processorOutput.receive(10000);
			// If the jsonPath throws an exception we expect the message to be sent to DLQ
			// If the jsonPath doesn't throw and exception and evaluates to null we expect the message to pass through the filter
			if(expectFiltered) {
				assertThat(sourceMessageX).isNull();
			} else {
				assertThat(sourceMessageX).isNotNull();
				assertThat(sourceMessageX.getPayload()).isNotNull();
				assertThat(new String(sourceMessageX.getPayload())).isEqualTo(jsonCdOne);
			}

			processorInput.send(new GenericMessage<>(jsonCodeOne.getBytes()));
			sourceMessage2 = processorOutput.receive(10000);
			assertThat(sourceMessage2).isNotNull();
			assertThat(sourceMessage2.getPayload()).isNotNull();
			assertThat(new String(sourceMessage2.getPayload())).isEqualTo(jsonCodeOne);

			processorInput.send(new GenericMessage<>(jsonCodeNine.getBytes()));
			sourceMessage3 = processorOutput.receive(10000);
			assertThat(sourceMessage3).isNull();
		}
	}

	@SpringBootApplication
	@Import({FilterFunctionConfiguration.class})
	public static class FilterProcessorTestApplication {
	}

}
