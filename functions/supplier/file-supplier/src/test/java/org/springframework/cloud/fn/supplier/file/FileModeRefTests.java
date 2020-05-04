/*
 * Copyright 2020 the original author or authors.
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

package org.springframework.cloud.fn.supplier.file;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.springframework.integration.file.FileHeaders;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Soby Chacko
 */
@TestPropertySource(properties = "file.consumer.mode=ref")
public class FileModeRefTests extends AbstractFileSupplierTests {

	@Test
	public void testBasicFlow() throws IOException {

		Path firstFile = tempDir.resolve("first.file");
		Files.write(firstFile, "first.file".getBytes());

		final Flux<Message<?>> messageFlux = fileSupplier.get();

		StepVerifier.create(messageFlux)
				.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo(firstFile.toAbsolutePath().toFile());
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.FILENAME, "first.file");
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.RELATIVE_PATH, "first.file");
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.ORIGINAL_FILE, firstFile.toFile());
						}
				)
				.thenCancel()
				.verify();
	}
}
