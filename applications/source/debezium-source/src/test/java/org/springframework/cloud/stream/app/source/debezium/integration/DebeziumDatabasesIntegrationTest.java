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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
@Tag("integration")
public class DebeziumDatabasesIntegrationTest {

	private static final Log logger = LogFactory.getLog(DebeziumDatabasesIntegrationTest.class);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
					.web(WebApplicationType.NONE)
					.properties(
							"spring.cloud.function.definition=debeziumSupplier",
							// Flattering:
							// https://debezium.io/documentation/reference/stable/transformations/event-flattening.html
							"cdc.debezium.transforms=unwrap",
							"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
							"cdc.debezium.transforms.unwrap.drop.tombstones=false",
							"cdc.debezium.transforms.unwrap.delete.handling.mode=rewrite",
							"cdc.debezium.transforms.unwrap.add.fields=name,db,op,table",

							"cdc.debezium.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"cdc.debezium.schema=false",

							"cdc.debezium.topic.prefix=my-topic",
							"cdc.debezium.name=my-connector",
							"cdc.debezium.database.server.id=85744",
							"cdc.debezium.database.server.name=my-app-connector");

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
					"--cdc.debezium.connector.class=io.debezium.connector.mysql.MySqlConnector",
					"--cdc.debezium.database.user=debezium",
					"--cdc.debezium.database.password=dbz",
					"--cdc.debezium.database.hostname=localhost",
					"--cdc.debezium.database.port=" + mySQL.getMappedPort(3306),

					// JdbcTemplate configuration
					String.format("--app.datasource.url=jdbc:mysql://localhost:%d/%s?enabledTLSProtocols=TLSv1.2",
							mySQL.getMappedPort(3306), DebeziumTestUtils.DATABASE_NAME),
					"--app.datasource.username=root",
					"--app.datasource.password=debezium")) {

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

	@Test
	public void postgres() {
		try (GenericContainer<?> postgres = new GenericContainer(DebeziumTestUtils.DEBEZIUM_EXAMPLE_POSTGRES_IMAGE)
				.withEnv("POSTGRES_USER", "postgres")
				.withEnv("POSTGRES_PASSWORD", "postgres")
				.withExposedPorts(5432)
				.withStartupTimeout(Duration.ofSeconds(120))
				.withStartupAttempts(3)) {

			postgres.start();

			try (ConfigurableApplicationContext context = applicationBuilder.run(
					"--cdc.debezium.connector.class=io.debezium.connector.postgresql.PostgresConnector",
					"--cdc.debezium.database.user=postgres",
					"--cdc.debezium.database.password=postgres",
					"--cdc.debezium.slot.name=debezium",
					"--cdc.debezium.database.dbname=postgres",
					"--cdc.debezium.database.hostname=localhost",
					"--cdc.debezium.database.port=" + postgres.getMappedPort(5432),
					// JdbcTemplate configuration
					String.format("--app.datasource.url=jdbc:postgresql://localhost:%d/%s",
							postgres.getMappedPort(5432), DebeziumTestUtils.DATABASE_NAME),
					"--app.datasource.username=postgres",
					"--app.datasource.password=postgres")) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> allMessages = new ArrayList<>();
				Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> {
					List<Message<?>> messageChunk = DebeziumTestUtils.receiveAll(outputDestination);
					if (!CollectionUtils.isEmpty(messageChunk)) {
						logger.info("Chunk size: " + messageChunk.size());
						allMessages.addAll(messageChunk);
					}
					// Message size should correspond to the number of insert statements in the sample inventor DB
					// configured in:
					// https://github.com/debezium/container-images/blob/main/examples/postgres/2.2/inventory.sql
					return allMessages.size() == 29; // Inventory DB entries
				});
			}

			postgres.stop();
		}
	}

}