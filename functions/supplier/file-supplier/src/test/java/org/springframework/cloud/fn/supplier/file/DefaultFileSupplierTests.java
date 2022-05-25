/*
 * Copyright 2020-2022 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.jdbc.metadata.JdbcMetadataStore;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Soby Chacko
 */
@TestPropertySource(properties = "metadata.store.type = jdbc")
public class DefaultFileSupplierTests extends AbstractFileSupplierTests {

	@Autowired
	@Qualifier("fileMessageSource")
	private FileReadingMessageSource fileMessageSource;

	@Autowired
	private ConcurrentMetadataStore metadataStore;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	public void testBasicFlow() throws IOException {
		Path firstFile = tempDir.resolve("first.file");
		Files.write(firstFile, "first.file".getBytes());

		final Flux<Message<?>> messageFlux = fileSupplier.get();

		//create file after subscription
		Path tempFile = tempDir.resolve("test.file");

		StepVerifier stepVerifier =
				StepVerifier.create(messageFlux)
						.assertNext((message) -> {
									assertThat(message.getPayload())
											.isEqualTo("first.file".getBytes());
									assertThat(message.getHeaders())
											.containsEntry(FileHeaders.FILENAME, "first.file");
									assertThat(message.getHeaders())
											.containsEntry(FileHeaders.RELATIVE_PATH, "first.file");
									assertThat(message.getHeaders())
											.containsEntry(FileHeaders.ORIGINAL_FILE, firstFile.toFile());
								}
						)
						.assertNext((message) -> {
							assertThat(message.getPayload())
									.isEqualTo("testing".getBytes());
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.FILENAME, "test.file");
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.RELATIVE_PATH, "test.file");
							assertThat(message.getHeaders())
									.containsEntry(FileHeaders.ORIGINAL_FILE, tempFile.toFile());
						})
						.thenCancel()
						.verifyLater();
		Files.write(tempFile, "testing".getBytes());
		stepVerifier.verify();

		assertThat(this.fileMessageSource.isRunning()).isTrue();

		assertThat(this.metadataStore).isInstanceOf(JdbcMetadataStore.class);

		List<String> metadataStoreContent =
				jdbcTemplate.queryForList("SELECT metadata_key FROM int_metadata_store", String.class);

		assertThat(metadataStoreContent).hasSize(2);
		assertThat(metadataStoreContent.get(0)).startsWith("local-file-system-metadata-");
		assertThat(metadataStoreContent.get(0)).endsWith("first.file");
		assertThat(metadataStoreContent.get(1)).endsWith("test.file");

		/* See AbstractFileSupplierTests.FileSupplierTestApplication.fileInboundChannelAdapterSpecCustomizer() -
		 through the ComponentCustomizer and @CustomizationAware on the FileSupplierConfiguration.fileMessageSource()
		 the provided customization is populated down to the bean under testing.
		*/
		assertThat(TestUtils.getPropertyValue(this.fileMessageSource, "watchEvents",
				FileReadingMessageSource.WatchEventType[].class))
				.isEqualTo(new FileReadingMessageSource.WatchEventType[]{ FileReadingMessageSource.WatchEventType.DELETE });
	}

}
