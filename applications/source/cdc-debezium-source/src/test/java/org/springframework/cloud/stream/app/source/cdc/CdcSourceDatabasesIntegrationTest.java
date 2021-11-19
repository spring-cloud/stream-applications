/*
 * Copyright 2020-2021 the original author or authors.
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

package org.springframework.cloud.stream.app.source.cdc;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.receiveAll;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
@Tag("integration")
public class CdcSourceDatabasesIntegrationTest {

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.stream.function.definition=cdcSupplier",
					"cdc.name=my-connector",
					"cdc.flattening.dropTombstones=false",
					"cdc.schema=false",
					"cdc.flattening.enabled=true",
					"cdc.stream.header.offset=true",
					// "cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void mysql() {
		GenericContainer debeziumMySQL = new GenericContainer<>("debezium/example-mysql:1.7.1.Final")
				.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
				.withEnv("MYSQL_USER", "mysqluser")
				.withEnv("MYSQL_PASSWORD", "mysqlpw")
				// .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("mysql")))
				.withExposedPorts(3306);
		debeziumMySQL.start();

		String MAPPED_PORT = String.valueOf(debeziumMySQL.getMappedPort(3306));

		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=mysql",
						"--cdc.config.database.user=debezium",
						"--cdc.config.database.password=dbz",
						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=" + MAPPED_PORT)) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
		}
	}

	@Test
	@Disabled
	public void sqlServer() {
		GenericContainer sqlServer = new GenericContainer(new ImageFromDockerfile()
				.withFileFromClasspath("Dockerfile", "sqlserver/Dockerfile")
				.withFileFromClasspath("import-data.sh", "sqlserver/import-data.sh")
				.withFileFromClasspath("inventory.sql", "sqlserver/inventory.sql")
				.withFileFromClasspath("entrypoint.sh", "sqlserver/entrypoint.sh"))
				.withEnv("ACCEPT_EULA", "Y")
				.withEnv("MSSQL_PID", "Standard")
				.withEnv("SA_PASSWORD", "Password!")
				.withEnv("MSSQL_AGENT_ENABLED", "true")
				.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("sqlServer")))
				.withExposedPorts(1433);
		//sqlServer.waitingFor(Wait.forLogMessage(".*(1 rows affected).*", 50)).start();
		//sqlServer.waitingFor(Wait.forLogMessage(".*(Service Broker manager has started).*", 50)).start();
		sqlServer.start();

		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=sqlserver",
						// "--cdc.config.database.user=Standard",
						"--cdc.config.database.user=sa",
						"--cdc.config.database.password=Password!",
						"--cdc.config.database.dbname=testDB",

						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=" + sqlServer.getMappedPort(1433))) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(30);
		}
	}

	@Test
	public void postgres() {
		GenericContainer postgres = new GenericContainer("debezium/example-postgres:1.7.1.Final")
				.withEnv("POSTGRES_USER", "postgres")
				.withEnv("POSTGRES_PASSWORD", "postgres")
				.withExposedPorts(5432);
		postgres.start();

		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=postgres",
						"--cdc.config.database.user=postgres",
						"--cdc.config.database.password=postgres",
						"--cdc.config.slot.name=debezium",
						"--cdc.config.database.dbname=postgres",
						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=" + postgres.getMappedPort(5432))) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here

			List<Message<?>> allMessages = new ArrayList<>();
			Awaitility.await().atMost(Duration.ofMinutes(5)).until(() -> {
				List<Message<?>> messageChunk = receiveAll(outputDestination);
				if (!CollectionUtils.isEmpty(messageChunk)) {
					System.out.println("Chunk size: " + messageChunk.size());
					allMessages.addAll(messageChunk);
				}
				return allMessages.size() == 5786;
			});
		}
		postgres.stop();
	}

	@Test
	@Disabled
	public void mongodb() {
		GenericContainer mongodb = new GenericContainer("debezium/example-mongodb:1.7.1.Final")
				.withEnv("MONGODB_USER", "debezium")
				.withEnv("MONGODB_PASSWORD", "dbz")
				.withExposedPorts(27017);
		mongodb.start();
		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=mongodb",
						"--cdc.config.tasks.max=1",
						"--cdc.config.mongodb.hosts=rs0/localhost:" + mongodb.getMappedPort(27017),
						"--cdc.config.mongodb.name=dbserver1",
						"--cdc.config.mongodb.user=debezium",
						"--cdc.config.mongodb.password=dbz",
						"--cdc.config.collection.include.list=inventory[.]*")) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(666);
		}
		mongodb.stop();
	}

}
