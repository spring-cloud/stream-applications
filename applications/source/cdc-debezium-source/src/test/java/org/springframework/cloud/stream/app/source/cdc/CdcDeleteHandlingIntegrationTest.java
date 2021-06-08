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

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.testcontainers.junit.jupiter.Testcontainers;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.cloud.fn.supplier.cdc.CdcSupplierConfiguration.ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.CDC_SUPPLIER_OUT_0;
import static org.springframework.cloud.stream.app.source.cdc.CdcTestUtils.receiveAll;

/**
 * @author Christian Tzolov
 * @author David Turanski
 */

@Testcontainers
public class CdcDeleteHandlingIntegrationTest extends CdcMySqlTestSupport {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(
					TestChannelBinderConfiguration.getCompleteConfiguration(TestCdcSourceApplication.class))
			.withPropertyValues(
					"spring.cloud.function.definition=cdcSupplier",
					"cdc.name=my-sql-connector",
					"cdc.schema=false",
					"cdc.flattening.enabled=true",
					"cdc.stream.header.offset=true",
					"cdc.connector=mysql",
					"cdc.config.database.user=debezium",
					"cdc.config.database.password=dbz",
					"cdc.config.database.hostname=localhost",
					"cdc.config.database.port=" + MAPPED_PORT,
					// "cdc.config.database.server.id=85744",
					"cdc.config.database.server.name=my-app-connector",
					"cdc.config.database.history=io.debezium.relational.history.MemoryDatabaseHistory");

	@ParameterizedTest
	@ValueSource(strings = {
			"cdc.flattening.deleteHandlingMode=none,cdc.flattening.dropTombstones=true",
			"cdc.flattening.deleteHandlingMode=none,cdc.flattening.dropTombstones=false",
			"cdc.flattening.deleteHandlingMode=drop,cdc.flattening.dropTombstones=true",
			"cdc.flattening.deleteHandlingMode=drop,cdc.flattening.dropTombstones=false",
			"cdc.flattening.deleteHandlingMode=rewrite,cdc.flattening.dropTombstones=true",
			"cdc.flattening.deleteHandlingMode=rewrite,cdc.flattening.dropTombstones=false"
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

		CdcCommonProperties props = context.getBean(CdcCommonProperties.class);
		boolean isKafkaPresent = ClassUtils.isPresent(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL,
				context.getClassLoader());

		CdcCommonProperties.DeleteHandlingMode deleteHandlingMode = props.getFlattening().getDeleteHandlingMode();
		boolean isDropTombstones = props.getFlattening().isDropTombstones();

		jdbcTemplate.update(
				"insert into `customers`(`first_name`,`last_name`,`email`) VALUES('Test666', 'Test666', 'Test666@spring.org')");
		String newRecordId = jdbcTemplate.query("select * from `customers` where `first_name` = ?",
				(rs, rowNum) -> rs.getString("id"), "Test666").iterator().next();

		List<Message<?>> messages = receiveAll(outputDestination);
		assertThat(messages).hasSizeGreaterThanOrEqualTo(52);

		JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

		Message<?> received;

		if (deleteHandlingMode == CdcCommonProperties.DeleteHandlingMode.drop) {
			// Do nothing
		}
		else if (deleteHandlingMode == CdcCommonProperties.DeleteHandlingMode.none) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), CDC_SUPPLIER_OUT_0);
			assertThat(received).isNotNull();
			assertThat(received.getPayload()).isEqualTo("null".getBytes());
		}
		else if (deleteHandlingMode == CdcCommonProperties.DeleteHandlingMode.rewrite) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), CDC_SUPPLIER_OUT_0);
			assertThat(received).isNotNull();
			assertThat(toString(received.getPayload()).contains("\"__deleted\":\"true\""));
		}

		if (!isDropTombstones && isKafkaPresent) {
			received = outputDestination.receive(Duration.ofSeconds(10).toMillis(), CDC_SUPPLIER_OUT_0);
			assertThat(received).isNotNull();
			// Tombstones event should have KafkaNull payload
			assertThat(received.getPayload().getClass().getCanonicalName())
					.isEqualTo(ORG_SPRINGFRAMEWORK_KAFKA_SUPPORT_KAFKA_NULL);

			String key = new String((byte[]) received.getHeaders().get("cdc_key"));
			// Tombstones event should carry the deleted record id in the cdc_key header
			assertThat(key).isEqualTo("{\"id\":" + newRecordId + "}");
		}

		received = outputDestination.receive(Duration.ofSeconds(1).toMillis(), CDC_SUPPLIER_OUT_0);
		assertThat(received).isNull();
	};
}
