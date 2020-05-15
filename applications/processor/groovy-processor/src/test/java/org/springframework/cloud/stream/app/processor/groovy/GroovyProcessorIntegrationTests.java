/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.cloud.stream.app.processor.groovy;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

public class GroovyProcessorIntegrationTests {

	@Test
	public void testGroovyProcessorWithScript() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(GroovyProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|groovyProcessorFunction",
						"--groovy-processor.script=script.groovy",
						"--groovy-processor.variables=limit=5\n foo=\\\40WORLD")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			String inMessage = "hello world";
			processorInput.send(new GenericMessage<>(inMessage.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("hello WORLD");
		}
	}

	@Test
	public void testGroovyProcessorWithGrab() {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(GroovyProcessorTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|groovyProcessorFunction",
						"--groovy-processor.script=script-with-grab.groovy")) {

			InputDestination processorInput = context.getBean(InputDestination.class);
			OutputDestination processorOutput = context.getBean(OutputDestination.class);

			String inMessage = "def foo=bar";
			processorInput.send(new GenericMessage<>(inMessage.getBytes(StandardCharsets.UTF_8)));
			Message<byte[]> sourceMessage = processorOutput.receive(10000);
			assertThat(new String(sourceMessage.getPayload())).isEqualTo("var foo = bar;" + System.lineSeparator());
		}
	}


	@SpringBootApplication
	@Import({GroovyProcessorConfiguration.class})
	public static class GroovyProcessorTestApplication {
	}
}
