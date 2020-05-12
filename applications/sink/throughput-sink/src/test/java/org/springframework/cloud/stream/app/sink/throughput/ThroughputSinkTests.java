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

package org.springframework.cloud.stream.app.sink.throughput;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

@ExtendWith(OutputCaptureExtension.class)
public class ThroughputSinkTests {

	@Test
	public void testThroughputSink(CapturedOutput output) throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(ThroughputSinkTestConfiguration.class))
				.web(WebApplicationType.NONE)
				.run()) {

			final Message<String> message = MessageBuilder.withPayload("hello").build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);
			Awaitility.await().until(output::getOut, value -> value.contains("Messages:") && value.contains("Throughput:"));
		}
	}

	@EnableAutoConfiguration
	@Import(ThroughputSinkConfiguration.class)
	public static class ThroughputSinkTestConfiguration {
	}
}
