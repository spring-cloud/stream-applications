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

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.fn.supplier.debezium.DebeziumConsumerConfiguration.BindingNameStrategy;
import org.springframework.cloud.fn.supplier.debezium.DebeziumTestUtils;
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
public class DebeziumSupplierAvroFormatTest {

	// E.g. docker run -it --rm --name apicurio -p 8080:8080 apicurio/apicurio-registry-mem:2.4.1.Final
	@Container
	static GenericContainer apicurio = new GenericContainer<>("apicurio/apicurio-registry-mem:2.4.1.Final")
			.withExposedPorts(8080)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	@Container
	static GenericContainer debeziumMySQL = new GenericContainer<>(DebeziumTestUtils.DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
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
							"cdc.format=AVRO",

							"cdc.debezium.key.converter=io.apicurio.registry.utils.converter.AvroConverter",
							"cdc.debezium.key.converter.apicurio.registry.auto-register=true",
							"cdc.debezium.key.converter.apicurio.registry.find-latest=true",
							"cdc.debezium.value.converter=io.apicurio.registry.utils.converter.AvroConverter",
							"cdc.debezium.value.converter.apicurio.registry.auto-register=true",
							"cdc.debezium.value.converter.apicurio.registry.find-latest=true",
							"cdc.debezium.schema.name.adjustment.mode=avro",

							"cdc.debezium.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
							"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

							"cdc.debezium.topic.prefix=my-topic",
							"cdc.debezium.name=my-connector",
							"cdc.debezium.database.server.id=85744",
							"cdc.debezium.database.server.name=my-app-connector",

							"cdc.debezium.connector.class=io.debezium.connector.mysql.MySqlConnector",
							"cdc.debezium.database.user=debezium",
							"cdc.debezium.database.password=dbz",
							"cdc.debezium.database.hostname=localhost");

	@Test
	public void mysqlWithAvroContentFormat() {

		String MYSQL_MAPPED_PORT = String.valueOf(debeziumMySQL.getMappedPort(3306));
		String APICURIO_URL = "http://localhost:" + String.valueOf(apicurio.getMappedPort(8080)) + "/apis/registry/v2";

		try (ConfigurableApplicationContext context = applicationBuilder.run(
				"--cdc.debezium.key.converter.apicurio.registry.url=" + APICURIO_URL,
				"--cdc.debezium.value.converter.apicurio.registry.url=" + APICURIO_URL,
				"--cdc.debezium.database.port=" + MYSQL_MAPPED_PORT)) {

			OutputDestination outputDestination = context.getBean(OutputDestination.class);
			BindingNameStrategy bindingNameStrategy = context.getBean(BindingNameStrategy.class);

			// Using local region here
			List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination,
					bindingNameStrategy.bindingName());

			assertThat(messages).isNotNull();
			// Message size should correspond to the number of insert statements in the sample inventor DB
			// configured by:
			// https://github.com/debezium/container-images/blob/main/examples/mysql/2.1/inventory.sql
			assertThat(messages).hasSizeGreaterThanOrEqualTo(52);
			// assertThat(messages).map(message ->
			// message.getHeaders().get("contentType")).isEqualTo("application/avro"); // TEST utils bug.
		}
	}
}
