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

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.receiveAll;

/**
 * @author Christian Tzolov
 */
public class CdcSourceDatabasesIntegrationTest<b> {

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.web(WebApplicationType.NONE)
			.properties("spring.cloud.stream.function.definition=cdcSupplier",
					"cdc.name=my-sql-connector",
					"cdc.flattering.dropTombstones=false",
					"cdc.schema=false",
					"cdc.flattering.enabled=true",
					"cdc.stream.header.offset=true",

					"cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void mysql() {
		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=mysql",
						"--cdc.config.database.user=debezium",
						"--cdc.config.database.password=dbz",
						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=3306")) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(52);
		}
	}

	@Test
	public void sqlServer() {

		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=sqlserver",
						//"--cdc.config.database.user=Standard",
						"--cdc.config.database.user=sa",
						"--cdc.config.database.password=Password!",
						"--cdc.config.database.dbname=testDB",

						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=1433"
				)) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(30);
		}
	}

	@Test
	public void postgres() {
		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=postgres",
						"--cdc.config.database.user=postgres",
						"--cdc.config.database.password=postgres",
						"--cdc.config.database.dbname=postgres",
						"--cdc.config.database.hostname=localhost",
						"--cdc.config.database.port=5432")) {
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			// Using local region here
			List<Message<?>> messages = receiveAll(outputDestination);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSize(5786);
		}
	}

	//@Test
	public void mongodb() {
		try (ConfigurableApplicationContext context = applicationBuilder
				.run("--cdc.connector=mongodb",
						"--cdc.config.tasks.max=1",
						"--cdc.config.mongodb.hosts=rs0/localhost:27017",
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
	}
}
