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

package org.springframework.cloud.stream.app.source.debezium.integration;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.cloud.fn.common.debezium.DebeziumProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.messaging.Message;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author David Turanski
 */
@Tag("integration")
@Testcontainers
public class DebeziumDeleteHandlingIntegrationTest {

	@Container
	static GenericContainer<?> mySqlContainer = new GenericContainer<>(DebeziumTestUtils.DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			.withExposedPorts(3306)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					TestChannelBinderConfiguration.getCompleteConfiguration(TestDebeziumSourceApplication.class))
			.withPropertyValues(
					"spring.cloud.function.definition=debeziumSupplier",

					"debezium.properties.schema=false",

					"debezium.properties.key.converter.schemas.enable=false",
					"debezium.properties.value.converter.schemas.enable=false",

					"debezium.properties.topic.prefix=my-topic",

					// enable flattering
					"debezium.properties.transforms=unwrap",
					"debezium.properties.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
					"debezium.properties.transforms.unwrap.add.fields=name,db",

					"debezium.properties.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory",
					"debezium.properties.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

					"debezium.properties.name=my-connector",
					"debezium.properties.connector.class=io.debezium.connector.mysql.MySqlConnector",
					"debezium.properties.database.user=debezium",
					"debezium.properties.database.password=dbz",
					"debezium.properties.database.hostname=localhost",
					"debezium.properties.database.port=" + mySqlContainer.getMappedPort(3306),
					"debezium.properties.database.server.id=85744",

					// JdbcTemplate configuration
					String.format("app.datasource.url=jdbc:mysql://localhost:%d/%s?enabledTLSProtocols=TLSv1.2",
							mySqlContainer.getMappedPort(3306), DebeziumTestUtils.DATABASE_NAME),
					"app.datasource.username=root",
					"app.datasource.password=debezium",
					"app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
					"app.datasource.type=com.zaxxer.hikari.HikariDataSource");

	@ParameterizedTest
	@ValueSource(strings = {
			"debezium.properties.transforms.unwrap.delete.handling.mode=none,debezium.properties.transforms.unwrap.drop.tombstones=true",
			"debezium.properties.transforms.unwrap.delete.handling.mode=none,debezium.properties.transforms.unwrap.drop.tombstones=false",
			"debezium.properties.transforms.unwrap.delete.handling.mode=drop,debezium.properties.transforms.unwrap.drop.tombstones=true",
			"debezium.properties.transforms.unwrap.delete.handling.mode=drop,debezium.properties.transforms.unwrap.drop.tombstones=false",
			"debezium.properties.transforms.unwrap.delete.handling.mode=rewrite,debezium.properties.transforms.unwrap.drop.tombstones=true",
			"debezium.properties.transforms.unwrap.delete.handling.mode=rewrite,debezium.properties.transforms.unwrap.drop.tombstones=false"
	})
	public void handleRecordDeletions(String properties) {
		contextRunner.withPropertyValues(properties.split(","))
				.run(consumer);
	}

	private String toString(Object object) {
		return new String((byte[]) object);
	}

	final ContextConsumer<? super ApplicationContext> consumer = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);
		DebeziumProperties props = context.getBean(DebeziumProperties.class);

		String deleteHandlingMode = props.getProperties().get("transforms.unwrap.delete.handling.mode");
		String isDropTombstones = props.getProperties().get("transforms.unwrap.drop.tombstones");

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();

		List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		Message<?> received;

		if (deleteHandlingMode.equals("drop")) {
			// Do nothing
		}
		else if (deleteHandlingMode.equals("none")) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), DebeziumTestUtils.BINDING_NAME);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("null".getBytes());
		}
		else if (deleteHandlingMode.equals("rewrite")) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), DebeziumTestUtils.BINDING_NAME);
			assertThat(received).isNotNull();
			assertThat(toString(received.getPayload())).contains("\"__deleted\":\"true\"");
		}

		if (!(isDropTombstones.equals("true"))) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), DebeziumTestUtils.BINDING_NAME);
			assertThat(received).isNotNull();
			// Tombstones event should have KafkaNull payload
			assertThat(received.getPayload()).isEqualTo("null".getBytes());

			Object keyRaw = received.getHeaders().get("debezium_key");
			String key = (keyRaw instanceof byte[]) ? new String((byte[]) keyRaw) : "" + keyRaw;

			// Tombstones event should carry the deleted record id in the debezium_key header
			assertThat(key).isEqualTo("{\"id\":" + newRecordId + "}");
		}

		received = outputDestination.receive(Duration.ofSeconds(1).toMillis(), DebeziumTestUtils.BINDING_NAME);
		assertThat(received).isNull();
	};
}
