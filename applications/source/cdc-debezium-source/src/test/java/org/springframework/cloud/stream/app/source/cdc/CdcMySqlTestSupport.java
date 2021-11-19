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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.testcontainers.containers.GenericContainer;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author David Turanski
 */

@Tag("integration")
public abstract class CdcMySqlTestSupport {

	static final String DATABASE_NAME = "inventory";

	static String MAPPED_PORT;

	static GenericContainer debeziumMySQL = new GenericContainer<>("debezium/example-mysql:1.7.1.Final")
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			// .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("mysql")))
			.withExposedPorts(3306);

	static {
		debeziumMySQL.start();
	}

	static JdbcTemplate jdbcTemplate;

	@BeforeAll
	static void setup() {
		MAPPED_PORT = String.valueOf(debeziumMySQL.getMappedPort(3306));
		jdbcTemplate = CdcTestUtils.jdbcTemplate(
				"com.mysql.cj.jdbc.Driver",
				"jdbc:mysql://localhost:" + MAPPED_PORT + "/" + DATABASE_NAME + "?enabledTLSProtocols=TLSv1.2",
				"root",
				"debezium");
	}
}
