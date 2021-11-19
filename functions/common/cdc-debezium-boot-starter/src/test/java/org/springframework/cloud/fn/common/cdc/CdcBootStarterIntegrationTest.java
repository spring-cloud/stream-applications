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

package org.springframework.cloud.fn.common.cdc;

import java.time.Duration;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */

@Tag("integration")
@Testcontainers
public class CdcBootStarterIntegrationTest {

	private static final String DATABASE_NAME = "inventory";

	private static String MAPPED_PORT;

	@Container
	static GenericContainer debeziumMySQL =
			new GenericContainer<>(DockerImageName.parse("debezium/example-mysql:1.7.1.Final"))
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
					"spring.datasource.type=com.zaxxer.hikari.HikariDataSource",
					"cdc.name=my-sql-connector",
					"cdc.schema=false",
					"cdc.flattening.enabled=true",
					"cdc.stream.header.offset=true",
					"cdc.connector=mysql",
					"cdc.config.database.user=debezium",
					"cdc.config.database.password=dbz",
					"cdc.config.database.hostname=localhost",
					"cdc.config.database.port=" + MAPPED_PORT,
					"cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void consumerTest() {
		contextRunner
				.withPropertyValues(
						"cdc.flattening.deleteHandlingMode=drop",
						"cdc.flattening.dropTombstones=true")
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
