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

package org.springframework.cloud.fn.common.cdc;

import org.junit.Test;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
public class CdcBootStarterIntegrationTest {

	private final JdbcTemplate jdbcTemplate = jdbcTemplate(
			"com.mysql.cj.jdbc.Driver",
			"jdbc:mysql://localhost:3306/inventory",
			"root",
			"debezium");

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(TestCdcApplication.class)
			.withPropertyValues(
					"cdc.name=my-sql-connector",
					"cdc.schema=false",
					"cdc.flattering.enabled=true",
					"cdc.stream.header.offset=true",
					"cdc.connector=mysql",
					"cdc.config.database.user=debezium",
					"cdc.config.database.password=dbz",
					"cdc.config.database.hostname=localhost",
					"cdc.config.database.port=3306",
					"cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void consumerTest() {
		contextRunner
				.withPropertyValues(
						"cdc.flattering.deleteHandlingMode=drop",
						"cdc.flattering.dropTombstones=true")
				.run(context -> {
					TestCdcApplication.TestSourceRecordConsumer testConsumer =
							context.getBean(TestCdcApplication.TestSourceRecordConsumer.class);
					jdbcTemplate.update("insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
					JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

					Thread.sleep(10000);
					assertThat(testConsumer.recordList).hasSizeGreaterThanOrEqualTo(52);
				});
	}

	public static JdbcTemplate jdbcTemplate(String jdbcDriver, String jdbcUrl, String user, String password) {
		DriverManagerDataSource dataSource = new DriverManagerDataSource();
		dataSource.setDriverClassName(jdbcDriver);
		dataSource.setUrl(jdbcUrl);
		dataSource.setUsername(user);
		dataSource.setPassword(password);
		return new JdbcTemplate(dataSource);
	}
}
