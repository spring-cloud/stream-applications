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

package org.springframework.cloud.stream.app.source.debezium.databases;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

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
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests integration with supported Debezium connector datastores. It uses the Debezium pre-build example-images for
 * those datastores and pre-generated data for them.
 *
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
@Tag("integration")
public class DebeziumDatabasesIntegrationTest {

	private static final Log logger = LogFactory.getLog(DebeziumDatabasesIntegrationTest.class);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration
					.getCompleteConfiguration(DebeziumDatabasesIntegrationTest.TestApplication.class))
			.web(WebApplicationType.NONE)
			.properties(
					"spring.cloud.function.definition=debeziumSupplier",
					// Flattening:
					// https://debezium.io/documentation/reference/stable/transformations/event-flattening.html
					"debezium.properties.transforms=unwrap",
					"debezium.properties.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
					"debezium.properties.transforms.unwrap.drop.tombstones=false",
					"debezium.properties.transforms.unwrap.delete.handling.mode=rewrite",
					"debezium.properties.transforms.unwrap.add.fields=name,db,op,table",

					"debezium.properties.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
					"debezium.properties.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

					"debezium.properties.schema=false",

					"debezium.properties.topic.prefix=my-topic",
					"debezium.properties.name=my-connector",
					"debezium.properties.database.server.id=85744");

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
					"--debezium.properties.connector.class=io.debezium.connector.mysql.MySqlConnector",
					"--debezium.properties.database.user=debezium",
					"--debezium.properties.database.password=dbz",
					"--debezium.properties.database.hostname=localhost",
					"--debezium.properties.database.port=" + mySQL.getMappedPort(3306))) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);

				assertThat(messages).isNotNull();
				// Message size should correspond to the number of insert statements in:
				// https://github.com/debezium/container-images/blob/main/examples/mysql/2.3/inventory.sql
				assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
			}
			mySQL.stop();
		}
	}

	@Test
	public void postgres() {
		try (GenericContainer<?> postgres = new GenericContainer<>(DebeziumTestUtils.DEBEZIUM_EXAMPLE_POSTGRES_IMAGE)
				.withEnv("POSTGRES_USER", "postgres")
				.withEnv("POSTGRES_PASSWORD", "postgres")
				.withExposedPorts(5432)
				.withStartupTimeout(Duration.ofSeconds(120))
				.withStartupAttempts(3)) {

			postgres.start();

			try (ConfigurableApplicationContext context = applicationBuilder.run(
					"--debezium.properties.connector.class=io.debezium.connector.postgresql.PostgresConnector",
					"--debezium.properties.database.user=postgres",
					"--debezium.properties.database.password=postgres",
					"--debezium.properties.slot.name=debezium",
					"--debezium.properties.database.dbname=postgres",
					"--debezium.properties.database.hostname=localhost",
					"--debezium.properties.database.port=" + postgres.getMappedPort(5432))) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> allMessages = new ArrayList<>();
				Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> {
					List<Message<?>> messageChunk = DebeziumTestUtils.receiveAll(outputDestination);
					if (!CollectionUtils.isEmpty(messageChunk)) {
						logger.info("Chunk size: " + messageChunk.size());
						allMessages.addAll(messageChunk);
					}
					// Message size should correspond to the number of insert statements in the sample inventor DB:
					// https://github.com/debezium/container-images/blob/main/examples/postgres/2.3/inventory.sql
					return allMessages.size() == 29; // Inventory DB entries
				});
			}

			postgres.stop();
		}
	}

	@Test
	public void mssql() {
		try (GenericContainer<?> mssql = new MSSQLServerContainer("mcr.microsoft.com/mssql/server:2022-latest")
				.acceptLicense()
				.withInitScript("docker/mssql/init.sql")
				.withEnv("MSSQL_AGENT_ENABLED", "true")
				.withEnv("MSSQL_PID", "Standard")
				.withStartupTimeout(Duration.ofSeconds(120))
				.withStartupAttempts(3)
				.withExposedPorts(1433)) {

			mssql.start();

			try (ConfigurableApplicationContext context = applicationBuilder.run(
					"--debezium.properties.connector.class=io.debezium.connector.sqlserver.SqlServerConnector",
					"--debezium.properties.database.user=" + ((MSSQLServerContainer<?>) mssql).getUsername(),
					"--debezium.properties.database.password=" + ((MSSQLServerContainer<?>) mssql).getPassword(),
					"--debezium.properties.database.encrypt=false",
					"--debezium.properties.database.names=testDB",
					"--debezium.properties.database.hostname=localhost",
					"--debezium.properties.database.port=" + mssql.getMappedPort(1433))) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> allMessages = new ArrayList<>();
				Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> {
					List<Message<?>> messageChunk = DebeziumTestUtils.receiveAll(outputDestination);
					if (!CollectionUtils.isEmpty(messageChunk)) {
						logger.info("Chunk size: " + messageChunk.size());
						allMessages.addAll(messageChunk);
					}
					// Message size should correspond to the number of insert statements in the sample inventor DB:
					// src/test/resources/docker/mssql/init.sql
					return allMessages.size() == 31; // Inventory DB entries
				});
			}
			mssql.stop();
		}
	}

	@Test
	@Disabled
	public void mongodb() {
		GenericContainer<?> mongodb =
				new GenericContainer<>(DockerImageName.parse(DebeziumTestUtils.DEBEZIUM_EXAMPLE_MONGODB_IMAGE))
						.withEnv("MONGODB_USER", "debezium")
						.withEnv("MONGODB_PASSWORD", "dbz")
						.withExposedPorts(27017)
						.withStartupTimeout(Duration.ofSeconds(120))
						.withStartupAttempts(3);

		mongodb.start();
		String id = mongodb.getContainerId();
		// String host = id.substring(0, 12);
		String host = mongodb.getHost();
		String port = "" + mongodb.getMappedPort(27017);
		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--debezium.properties.connector.class=io.debezium.connector.mongodb.MongoDbConnector",
						"--debezium.properties.topic.prefix=fullfillment",
						"--debezium.properties.tasks.max=1",
						"--debezium.properties.mongodb.connection.string=mongodb://" + host + ":" + port
								+ "/?replicaSet=rs0",
						"--debezium.properties.topic.prefix=dbserver1",
						"--debezium.properties.mongodb.user=debezium",
						"--debezium.properties.mongodb.password=dbz",
						"--debezium.properties.collection.include.list=inventory[.]*")) {

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			// Number of entries should match the entries inserted by:
			// https://github.com/debezium/container-images/blob/main/examples/mongodb/2.3/init-inventory.sh
			assertThat(messages).hasSize(666);
		}
		mongodb.stop();
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = {MongoAutoConfiguration.class, DataSourceAutoConfiguration.class})
	public static class TestApplication {
	}

}
