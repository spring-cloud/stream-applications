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
 */
public class CdcFlatteringIntegrationTest extends CdcTestSupport {

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
	public void noFlatteredResponseNoKafka() {
		contextRunner.withPropertyValues("cdc.flattering.enabled=false")
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(noFlatteringTest);
	}

	@Test
	public void noFlatteredResponseWithKafka() {
		contextRunner.withPropertyValues("cdc.flattering.enabled=false")
				.run(noFlatteringTest);
	}

	final ContextConsumer<? super ApplicationContext> noFlatteringTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		boolean isKafkaPresent = ClassUtils.isPresent(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		assertJsonEquals(resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector");
		assertJsonEquals("{\"databaseName\":\"inventory\"}", messages.get(1).getHeaders().get("cdc_key"));

		assertJsonEquals(resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
				toString(messages.get(39).getPayload()));
		assertThat(messages.get(39).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.products");
		assertJsonEquals("{\"id\":106}", messages.get(39).getHeaders().get("cdc_key"));

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = receiveAll(outputDestination);

		assertThat(messages).hasSize(isKafkaPresent ? 4 : 3);

		assertJsonEquals(resourceToString("classpath:/json/mysql_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
		assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(1).getHeaders().get("cdc_key"));

		assertJsonEquals(resourceToString("classpath:/json/mysql_delete_inventory_customers.json"),
				toString(messages.get(2).getPayload()));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");

		assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(1).getHeaders().get("cdc_key"));

		if (isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_topic"))
					.isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(3).getHeaders().get("cdc_key"));
		}
	};

	@Test
	public void flatteredResponseNoKafka() {
		contextRunner.withPropertyValues(
				"cdc.flattering.enabled=true",
				"cdc.flattering.deleteHandlingMode=none",
				"cdc.flattering.dropTombstones=false",
				"cdc.flattering.addHeaders=op",
				"cdc.flattering.addFields=name,db")
				.withClassLoader(new FilteredClassLoader(KafkaNull.class)) // Remove Kafka from the classpath
				.run(flatteringTest);
	}

	@Test
	public void flatteredResponseWithKafka() {
		contextRunner.withPropertyValues(
				"cdc.flattering.enabled=true",
				"cdc.flattering.deleteHandlingMode=none",
				"cdc.flattering.dropTombstones=false",
				"cdc.flattering.addHeaders=op",
				"cdc.flattering.addFields=name,db")
				.run(flatteringTest);
	}

	@Test
	public void flatteredResponseWithKafkaDropTombstone() {
		contextRunner.withPropertyValues(
				"cdc.flattering.enabled=true",
				"cdc.flattering.deleteHandlingMode=none",
				"cdc.flattering.dropTombstones=true",
				"cdc.flattering.addHeaders=op",
				"cdc.flattering.addFields=name,db")
				.run(flatteringTest);
	}

	final ContextConsumer<? super ApplicationContext> flatteringTest = context -> {
		OutputDestination outputDestination = context.getBean(OutputDestination.class);
		boolean isKafkaPresent = ClassUtils.isPresent(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		List<Message<?>> messages = receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		CdcCommonProperties.Flattering flatteringProps = context.getBean(CdcCommonProperties.class).getFlattering();

		assertJsonEquals(resourceToString(
				"classpath:/json/mysql_ddl_drop_inventory_address_table.json"),
				toString(messages.get(1).getPayload()));
		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector");
		assertJsonEquals("{\"databaseName\":\"inventory\"}",
				messages.get(1).getHeaders().get("cdc_key"));

		if (flatteringProps.isEnabled()) {
			assertJsonEquals(resourceToString("classpath:/json/mysql_flattered_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		else {
			assertJsonEquals(resourceToString("classpath:/json/mysql_insert_inventory_products_106.json"),
					toString(messages.get(39).getPayload()));
		}
		assertThat(messages.get(39).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.products");
		assertJsonEquals("{\"id\":106}", messages.get(39).getHeaders().get("cdc_key"));

		if (flatteringProps.isEnabled() && flatteringProps.getAddHeaders().contains("op")) {
			assertThat(messages.get(39).getHeaders().get("__op")).isEqualTo("c");
		}

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();
		jdbcTemplate.update("UPDATE `customers` SET `last_name`='Test999' WHERE first_name = 'Test666'");
		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		messages = receiveAll(outputDestination);

		assertThat(messages).hasSize((!flatteringProps.isDropTombstones() && isKafkaPresent) ? 4 : 3);

		assertJsonEquals(resourceToString("classpath:/json/mysql_flattered_update_inventory_customers.json"),
				toString(messages.get(1).getPayload()));

		assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
		assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(1).getHeaders().get("cdc_key"));
		if (!StringUtils.isEmpty(flatteringProps.getAddHeaders()) && flatteringProps.getAddHeaders().contains("op")) {
			assertThat(messages.get(1).getHeaders().get("__op")).isEqualTo("u");
		}

		if (flatteringProps.getDeleteHandlingMode() == CdcCommonProperties.DeleteHandlingMode.none) {
			assertThat(toString(messages.get(2).getPayload())).isEqualTo("null");
			assertThat(messages.get(1).getHeaders().get("cdc_topic")).isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(1).getHeaders().get("cdc_key"));
			if (!StringUtils.isEmpty(flatteringProps.getAddHeaders())
					&& flatteringProps.getAddHeaders().contains("op")) {
				assertThat(messages.get(2).getHeaders().get("__op")).isEqualTo("d");
			}
		}

		if (!flatteringProps.isDropTombstones() && isKafkaPresent) {
			assertThat(messages.get(3).getPayload().getClass().getCanonicalName())
					.isEqualTo(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
							"Tombstones event should have KafkaNull payload");
			assertThat(messages.get(3).getHeaders().get("cdc_topic"))
					.isEqualTo("my-app-connector.inventory.customers");
			assertJsonEquals("{\"id\":" + newRecordId + "}", messages.get(3).getHeaders().get("cdc_key"));
		}
	};

	private String toString(Object object) {
		return new String((byte[]) object);
	}

}
