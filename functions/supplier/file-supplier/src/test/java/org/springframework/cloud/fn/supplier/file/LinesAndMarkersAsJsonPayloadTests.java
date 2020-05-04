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
import java.nio.file.StandardOpenOption;

import org.junit.jupiter.api.Test;
import org.springframework.integration.file.splitter.FileSplitter;
import org.springframework.integration.json.JsonPathUtils;
import org.springframework.integration.support.json.JsonObjectMapperProvider;
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
@TestPropertySource(properties = {"file.consumer.mode=lines", "file.consumer.withMarkers = true"})
public class LinesAndMarkersAsJsonPayloadTests extends AbstractFileSupplierTests {

	@Test
	public void testLinesWithMarkers() throws Exception {
		Path firstFile = tempDir.resolve("test.file");
		Files.write(firstFile, "first line\n".getBytes());
		Files.write(firstFile, "second line\n".getBytes(), StandardOpenOption.APPEND);

		final Flux<Message<?>> messageFlux = fileSupplier.get();

		StepVerifier.create(messageFlux)
				.assertNext((message) -> {
							try {
								final Object evaluate = JsonPathUtils.evaluate(message.getPayload(), "$.mark");
								assertThat(evaluate).isEqualTo(FileSplitter.FileMarker.Mark.START.name());
							} catch (IOException e) {
								// passt through
							}
						}
				)
				.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("first line"))
				.assertNext((message) -> assertThat(message.getPayload()).isEqualTo("second line"))
				.assertNext((message) -> {
							try {
								final Object fileMarker = JsonPathUtils.evaluate(message.getPayload(), "$.mark");
								assertThat(fileMarker).isEqualTo(FileSplitter.FileMarker.Mark.END.name());
								FileSplitter.FileMarker fileMarker1 = JsonObjectMapperProvider.newInstance()
										.fromJson(fileMarker, FileSplitter.FileMarker.class);
								assertThat(FileSplitter.FileMarker.Mark.END).isEqualTo(fileMarker1.getMark());
								assertThat(firstFile.toAbsolutePath()).isEqualTo(fileMarker1.getFilePath());
								assertThat(fileMarker1.getLineCount()).isEqualTo(2);
							} catch (IOException e) {
								// passt through
							}
						}
				)
				.thenCancel()
				.verify();
	}
}
