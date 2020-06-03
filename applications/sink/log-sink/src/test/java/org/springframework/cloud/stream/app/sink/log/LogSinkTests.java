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

package org.springframework.cloud.stream.app.sink.log;

import java.nio.charset.StandardCharsets;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.cloud.fn.consumer.log.LogConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Soby Chacko
 */
@ExtendWith(OutputCaptureExtension.class)
public class LogSinkTests {

	@Test
	public void testSourceFromSupplier(CapturedOutput output) {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration.getCompleteConfiguration(LogSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=byteArrayTextToString|logConsumer")) {

			InputDestination source = context.getBean(InputDestination.class);
			source.send(new GenericMessage<byte[]>("hello".getBytes(StandardCharsets.UTF_8)));
			Awaitility.await().until(output::getOut, value -> value.contains("hello"));
		}
	}

	@SpringBootApplication
	@Import(LogConsumerConfiguration.class)
	public static class LogSinkTestApplication {
	}
}
