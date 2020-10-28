/*
 * Copyright 2020-2020 the original author or authors.
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
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.app.test.integration.LogMatcher;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.receiveAll;

/**
 * @author Christian Tzolov
 * @author David Turanski
 */
public class CdcSourceDatabasesIntegrationTest extends CdcTestSupport {

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
					.web(WebApplicationType.NONE)
					.properties("spring.cloud.stream.function.definition=cdcSupplier",
							"cdc.name=my-sql-connector",
							"cdc.flattering.dropTombstones=false",
							"cdc.schema=false",
							"cdc.flattering.enabled=true",
							"cdc.stream.header.offset=true",
							// "cdc.config.database.server.id=85744",
							"cdc.config.database.server.name=my-app-connector",
							"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void mysql() {
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
	public void sqlServer() {
		LogMatcher logMatcher = LogMatcher.contains("(1 rows affected)").times(26);
		GenericContainer sqlServer = new GenericContainer(new ImageFromDockerfile()
				.withFileFromClasspath("Dockerfile", "sqlserver/Dockerfile")
				.withFileFromClasspath("import-data.sh", "sqlserver/import-data.sh")
				.withFileFromClasspath("inventory.sql", "sqlserver/inventory.sql")
				.withFileFromClasspath("entrypoint.sh", "sqlserver/entrypoint.sh"))
						.withEnv("ACCEPT_EULA", "Y")
						.withEnv("MSSQL_PID", "Standard")
						.withEnv("SA_PASSWORD", "Password!")
						.withEnv("MSSQL_AGENT_ENABLED", "true")
						.withLogConsumer(logMatcher)
						.withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("sqlServer")))
						.withExposedPorts(1433);
		sqlServer.start();
		assertThat(sqlServer.isRunning());
		await().atMost(Duration.ofSeconds(60)).until(logMatcher.matches());

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
		GenericContainer postgres = new GenericContainer("debezium/example-postgres:1.0")
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
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(5786);
		}
		postgres.stop();
	}

	// @Test
	public void mongodb() {
		GenericContainer mongodb = new GenericContainer("debezium/example-mongodb:1.0")
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
						"--cdc.config.database.whitelist=inventory")) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(666);
		}
		mongodb.stop();
	}
}
