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

package org.springframework.cloud.fn.supplier.debezium.reactive;

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.debezium.DebeziumTestUtils;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
@Tag("integration")
public class DebeziumDatabasesIntegrationTest2 {

	private static final Log logger = LogFactory.getLog(DebeziumDatabasesIntegrationTest2.class);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestDebeziumSupplierApplication2.class))
					.web(WebApplicationType.NONE)
					.properties(
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
					"--cdc.debezium.database.port=" + mySQL.getMappedPort(3306))) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);

				List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination, "destination-out-0");

				assertThat(messages).isNotNull();
				// // Message size should correspond to the number of insert statements in:
				// // https://github.com/debezium/container-images/blob/main/examples/mysql/2.1/inventory.sql
				assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
			}
			mySQL.stop();
		}
	}

}
