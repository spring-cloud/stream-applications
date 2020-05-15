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

package org.springframework.cloud.stream.app.file.sink;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.consumer.file.FileConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.FileCopyUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Soby Chacko
 */
public class FileSinkTests {

	@TempDir
	static Path tempDir;

	@Test
	public void testFileSink() throws Exception {
		try (ConfigurableApplicationContext context = new SpringApplicationBuilder(
				TestChannelBinderConfiguration
						.getCompleteConfiguration(FileSinkTestApplication.class))
				.web(WebApplicationType.NONE)
				.run("--spring.cloud.function.definition=fileConsumer",
						"--file.consumer.name=test",
						"--file.consumer.suffix=txt",
						"--file.consumer.directory=" + tempDir.toAbsolutePath().toString())) {

			final Message<String> message = MessageBuilder.withPayload("hello").build();
			InputDestination source = context.getBean(InputDestination.class);
			source.send(message);
			File file = new File(tempDir.toFile(), "test.txt");
			assertThat(file.exists()).isTrue();
			assertThat("hello" + System.lineSeparator())
					.isEqualTo(FileCopyUtils.copyToString(new FileReader(file)));
		}
	}

	@SpringBootApplication
	@Import(FileConsumerConfiguration.class)
	public static class FileSinkTestApplication {
	}
}
