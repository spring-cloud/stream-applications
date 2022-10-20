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

import com.jayway.jsonpath.PathNotFoundException;
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

	static Throwable rootCause(Throwable x) {
		return x.getCause() == null ? x : rootCause(x.getCause());
	}

	@Test
	@Disabled
	public void testFilterProcessorFunctionWithException() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(FilterProcessorTestApplication.class))
			.web(WebApplicationType.NONE)
			.run("--spring.cloud.function.definition=byteArrayTextToString|filterFunction",
				"--filter.function.expression=#jsonPath(payload, '$.code') != '09'")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			String json = "{\"cd\":\"01\"}";

			try {
				processorInput.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)));
				Message<byte[]> sourceMessage = processorOutput.receive(10000);
				assertThat(sourceMessage).isNull();
			}
			catch (Throwable x) {
				Throwable root = rootCause(x);
				assertThat(root).isInstanceOf(PathNotFoundException.class);
			}
			try {
				processorInput.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)));
				Message<byte[]> sourceMessage2 = processorOutput.receive(10000);
				assertThat(sourceMessage2).isNull();
			}
			catch (Throwable x) {
				Throwable root = rootCause(x);
				assertThat(root).isInstanceOf(PathNotFoundException.class);
			}
		}
	}

	@Test
	public void testFilterProcessorFunctionWithValidMessage() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(FilterProcessorTestApplication.class))
			.web(WebApplicationType.NONE)
			.run("--spring.cloud.function.definition=byteArrayTextToString|filterFunction",
				"--filter.function.expression=#jsonPath(payload, '$.code') != '09'")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);
			String json = "{\"code\":\"01\"}";
			processorInput.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(sourceMessage).isNotNull();
			assertThat(new String(sourceMessage.getPayload())).isEqualTo(json);

			processorInput.send(new GenericMessage<>(json.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage2 = processorOutput.receive(10000);
			assertThat(sourceMessage2).isNotNull();
			assertThat(new String(sourceMessage2.getPayload())).isEqualTo(json);

		}
	}

	@SpringBootApplication
	@Import({FilterFunctionConfiguration.class})
	public static class FilterProcessorTestApplication {
	}

}
