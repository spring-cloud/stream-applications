/*
 * Copyright 2020-2023 the original author or authors.
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

package org.springframework.cloud.stream.app.source.debezium.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.messaging.Message;
import org.springframework.util.StreamUtils;

/**
 * @author Christian Tzolov
 */
public final class DebeziumTestUtils {

	public static final String DATABASE_NAME = "inventory";

	public static final String BINDING_NAME = "debeziumSupplier-out-0";

	public static final String IMAGE_TAG = "2.3.3.Final";

	public static final String DEBEZIUM_EXAMPLE_MYSQL_IMAGE = "debezium/example-mysql:" + IMAGE_TAG;

	public static final String DEBEZIUM_EXAMPLE_POSTGRES_IMAGE = "debezium/example-postgres:" + IMAGE_TAG;

	public static final String DEBEZIUM_EXAMPLE_MONGODB_IMAGE = "debezium/example-mongodb:" + IMAGE_TAG;


	private DebeziumTestUtils() {

	}

	public static List<Message<?>> receiveAll(OutputDestination outputDestination) {
		return receiveAll(outputDestination, BINDING_NAME);
	}

	public static List<Message<?>> receiveAll(OutputDestination outputDestination, String bindingName) {
		List<Message<?>> list = new ArrayList<>();
		Message<?> received;
		do {
			received = outputDestination.receive(Duration.ofSeconds(30).toMillis(), bindingName);
			if (received != null) {
				list.add(received);
			}
		}
		while (received != null);
		return list;
	}

	public static String resourceToString(String resourcePath) throws IOException {
		return StreamUtils.copyToString(
				new DefaultResourceLoader().getResource(resourcePath).getInputStream(), StandardCharsets.UTF_8);
	}
}
