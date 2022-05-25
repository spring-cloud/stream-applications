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

import java.nio.file.Path;
import java.util.Date;
import java.util.function.Supplier;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.fn.common.config.ComponentCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.dsl.FileInboundChannelAdapterSpec;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Soby Chacko
 */
@SpringBootTest
@DirtiesContext
public class AbstractFileSupplierTests {

	@TempDir
	static Path tempDir;

	@Autowired
	Supplier<Flux<Message<?>>> fileSupplier;

	@BeforeAll
	public static void beforeAll() {
		System.setProperty("file.supplier.directory", tempDir.toAbsolutePath().toString());
	}

	@AfterAll
	public static void afterAll() {
		System.clearProperty("file.supplier.directory");
	}

	@SpringBootApplication
	static class FileSupplierTestApplication {

		@Bean
		ComponentCustomizer<FileInboundChannelAdapterSpec> fileInboundChannelAdapterSpecCustomizer() {
			return (adapterSpec, beanName) -> adapterSpec.watchEvents(FileReadingMessageSource.WatchEventType.DELETE);
		}

		@Bean
		ComponentCustomizer<Date> fakeCustomizer() {
			return (date, beanName) -> {
				throw new RuntimeException("Must not happen");
			};
		}

	}

}
