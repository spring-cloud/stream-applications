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

package org.springframework.cloud.stream.app.source.debezium.streambridge;

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.app.source.debezium.integration.DebeziumTestUtils;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@Tag("integration")
public class StreamBridgeIntegrationTest {

	private static final Log logger = LogFactory.getLog(StreamBridgeIntegrationTest.class);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestStreamBridgeSourceApplication.class))
					.web(WebApplicationType.NONE)
					.properties(
							"spring.cloud.function.definition=debeziumSupplier",
							// Flattering:
							// https://debezium.io/documentation/reference/stable/transformations/event-flattening.html
							"debezium.inner.transforms=unwrap",
							"debezium.inner.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
							"debezium.inner.transforms.unwrap.drop.tombstones=false",
							"debezium.inner.transforms.unwrap.delete.handling.mode=rewrite",
							"debezium.inner.transforms.unwrap.add.fields=name,db,op,table",

							"debezium.inner.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"debezium.inner.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"debezium.inner.schema=false",

							"debezium.inner.topic.prefix=my-topic",
							"debezium.inner.name=my-connector",
							"debezium.inner.database.server.id=85744",
							"debezium.inner.database.server.name=my-app-connector");

	@Test
	public void mysql() {

		try (GenericContainer<?> mySQL = new GenericContainer<>(DebeziumTestUtils.DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
				.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
				.withEnv("MYSQL_USER", "mysqluser")
				.withEnv("MYSQL_PASSWORD", "mysqlpw")
				.withExposedPorts(3306)
				.withStartupTimeout(Duration.ofSeconds(120))
				.withStartupAttempts(3)) {

			mySQL.start();

			try (ConfigurableApplicationContext context = applicationBuilder.run(
					"--debezium.inner.connector.class=io.debezium.connector.mysql.MySqlConnector",
					"--debezium.inner.database.user=debezium",
					"--debezium.inner.database.password=dbz",
					"--debezium.inner.database.hostname=localhost",
					"--debezium.inner.database.port=" + mySQL.getMappedPort(3306))) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);

				assertThat(messages).isNotNull();
				// Message size should correspond to the number of insert statements in:
				// https://github.com/debezium/container-images/blob/main/examples/mysql/2.2/inventory.sql
				assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
			}

			mySQL.stop();
		}
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { MongoAutoConfiguration.class, DataSourceAutoConfiguration.class })
	@Import(StreamBridgeDebeziumConsumerConfiguration.class)
	public static class TestStreamBridgeSourceApplication {
	}
}
