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

package org.springframework.cloud.fn.supplier.debezium.custom;

import java.io.File;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine.Builder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.fn.supplier.debezium.TestJdbcTemplateConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.jdbc.JdbcTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * This test illustrate how to leverage the DebeziumEngineAutoConfiguration to build a supplier function with a custom
 * change event Consumer.
 *
 * @author Christian Tzolov
 */
@Tag("integration")
@Testcontainers
public class DebeziumEngineBuilderAutoConfigurationTest {
	private static final Log logger = LogFactory.getLog(DebeziumEngineBuilderAutoConfigurationTest.class);

	private static final String DATABASE_NAME = "inventory";
	public static final String IMAGE_TAG = "2.2.0.Final";
	public static final String DEBEZIUM_EXAMPLE_MYSQL_IMAGE = "debezium/example-mysql:" + IMAGE_TAG;

	@TempDir
	static File anotherTempDir;

	@Container
	static GenericContainer<?> debeziumMySQL = new GenericContainer<>(DEBEZIUM_EXAMPLE_MYSQL_IMAGE)
			.withEnv("MYSQL_ROOT_PASSWORD", "debezium")
			.withEnv("MYSQL_USER", "mysqluser")
			.withEnv("MYSQL_PASSWORD", "mysqlpw")
			.withExposedPorts(3306);

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(DebeziumCustomConsumerApplication.class)
			.withPropertyValues(
					"spring.datasource.type=com.zaxxer.hikari.HikariDataSource",

					"debezium.properties.offset.storage=org.apache.kafka.connect.storage.FileOffsetBackingStore",
					"debezium.properties.offset.storage.file.filename=" + anotherTempDir.getAbsolutePath()
							+ "offsets.dat",
					"debezium.properties.offset.flush.interval.ms=60000",

					"debezium.properties.schema.history.internal=io.debezium.storage.file.history.FileSchemaHistory", // new
					"debezium.properties.schema.history.internal.file.filename=" + anotherTempDir.getAbsolutePath()
							+ "schemahistory.dat", // new

					"debezium.properties.topic.prefix=my-topic", // new

					"debezium.properties.name=my-sql-connector",
					"debezium.properties.connector.class=io.debezium.connector.mysql.MySqlConnector",

					"debezium.properties.database.user=debezium",
					"debezium.properties.database.password=dbz",
					"debezium.properties.database.hostname=localhost",
					"debezium.properties.database.port=" + debeziumMySQL.getMappedPort(3306),
					"debezium.properties.database.server.id=85744",
					"debezium.properties.database.server.name=my-app-connector",
					"debezium.properties.database.history=io.debezium.relational.history.MemoryDatabaseHistory",

					// JdbcTemplate configuration
					String.format("app.datasource.url=jdbc:mysql://localhost:%d/%s?enabledTLSProtocols=TLSv1.2",
							debeziumMySQL.getMappedPort(3306), DATABASE_NAME),
					"app.datasource.username=root",
					"app.datasource.password=debezium",
					"app.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
					"app.datasource.type=com.zaxxer.hikari.HikariDataSource");

	@Test
	public void consumerTest() {

		logger.info("Temp dir: " + anotherTempDir.getAbsolutePath());

		contextRunner
				.withPropertyValues(
						// Flattering:
						// https://debezium.io/documentation/reference/stable/transformations/event-flattening.html
						"debezium.properties.transforms=unwrap",
						"debezium.properties.transforms.unwrap.type=io.debezium.transforms.ExtractNewRecordState",
						"debezium.properties.transforms.unwrap.drop.tombstones=false",
						"debezium.properties.transforms.unwrap.delete.handling.mode=rewrite",
						"debezium.properties.transforms.unwrap.add.fields=name,db")
				.run(context -> {
					JdbcTemplate jdbcTemplate = context.getBean(JdbcTemplate.class);

					DebeziumCustomConsumerApplication.TestDebeziumConsumer testConsumer = context
							.getBean(DebeziumCustomConsumerApplication.TestDebeziumConsumer.class);
					jdbcTemplate.update(
							"insert into `customers`(`first_name`,`last_name`,`email`) " +
									"VALUES('Test666', 'Test666', 'Test666@spring.org')");
					JdbcTestUtils.deleteFromTableWhere(jdbcTemplate, "customers", "first_name = ?", "Test666");

					await().atMost(Duration.ofSeconds(30))
							.untilAsserted(() -> assertThat(testConsumer.recordList).hasSizeGreaterThanOrEqualTo(52));
				});
	}

	@SpringBootConfiguration
	@EnableAutoConfiguration(exclude = { DataSourceAutoConfiguration.class, MongoAutoConfiguration.class })
	@Import(TestJdbcTemplateConfiguration.class)
	public static class DebeziumCustomConsumerApplication {

		@Bean
		public EmbeddedEngineExecutorService embeddedEngine(Consumer<ChangeEvent<byte[], byte[]>> changeEventConsumer,
				Builder<ChangeEvent<byte[], byte[]>> debeziumEngineBuilder) {
			return new EmbeddedEngineExecutorService(debeziumEngineBuilder.notifying(changeEventConsumer).build());
		}

		@Bean
		public Consumer<ChangeEvent<byte[], byte[]>> customConsumer() {
			return new TestDebeziumConsumer();
		}

		/**
		 * Custom, test, change event Consumer.
		 */
		public static class TestDebeziumConsumer implements Consumer<ChangeEvent<byte[], byte[]>> {

			public Map<Object, Object> keyValue = new HashMap<>();

			public List<ChangeEvent<byte[], byte[]>> recordList = new CopyOnWriteArrayList<>();

			public TestDebeziumConsumer() {
			}

			@Override
			public void accept(ChangeEvent<byte[], byte[]> changeEvent) {
				if (changeEvent != null) { // ignore null records
					recordList.add(changeEvent);
					keyValue.put(changeEvent.key(), changeEvent.value());
					System.out.println("SIZE=" + recordList.size());
					System.out.println("[Debezium Event]: " + changeEvent.toString());
				}
			}
		}
	}

}
