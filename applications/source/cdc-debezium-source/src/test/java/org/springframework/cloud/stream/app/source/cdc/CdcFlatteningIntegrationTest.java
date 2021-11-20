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

import java.util.List;

import net.javacrumbs.jsonunit.core.Configuration;
import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.runner.ContextConsumer;
import org.springframework.cloud.fn.common.cdc.CdcCommonProperties;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.kafka.support.KafkaNull;
import org.springframework.messaging.Message;
import org.springframework.test.jdbc.JdbcTestUtils;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import static net.javacrumbs.jsonunit.JsonAssert.assertJsonEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.fn.supplier.cdc.CdcSupplierConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.receiveAll;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.resourceToString;

/**
 * @author Christian Tzolov
 * @author David Turanski
 * @author Artem Bilan
 */
public class CdcFlatteningIntegrationTest extends CdcMySqlTestSupport {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.withPropertyValues(
					"spring.cloud.stream.function.definition=cdcSupplier",
					"cdc.name=my-sql-connector",
					"cdc.schema=false",
					"cdc.stream.header.offset=false",
					"cdc.connector=mysql",
					"cdc.config.database.user=debezium",
					"cdc.config.database.password=dbz",
					"cdc.config.database.hostname=localhost",
					"cdc.config.database.port=" + MAPPED_PORT,
					// "cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@Test
	public void noFlattenedResponseNoKafka() {
		contextRunner.withPropertyValues("cdc.flattening.enabled=false")
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(noFlatteningTest);
	}

	@Test
	public void noFlattenedResponseWithKafka() {
		contextRunner.withPropertyValues("cdc.flattening.enabled=false")
				.run(noFlatteningTest);
	}

	final ContextConsumer<? super ApplicationContext> noFlatteningTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		boolean isKafkaPresent = ClassUtils.isPresent(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		assertJsonEquals(resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()),
				Configuration.empty().whenIgnoringPaths("schemaName", "tableChanges", "source.sequence", "source.ts_ms"));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector");
		assertJsonEquals("{\"databaseName\":\"inventory\"}", toString(messages.get(1).getHeaders().get("cdc_key")));

		assertJsonEquals(resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
				toString(messages.get(39).getPayload()),
				Configuration.empty().whenIgnoringPaths("source.sequence", "source.ts_ms"));
		assertThat(messages.get(39).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.products");
		assertJsonEquals("{\"id\":106}", toString(messages.get(39).getHeaders().get("cdc_key")));

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = receiveAll(outputDestination);

		assertThat(messages).hasSize(isKafkaPresent ? 4 : 3);

		assertJsonEquals(resourceToString("classpath:/json/mysql_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()), Configuration.empty().whenIgnoringPaths("source.sequence"));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
		assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(1).getHeaders().get("cdc_key")));

		assertJsonEquals(resourceToString("classpath:/json/mysql_delete_inventory_customers.json"),
				toString(messages.get(2).getPayload()), Configuration.empty().whenIgnoringPaths("source.sequence"));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");

		assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(1).getHeaders().get("cdc_key")));

		if (isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_topic"))
					.isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(3).getHeaders().get("cdc_key")));
		}
	};

	@Test
	public void flattenedResponseNoKafka() {
		contextRunner.withPropertyValues(
				"cdc.flattening.enabled=true",
				"cdc.flattening.deleteHandlingMode=none",
				"cdc.flattening.dropTombstones=false",
				"cdc.flattening.addHeaders=op",
				"cdc.flattening.addFields=name,db")
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(flatteningTest);
	}

	@Test
	public void flattenedResponseWithKafka() {
		contextRunner.withPropertyValues(
				"cdc.flattening.enabled=true",
				"cdc.flattening.deleteHandlingMode=none",
				"cdc.flattening.dropTombstones=false",
				"cdc.flattening.addHeaders=op",
				"cdc.flattening.addFields=name,db")
				.run(flatteningTest);
	}

	@Test
	public void flattenedResponseWithKafkaDropTombstone() {
		contextRunner.withPropertyValues(
				"cdc.flattening.enabled=true",
				"cdc.flattening.deleteHandlingMode=none",
				"cdc.flattening.dropTombstones=true",
				"cdc.flattening.addHeaders=op",
				"cdc.flattening.addFields=name,db")
				.run(flatteningTest);
	}

	final ContextConsumer<? super ApplicationContext> flatteningTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		boolean isKafkaPresent = ClassUtils.isPresent(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		CdcCommonProperties.Flattening flatteningProps = context.getBean(CdcCommonProperties.class).getFlattening();

		assertJsonEquals(resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()),
				Configuration.empty().whenIgnoringPaths("schemaName", "tableChanges", "source.sequence", "source.ts_ms"));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector");
		assertJsonEquals("{\"databaseName\":\"inventory\"}",
				toString(messages.get(1).getHeaders().get("cdc_key")));

		if (flatteningProps.isEnabled()) {
			assertJsonEquals(resourceToString("classpath:/json/mysql_flattened_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		else {
			assertJsonEquals(resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		assertThat(messages.get(39).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.products");
		assertJsonEquals("{\"id\":106}", toString(messages.get(39).getHeaders().get("cdc_key")));

		if (flatteningProps.isEnabled() && flatteningProps.getAddHeaders().contains("op")) {
			assertThat(messages.get(39).getHeaders().get("__op")).isEqualTo("r");
		}

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = receiveAll(outputDestination);

		assertThat(messages).hasSize((!flatteningProps.isDropTombstones() && isKafkaPresent) ? 4 : 3);

		assertJsonEquals(resourceToString("classpath:/json/mysql_flattened_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()));

		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
		assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(1).getHeaders().get("cdc_key")));
		if (!StringUtils.isEmpty(flatteningProps.getAddHeaders()) && flatteningProps.getAddHeaders().contains("op")) {
			assertThat(messages.get(1).getHeaders().get("__op")).isEqualTo("u");
		}

		if (flatteningProps.getDeleteHandlingMode() == CdcCommonProperties.DeleteHandlingMode.none) {
			assertThat(toString(messages.get(2).getPayload())).isEqualTo("null");
			assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(1).getHeaders().get("cdc_key")));
			if (!StringUtils.isEmpty(flatteningProps.getAddHeaders())
					&& flatteningProps.getAddHeaders().contains("op")) {
				assertThat(messages.get(2).getHeaders().get("__op")).isEqualTo("d");
			}
		}

		if (!flatteningProps.isDropTombstones() && isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_topic"))
					.isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", toString(messages.get(3).getHeaders().get("cdc_key")));
		}
	};

	private String toString(Object object) {
		return new String((byte[]) object);
	}

}
