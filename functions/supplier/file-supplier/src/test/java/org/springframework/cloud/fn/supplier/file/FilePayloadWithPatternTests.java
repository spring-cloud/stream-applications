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
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import org.junit.jupiter.api.Test;
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
@TestPropertySource(properties = {"file.consumer.mode=ref", "file.supplier.filenamePattern = *.txt"})
public class FilePayloadWithPatternTests extends AbstractFileSupplierTests {

	@Test
	public void testPattern() throws IOException {

		Path txtFile1 = tempDir.resolve("test1.txt");
		Files.write(txtFile1, "one".getBytes());
		Path nonTxtExtension = tempDir.resolve("hello.bin");
		Files.write(nonTxtExtension, ByteBuffer.allocate(4).putInt(1).array());
		Path txtFile2 = tempDir.resolve("test2.txt");
		Files.write(txtFile2, "two".getBytes());

		final Flux<Message<?>> messageFlux = fileSupplier.get();

		StepVerifier.create(messageFlux)
				.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo(txtFile1.toAbsolutePath().toFile());
						}
				)
				.assertNext((message) -> {
					assertThat(message.getPayload())
							.isEqualTo(txtFile2.toAbsolutePath().toFile());
				})
				.expectNoEvent(Duration.ofSeconds(1))
				.thenCancel()
				.verify();
	}
}
