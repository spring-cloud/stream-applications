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

package org.springframework.cloud.fn.supplier.debezium.streaming;

import java.time.Duration;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.debezium.BindingNameStrategy;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@Tag("integration")
@Testcontainers
public class DebeziumSupplierBindingNameTest {

	@Container
	static GenericContainer mySQL = new GenericContainer<>(DebeziumTestUtils.DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			.withExposedPorts(3306)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestDebeziumSupplierApplication.class))
					.web(WebApplicationType.NONE)
					.properties(
							"cdc.debezium.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"cdc.debezium.schema=false",

							"cdc.debezium.topic.prefix=my-topic",
							"cdc.debezium.name=my-connector",
							"cdc.debezium.database.server.id=85744",
							"cdc.debezium.database.server.name=my-app-connector",

							"cdc.debezium.connector.class=io.debezium.connector.mysql.MySqlConnector",
							"cdc.debezium.database.user=debezium",
							"cdc.debezium.database.password=dbz",
							"cdc.debezium.database.hostname=localhost");

	@Test
	public void defaultBindingName() {
		innerTest(new String[] { "--cdc.debezium.database.port=" + String.valueOf(mySQL.getMappedPort(3306)) },
				"debezium-out-0");
	}

	@Test
	public void customFunctionDefinition() {
		innerTest(
				new String[] { "--spring.cloud.function.definition=mySupplier",
						"--cdc.debezium.database.port=" + String.valueOf(mySQL.getMappedPort(3306)) },
				"mySupplier-out-0");
	}

	@Test
	public void overrideBindingName() {
		innerTest(
				new String[] { "--cdc.bindingName=customBindingName",
						"--cdc.debezium.database.port=" + String.valueOf(mySQL.getMappedPort(3306)) },
				"customBindingName");
	}

	private void innerTest(String[] testProperties, String expectedBindingName) {

		try (ConfigurableApplicationContext context = applicationBuilder.run(testProperties)) {

			// Test binding name
			BindingNameStrategy bindingNameStrategy = context.getBean(BindingNameStrategy.class);
			String bindingName = bindingNameStrategy.bindingName();
			assertThat(bindingName).isEqualTo(expectedBindingName);

			// Test debezium
			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination, bindingName);
			assertThat(messages).isNotNull();
			assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
		}
	}
}
