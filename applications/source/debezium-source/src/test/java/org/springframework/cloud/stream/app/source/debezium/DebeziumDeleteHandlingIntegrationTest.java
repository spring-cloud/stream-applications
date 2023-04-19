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

package org.springframework.cloud.stream.app.source.debezium;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.cloud.fn.supplier.debezium.DebeziumConsumerConfiguration;
import org.springframework.cloud.fn.supplier.debezium.DebeziumConsumerConfiguration.BindingNameStrategy;
import org.springframework.cloud.fn.supplier.debezium.DebeziumProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.messaging.Message;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author David Turanski
 */
@Testcontainers
@Tag("integration")
public class DebeziumDeleteHandlingIntegrationTest {

	static final String DATABASE_NAME = "inventory";

	@Container
	static GenericContainer mySqlContainer = new GenericContainer<>("debezium/example-mysql:2.1.4.Final")
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			.withExposedPorts(3306)
			.withStartupTimeout(Duration.ofSeconds(120))
			.withStartupAttempts(3);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.withPropertyValues(
					"spring.cloud.function.definition=debezium",

					"cdc.debezium.schema=false",

					"cdc.debezium.key.converter.schemas.enable=false",
					"cdc.debezium.value.converter.schemas.enable=false",

					"cdc.debezium.topic.prefix=my-topic", // new

					// enable flattering
					"cdc.debezium.transforms=unwrap",
					"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
					"cdc.debezium.transforms.unwrap.add.fields=name,db",

					"cdc.debezium.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory", // new
					"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

					"cdc.debezium.name=my-connector",
					"cdc.debezium.connector.class=io.debezium.connector.mysql.MySqlConnector",
					"cdc.debezium.database.user=debezium",
					"cdc.debezium.database.password=dbz",
					"cdc.debezium.database.hostname=localhost",
					"cdc.debezium.database.port=" + mySqlContainer.getMappedPort(3306),
					"cdc.debezium.database.server.id=85744",
					"cdc.debezium.database.server.name=my-app-connector",
					"cdc.debezium.database.history=io.debezium.relational.history.MemoryDatabaseHistory",

					// JdbcTemplate configuration
					String.format("app.datasource.url=jdbc:mysql://localhost:%d/%s?enabledTLSProtocols=TLSv1.2",
							mySqlContainer.getMappedPort(3306), DATABASE_NAME),
					"app.datasource.username=root",
					"app.datasource.password=debezium",
					"app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
					"app.datasource.type=com.zaxxer.hikari.HikariDataSource");

	@ParameterizedTest
	@ValueSource(strings = {
			"cdc.debezium.transforms.unwrap.delete.handling.mode=none,cdc.debezium.transforms.unwrap.drop.tombstones=true",
			"cdc.debezium.transforms.unwrap.delete.handling.mode=none,cdc.debezium.transforms.unwrap.drop.tombstones=false",
			"cdc.debezium.transforms.unwrap.delete.handling.mode=drop,cdc.debezium.transforms.unwrap.drop.tombstones=true",
			"cdc.debezium.transforms.unwrap.delete.handling.mode=drop,cdc.debezium.transforms.unwrap.drop.tombstones=false",
			"cdc.debezium.transforms.unwrap.delete.handling.mode=rewrite,cdc.debezium.transforms.unwrap.drop.tombstones=true",
			"cdc.debezium.transforms.unwrap.delete.handling.mode=rewrite,cdc.debezium.transforms.unwrap.drop.tombstones=false"
	})
	public void handleRecordDeletions(String properties) {
		contextRunner.withPropertyValues(properties.split(","))
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the
				.run(consumer);

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
		BindingNameStrategy bindingNameStrategy = context.getBean(BindingNameStrategy.class);
		boolean isKafkaPresent = ClassUtils.isPresent(
				DebeziumConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		String deleteHandlingMode = props.getDebezium().get("transforms.unwrap.delete.handling.mode");
		String isDropTombstones = props.getDebezium().get("transforms.unwrap.drop.tombstones");

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();

		List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination, bindingNameStrategy.bindingName());
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		Message<?> received;

		if (deleteHandlingMode.equals("drop")) {
			// Do nothing
		}
		else if (deleteHandlingMode.equals("none")) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(),
					bindingNameStrategy.bindingName());
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("null".getBytes());
		}
		else if (deleteHandlingMode.equals("rewrite")) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(),
					bindingNameStrategy.bindingName());
			assertThat(received).isNotNull();
			assertThat(toString(received.getPayload()).contains("\"__deleted\":\"true\""));
		}

		if (!(isDropTombstones.equals("true")) && isKafkaPresent) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(),
					bindingNameStrategy.bindingName());
			assertThat(received).isNotNull();
			// Tombstones event should have KafkaNull payload
			assertThat(received.getPayload().getClass().getCanonicalName())
					.isEqualTo(DebeziumConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL);

			Object keyRaw = received.getHeaders().get("cdc_key");
			String key = (keyRaw instanceof byte[]) ? new String((byte[]) keyRaw) : "" + keyRaw;

			// Tombstones event should carry the deleted record id in the cdc_key header
			assertThat(key).isEqualTo("{\"id\":" + newRecordId + "}");
		}

		received = outputDestination.receive(Duration.ofSeconds(1).toMillis(), bindingNameStrategy.bindingName());
		assertThat(received).isNull();
	};
}
