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

package org.springframework.cloud.fn.supplier.cdc.streaming;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.cdc.BindingNameStrategy;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 */
@Tag("integration")
public class CdcSourceBindingNameTest {

	private static final String DEBEZIUM_EXAMPLE_POSTGRES_IMAGE = "debezium/example-postgres:2.1.4.Final";

	private static final String DEBEZIUM_EXAMPLE_MYSQL_IMAGE = "debezium/example-mysql:2.1.4.Final";

	private static final Log logger = LogFactory.getLog(CdcSourceBindingNameTest.class);

	private final SpringApplicationBuilder applicationBuilder = new SpringApplicationBuilder(
			TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
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
	public void mysqlWithDefaultBindingName() {
		innerTest(new String[0], "cdcSupplier-out-0");
	}

	@Test
	public void mysqlWithCustomFunctionDefinition() {
		innerTest(new String[]{"--spring.cloud.function.definition=mySupplier"}, "mySupplier-out-0");
	}

	@Test
	public void mysqlWithOverrideBindingName() {
		innerTest(new String[]{"--cdc.overrideBindingName=customBindingName"}, "customBindingName");
	}

	private void innerTest(String[] testProperties, String expectedBindingName) {
		try (
				GenericContainer debeziumMySQL = new GenericContainer<>(DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
						.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
						.withEnv("MYSQL_USER", "mysqluser")
						.withEnv("MYSQL_PASSWORD", "mysqlpw")
						.withExposedPorts(3306)
						.withStartupTimeout(Duration.ofSeconds(120))
						.withStartupAttempts(3)) {

			debeziumMySQL.start();

			String MYSQL_MAPPED_PORT = String.valueOf(debeziumMySQL.getMappedPort(3306));

			String[] result = Arrays.copyOf(testProperties, testProperties.length + 1);
			result[result.length - 1] = "--cdc.debezium.database.port=" + MYSQL_MAPPED_PORT;

			try (ConfigurableApplicationContext context = applicationBuilder.run(result)) {

				OutputDestination outputDestination = context.getBean(OutputDestination.class);
				BindingNameStrategy bindingNameStrategy = context.getBean(BindingNameStrategy.class);
				String bindingName = bindingNameStrategy.bindingName();
				assertThat(bindingName).isEqualTo(expectedBindingName);

				// Using local region here
				List<Message<?>> messages = CdcTestUtils.receiveAll(outputDestination,
						bindingNameStrategy.bindingName());
				assertThat(messages).isNotNull();
				assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
			}
		}
	}
}
