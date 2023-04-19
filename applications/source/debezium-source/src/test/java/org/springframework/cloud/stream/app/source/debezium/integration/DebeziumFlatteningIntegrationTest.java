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

import net.javacrumbs.jsonunit.JsonAssert;
import net.javacrumbs.jsonunit.core.Configuration;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.cloud.fn.supplier.debezium.DebeziumProperties;
import org.springframework.cloud.fn.supplier.debezium.DebeziumReactiveConsumerConfiguration;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.messaging.Message;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
@Testcontainers
@Tag("integration")
public class DebeziumFlatteningIntegrationTest {

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
					TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.withPropertyValues(
					"spring.cloud.function.definition=debeziumSupplier",

					"cdc.debezium.schema=false",

					"cdc.debezium.key.converter.schemas.enable=false",
					"cdc.debezium.value.converter.schemas.enable=false",

					"cdc.debezium.topic.prefix=my-topic", // new

					"cdc.debezium.name=my-sql-connector",

					"cdc.debezium.schema.history.internal=io.debezium.relational.history.MemorySchemaHistory", // new
					"cdc.debezium.offset.storage=org.apache.kafka.connect.storage.MemoryOffsetBackingStore",

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
							mySqlContainer.getMappedPort(3306), DebeziumTestUtils.DATABASE_NAME),
					"app.datasource.username=root",
					"app.datasource.password=debezium",
					"app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
					"app.datasource.type=com.zaxxer.hikari.HikariDataSource");

	@Test
	public void noFlattenedResponseNoKafka() {
		contextRunner
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(noFlatteningTest);
	}

	@Test
	public void noFlattenedResponseWithKafka() {
		contextRunner.run(noFlatteningTest);
	}

	final ContextConsumer<? super ApplicationContext> noFlatteningTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		boolean isKafkaPresent = ClassUtils.isPresent(
				DebeziumReactiveConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		JsonAssert.assertJsonEquals(DebeziumTestUtils.resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()),
				Configuration.empty().whenIgnoringPaths("schemaName", "tableChanges", "source.sequence",
						"source.ts_ms", "ts_ms"));
		assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic");
		JsonAssert.assertJsonEquals("{\"databaseName\":\"inventory\"}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		JsonAssert.assertJsonEquals(
				DebeziumTestUtils.resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
				toString(messages.get(39).getPayload()),
				Configuration.empty().whenIgnoringPaths("source.sequence", "source.ts_ms"));
		assertThat(messages.get(39).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.products");
		JsonAssert.assertJsonEquals("{\"id\":106}", toString(messages.get(39).getHeaders().get("cdc_key")));

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = DebeziumTestUtils.receiveAll(outputDestination);

		assertThat(messages).hasSize(isKafkaPresent ? 4 : 3);

		JsonAssert.assertJsonEquals(
				DebeziumTestUtils.resourceToString("classpath:/json/mysql_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()), Configuration.empty().whenIgnoringPaths("source.sequence"));
		assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.customers");
		JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		JsonAssert.assertJsonEquals(
				DebeziumTestUtils.resourceToString("classpath:/json/mysql_delete_inventory_customers.json"),
				toString(messages.get(2).getPayload()), Configuration.empty().whenIgnoringPaths("source.sequence"));
		assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.customers");

		JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		if (isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(DebeziumReactiveConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_destination"))
					.isEqualTo("my-topic.inventory.customers");
			JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
					toString(messages.get(3).getHeaders().get("cdc_key")));
		}
	};

	@Test
	public void flattenedResponseNoKafka() {
		contextRunner
				.withPropertyValues("cdc.debezium.transforms=unwrap",
						"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
						"cdc.debezium.transforms.unwrap.add.fields=name,db,op",
						"cdc.debezium.transforms.unwrap.delete.handling.mode=none",
						"cdc.debezium.transforms.unwrap.drop.tombstones=false")
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(flatteningTest);
	}

	@Test
	public void flattenedResponseWithKafka() {
		contextRunner
				.withPropertyValues("cdc.debezium.transforms=unwrap",
						"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
						"cdc.debezium.transforms.unwrap.add.fields=name,db,op",
						"cdc.debezium.transforms.unwrap.delete.handling.mode=none",
						"cdc.debezium.transforms.unwrap.drop.tombstones=false")
				.run(flatteningTest);
	}

	@Test
	public void flattenedResponseWithKafkaDropTombstone() {
		contextRunner
				.withPropertyValues("cdc.debezium.transforms=unwrap",
						"cdc.debezium.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
						"cdc.debezium.transforms.unwrap.add.fields=name,db,op",
						"cdc.debezium.transforms.unwrap.delete.handling.mode=none",
						"cdc.debezium.transforms.unwrap.drop.tombstones=true")
				.run(flatteningTest);
	}

	final ContextConsumer<? super ApplicationContext> flatteningTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

		boolean isKafkaPresent = ClassUtils.isPresent(
				DebeziumReactiveConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = DebeziumTestUtils.receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		DebeziumProperties props = context.getBean(DebeziumProperties.class);

		String deleteHandlingMode = props.getDebezium().get("transforms.unwrap.delete.handling.mode");
		String isDropTombstones = props.getDebezium().get("transforms.unwrap.drop.tombstones");

		JsonAssert.assertJsonEquals(DebeziumTestUtils.resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()),
				Configuration.empty().whenIgnoringPaths("schemaName", "tableChanges", "source.sequence",
						"source.ts_ms", "ts_ms"));
		assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic");
		JsonAssert.assertJsonEquals("{\"databaseName\":\"inventory\"}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		if (isFlatteningEnabled(props)) {
			JsonAssert.assertJsonEquals(
					DebeziumTestUtils
							.resourceToString("classpath:/json/mysql_flattened_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		else {
			JsonAssert.assertJsonEquals(
					DebeziumTestUtils.resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		assertThat(messages.get(39).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.products");
		JsonAssert.assertJsonEquals("{\"id\":106}", toString(messages.get(39).getHeaders().get("cdc_key")));

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = DebeziumTestUtils.receiveAll(outputDestination);

		assertThat(messages).hasSize((isDropTombstones.equals("false") && isKafkaPresent) ? 4 : 3);

		JsonAssert.assertJsonEquals(
				DebeziumTestUtils.resourceToString("classpath:/json/mysql_flattened_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()));

		assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.customers");
		JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		if (deleteHandlingMode.equals("none")) {
			assertThat(toString(messages.get(2).getPayload())).isEqualTo("null");
			assertThat(messages.get(1).getHeaders().get("cdc_destination")).isEqualTo("my-topic.inventory.customers");
			JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
					toString(messages.get(1).getHeaders().get("cdc_key")));
		}

		if (isDropTombstones.equals("false") && isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(DebeziumReactiveConsumerConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_destination"))
					.isEqualTo("my-topic.inventory.customers");
			JsonAssert.assertJsonEquals("{\"id\":" + newRecordId + "}",
					toString(messages.get(3).getHeaders().get("cdc_key")));
		}
	};

	private static boolean isFlatteningEnabled(DebeziumProperties props) {
		String unwrapType = props.getDebezium().get("transforms.unwrap.type");
		return StringUtils.hasText(unwrapType) && unwrapType.equals("io.debezium.transforms.ExtractNewRecordState");
	}

	private String toString(Object object) {
		if (object instanceof String) {
			return (String) object;
		}
		return new String((byte[]) object);
	}

}
