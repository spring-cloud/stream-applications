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

package org.springframework.cloud.fn.consumer.file;

import java.nio.file.Path;
import java.util.function.Consumer;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.test.annotation.DirtiesContext;

/**
 * @author Soby Chacko
 */
@SpringBootTest
@DirtiesContext
public class AbstractFileConsumerTests {

	@TempDir
	static Path tempDir;

	@Autowired
	Consumer<Message<?>> fileConsumer;

	@BeforeAll
	public static void beforeAll() {
		System.setProperty("file.consumer.directory", tempDir.toAbsolutePath().toString());
	}

	@AfterAll
	public static void afterAll() {
		System.clearProperty("file.consumer.directory");
	}

	@SpringBootApplication
	static class TestApplication {
	}
}
