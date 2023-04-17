/*
 * Copyright 2023-2023 the original author or authors.
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

package org.springframework.cloud.fn.supplier.cdc.consumer;

import java.io.File;
import java.time.Duration;

import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.supplier.cdc.EmbeddedEngineExecutorService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Christian Tzolov
 */
@Tag("integration")
@Testcontainers
public class CdcBootStarterIntegrationTest {
	private static final Log logger = LogFactory.getLog(EmbeddedEngineExecutorService.class);

	private static final String DATABASE_NAME = "inventory";

	private static String MAPPED_PORT;

	@TempDir
	static File anotherTempDir;

	@Container
	static GenericContainer debeziumMySQL = new GenericContainer<>(
			DockerImageName.parse("debezium/example-mysql:2.1.4.Final"))
					.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
					.withEnv("MYSQL_USER", "mysqluser")
					.withEnv("MYSQL_PASSWORD", "mysqlpw")
					// .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("mysql")))
					.withExposedPorts(3306);

	private static JdbcTemplate jdbcTemplate;

	@BeforeAll
	static void setup() {
		MAPPED_PORT = String.valueOf(debeziumMySQL.getMappedPort(3306));
		jdbcTemplate = jdbcTemplate(
				"com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://localhost:" + MAPPED_PORT + "/" + DATABASE_NAME + "?enabledTLSProtocols=TLSv1.2",
				"root",
				"debezium");
	}

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestCdcApplication.class)
			.withPropertyValues(
					"cdc.disableDefaultConsumer=true",
					"spring.datasource.type=com.zaxxer.hikari.HikariDataSource",

					"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.FileOffsetBackingStore",
					"cdc.debezium.offset.storage.file.filename=" + anotherTempDir.getAbsolutePath() + "offsets.dat",
					"cdc.debezium.offset.flush.interval.ms=60000",

					"cdc.debezium.schema.history.internal=io.debezium.storage.file.history.FileSchemaHistory", // new
					"cdc.debezium.schema.history.internal.file.filename=" + anotherTempDir.getAbsolutePath()
							+ "schemahistory.dat", // new

					"cdc.debezium.topic.prefix=my-topic", // new

					"cdc.debezium.name=my-sql-connector",
					"cdc.debezium.connector.class=io.debezium.connector.mysql.MySqlConnector",

					"cdc.debezium.database.user=debezium",
					"cdc.debezium.database.password=dbz",
					"cdc.debezium.database.hostname=localhost",
					"cdc.debezium.database.port=" + MAPPED_PORT,
					"cdc.debezium.database.server.id=85744",
					"cdc.debezium.database.server.name=my-app-connector",
					"cdc.debezium.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void consumerTest() {

		logger.info("Temp dir: " + anotherTempDir.getAbsolutePath());

		contextRunner
				.withPropertyValues(
						// Flattering:
						// https://debezium.io/documentation/reference/stable/transformations/event-flattening.html
						"cdc.debezium.transforms=unwrap",
						"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
						"cdc.debezium.transforms.unwrap.drop.tombstones=false",
						"cdc.debezium.transforms.unwrap.delete.handling.mode=rewrite",
						"cdc.debezium.transforms.unwrap.add.fields=name,db")
				.run(context -> {
					TestCdcApplication.TestSourceRecordConsumer testConsumer = context
							.getBean(TestCdcApplication.TestSourceRecordConsumer.class);
					jdbcTemplate.update(
							"insert into `customers`(`first_name`,`last_name`,`email`) " +
									"VALUES('Test666', 'Test666', 'Test666@spring.org')");
					JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

					await().atMost(Duration.ofSeconds(30))
							.untilAsserted(() -> assertThat(testConsumer.recordList).hasSizeGreaterThanOrEqualTo(52));
				});
	}

	public static JdbcTemplate jdbcTemplate(String jdbcDriver, String jdbcUrl, String user, String password) {
		HikariDataSource dataSource = new HikariDataSource();
		dataSource.setDriverClassName(jdbcDriver);
		dataSource.setJdbcUrl(jdbcUrl);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		return new JdbcTemplate(dataSource);
	}

}
